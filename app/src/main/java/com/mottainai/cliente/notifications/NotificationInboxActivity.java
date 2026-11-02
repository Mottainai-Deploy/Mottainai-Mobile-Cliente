package com.mottainai.cliente.notifications;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.mottainai.cliente.MainActivity;

/** Encaminha notificações antigas que ainda apontavam para esta atividade; não apresenta uma tela própria. */
public final class NotificationInboxActivity extends Activity {
    /** Mantém os avisos já entregues acessíveis pelo catálogo após a remoção da caixa de entrada. */
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        Intent destination=new Intent(this,MainActivity.class).putExtra("screen",NotificationNavigation.OFFERS);
        destination.putExtra("notification_owner",getIntent().getStringExtra("notification_owner"));
        destination.putExtra("notification_id",getIntent().getStringExtra("notification_id"));
        startActivity(destination);finish();
    }
}