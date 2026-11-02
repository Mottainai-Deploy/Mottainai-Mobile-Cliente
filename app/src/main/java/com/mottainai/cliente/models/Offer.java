package com.mottainai.cliente.models;

import com.mottainai.cliente.R;
import com.mottainai.cliente.network.dto.CatalogPromotion;
import com.mottainai.cliente.utils.ProductImageSource;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Representa Offer, responsável por os dados apresentados pela interface. */
public class Offer {

    private final String id;
    private final String title;
    private final String storeName;
    private final double distanceKm;
    private final int discountPercent;
    private final String expiryLabel;
    private final String category;
    private final int thumbnailColorRes;
    private final String badgeLabel;
    private final String description;
    private final String priceLabel;
    private final String itemsLabel;
    private final boolean catalogOffer;
    private final String imageUrl;
    private final String imageSearchName;

    /** Inicializa Offer com os dados e as dependências recebidos. */
    public Offer(String id, String title, String storeName, double distanceKm, int discountPercent,
                 String expiryLabel, String category, int thumbnailColorRes, String badgeLabel,
                 String description) {
        this.id = id;
        this.title = title;
        this.storeName = storeName;
        this.distanceKm = distanceKm;
        this.discountPercent = discountPercent;
        this.expiryLabel = expiryLabel;
        this.category = category;
        this.thumbnailColorRes = thumbnailColorRes;
        this.badgeLabel = badgeLabel;
        this.description = description;
        this.priceLabel = null;
        this.itemsLabel = null;
        this.catalogOffer = false;
        this.imageUrl = null;
        this.imageSearchName = null;
    }

    /** Inicializa Offer com os dados e as dependências recebidos. */
    public Offer(CatalogPromotion promotion) {
        this.id = promotion.id;
        this.imageUrl = ProductImageSource.forPromotion(promotion);
        this.imageSearchName = ProductImageSource.searchName(promotion);
        this.title = promotion.name == null ? "Promoção" : promotion.name;
        this.storeName = promotion.store == null || promotion.store.name == null
                ? "Loja parceira" : promotion.store.name;
        this.distanceKm = 0;
        this.discountPercent = 0;
        this.expiryLabel = promotion.endsAt == null ? ""
                : "Até " + promotion.endsAt.replace('T', ' ').substring(0,
                Math.min(16, promotion.endsAt.length()));
        this.category = null;
        this.thumbnailColorRes = R.color.card_light_green;
        this.badgeLabel = null;
        this.description = promotion.description == null ? "" : promotion.description;
        this.catalogOffer = true;
        StringBuilder prices = new StringBuilder();
        if (promotion.items != null) {
            NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
            for (CatalogPromotion.Item item : promotion.items) {
                if (item == null || item.name == null) continue;
                if (prices.length() > 0) prices.append("\n");
                prices.append(item.name);
                if (item.promotionalPrice != null) {
                    prices.append(" · ");
                    if (item.originalPrice != null) {
                        prices.append(currency.format(item.originalPrice)).append(" → ");
                    }
                    prices.append(currency.format(item.promotionalPrice));
                }
            }
        }
        this.itemsLabel = prices.toString();
        BigDecimal lowest = null;
        if (promotion.items != null) {
            for (CatalogPromotion.Item item : promotion.items) {
                if (item != null && item.promotionalPrice != null &&
                        (lowest == null || item.promotionalPrice.compareTo(lowest) < 0)) {
                    lowest = item.promotionalPrice;
                }
            }
        }
        this.priceLabel = lowest == null ? null
                : "A partir de " + NumberFormat.getCurrencyInstance(
                Locale.forLanguageTag("pt-BR")).format(lowest);
    }

    /** Retorna o identificador do registro. */
    public String getId() {
        return id;
    }

    /** Retorna o título da oferta. */
    public String getTitle() {
        return title;
    }

    /** Retorna o nome da loja responsável pela oferta. */
    public String getStoreName() {
        return storeName;
    }

    /** Retorna a distância em quilômetros até a loja. */
    public double getDistanceKm() {
        return distanceKm;
    }

    /** Retorna o percentual de desconto da oferta. */
    public int getDiscountPercent() {
        return discountPercent;
    }

    /** Retorna o texto de validade apresentado na oferta. */
    public String getExpiryLabel() {
        return expiryLabel;
    }

    /** Retorna a categoria associada à oferta. */
    public String getCategory() {
        return category;
    }

    /** Retorna a cor usada na apresentação da miniatura. */
    public int getThumbnailColorRes() {
        return thumbnailColorRes;
    }

    /** Retorna o texto do selo da oferta. */
    public String getBadgeLabel() {
        return badgeLabel;
    }

    /** Retorna a descrição da oferta. */
    public String getDescription() {
        return description;
    }

    /** Retorna o preço formatado da oferta. */
    public String getPriceLabel() { return priceLabel; }
    /** Retorna a descrição dos itens incluídos na oferta. */
    public String getItemsLabel() { return itemsLabel; }
    /** Indica se a oferta foi obtida do catálogo integrado. */
    public boolean isCatalogOffer() { return catalogOffer; }
    /** Retorna a URL disponível para a imagem do produto. */
    public String getImageUrl() { return imageUrl; }
    /** Retorna o nome usado para procurar a imagem do produto. */
    public String getImageSearchName() { return imageSearchName; }
}
