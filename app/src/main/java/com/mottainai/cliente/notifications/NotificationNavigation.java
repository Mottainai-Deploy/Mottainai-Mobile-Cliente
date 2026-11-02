package com.mottainai.cliente.notifications;

import android.content.Context;
import android.content.Intent;
import java.io.IOException;

/** Encaminha o toque no aviso para uma área já existente do app, sem uma tela intermediária de notificações. */
public final class NotificationNavigation {
    public static final String HOME="2005:3812",OFFERS="2005:3914",DETAIL="2005:4104";
    /** Impede instanciar o coordenador de navegação. */
    private NotificationNavigation() {}
    /** Abre o produto de um aviso individual válido; um resumo leva ao catálogo. */
    static String screen(RedisNotificationApi.Item item,int count){
        if(count!=1||item==null||!item.valid()||item.expired())return OFFERS;
        return item.promotionId!=null?DETAIL:HOME;
    }
    /** Marca apenas o aviso individual tocado, preservando a titularidade e sem bloquear a abertura da oferta. */
    public static void markOpened(Context context,Intent intent){
        String owner=intent.getStringExtra("notification_owner"),id=intent.getStringExtra("notification_id");
        intent.removeExtra("notification_id");
        if(!RedisNotificationRepository.owns(context,owner)||!RedisNotificationApi.validId(id))return;
        Context app=context.getApplicationContext();
        CustomerNotifications.IO.execute(()->{
            if(!RedisNotificationRepository.owns(app,owner))return;
            try{new RedisNotificationRepository(app).read(owner,id);}
            catch(IOException|RuntimeException ignored){/* Uma falha de leitura não impede consultar a promoção. */}
        });
    }
}