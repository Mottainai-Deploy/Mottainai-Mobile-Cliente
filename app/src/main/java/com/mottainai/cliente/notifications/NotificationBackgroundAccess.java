package com.mottainai.cliente.notifications;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/** Explica a autorização adicional do Android sem impedir os avisos enquanto o app está aberto. */
public final class NotificationBackgroundAccess {
    /** Impede instanciar o orientador de permissões. */
    private NotificationBackgroundAccess() {}
    /** Oferece acesso em segundo plano separadamente, somente após a adesão explícita aos avisos. */
    public static void offer(Activity activity) {
        if(activity.isFinishing() || activity.isDestroyed() || Build.VERSION.SDK_INT<29
            || !CustomerNotifications.enabled(activity) || ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED) return;
        String owner=RedisNotificationRepository.owner(activity); if(owner==null)return;
        android.content.SharedPreferences preferences=activity.getSharedPreferences("customer_notification_delivery",0);
        if(preferences.getBoolean("background_explained_"+owner,false))return;
        preferences.edit().putBoolean("background_explained_"+owner,true).apply();
        new AlertDialog.Builder(activity).setTitle("Avisos com o app fechado")
            .setMessage("Para encontrar ofertas próximas enquanto você se desloca com o app fechado, permita a localização o tempo todo nas permissões do Mottainai. Você pode continuar usando os avisos com o app aberto sem essa autorização.")
            .setPositiveButton("Configurar",(dialog,which)->{
                if(Build.VERSION.SDK_INT==29)ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},7403);
                else activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:"+activity.getPackageName())));
            }).setNegativeButton("Agora não",null).show();
    }
}