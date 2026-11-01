package com.mottainai.cliente.repository;

import com.mottainai.cliente.R;
import com.mottainai.cliente.models.Offer;

import java.util.ArrayList;
import java.util.List;

public class MockOfferRepository {

    private static final List<Offer> OFFERS = buildOffers();

    private static List<Offer> buildOffers() {
        List<Offer> offers = new ArrayList<>();
        offers.add(new Offer("leite-integral", "Leite integral", "Mercado Verde", 0.4, 40,
                "vence hoje", "Laticínios", R.color.thumb_green, "RESGATE HOJE",
                "Leite fresco perto do vencimento, ainda ótimo para consumo. Retire hoje na loja."));
        offers.add(new Offer("cafe-em-po", "Café em pó 500g", "Padaria Sol", 0.8, 30,
                "2 dias", "Padaria", R.color.thumb_tan, "RESGATE HOJE",
                "Pacote de 500g com pequena avaria na embalagem. Sabor e qualidade intactos."));
        offers.add(new Offer("frutas-da-estacao", "Frutas da estação", "Mercado Verde", 0.4, 35,
                "vence hoje", "Hortifruti", R.color.thumb_orange, null,
                "Seleção de frutas frescas da estação, colhidas nos últimos dias."));
        offers.add(new Offer("iogurte-natural", "Iogurte natural", "Horta & Cia", 0.9, 25,
                "2 dias", "Laticínios", R.color.thumb_peach, null,
                "Iogurte natural sem açúcar, produção própria da Horta & Cia."));
        offers.add(new Offer("pao-artesanal", "Pão artesanal", "Padaria Sol", 0.8, 30,
                "amanhã", "Padaria", R.color.thumb_green, null,
                "Pães de fermentação natural assados no início do dia."));
        offers.add(new Offer("queijo-minas", "Queijo minas", "Mercado Verde", 0.4, 40,
                "3 dias", "Laticínios", R.color.thumb_brown, null,
                "Queijo minas frescal, ainda dentro do prazo de consumo ideal."));
        return offers;
    }

    public List<Offer> getAllOffers() {
        return new ArrayList<>(OFFERS);
    }

    public List<Offer> getNearbyOffers() {
        List<Offer> nearby = new ArrayList<>();
        for (Offer offer : OFFERS) {
            if (offer.getBadgeLabel() != null) {
                nearby.add(offer);
            }
        }
        return nearby;
    }

    public Offer findById(String id) {
        for (Offer offer : OFFERS) {
            if (offer.getId().equals(id)) {
                return offer;
            }
        }
        return null;
    }

    public List<Offer> filter(String query, String category, boolean withinOneKm) {
        List<Offer> result = new ArrayList<>();
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        for (Offer offer : OFFERS) {
            boolean matchesQuery = normalizedQuery.isEmpty()
                    || offer.getTitle().toLowerCase().contains(normalizedQuery)
                    || offer.getStoreName().toLowerCase().contains(normalizedQuery);
            boolean matchesCategory = category == null || category.equals(offer.getCategory());
            boolean matchesDistance = !withinOneKm || offer.getDistanceKm() <= 1.0;
            if (matchesQuery && matchesCategory && matchesDistance) {
                result.add(offer);
            }
        }
        return result;
    }
}
