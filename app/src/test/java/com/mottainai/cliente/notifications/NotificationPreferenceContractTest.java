package com.mottainai.cliente.notifications;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.Request;
import okio.Buffer;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import static org.junit.Assert.*;

/** Confere as rotas e os corpos reais enviados à API, sem usar contas ou serviços de produção. */
public class NotificationPreferenceContractTest {
    /** Cria o contrato com um destino que nunca é acessado durante os testes. */
    private NotificationPreferenceApi api() {
        return new Retrofit.Builder().baseUrl("https://example.invalid/")
            .addConverterFactory(GsonConverterFactory.create()).build().create(NotificationPreferenceApi.class);
    }
    /** A adesão usa a identidade autenticada e não envia um customerId escolhido pelo aparelho. */
    @Test public void savesConsentAndRadiusToAuthenticatedRoute() throws Exception {
        Request request=api().save("firebase-owner",new NotificationPreferenceApi.Preference(true,10)).request();
        assertEquals("PUT",request.method());
        assertEquals("/api/v1/client/notification-preferences",request.url().encodedPath());
        assertEquals("firebase-owner",request.tag(String.class));
        Buffer buffer=new Buffer();request.body().writeTo(buffer);
        JsonObject body=new Gson().fromJson(buffer.readUtf8(),JsonObject.class);
        assertEquals(2,body.size());assertTrue(body.get("enabled").getAsBoolean());
        assertEquals(10,body.get("radiusKm").getAsInt());assertFalse(body.has("customerId"));
    }
    /** A posição conserva o horário UTC e a precisão do contrato do servidor. */
    @Test public void sendsPositionToSeparateLocationRoute() throws Exception {
        NotificationPreferenceApi.Position position=new Gson().fromJson(
            "{\"latitude\":-23.54,\"longitude\":-47.01,\"accuracyMeters\":20,\"observedAt\":\"2026-11-02T12:00:00Z\"}",
            NotificationPreferenceApi.Position.class);
        Request request=api().locate("owner",position).request();
        assertEquals("/api/v1/client/notification-preferences/location",request.url().encodedPath());
        Buffer buffer=new Buffer();request.body().writeTo(buffer);
        JsonObject body=new Gson().fromJson(buffer.readUtf8(),JsonObject.class);
        assertEquals(4,body.size());assertEquals("2026-11-02T12:00:00Z",body.get("observedAt").getAsString());
        assertEquals(20,body.get("accuracyMeters").getAsDouble(),0);
    }
    /** Consulta e remoção também preservam a conta esperada no interceptor Firebase. */
    @Test public void readsAndClearsUsingSameOwner() {
        assertEquals("GET",api().get("owner").request().method());
        Request clear=api().clear("owner").request();
        assertEquals("DELETE",clear.method());assertEquals("owner",clear.tag(String.class));
        assertEquals("/api/v1/client/notification-preferences/location",clear.url().encodedPath());
    }
    /** Impede enviar posições antigas, futuras, sem precisão válida ou fora dos limites geográficos. */
    @Test public void rejectsStaleOrInvalidLocation() {
        long now=1793620800000L;
        assertTrue(NotificationLocationPolicy.valid(-23,-47,20,now-120000,now));
        assertFalse(NotificationLocationPolicy.valid(-23,-47,20,now-120001,now));
        assertFalse(NotificationLocationPolicy.valid(-23,-47,20,now+30001,now));
        assertFalse(NotificationLocationPolicy.valid(Double.NaN,-47,20,now,now));
        assertFalse(NotificationLocationPolicy.valid(-23,181,20,now,now));
        assertFalse(NotificationLocationPolicy.valid(-23,-47,-1,now,now));
        assertFalse(NotificationLocationPolicy.valid(-23,-47,10001,now,now));
    }
    /** Interpreta o horário em segundos e rejeita metadados incompletos da publicação Redis. */
    @Test public void handlesPromotionMetadataAndSecondsTimestamp() {
        String json="{\"id\":\"customer-promotion-delivery-1\",\"title\":\"Oferta\",\"body\":\"Confira\",\"status\":\"unread\",\"priority\":3,\"createdAt\":1793620800,\"promotionId\":1,\"storeId\":2,\"productId\":3,\"expiresAt\":4102444800000}";
        RedisNotificationApi.Item item=new Gson().fromJson(json,RedisNotificationApi.Item.class);
        assertTrue(item.valid());assertEquals(1793620800000L,item.createdAtMillis());assertFalse(item.expired());
        item.expiresAt=1L;assertTrue(item.expired());assertFalse(item.valid());
        item.expiresAt=4102444800000L;item.storeId=null;assertFalse(item.valid());
    }
}