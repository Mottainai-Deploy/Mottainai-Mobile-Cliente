package com.mottainai.cliente.repository;

import com.mottainai.cliente.network.dto.CatalogPromotion;
import com.mottainai.cliente.network.dto.CatalogStore;
import com.mottainai.cliente.network.dto.CustomerProfileResponse;
import com.mottainai.cliente.network.dto.LoyaltyAccount;
import com.mottainai.cliente.network.dto.LoyaltyTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class DemoData {

    private DemoData() { }

    public static List<CatalogStore> stores() {
        List<CatalogStore> stores = new ArrayList<>();
        stores.add(store("mercado-verde", "Mercado Verde", "Avenida Paulista", "1000",
                "Bela Vista", "São Paulo", "SP", "-23.5631", "-46.6544"));
        stores.add(store("padaria-sol", "Padaria Sol", "Rua Augusta", "500",
                "Consolação", "São Paulo", "SP", "-23.5512", "-46.6538"));
        stores.add(store("horta-cia", "Horta & Cia", "Rua Oscar Freire", "1200",
                "Pinheiros", "São Paulo", "SP", "-23.5629", "-46.6790"));
        return stores;
    }

    public static List<CatalogPromotion> promotions() {
        List<CatalogStore> stores = stores();
        List<CatalogPromotion> promotions = new ArrayList<>();
        promotions.add(promotion("teste-foto-nutella", "Nutella · teste de imagem externa",
                "Oferta fictícia de demonstração. A foto ilustrativa é consultada no Open Food Facts.",
                stores.get(0), "Nutella", "25.00", "15.00"));
        promotions.add(promotion("leite-integral", "Leite integral",
                "Produto próximo do vencimento, ainda próprio para consumo.", stores.get(0),
                "Leite integral 1L", "8.99", "5.49"));
        promotions.add(promotion("cafe-em-po", "Café em pó 500g",
                "Pacote com pequena avaria na embalagem. Qualidade preservada.", stores.get(1),
                "Café em pó 500g", "24.90", "17.40"));
        promotions.add(promotion("frutas-da-estacao", "Frutas da estação",
                "Seleção de frutas frescas da estação.", stores.get(0),
                "Caixa de frutas", "18.00", "12.00"));
        promotions.add(promotion("iogurte-natural", "Iogurte natural",
                "Iogurte natural sem açúcar, de produção local.", stores.get(2),
                "Iogurte natural 500g", "12.50", "8.90"));
        promotions.add(promotion("pao-artesanal", "Pão artesanal",
                "Pão de fermentação natural assado hoje.", stores.get(1),
                "Pão artesanal", "16.00", "11.00"));
        promotions.add(promotion("queijo-minas", "Queijo minas",
                "Queijo minas frescal dentro do prazo de consumo.", stores.get(0),
                "Queijo minas 300g", "21.00", "15.00"));
        return promotions;
    }

    public static CatalogPromotion promotion(String id) {
        for (CatalogPromotion promotion : promotions()) {
            if (promotion.id.equals(id)) return promotion;
        }
        return null;
    }

    public static LoyaltyAccount loyaltyAccount() {
        LoyaltyAccount account = new LoyaltyAccount();
        account.id = "demo-account";
        account.customerId = "demo-customer";
        account.pointsBalance = 420;
        return account;
    }

    public static List<LoyaltyTransaction> transactions(LocalDate month) {
        List<LoyaltyTransaction> transactions = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int firstDay = Math.min(5, month.lengthOfMonth());
        int secondDay = Math.min(12, month.lengthOfMonth());
        if (month.getYear() == today.getYear() && month.getMonth() == today.getMonth()) {
            firstDay = Math.min(firstDay, today.getDayOfMonth());
            secondDay = Math.min(secondDay, today.getDayOfMonth());
        }
        transactions.add(transaction("demo-earned-" + month, "EARNED", 35,
                "Compra no Mercado Verde", month.withDayOfMonth(firstDay).atTime(10, 30)));
        transactions.add(transaction("demo-saved-" + month, "EARNED", 20,
                "Compra na Padaria Sol", month.withDayOfMonth(secondDay).atTime(15, 10)));
        return transactions;
    }

    public static CustomerProfileResponse customerProfile() {
        CustomerProfileResponse profile = new CustomerProfileResponse();
        profile.id = 0;
        profile.fullName = "Maria Oliveira";
        profile.email = "maria.oliveira@exemplo.com";
        profile.phone = "(11) 99999-0000";
        profile.marketingConsent = false;
        return profile;
    }

    private static CatalogStore store(String id, String name, String street, String number,
                                      String neighborhood, String city, String state,
                                      String latitude, String longitude) {
        CatalogStore store = new CatalogStore();
        store.id = id;
        store.name = name;
        store.latitude = new BigDecimal(latitude);
        store.longitude = new BigDecimal(longitude);
        store.address = new CatalogStore.Address();
        store.address.street = street;
        store.address.number = number;
        store.address.neighborhood = neighborhood;
        store.address.city = city;
        store.address.state = state;
        return store;
    }

    private static CatalogPromotion promotion(String id, String name, String description,
                                               CatalogStore store,
                                               String itemName, String originalPrice,
                                               String promotionalPrice) {
        CatalogPromotion promotion = new CatalogPromotion();
        promotion.id = id;
        promotion.name = name;
        promotion.description = description;
        promotion.promotionType = "DEMO";
        promotion.startsAt = LocalDate.now().minusDays(1).atStartOfDay().toString();
        promotion.endsAt = LocalDate.now().plusDays(3).atTime(23, 59).toString();
        promotion.store = store;
        promotion.items = new ArrayList<>();
        CatalogPromotion.Item item = new CatalogPromotion.Item();
        item.id = id + "-item";
        item.productId = id + "-product";
        item.name = itemName;
        item.originalPrice = new BigDecimal(originalPrice);
        item.promotionalPrice = new BigDecimal(promotionalPrice);
        item.quantityAvailable = 8;
        promotion.items.add(item);
        return promotion;
    }

    private static LoyaltyTransaction transaction(String id, String type, int points,
                                                   String description, LocalDateTime createdAt) {
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.id = id;
        transaction.transactionType = type;
        transaction.points = points;
        transaction.description = description;
        transaction.createdAt = createdAt.toString();
        return transaction;
    }
}
