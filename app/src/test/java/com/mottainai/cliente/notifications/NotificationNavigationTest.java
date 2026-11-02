package com.mottainai.cliente.notifications;

import org.junit.Test;
import static org.junit.Assert.*;

/** Confere o destino de notificações sem depender de uma caixa de avisos ou de autenticação simulada. */
public class NotificationNavigationTest {
    /** Um aviso de produto abre diretamente os detalhes da promoção publicada. */
    @Test public void individualPromotionOpensDetails(){assertEquals(NotificationNavigation.DETAIL,NotificationNavigation.screen(promotion(),1));}
    /** Um resumo com vários produtos abre o catálogo, sem escolher um produto arbitrariamente. */
    @Test public void promotionSummaryOpensCatalog(){assertEquals(NotificationNavigation.OFFERS,NotificationNavigation.screen(promotion(),3));}
    /** Um comunicado sem promoção abre o início, sem inventar um produto associado. */
    @Test public void genericNoticeOpensHome(){RedisNotificationApi.Item item=promotion();
        item.promotionId=null;item.storeId=null;item.productId=null;item.expiresAt=null;
        assertEquals(NotificationNavigation.HOME,NotificationNavigation.screen(item,1));}
    /** Um aviso vencido ou malformado não encaminha para detalhes de uma promoção. */
    @Test public void unavailablePromotionFallsBackToCatalog(){RedisNotificationApi.Item item=promotion();
        item.expiresAt=System.currentTimeMillis()-1;assertEquals(NotificationNavigation.OFFERS,NotificationNavigation.screen(item,1));
        item=promotion();item.productId=null;assertEquals(NotificationNavigation.OFFERS,NotificationNavigation.screen(item,1));
        assertEquals(NotificationNavigation.OFFERS,NotificationNavigation.screen(null,1));}
    /** Prepara um evento válido com o mesmo conjunto de metadados enviado pela API Redis. */
    private RedisNotificationApi.Item promotion(){RedisNotificationApi.Item item=new RedisNotificationApi.Item();
        item.id="customer-promotion-delivery-42";item.title="Produto em oferta";item.body="Confira na loja";
        item.status="unread";item.priority=3;item.createdAt=System.currentTimeMillis()/1000;
        item.promotionId=1;item.storeId=2;item.productId=3;item.expiresAt=System.currentTimeMillis()+3600000;return item;}
}