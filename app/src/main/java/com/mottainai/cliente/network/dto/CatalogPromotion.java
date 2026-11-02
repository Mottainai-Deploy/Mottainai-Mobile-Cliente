package com.mottainai.cliente.network.dto;

import java.math.BigDecimal;
import java.util.List;

/** Representa CatalogPromotion, responsável por a comunicação com os serviços externos. */
public class CatalogPromotion {
    public String id;
    public String name;
    public String description;
    /** Imagem pública opcional sugerida para o produto; APIs mais antigas podem omiti-la. */
    public String imageUrl;
    public String promotionType;
    public String startsAt;
    public String endsAt;
    public CatalogStore store;
    public List<Item> items;

    /** Representa Item, responsável por a comunicação com os serviços externos. */
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
