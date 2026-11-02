package com.mottainai.cliente.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.mottainai.cliente.network.dto.CatalogPromotion;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

/** Reúne os cenários de teste de DemoData. */
public class DemoDataTest {

    /** Verifica se a oferta usada pela assistente existe no catálogo de demonstração. */
    @Test
    public void assistantOfferIsAvailableInTheOfflineCatalog() {
        CatalogPromotion coffee = DemoData.promotion("cafe-em-po");

        assertNotNull(coffee);
        assertEquals("Padaria Sol", coffee.store.name);
        assertEquals("Café em pó 500g", coffee.items.get(0).name);
    }

    /** Verifica os dados de catálogo, pontos e histórico recente da demonstração. */
    @Test
    public void demoProvidesCatalogPointsAndRecentHistory() {
        LocalDate recentMonth = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        List<CatalogPromotion> promotions = DemoData.promotions();

        assertEquals(7, promotions.size());
        assertEquals("Nutella", DemoData.promotion("teste-foto-nutella").items.get(0).name);
        assertEquals(Integer.valueOf(420), DemoData.loyaltyAccount().pointsBalance);
        assertEquals(2, DemoData.transactions(recentMonth).size());
    }
}
