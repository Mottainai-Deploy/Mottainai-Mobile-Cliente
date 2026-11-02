package com.mottainai.cliente.models;

/** Raios aprovados pelo produto, compartilhados pelas telas relacionadas à localização. */
public enum OfferRadius {
    KM_5(5), KM_10(10);

    public static final OfferRadius DEFAULT = KM_5;
    private final int kilometers;

    /** Inicializa OfferRadius com os dados e as dependências recebidos. */
    OfferRadius(int kilometers) { this.kilometers = kilometers; }

    /** Retorna a distância em quilômetros representada pela opção. */
    public int kilometers() { return kilometers; }

    /** Localiza a opção de raio correspondente à distância permitida. */
    public static OfferRadius fromKilometers(int kilometers) {
        for (OfferRadius radius : values()) {
            if (radius.kilometers == kilometers) return radius;
        }
        throw new IllegalArgumentException("Raio não permitido: " + kilometers);
    }
}
