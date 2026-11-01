package com.mottainai.cliente.utils;

import com.mottainai.cliente.models.PartnerStore;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class GoogleMapsUrl {
    private GoogleMapsUrl() { }

    public static String forStore(PartnerStore store) {
        if (store == null) return null;
        String destination = store.hasValidLocation()
                ? store.getLatitude() + "," + store.getLongitude() : store.getRouteAddress();
        if (destination == null) return null;
        try {
            return "https://www.google.com/maps/search/?api=1&query="
                    + URLEncoder.encode(destination, "UTF-8");
        } catch (UnsupportedEncodingException impossible) {
            throw new AssertionError("UTF-8 is required by the Java platform", impossible);
        }
    }
}
