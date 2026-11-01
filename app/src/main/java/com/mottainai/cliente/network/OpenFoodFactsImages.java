package com.mottainai.cliente.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mottainai.cliente.utils.ProductImageSource;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class OpenFoodFactsImages {
    public static final class Photo {
        public final String url;
        public final String productPage;
        public final String productName;

        Photo(String url, String code, String name) {
            this.url = url;
            this.productPage = "https://world.openfoodfacts.org/product/" + code;
            this.productName = name;
        }
    }

    private static final class Cached {
        final Photo photo;
        final long expires;
        Cached(Photo photo) {
            this.photo = photo;
            expires = System.nanoTime() + TimeUnit.MINUTES.toNanos(photo == null ? 1 : 360);
        }
    }

    private static final class Holder {
        static final OpenFoodFactsImages INSTANCE = new OpenFoodFactsImages(
                new OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS)
                        .callTimeout(8, TimeUnit.SECONDS).retryOnConnectionFailure(false)
                        .followRedirects(false).build(), 7000);
    }

    public static OpenFoodFactsImages getInstance() { return Holder.INSTANCE; }

    private final OkHttpClient client;
    private final long intervalMillis;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Cached> cache = new LinkedHashMap<>(128, 0.75f, true);
    private final Map<String, List<Consumer<Photo>>> pending = new LinkedHashMap<>();
    private boolean running;
    private long nextRequest;

    OpenFoodFactsImages(OkHttpClient client, long intervalMillis) {
        this.client = client;
        this.intervalMillis = intervalMillis;
    }

    public void find(String productName, Consumer<Photo> callback) {
        String query = normalize(productName);
        synchronized (this) {
            Cached hit = cache.get(query);
            if (hit != null && hit.expires > System.nanoTime()) {
                callback.accept(hit.photo);
                return;
            }
            if (query.length() < 3 || query.length() > 150 ||
                    (!pending.containsKey(query) && pending.size() >= 32)) {
                callback.accept(null);
                return;
            }
            pending.computeIfAbsent(query, ignored -> new ArrayList<>()).add(callback);
            if (!running) {
                running = true;
                worker.schedule(this::fetchNext, Math.max(0, nextRequest - System.nanoTime()),
                        TimeUnit.NANOSECONDS);
            }
        }
    }

    private void fetchNext() {
        String query;
        synchronized (this) { query = pending.keySet().iterator().next(); }
        Photo photo = null;
        long pause = intervalMillis;
        // Menos de dez buscas por minuto; falhas de metadados nunca atrasam o catálogo.
        HttpUrl url = HttpUrl.get("https://world.openfoodfacts.org/cgi/search.pl").newBuilder()
                .addQueryParameter("search_terms", query).addQueryParameter("search_simple", "1")
                .addQueryParameter("action", "process").addQueryParameter("json", "1")
                .addQueryParameter("page_size", "5")
                .addQueryParameter("fields", "code,product_name,brands,quantity,image_front_url").build();
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "MottainaiCliente/1.0 (https://github.com/Mottainai-One/Mottainai-Mobile-Cliente)")
                .header("Accept", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                photo = parse(response.body().string(), query);
            } else if (response.code() == 429 || response.code() == 503) {
                pause = Math.max(60000, intervalMillis);
            }
        } catch (IOException | RuntimeException unavailable) {
            // Mantém o placeholder. Não registra buscas de produtos nem repete em loop.
        }
        List<Consumer<Photo>> callbacks;
        synchronized (this) {
            cache.put(query, new Cached(photo));
            while (cache.size() > 128) cache.remove(cache.keySet().iterator().next());
            callbacks = pending.remove(query);
            nextRequest = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(pause);
            running = !pending.isEmpty();
            if (running) worker.schedule(this::fetchNext, pause, TimeUnit.MILLISECONDS);
        }
        for (Consumer<Photo> callback : callbacks) callback.accept(photo);
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    static Photo parse(String json, String query) {
        try {
            JsonArray products = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("products");
            if (products == null) return null;
            for (JsonElement entry : products) {
                if (!entry.isJsonObject()) continue;
                JsonObject product = entry.getAsJsonObject();
                String code = text(product, "code");
                String name = text(product, "product_name");
                String image = ProductImageSource.httpsUrl(text(product, "image_front_url"));
                if (image == null || !"images.openfoodfacts.org".equalsIgnoreCase(HttpUrl.get(image).host())
                        || !code.matches("[0-9]{8,14}")) continue;
                String searchable = normalize(name + " " + text(product, "brands"));
                boolean matches = true;
                for (String word : normalize(query).split(" +")) {
                    // Ignora tamanhos de embalagem e conectivos em português; nunca supõe outro alimento.
                    if (word.length() > 2 && word.matches("[a-z]+")
                            && !word.equals("com") && !word.equals("sem")
                            && !searchable.contains(word)) matches = false;
                }
                if (matches && !name.isEmpty()) return new Photo(image, code, name);
            }
        } catch (RuntimeException invalidResponse) { return null; }
        return null;
    }

    private static String text(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    void close() { worker.shutdownNow(); }
}
