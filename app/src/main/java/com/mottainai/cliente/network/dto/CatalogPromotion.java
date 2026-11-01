package com.mottainai.cliente.network.dto;

import java.math.BigDecimal;
import java.util.List;

public class CatalogPromotion {
    public String id;
    public String name;
    public String description;
    public String imageUrl;
    public String promotionType;
    public String startsAt;
    public String endsAt;
    public CatalogStore store;
    public List<Item> items;

    public static class Item {
        public String id;
        public String productId;
        public String imageUrl;
        public String name;
        public BigDecimal originalPrice;
        public BigDecimal promotionalPrice;
        public Integer quantityAvailable;
    }
}
