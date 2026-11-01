package com.mottainai.cliente.utils;

import com.google.gson.Gson;
import com.mottainai.cliente.network.dto.LoyaltyAccount;
import org.junit.Test;
import static org.junit.Assert.*;

public class LoyaltyPresentationTest {
    @Test public void formatsOfficialPointsIncludingZeroAndNegativeValues() {
        assertEquals("2.000 pts", LoyaltyPresentation.points(2000));
        assertEquals("0 pts", LoyaltyPresentation.points(0));
        assertEquals("-5 pts", LoyaltyPresentation.points(-5));
        assertEquals("— pts", LoyaltyPresentation.points(null));
    }

    @Test public void legacyBalanceDoesNotInventAStatus() {
        LoyaltyAccount account = new Gson().fromJson("{\"pointsBalance\":5000}", LoyaltyAccount.class);
        assertEquals("Seu status sustentável aparecerá aqui.", LoyaltyPresentation.status(account));
        assertEquals("", LoyaltyPresentation.nextStatus(account));
    }

    @Test public void showsOnlyTheStatusAndProgressProvidedByServer() {
        LoyaltyAccount account = new Gson().fromJson(
                "{\"pointsBalance\":5000,\"statusName\":\" Broto \",\"pointsToNextStatus\":20}", LoyaltyAccount.class);
        assertEquals("Broto", LoyaltyPresentation.status(account));
        assertEquals("Para o próximo nível: 20 pts", LoyaltyPresentation.nextStatus(account));
    }

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
