package com.mottainai.cliente.utils;

import com.google.gson.Gson;
import com.mottainai.cliente.network.dto.LoyaltyAccount;
import org.junit.Test;
import static org.junit.Assert.*;

/** Reúne os cenários de teste de LoyaltyPresentation. */
public class LoyaltyPresentationTest {
    /** Verifica a formatação dos pontos recebidos, incluindo zero e valores negativos. */
    @Test public void formatsOfficialPointsIncludingZeroAndNegativeValues() {
        assertEquals("2.000 pts", LoyaltyPresentation.points(2000));
        assertEquals("0 pts", LoyaltyPresentation.points(0));
        assertEquals("-5 pts", LoyaltyPresentation.points(-5));
        assertEquals("— pts", LoyaltyPresentation.points(null));
    }

    /** Verifica se um saldo legado não produz um status não informado. */
    @Test public void legacyBalanceDoesNotInventAStatus() {
        LoyaltyAccount account = new Gson().fromJson("{\"pointsBalance\":5000}", LoyaltyAccount.class);
        assertEquals("Seu status sustentável aparecerá aqui.", LoyaltyPresentation.status(account));
        assertEquals("", LoyaltyPresentation.nextStatus(account));
    }

    /** Verifica a apresentação somente do status e do progresso recebidos do serviço. */
    @Test public void showsOnlyTheStatusAndProgressProvidedByServer() {
        LoyaltyAccount account = new Gson().fromJson(
                "{\"pointsBalance\":5000,\"statusName\":\" Broto \",\"pointsToNextStatus\":20}", LoyaltyAccount.class);
        assertEquals("Broto", LoyaltyPresentation.status(account));
        assertEquals("Para o próximo nível: 20 pts", LoyaltyPresentation.nextStatus(account));
    }

    /** Verifica se um progresso ausente ou inválido não produz métricas fictícias. */
    @Test public void missingOrInvalidProgressDoesNotBecomeFakeMetrics() {
        assertEquals("Seu status sustentável aparecerá aqui.", LoyaltyPresentation.status(null));
        assertEquals("", LoyaltyPresentation.nextStatus(null));
        LoyaltyAccount account = new LoyaltyAccount();
        account.statusName = " ";
        account.pointsToNextStatus = -1;
        assertEquals("Seu status sustentável aparecerá aqui.", LoyaltyPresentation.status(account));
        assertEquals("", LoyaltyPresentation.nextStatus(account));
    }
}
