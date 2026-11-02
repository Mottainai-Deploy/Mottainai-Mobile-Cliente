package com.mottainai.cliente.utils;

/** Representa GeoCoordinates, responsável por as operações auxiliares deste recurso. */
public final class GeoCoordinates {
    /** Impede a criação de instâncias desta classe utilitária. */
    private GeoCoordinates() { }

    /** Valida os limites e a presença das coordenadas geográficas. */
    public static boolean areValid(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }
}
