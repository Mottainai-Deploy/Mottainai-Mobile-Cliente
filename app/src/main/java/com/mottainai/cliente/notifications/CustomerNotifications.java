package com.mottainai.cliente.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.mottainai.cliente.R;
import org.json.JSONArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Converte avisos reais da caixa Redis em notificações Android, com adesão e deduplicação por conta. */
public final class CustomerNotifications {
    private static final String CHANNEL = "customer_offers";
    private static final int JOB_ID = 7401;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean POLLING = new AtomicBoolean();
    private static Runnable foreground;

    /** Impede criar instâncias do coordenador compartilhado. */
    private CustomerNotifications() {}

    /** Obtém apenas preferências auxiliares, nunca tokens nem dados oficiais da caixa. */
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("customer_notification_delivery", Context.MODE_PRIVATE);
    }

    /** Lê a escolha da conta atual sem reaproveitar a preferência de outra pessoa. */
    public static boolean enabled(Context context) {
        String owner = RedisNotificationRepository.owner(context);
        return owner != null && prefs(context).getBoolean("confirmed_" + owner, false)
                && prefs(context).getBoolean("enabled_" + owner, false);
    }

    /** Exige a permissão do Android e o canal habilitado antes de emitir um aviso. */
    public static boolean permitted(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = manager == null ? null : manager.getNotificationChannel(CHANNEL);
        return (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                && NotificationManagerCompat.from(context).areNotificationsEnabled()
                && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }

    /** Registra a adesão somente quando a integração e a permissão estão disponíveis. */
    public static boolean setEnabled(Context context, boolean enabled) {
        if (RedisNotificationRepository.owner(context)==null) return false;
        setEnabled(context,enabled,ignored -> {});
        return true;
    }

    /** Confirma a alteração de adesão somente depois da gravação na API Cliente. */
    public static void setEnabled(Context context, boolean enabled, java.util.function.Consumer<Boolean> result) {
        NotificationPreferenceRepository.choose(context,enabled,result);
    }

    /** Verifica sessão, configuração e preferência antes de acessar ou exibir dados. */
    private static boolean eligible(Context context) {
        return RedisNotificationRepository.configured() && enabled(context) && permitted(context)
                && com.mottainai.cliente.utils.ClientAccessGuard.hasLocationPermission(context)
                && com.mottainai.cliente.utils.ClientAccessGuard.isLocationEnabled(context);
    }

    /** Consulta em primeiro plano a cada minuto e agenda a alternativa periódica do Android. */
    public static void start(Context context) {
        stopForeground();
        Context app = context.getApplicationContext();
        NotificationPreferenceRepository.foreground(true);
        if (!RedisNotificationRepository.configured() || !NotificationPreferenceRepository.needed(app)) { cancel(app); return; }
        JobScheduler scheduler = app.getSystemService(JobScheduler.class);
        if (scheduler != null && scheduler.getPendingJob(JOB_ID) == null) {
            JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(app, NotificationPollJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15 * 60_000L)
                    .setPersisted(true).build();
            scheduler.schedule(job);
        }
        foreground = new Runnable() {
            /** Mantém a consulta apenas enquanto o aplicativo está em primeiro plano. */
            @Override public void run() {
                if (foreground != this || !eligible(app)) return;
                IO.execute(() -> poll(app));
                MAIN.postDelayed(this, 60_000L);
            }
        };
        MAIN.post(foreground);
    }

    /** Interrompe a consulta frequente ao sair da tela; o agendamento periódico continua se autorizado. */
    public static void stopForeground() {
        NotificationPreferenceRepository.foreground(false);
        if (foreground != null) MAIN.removeCallbacks(foreground);
        foreground = null;
    }

    /** Encerra tarefas e remove avisos visíveis ao desativar ou sair da conta. */
    public static synchronized void cancel(Context context) {
        stopForeground();
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler != null) scheduler.cancel(JOB_ID);
        NotificationManagerCompat.from(context).cancelAll();
    }

    /** Busca dados reais; falhas não produzem avisos fictícios nem apagam a sessão do Cliente. */
    public static void poll(Context context) {
        if (!POLLING.compareAndSet(false, true)) return;
        try {
            if (!NotificationPreferenceRepository.needed(context)) return;
            NotificationPreferenceRepository.synchronize(context);
            if (!eligible(context)) return;
            String owner = RedisNotificationRepository.owner(context);
            RedisNotificationApi.Inbox inbox = new RedisNotificationRepository(context).load(owner);
            if (!eligible(context) || !RedisNotificationRepository.owns(context, owner)
                    || Thread.currentThread().isInterrupted()) return;
            Set<String> seen = seen(context, owner);
            List<RedisNotificationApi.Item> fresh = new ArrayList<>();
            for (RedisNotificationApi.Item item : inbox.notifications)
                if (item != null && item.valid() && !item.expired() && "unread".equals(item.status) && !seen.contains(item.id)) fresh.add(item);
            if (!fresh.isEmpty()) publish(context, owner, fresh, inbox.unreadCount, seen);
        } catch (IOException | RuntimeException ignored) {
            // A próxima consulta tenta novamente; o log não contém tokens, coordenadas ou dados pessoais.
            android.util.Log.w("CustomerNotifications", "Consulta de avisos pendente (" + ignored.getClass().getSimpleName() + ")");
        } finally { POLLING.set(false); }
    }

    /** Recupera no máximo duzentos identificadores entregues, separados por usuário e perfil. */
    private static Set<String> seen(Context context, String owner) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        try {
            JSONArray values = new JSONArray(prefs(context).getString("seen_" + owner, "[]"));
            for (int index = Math.max(0, values.length() - 200); index < values.length(); index++) result.add(values.getString(index));
        } catch (org.json.JSONException ignored) { /* Um cache inválido é descartado sem afetar o servidor. */ }
        return result;
    }

    /** Publica um único resumo por lote para evitar uma sequência de sons ao recuperar a caixa. */
    private static synchronized void publish(Context context, String owner, List<RedisNotificationApi.Item> items,
                                int unreadCount, Set<String> seen) {
        if (!eligible(context) || !RedisNotificationRepository.owns(context, owner)) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Ofertas e novidades", NotificationManager.IMPORTANCE_DEFAULT));
        RedisNotificationApi.Item first = items.get(0);
        String screen=NotificationNavigation.screen(first,items.size());
        Intent intent = new Intent(context, com.mottainai.cliente.MainActivity.class)
                .setData(Uri.parse("mottainai://notifications/" + Uri.encode(owner)))
                .putExtra("screen",screen).putExtra("notification_owner",owner);
        if (items.size()==1)intent.putExtra("notification_id",first.id);
        if (NotificationNavigation.DETAIL.equals(screen))intent.putExtra("offer_id",first.promotionId.toString());
        PendingIntent tap = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_customer_notification)
                .setColor(ContextCompat.getColor(context, R.color.design_primary))
                .setContentTitle(items.size() == 1 ? first.title : "Novas ofertas para você")
                .setContentText(items.size() == 1 ? first.body : items.size() + " ofertas disponíveis")
                .setContentIntent(tap).setAutoCancel(true).setNumber(items.size())
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        NotificationCompat.InboxStyle summary = new NotificationCompat.InboxStyle();
        for (int index = 0; index < Math.min(items.size(), 5); index++) summary.addLine(items.get(index).title);
        if (items.size() == 1) notification.setStyle(new NotificationCompat.BigTextStyle().bigText(first.body));
        else notification.setStyle(summary);
        try {
            manager.notify("customer-inbox:" + owner, 0, notification.build());
        } catch (SecurityException denied) { return; }
        for (RedisNotificationApi.Item item : items) seen.add(item.id);
        while (seen.size() > 200) seen.remove(seen.iterator().next());
        prefs(context).edit().putString("seen_" + owner, new JSONArray(new ArrayList<>(seen)).toString()).apply();
    }
}
