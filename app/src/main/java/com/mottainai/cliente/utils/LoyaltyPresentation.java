package com.mottainai.cliente.utils;

import com.mottainai.cliente.network.dto.LoyaltyAccount;

import java.util.Locale;

public final class LoyaltyPresentation {
    private LoyaltyPresentation() { }

    public static String points(Integer points) {
        return points == null ? "— pts" : String.format(Locale.forLanguageTag("pt-BR"), "%,d pts", points);
    }

    public static String status(LoyaltyAccount account) {
        return account == null || account.statusName == null || account.statusName.trim().isEmpty()
                ? "Seu status sustentável aparecerá aqui." : account.statusName.trim();
    }

    public static String nextStatus(LoyaltyAccount account) {
        return account == null || account.pointsToNextStatus == null || account.pointsToNextStatus < 0
                ? "" : "Para o próximo nível: " + points(account.pointsToNextStatus);
    }
}
