package com.mottainai.cliente.models;

import com.mottainai.cliente.network.dto.CatalogStore;
import com.mottainai.cliente.utils.GeoCoordinates;

import java.util.StringJoiner;

/** Representa PartnerStore, responsável por os dados apresentados pela interface. */
public class PartnerStore {

    private final String id;
    private final String name;
    private final double distanceKm;
    private final String hoursLabel;
    private final boolean openNow;
    private final float pinXPercent;
    private final float pinYPercent;
    private final Double latitude;
    private final Double longitude;
    private final String addressLabel;
    private final String routeAddress;

    /** Inicializa PartnerStore com os dados e as dependências recebidos. */
    public PartnerStore(String id, String name, double distanceKm, String hoursLabel, boolean openNow,
                         float pinXPercent, float pinYPercent) {
        this.id = id;
        this.name = name;
        this.distanceKm = distanceKm;
        this.hoursLabel = hoursLabel;
        this.openNow = openNow;
        this.pinXPercent = pinXPercent;
        this.pinYPercent = pinYPercent;
        this.latitude = null;
        this.longitude = null;
        this.addressLabel = "";
        this.routeAddress = null;
    }

    /** Inicializa PartnerStore com os dados e as dependências recebidos. */
    public PartnerStore(CatalogStore store) {
        this.id = store.id;
        this.name = store.name == null ? "Loja parceira" : store.name;
        this.distanceKm = 0;
        this.hoursLabel = "";
        this.openNow = false;
        this.pinXPercent = 0;
        this.pinYPercent = 0;
        this.latitude = store.latitude == null ? null : store.latitude.doubleValue();
        this.longitude = store.longitude == null ? null : store.longitude.doubleValue();
        if (store.address == null) {
            this.addressLabel = "Endereço indisponível";
            this.routeAddress = null;
        } else {
            StringJoiner address = new StringJoiner(", ");
            for (String part : new String[]{store.address.street, store.address.number,
                    store.address.neighborhood, store.address.city, store.address.state,
                    store.address.zipCode}) {
                if (hasText(part)) address.add(part.trim());
            }
            this.addressLabel = address.length() == 0 ? "Endereço indisponível" : address.toString();
            this.routeAddress = hasText(store.address.street) && hasText(store.address.number)
                    && hasText(store.address.city) && hasText(store.address.state)
                    ? address.toString() : null;
        }
    }

    /** Retorna o identificador do registro. */
    public String getId() {
        return id;
    }

    /** Retorna o nome apresentado pelo registro. */
    public String getName() {
        return name;
    }

    /** Retorna a distância em quilômetros até a loja. */
    public double getDistanceKm() {
        return distanceKm;
    }

    /** Retorna o texto de horário de atendimento da loja. */
    public String getHoursLabel() {
        return hoursLabel;
    }

    /** Informa se a loja está marcada como aberta no momento. */
    public boolean isOpenNow() {
        return openNow;
    }

    /** Retorna a posição proporcional do marcador na apresentação local do mapa. */
    public float getPinXPercent() {
        return pinXPercent;
    }

    /** Retorna a posição proporcional do marcador na apresentação local do mapa. */
    public float getPinYPercent() {
        return pinYPercent;
    }

    /** Retorna a coordenada geográfica da loja. */
    public Double getLatitude() { return latitude; }
    /** Retorna a coordenada geográfica da loja. */
    public Double getLongitude() { return longitude; }
    /** Retorna o endereço formatado para exibição. */
    public String getAddressLabel() { return addressLabel; }
    /** Retorna o endereço usado para abrir a rota até a loja. */
    public String getRouteAddress() { return routeAddress; }

    /** Verifica se existe conteúdo textual útil no valor informado. */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
    /** Verifica se a loja possui coordenadas válidas para o mapa. */
    public boolean hasValidLocation() {
        return GeoCoordinates.areValid(latitude, longitude);
    }
}
