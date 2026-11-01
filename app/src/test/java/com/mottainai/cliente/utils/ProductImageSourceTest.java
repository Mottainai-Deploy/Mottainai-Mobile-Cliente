package com.mottainai.cliente.utils;

import com.mottainai.cliente.network.dto.CatalogPromotion;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProductImageSourceTest {
    @Test public void legacyResponseUsesPlaceholder() {
        assertNull(ProductImageSource.forPromotion(new CatalogPromotion()));
        assertNull(ProductImageSource.forPromotion(null));
    }

    @Test public void onlyHttpsWithoutCredentialsIsAccepted() {
        assertEquals("https://images.example.org/product.jpg", ProductImageSource.httpsUrl(
                " https://images.example.org/product.jpg "));
        for (String invalid : new String[]{"http://example.org/a", "file:///a", "content://a/b",
                "data:image/png;base64,AAA", "https://user:password@example.org/a", "/a.jpg",
                "https://", "https://example.org/a b"}) {
            assertNull(invalid, ProductImageSource.httpsUrl(invalid));
        }
    }

    @Test public void singleProductUsesItsOwnPhoto() {
        CatalogPromotion promotion = new CatalogPromotion();
        CatalogPromotion.Item product = new CatalogPromotion.Item();
        product.imageUrl = "https://example.org/product.jpg";
        promotion.items = Collections.singletonList(product);
        assertEquals(product.imageUrl, ProductImageSource.forPromotion(promotion));
    }

    @Test public void multipleProductsRequireExplicitCover() {
        CatalogPromotion promotion = new CatalogPromotion();
        CatalogPromotion.Item product = new CatalogPromotion.Item();
        product.imageUrl = "https://example.org/product.jpg";
        promotion.items = Arrays.asList(product, new CatalogPromotion.Item());
        assertNull(ProductImageSource.forPromotion(promotion));
        promotion.imageUrl = "https://example.org/cover.jpg";
        assertEquals(promotion.imageUrl, ProductImageSource.forPromotion(promotion));
    }

    @Test public void nameLookupUsesOnlyOneKnownProduct() {
        CatalogPromotion promotion = new CatalogPromotion();
        CatalogPromotion.Item item = new CatalogPromotion.Item();
        item.name = " Café em pó ";
        promotion.name = "Ofertas imperdíveis";
        promotion.items = Collections.singletonList(item);
        assertEquals("Café em pó", ProductImageSource.searchName(promotion));
        promotion.items = Arrays.asList(item, new CatalogPromotion.Item());
        assertNull(ProductImageSource.searchName(promotion));
        assertNull(ProductImageSource.searchName(null));
    }

    @Test public void missingItemDoesNotCrash() {
        CatalogPromotion promotion = new CatalogPromotion();
        promotion.items = Collections.singletonList(null);
        assertNull(ProductImageSource.forPromotion(promotion));
    }
}
