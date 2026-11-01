package com.mottainai.cliente.models;

public enum OfferRadius {
    KM_5(5), KM_10(10);

    public static final OfferRadius DEFAULT = KM_5;
    private final int kilometers;

    OfferRadius(int kilometers) { this.kilometers = kilometers; }

    public int kilometers() { return kilometers; }

    public static OfferRadius fromKilometers(int kilometers) {
        for (OfferRadius radius : values()) {
            if (radius.kilometers == kilometers) return radius;
        }
        throw new IllegalArgumentException("Raio não permitido: " + kilometers);
    }
}
