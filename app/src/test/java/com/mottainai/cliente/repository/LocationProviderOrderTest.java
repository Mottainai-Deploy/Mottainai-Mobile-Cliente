package com.mottainai.cliente.repository;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifica a prioridade do GPS sem solicitar localização ou permissões reais no teste JVM. */
public class LocationProviderOrderTest {
    /** Usa GPS primeiro, mesmo quando a lista do sistema informa a rede antes dele. */
    @Test public void preciseLocationPrioritizesGpsOverNetwork() {
        assertEquals(Arrays.asList("gps", "network"), ForegroundLocationRepository.orderedProviders(
                Arrays.asList("network", "gps"), true));
    }

    /** Mantém a localização aproximada pela rede sem tentar GPS sem permissão precisa. */
    @Test public void approximateLocationUsesOnlyNetwork() {
        assertEquals(Collections.singletonList("network"), ForegroundLocationRepository.orderedProviders(
                Arrays.asList("gps", "network"), false));
    }

    /** Permite consultar somente o GPS quando ele é a única fonte habilitada e autorizada. */
    @Test public void preciseLocationCanUseGpsWithoutNetwork() {
        assertEquals(Collections.singletonList("gps"), ForegroundLocationRepository.orderedProviders(
                Collections.singletonList("gps"), true));
    }

    /** Mantém a rede disponível quando o aparelho não oferece um provedor GPS habilitado. */
    @Test public void networkRemainsAvailableWithoutGps() {
        assertEquals(Collections.singletonList("network"), ForegroundLocationRepository.orderedProviders(
                Collections.singletonList("network"), true));
    }

    /** Não tenta usar um GPS que a permissão aproximada não autoriza. */
    @Test public void approximatePermissionCannotUseGpsOnly() {
        assertTrue(ForegroundLocationRepository.orderedProviders(Collections.singletonList("gps"), false).isEmpty());
    }

    /** Retorna ausência de fontes utilizáveis quando o sistema não oferece localização ativa. */
    @Test public void missingOrUnusableProvidersDoNotInventLocation() {
        assertTrue(ForegroundLocationRepository.orderedProviders(null, true).isEmpty());
        assertTrue(ForegroundLocationRepository.orderedProviders(Collections.emptyList(), true).isEmpty());
        assertTrue(ForegroundLocationRepository.orderedProviders(Collections.singletonList("passive"), true).isEmpty());
    }
}
