package com.mottainai.cliente.models;

/** Representa UserProfile, responsável por os dados apresentados pela interface. */
public class UserProfile {

    private final String name;
    private final String initials;
    private final String levelLabel;
    private final String levelSubLabel;
    private final int pointsBalance;
    private final int levelProgressPercent;
    private final double kgAvoided;
    private final int itemsSavedCount;
    private final double moneySavedBrl;

    /** Inicializa UserProfile com os dados e as dependências recebidos. */
    public UserProfile(String name, String initials, String levelLabel, String levelSubLabel,
                        int pointsBalance, int levelProgressPercent, double kgAvoided,
                        int itemsSavedCount, double moneySavedBrl) {
        this.name = name;
        this.initials = initials;
        this.levelLabel = levelLabel;
        this.levelSubLabel = levelSubLabel;
        this.pointsBalance = pointsBalance;
        this.levelProgressPercent = levelProgressPercent;
        this.kgAvoided = kgAvoided;
        this.itemsSavedCount = itemsSavedCount;
        this.moneySavedBrl = moneySavedBrl;
    }

    /** Retorna o nome apresentado pelo registro. */
    public String getName() {
        return name;
    }

    /** Retorna as iniciais usadas na apresentação do perfil. */
    public String getInitials() {
        return initials;
    }

    /** Retorna o nome do nível apresentado no perfil. */
    public String getLevelLabel() {
        return levelLabel;
    }

    /** Retorna a descrição complementar do nível do cliente. */
    public String getLevelSubLabel() {
        return levelSubLabel;
    }

    /** Retorna o saldo de pontos do perfil. */
    public int getPointsBalance() {
        return pointsBalance;
    }

    /** Retorna o percentual de progresso do nível do cliente. */
    public int getLevelProgressPercent() {
        return levelProgressPercent;
    }

    /** Retorna a quantidade de desperdício evitado disponível no perfil. */
    public double getKgAvoided() {
        return kgAvoided;
    }

    /** Retorna a quantidade de itens salvos disponível no perfil. */
    public int getItemsSavedCount() {
        return itemsSavedCount;
    }

    /** Retorna a economia em reais disponível no perfil. */
    public double getMoneySavedBrl() {
        return moneySavedBrl;
    }
}
