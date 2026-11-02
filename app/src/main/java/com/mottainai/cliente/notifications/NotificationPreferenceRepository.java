package com.mottainai.cliente.notifications;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mottainai.cliente.BuildConfig;
import com.mottainai.cliente.network.ApiClient;
import com.mottainai.cliente.repository.ForegroundLocationRepository;
import com.mottainai.cliente.repository.LoadState;
import com.mottainai.cliente.utils.FirebaseSupport;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import retrofit2.Response;

/** Sincroniza a escolha por conta e a posição real sem bloquear a interface ou guardar coordenadas. */
public final class NotificationPreferenceRepository {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static volatile boolean foreground;

    /** Impede instanciar o coordenador compartilhado. */
    private NotificationPreferenceRepository() {}
    /** Abre apenas o estado auxiliar separado por conta; não armazena tokens ou coordenadas. */
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("customer_notification_delivery",0); }
    /** Usa o mesmo raio de cinco ou dez quilômetros selecionado no catálogo e no mapa. */
    static int radius(Context c) { return "10 km".equals(c.getSharedPreferences("cliente_preferences",0).getString("radius","5 km"))?10:5; }
    /** Informa se há uma confirmação ou uma preferência antiga local a sincronizar. */
    static boolean needed(Context c) {
        String owner=RedisNotificationRepository.owner(c);
        return owner!=null && (prefs(c).getBoolean("pending_"+owner,false) || prefs(c).getBoolean("enabled_"+owner,false));
    }
    /** Acompanha o ciclo de vida para respeitar a autorização de localização em segundo plano. */
    static void foreground(boolean value) { foreground=value; }
    /** Salva a intenção e confirma na interface somente após a resposta da API para a mesma conta. */
    public static void choose(Context context, boolean enabled, Consumer<Boolean> callback) {
        Context app=context.getApplicationContext(); String owner=RedisNotificationRepository.owner(app);
        if(owner==null || enabled && (!RedisNotificationRepository.configured() || !CustomerNotifications.permitted(app))) {
            callback.accept(false); return;
        }
        synchronized(NotificationPreferenceRepository.class) {
            long revision=prefs(app).getLong("revision_"+owner,0)+1;
            prefs(app).edit().putBoolean("desired_"+owner,enabled).putBoolean("pending_"+owner,true)
                .putLong("revision_"+owner,revision).putBoolean("enabled_"+owner,false).apply();
        }
        if(!enabled) CustomerNotifications.cancel(app);
        CustomerNotifications.IO.execute(() -> {
            try { synchronize(app); }
            catch(IOException | RuntimeException ignored) { /* A posição pode ser reenviada sem negar uma adesão já confirmada. */ }
            boolean result=RedisNotificationRepository.owns(app,owner)
                &&prefs(app).getBoolean("confirmed_"+owner,false)&&!prefs(app).getBoolean("pending_"+owner,true)
                &&prefs(app).getBoolean("enabled_"+owner,false)==enabled;
            MAIN.post(() -> { boolean own=RedisNotificationRepository.owns(app,owner); if(own)CustomerNotifications.start(app); callback.accept(result && own); });
        });
    }
    /** Reenvia o raio escolhido sem alterar o consentimento já confirmado. */
    public static void radiusChanged(Context c) {
        if(CustomerNotifications.enabled(c)) choose(c,true,ignored -> {});
    }
    /** Atualiza adesão e posição antes da consulta ao Redis; deve executar somente no trabalhador. */
    static void synchronize(Context c) throws IOException {
        String owner=RedisNotificationRepository.owner(c); if(owner==null || !needed(c)) return;
        FirebaseAuth auth=FirebaseSupport.authIfConfigured(c); FirebaseUser user=auth==null?null:auth.getCurrentUser();
        if(user==null) return; String uid=user.getUid();
        NotificationPreferenceApi api=ApiClient.create(c,BuildConfig.API_BASE_URL,NotificationPreferenceApi.class,false);
        long revision=prefs(c).getLong("revision_"+owner,0);
        boolean pending=prefs(c).getBoolean("pending_"+owner,false);
        boolean legacy=!prefs(c).getBoolean("confirmed_"+owner,false) && prefs(c).getBoolean("enabled_"+owner,false);
        boolean radiusChanged=prefs(c).getInt("radius_"+owner,0)!=radius(c);
        boolean desired=pending?prefs(c).getBoolean("desired_"+owner,false):prefs(c).getBoolean("enabled_"+owner,false);
        Response<NotificationPreferenceApi.Preference> response=(pending || legacy || radiusChanged)
            ?api.save(uid,new NotificationPreferenceApi.Preference(desired,radius(c))).execute():api.get(uid).execute();
        if(!RedisNotificationRepository.owns(c,owner)) return;
        NotificationPreferenceApi.Preference state=response.body();
        if(!response.isSuccessful() || state==null || state.enabled==null || state.radiusKm==null
            || state.radiusKm!=5 && state.radiusKm!=10) throw new IOException("Não foi possível salvar seus avisos.");
        synchronized(NotificationPreferenceRepository.class) {
            if(prefs(c).getLong("revision_"+owner,0)!=revision) return;
            prefs(c).edit().putBoolean("pending_"+owner,false).putBoolean("confirmed_"+owner,true)
                .putBoolean("enabled_"+owner,state.enabled).putInt("radius_"+owner,state.radiusKm).apply();
        }
        if(!state.enabled) return;
        if(!ForegroundLocationRepository.hasPermission(c) || !ForegroundLocationRepository.isEnabled(c)) {
            api.clear(uid).execute(); return;
        }
        if(!foreground && Build.VERSION.SDK_INT>=29 && ContextCompat.checkSelfPermission(c,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION)!=PackageManager.PERMISSION_GRANTED) return;
        Location location=position(c);
        if(location==null || !RedisNotificationRepository.owns(c,owner) || !CustomerNotifications.enabled(c)
            || prefs(c).getLong("revision_"+owner,0)!=revision) return;
        Response<NotificationPreferenceApi.Preference> located=api.locate(uid,new NotificationPreferenceApi.Position(location)).execute();
        if(!located.isSuccessful()) throw new IOException("Não foi possível atualizar as ofertas próximas.");
    }
    /** Obtém uma observação atual pelos provedores já usados no mapa, com limite de tempo e cancelamento. */
    private static Location position(Context c) {
        ForegroundLocationRepository repository=new ForegroundLocationRepository(c);
        CountDownLatch ready=new CountDownLatch(1); AtomicReference<Location> result=new AtomicReference<>();
        MAIN.post(() -> repository.request(state -> {
            if(state.status==LoadState.Status.LOADING) return;
            Location location=state.data;
            if(location!=null && location.hasAccuracy() && NotificationLocationPolicy.valid(location.getLatitude(),
                location.getLongitude(),location.getAccuracy(),location.getTime(),System.currentTimeMillis())) result.set(location);
            ready.countDown();
        }));
        try { ready.await(18,TimeUnit.SECONDS); }
        catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        finally { MAIN.post(repository::cancel); }
        return result.get();
    }
}