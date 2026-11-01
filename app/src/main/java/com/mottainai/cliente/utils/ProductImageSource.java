package com.mottainai.cliente.utils;

import com.mottainai.cliente.network.dto.CatalogPromotion;
import java.net.URI;

public final class ProductImageSource {
    private ProductImageSource() {}

    public static String httpsUrl(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            URI uri = new URI(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null ? uri.toASCIIString() : null;
        } catch (Exception invalid) {
            return null;
        }
    }

    public static String searchName(CatalogPromotion promotion) {
        if (promotion == null || promotion.items == null || promotion.items.size() != 1
                || promotion.items.get(0) == null) return null;
        String name = promotion.items.get(0).name;
        return name == null || name.trim().isEmpty() ? null : name.trim();
    }

    public static String forPromotion(CatalogPromotion promotion) {
        if (promotion == null) return null;
        String cover = httpsUrl(promotion.imageUrl);
        if (cover != null) return cover;
        // A multi-product promotion needs an explicit cover, not an arbitrary item's photo.
        if (promotion.items == null || promotion.items.size() != 1
                || promotion.items.get(0) == null) return null;
        return httpsUrl(promotion.items.get(0).imageUrl);
    }
}
