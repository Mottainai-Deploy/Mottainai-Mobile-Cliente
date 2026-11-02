package com.mottainai.cliente.notifications;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

/** Verifica o contrato publicado pela API Redis sem autenticação falsa ou dados de produção. */
public class RedisNotificationContractTest {
    /** Lê os nomes e tipos dos campos publicados, inclusive o horário em milissegundos. */
    @Test public void readsPublishedInboxFields() {
        RedisNotificationApi.Inbox inbox = new Gson().fromJson("{\"unreadCount\":8,\"notifications\":[{"
                + "\"id\":\"stream-123-0\",\"title\":\"Uma oferta para você\",\"body\":\"Confira as novidades\","
                + "\"priority\":3,\"status\":\"unread\",\"createdAt\":1793484000000}]}", RedisNotificationApi.Inbox.class);
        assertEquals(8, inbox.unreadCount);
        assertEquals(1, inbox.notifications.size());
        assertTrue(inbox.notifications.get(0).valid());
        assertEquals(1793484000000L, inbox.notifications.get(0).createdAt);
    }

    /** Impede que um identificador de aviso seja interpretado como caminho ou chave composta. */
    @Test public void rejectsIdentifiersWithPathsOrRedisSeparators() {
        assertTrue(RedisNotificationApi.validId("stream-123-0"));
        for (String id : new String[]{"", ".", "..", "../other", "user:7", "a/b", "a?limit=100", "a\nb"})
            assertFalse(id, RedisNotificationApi.validId(id));
        assertFalse(RedisNotificationApi.validId(null));
        assertFalse(RedisNotificationApi.validId(new String(new char[129]).replace('\0', 'a')));
    }

    /** Não apresenta mensagens com campos essenciais ausentes. */
    @Test public void missingFieldsDoNotProduceANotification() {
        assertFalse(new RedisNotificationApi.Item().valid());
        RedisNotificationApi.Item item = item(); item.title = null; assertFalse(item.valid());
        item = item(); item.body = null; assertFalse(item.valid());
        item = item(); item.status = "unknown"; assertFalse(item.valid());
    }

    /** Respeita a faixa de prioridade e rejeita um horário ausente. */
    @Test public void validatesPriorityAndTimestamp() {
        RedisNotificationApi.Item item = item();
        for (int priority = 1; priority <= 3; priority++) { item.priority = priority; assertTrue(item.valid()); }
        item.priority = 4; assertFalse(item.valid());
        item.priority = 0; assertFalse(item.valid());
        item.priority = 1; item.createdAt = 0; assertFalse(item.valid());
    }

    /** Mantém as mensagens já lidas representáveis sem convertê-las em novas notificações. */
    @Test public void readMessagesRemainValidInboxItems() {
        RedisNotificationApi.Item item = item(); item.status = "read";
        assertTrue(item.valid()); assertEquals("read", item.status);
    }

    /** Exige uma credencial curta e impede caracteres de quebra de cabeçalho. */
    @Test public void accessTokenMustBeShortLivedAndHaveSafeTransportFormat() {
        RedisNotificationApi.Access access = new RedisNotificationApi.Access();
        access.accessToken = "header.payload.signature"; access.expiresIn = 300;
        assertTrue(access.valid());
        access.expiresIn = 0; assertFalse(access.valid());
        access.expiresIn = 3601; assertFalse(access.valid());
        access.expiresIn = 300; access.accessToken = "header.payload.signature\n"; assertFalse(access.valid());
        access.accessToken = "not-a-token"; assertFalse(access.valid());
    }

    /** Prepara um registro mínimo válido utilizado pelos cenários de validação. */
    private RedisNotificationApi.Item item() {
        RedisNotificationApi.Item item = new RedisNotificationApi.Item();
        item.id = "offer-1"; item.title = "Oferta"; item.body = "Confira no catálogo";
        item.status = "unread"; item.priority = 1; item.createdAt = 1793484000000L;
        return item;
    }
}
