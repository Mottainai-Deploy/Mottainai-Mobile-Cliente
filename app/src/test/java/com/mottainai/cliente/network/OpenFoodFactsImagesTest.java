package com.mottainai.cliente.network;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.MediaType;
import org.junit.Test;
import static org.junit.Assert.*;

public class OpenFoodFactsImagesTest {
    private static final String RESPONSE = "{\"products\":[{\"code\":\"3017620422003\","
            + "\"product_name\":\"Nutella\",\"brands\":\"Ferrero\","
            + "\"image_front_url\":\"https://images.openfoodfacts.org/product.jpg\"}]}";

    @Test public void readsPhotoAndTrustedAttribution() {
        OpenFoodFactsImages.Photo photo = OpenFoodFactsImages.parse(RESPONSE, "Nutella 400g");
        assertNotNull(photo);
        assertEquals("https://images.openfoodfacts.org/product.jpg", photo.url);
        assertEquals("https://world.openfoodfacts.org/product/3017620422003", photo.productPage);
        assertEquals("Nutella", photo.productName);
    }

    @Test public void ignoresUnrelatedProductAndUnsafeImages() {
        assertNull(OpenFoodFactsImages.parse(RESPONSE, "Leite integral"));
        assertNull(OpenFoodFactsImages.parse(RESPONSE.replace("https://", "http://"), "nutella"));
        assertNull(OpenFoodFactsImages.parse(RESPONSE.replace("images.openfoodfacts.org", "evil.example"), "nutella"));
        assertNull(OpenFoodFactsImages.parse(RESPONSE.replace("3017620422003", "../x"), "nutella"));
    }

    @Test public void toleratesMissingAndInvalidResponses() {
        for (String json : new String[]{"{}", "{\"products\":[]}", "not json", "null",
                "{\"products\":[null,{},42]}", "{\"products\":{}}"}) {
            assertNull(OpenFoodFactsImages.parse(json, "nutella"));
        }
    }

    @Test public void normalizesNamesWithoutSendingPrivateData() {
        assertEquals("cafe em po 500g", OpenFoodFactsImages.normalize(" Café em pó 500g "));
        assertEquals("", OpenFoodFactsImages.normalize(null));
    }

    @Test public void deduplicatesAndCachesWithoutAuthorization() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> query = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls.incrementAndGet();
            auth.set(chain.request().header("Authorization"));
            query.set(chain.request().url().queryParameter("search_terms"));
            assertTrue(chain.request().header("User-Agent").startsWith("MottainaiCliente/"));
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body(ResponseBody.create(MediaType.get("application/json"), RESPONSE)).build();
        }).build();
        OpenFoodFactsImages images = new OpenFoodFactsImages(client, 0);
        try {
            CountDownLatch results = new CountDownLatch(2);
            images.find("Nutella", photo -> { assertNotNull(photo); results.countDown(); });
            images.find(" NUTELLA ", photo -> { assertNotNull(photo); results.countDown(); });
            assertTrue(results.await(3, TimeUnit.SECONDS));
            AtomicReference<OpenFoodFactsImages.Photo> cached = new AtomicReference<>();
            images.find("nutella", cached::set);
            assertNotNull(cached.get());
            assertEquals(1, calls.get());
            assertNull(auth.get());
            assertEquals("nutella", query.get());
        } finally { images.close(); }
    }

    @Test public void failuresAreCachedInsteadOfRetriedInALoop() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls.incrementAndGet();
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(429).message("Limited").body(ResponseBody.create(MediaType.get("text/plain"), "")).build();
        }).build();
        OpenFoodFactsImages images = new OpenFoodFactsImages(client, 0);
        try {
            CountDownLatch result = new CountDownLatch(1);
            images.find("Nutella", photo -> { assertNull(photo); result.countDown(); });
            assertTrue(result.await(3, TimeUnit.SECONDS));
            images.find("Nutella", photo -> assertNull(photo));
            assertEquals(1, calls.get());
        } finally { images.close(); }
    }
}
