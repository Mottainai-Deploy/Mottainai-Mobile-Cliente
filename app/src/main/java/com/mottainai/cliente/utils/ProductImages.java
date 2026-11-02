package com.mottainai.cliente.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.mottainai.cliente.R;
import com.mottainai.cliente.models.Offer;
import com.mottainai.cliente.network.OpenFoodFactsImages;
import java.lang.ref.WeakReference;

/** Representa ProductImages, responsável por as operações auxiliares deste recurso. */
public final class ProductImages {
    private static final String IMAGE_CACHE = "product_image_urls";
    private static final long IMAGE_CACHE_TTL_MILLIS = 14L * 24 * 60 * 60 * 1000;

    /** Impede a criação de instâncias desta classe utilitária. */
    private ProductImages() {}

    /** Inicia o carregamento dos dados ou da imagem para o componente solicitado. */
    public static void load(ImageView view, Offer offer, TextView attribution) {
        Object request = new Object();
        view.setTag(R.id.product_image_request, request);
        view.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(view.getContext(), offer.getThumbnailColorRes())));
        if (attribution != null) attribution.setVisibility(View.GONE);
        String source = ProductImageSource.httpsUrl(offer.getImageUrl());
        String searchName = offer.getImageSearchName();
        if (source == null && searchName != null) source = cachedUrl(view.getContext(), searchName);
        view.setContentDescription(view.getContext().getString(source == null
                ? R.string.product_image_unavailable : R.string.product_image_description, offer.getTitle()));
        display(view, source);
        if (source != null || searchName == null) return;

        WeakReference<ImageView> imageReference = new WeakReference<>(view);
        OpenFoodFactsImages.getInstance().find(searchName, photo -> {
            ImageView target = imageReference.get();
            if (target == null || photo == null) return;
            target.post(() -> {
                ImageView current = imageReference.get();
                if (current == null || current.getTag(R.id.product_image_request) != request
                        || !current.isAttachedToWindow() || destroyed(current.getContext())) return;
                cacheUrl(current.getContext(), searchName, photo.url);
                current.setContentDescription(current.getContext().getString(
                        R.string.product_image_illustrative_description, photo.productName));
                display(current, photo.url);
            });
        });
    }

    /** Limpa a imagem e os recursos associados ao componente. */
    public static void clear(ImageView view) {
        view.setTag(R.id.product_image_request, null);
        Glide.with(view).clear(view);
    }

    /** Apresenta a imagem resolvida no componente visual. */
    private static void display(ImageView view, String source) {
        Glide.with(view).load(source).placeholder(R.drawable.ic_leaf)
                .error(R.drawable.ic_leaf).fallback(R.drawable.ic_leaf).centerInside().into(view);
    }

    /** Consulta a URL de imagem preservada no cache. */
    private static String cachedUrl(Context context, String productName) {
        String entry = context.getSharedPreferences(IMAGE_CACHE, Context.MODE_PRIVATE)
                .getString(productName, null);
        if (entry == null) return null;
        int separator = entry.indexOf('|');
        if (separator < 1) return null;
        try {
            if (Long.parseLong(entry.substring(0, separator)) < System.currentTimeMillis()) return null;
        } catch (NumberFormatException invalid) { return null; }
        return ProductImageSource.httpsUrl(entry.substring(separator + 1));
    }

    /** Preserva a URL resolvida para evitar consultas repetidas de imagem. */
    private static void cacheUrl(Context context, String productName, String url) {
        String safeUrl = ProductImageSource.httpsUrl(url);
        if (safeUrl == null) return;
        context.getSharedPreferences(IMAGE_CACHE, Context.MODE_PRIVATE).edit()
                .putString(productName, (System.currentTimeMillis() + IMAGE_CACHE_TTL_MILLIS)
                        + "|" + safeUrl).apply();
    }

    /** Verifica se o contexto visual deixou de estar disponível para receber a imagem. */
    private static boolean destroyed(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return ((Activity) context).isDestroyed();
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
        return false;
    }
}
