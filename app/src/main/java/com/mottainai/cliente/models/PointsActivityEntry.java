package com.mottainai.cliente.models;

/** Representa PointsActivityEntry, responsável por os dados apresentados pela interface. */
public class PointsActivityEntry {

    private final String label;
    private final int pointsDelta;
    private final int colorRes;

    /** Inicializa PointsActivityEntry com os dados e as dependências recebidos. */
    public PointsActivityEntry(String label, int pointsDelta, int colorRes) {
        this.label = label;
        this.pointsDelta = pointsDelta;
        this.colorRes = colorRes;
    }

    /** Retorna o texto de identificação do lançamento de pontos. */
    public String getLabel() {
        return label;
    }

    /** Retorna a variação de pontos representada pelo lançamento. */
    public int getPointsDelta() {
        return pointsDelta;
    }

    /** Retorna a cor associada à apresentação do lançamento. */
    public int getColorRes() {
        return colorRes;
    }
}
