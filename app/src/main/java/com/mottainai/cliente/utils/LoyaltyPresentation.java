package com.mottainai.cliente.utils;

import com.mottainai.cliente.network.dto.LoyaltyAccount;

import java.util.Locale;

/** Exibe somente valores do servidor; nunca calcula nível, crédito de pontos ou métrica ambiental. */
public final class LoyaltyPresentation {
    /** Impede a criação de instâncias desta classe utilitária. */
    private LoyaltyPresentation() { }

    /** Formata o saldo de pontos para apresentação. */
    public static String points(Integer points) {
        return points == null ? "— pts" : String.format(Locale.forLanguageTag("pt-BR"), "%,d pts", points);
    }

    /** Retorna o nome do status sustentável recebido ou a indicação de status ainda indisponível. */
    public static String status(LoyaltyAccount account) {
        return account == null || account.statusName == null || account.statusName.trim().isEmpty()
                ? "Seu status sustentável aparecerá aqui." : account.statusName.trim();
    }

    /** Obtém a informação de progresso disponível para o próximo status. */
    public static String nextStatus(LoyaltyAccount account) {
        return account == null || account.pointsToNextStatus == null || account.pointsToNextStatus < 0
                ? "" : "Para o próximo nível: " + points(account.pointsToNextStatus);
    }
}
