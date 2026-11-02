package com.mottainai.cliente.repository;

import com.mottainai.cliente.models.PartnerStore;

import java.util.ArrayList;
import java.util.List;

/** Implementação temporária para o futuro endpoint /stores. Consulte {@link MockOfferRepository}. */
public class MockStoreRepository {

    private static final List<PartnerStore> STORES = buildStores();

    /** Prepara as lojas locais utilizadas pela apresentação de exemplo. */
    private static List<PartnerStore> buildStores() {
        List<PartnerStore> stores = new ArrayList<>();
        stores.add(new PartnerStore("mercado-verde", "Mercado Verde", 0.4, "aberto agora", true, 0.30f, 0.78f));
        stores.add(new PartnerStore("horta-e-cia", "Horta & Cia", 0.9, "fecha às 20h", true, 0.72f, 0.30f));
        stores.add(new PartnerStore("padaria-sol", "Padaria Sol", 0.8, "aberto agora", true, 0.55f, 0.55f));
        stores.add(new PartnerStore("empório-raiz", "Empório Raiz", 1.4, "fecha às 19h", true, 0.20f, 0.20f));
        return stores;
    }

    /** Retorna as lojas locais utilizadas na apresentação de proximidade. */
    public List<PartnerStore> getNearbyStores() {
        return new ArrayList<>(STORES);
    }
}
