package com.mottainai.cliente.models;

import com.mottainai.cliente.R;
import com.mottainai.cliente.network.dto.CatalogPromotion;
import com.mottainai.cliente.utils.ProductImageSource;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

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

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStoreName() {
        return storeName;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public String getExpiryLabel() {
        return expiryLabel;
    }

    public String getCategory() {
        return category;
    }

    public int getThumbnailColorRes() {
        return thumbnailColorRes;
    }

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public String getDescription() {
        return description;
    }

    public String getPriceLabel() { return priceLabel; }
    public String getItemsLabel() { return itemsLabel; }
    public boolean isCatalogOffer() { return catalogOffer; }
    public String getImageUrl() { return imageUrl; }
    public String getImageSearchName() { return imageSearchName; }
}
