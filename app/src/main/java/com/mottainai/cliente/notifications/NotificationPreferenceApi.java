package com.mottainai.cliente.notifications;

import android.location.Location;
import java.time.Instant;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Tag;

/** Contrato Firebase para salvar a adesão e a posição temporária dos avisos. */
interface NotificationPreferenceApi {
    /** Consulta somente a preferência da conta autenticada. */
    @GET("api/v1/client/notification-preferences") Call<Preference> get(@Tag String uid);
    /** Salva adesão explícita e raio, sem receber o identificador de outra pessoa. */
    @PUT("api/v1/client/notification-preferences") Call<Preference> save(@Tag String uid, @Body Preference value);
    /** Envia a observação real do provedor, sem substituir o instante pela hora do envio. */
    @PUT("api/v1/client/notification-preferences/location") Call<Preference> locate(@Tag String uid, @Body Position value);
    /** Apaga a posição do servidor quando a autorização do aparelho é revogada. */
    @DELETE("api/v1/client/notification-preferences/location") Call<Void> clear(@Tag String uid);

    /** Preferência mínima confirmada pelo servidor. */
    final class Preference {
        Boolean enabled; Integer radiusKm;
        /** Monta o corpo aceito pela API Cliente. */
        Preference(boolean enabled, int radiusKm) { this.enabled=enabled; this.radiusKm=radiusKm; }
    }
    /** Posição recente com precisão e instante UTC da observação. */
    final class Position {
        double latitude, longitude, accuracyMeters; String observedAt;
        /** Copia a posição obtida no aparelho sem inventar coordenadas ou datas. */
        Position(Location location) {
            latitude=location.getLatitude(); longitude=location.getLongitude();
            accuracyMeters=location.getAccuracy(); observedAt=Instant.ofEpochMilli(location.getTime()).toString();
        }
    }
}