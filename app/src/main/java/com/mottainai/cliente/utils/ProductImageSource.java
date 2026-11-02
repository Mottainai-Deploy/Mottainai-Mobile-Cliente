package com.mottainai.cliente.utils;

import com.mottainai.cliente.network.dto.CatalogPromotion;
import java.net.URI;

/** Valida URLs públicas de imagens e seleciona um produto não ambíguo por promoção. */
public final class ProductImageSource {
    /** Impede a criação de instâncias desta classe utilitária. */
    private ProductImageSource() {}

    /** Aceita somente uma URL HTTPS válida para a imagem do produto. */
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

    /** Prepara o nome do produto utilizado na consulta de imagem. */
    public static String searchName(CatalogPromotion promotion) {
        if (promotion == null || promotion.items == null || promotion.items.size() != 1
                || promotion.items.get(0) == null) return null;
        String name = promotion.items.get(0).name;
        return name == null || name.trim().isEmpty() ? null : name.trim();
    }

    /** Determina a fonte de imagem correspondente aos itens da promoção. */
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
