package com.mottainai.cliente.widgets;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.mottainai.cliente.BuildConfig;
import com.mottainai.cliente.models.PartnerStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Representa PartnerMapView, responsável por um componente visual do aplicativo. */
public class PartnerMapView extends FrameLayout {

    @Nullable private final MapView map;
    private final List<PartnerStore> stores = new ArrayList<>();
    private GoogleMap googleMap;
    private boolean created;
    private Consumer<PartnerStore> onStoreSelected;
    private LatLng userPosition;

    /** Inicializa PartnerMapView com os dados e as dependências recebidos. */
    public PartnerMapView(Context context) {
        this(context, null);
    }

    /** Inicializa PartnerMapView com os dados e as dependências recebidos. */
    public PartnerMapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /** Inicializa PartnerMapView com os dados e as dependências recebidos. */
    public PartnerMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (BuildConfig.MAPS_API_KEY.trim().isEmpty()) {
            map = null;
            return;
        }
        map = new MapView(context);
        addView(map, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        map.setOnTouchListener((view, event) -> {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(
                    event.getActionMasked() != android.view.MotionEvent.ACTION_UP
                            && event.getActionMasked() != android.view.MotionEvent.ACTION_CANCEL);
            return false;
        });
    }

    /** Calcula as dimensões do componente de acordo com o espaço disponível. */
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getLayoutParams() != null && getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (width > 0) heightMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /** Confere se a configuração necessária ao serviço está disponível. */
    public boolean isConfigured() {
        return map != null;
    }

    /** Inicializa o mapa e prepara os eventos e marcadores das lojas. */
    public void createMap(@Nullable Bundle state) {
        if (map == null || created) return;
        map.onCreate(state);
        created = true;
        map.getMapAsync(ready -> {
            if (!created) return;
            googleMap = ready;
            googleMap.setOnMarkerClickListener(marker -> {
                if (onStoreSelected == null || !(marker.getTag() instanceof PartnerStore)) return false;
                onStoreSelected.accept((PartnerStore) marker.getTag());
                return true;
            });
            renderStores();
        });
    }

    /** Substitui as lojas apresentadas pelo componente. */
    public void setStores(List<PartnerStore> newStores) {
        stores.clear();
        stores.addAll(newStores);
        renderStores();
    }

    /** Registra a ação executada ao selecionar uma loja no mapa. */
    public void setOnStoreSelected(Consumer<PartnerStore> listener) {
        onStoreSelected = listener;
    }

    /** Atualiza a posição atual do usuário exibida no mapa. */
    public void setUserPosition(Double latitude, Double longitude) {
        userPosition = latitude == null || longitude == null ? null : new LatLng(latitude, longitude);
        renderStores();
    }

    /** Atualiza os marcadores das lojas e a área apresentada pelo mapa. */
    private void renderStores() {
        if (googleMap == null) return;
        googleMap.clear();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        List<LatLng> locations = new ArrayList<>();
        if (userPosition != null) {
            googleMap.addMarker(new MarkerOptions().position(userPosition).title("Sua localização")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            bounds.include(userPosition);
            locations.add(userPosition);
        }
        for (PartnerStore store : stores) {
            if (!store.hasValidLocation()) continue;
            LatLng position = new LatLng(store.getLatitude(), store.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions().position(position).title(store.getName())
                    .snippet(store.getAddressLabel()));
            if (marker != null) marker.setTag(store);
            bounds.include(position);
            locations.add(position);
        }
        if (locations.size() == 1) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locations.get(0), 15f));
        } else if (locations.size() > 1) {
            LatLngBounds visibleBounds = bounds.build();
            if (visibleBounds.northeast.latitude - visibleBounds.southwest.latitude < 0.005
                    && visibleBounds.northeast.longitude - visibleBounds.southwest.longitude < 0.005) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locations.get(0), 14f));
            } else {
                map.post(() -> {
                    if (created && googleMap != null && map.getWidth() > 0 && map.getHeight() > 0) {
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(visibleBounds, 48));
                    }
                });
            }
        }
    }

    /** Inicia o ciclo de exibição do mapa. */
    public void startMap() { if (map != null && created) map.onStart(); }
    /** Retoma a atualização visual do mapa. */
    public void resumeMap() { if (map != null && created) map.onResume(); }
    /** Pausa a atualização visual do mapa. */
    public void pauseMap() { if (map != null && created) map.onPause(); }
    /** Interrompe a exibição ativa do mapa. */
    public void stopMap() { if (map != null && created) map.onStop(); }
    /** Libera os recursos mantidos pelo mapa. */
    public void releaseMap() {
        onStoreSelected = null;
        if (map != null && created) {
            map.onDestroy();
            created = false;
            googleMap = null;
        }
    }
    /** Preserva o estado necessário para restaurar o mapa. */
    public void saveMapState(Bundle state) { if (map != null && created) map.onSaveInstanceState(state); }
    /** Notifica o mapa para reduzir o uso de memória. */
    public void mapLowMemory() { if (map != null && created) map.onLowMemory(); }
}
