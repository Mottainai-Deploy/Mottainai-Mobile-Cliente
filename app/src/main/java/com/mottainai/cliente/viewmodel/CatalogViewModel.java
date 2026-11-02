package com.mottainai.cliente.viewmodel;

import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mottainai.cliente.network.dto.CatalogPage;
import com.mottainai.cliente.network.dto.CatalogPromotion;
import com.mottainai.cliente.network.dto.CatalogStore;
import com.mottainai.cliente.repository.CatalogRepository;
import com.mottainai.cliente.repository.CatalogPages;
import com.mottainai.cliente.repository.LoadState;
import com.mottainai.cliente.models.OfferRadius;
import com.mottainai.cliente.repository.ForegroundLocationRepository;
import com.mottainai.cliente.utils.GeoCoordinates;
import com.mottainai.cliente.utils.SessionManager;

import java.util.Objects;

/** Representa CatalogViewModel, responsável por o estado observado pela interface. */
public class CatalogViewModel extends AndroidViewModel {
    private final CatalogRepository repository;
    private final ForegroundLocationRepository locationRepository;
    private final boolean demoMode;
    private final MutableLiveData<LoadState<Location>> location = new MutableLiveData<>();
    private final MutableLiveData<LoadState<CatalogPage<CatalogPromotion>>> promotions = new MutableLiveData<>();
    private final MutableLiveData<LoadState<CatalogPage<CatalogStore>>> stores = new MutableLiveData<>();
    private final MutableLiveData<LoadState<CatalogPromotion>> detail = new MutableLiveData<>();
    private int promotionRequest;
    private int storeRequest;
    private CatalogPage<CatalogPromotion> promotionPages;
    private CatalogPage<CatalogStore> storePages;
    private String promotionStoreId;
    private String promotionQuery = "";
    private String storeQuery = "";
    private int promotionPage;
    private int storePage;
    private String detailId;
    private Double storeLatitude;
    private Double storeLongitude;
    private OfferRadius storeRadius = OfferRadius.DEFAULT;

    /** Inicializa CatalogViewModel com os dados e as dependências recebidos. */
    public CatalogViewModel(@NonNull Application application) {
        super(application);
        repository = new CatalogRepository(application);
        locationRepository = new ForegroundLocationRepository(application);
        demoMode = new SessionManager(application).isDemoMode();
    }

    /** Obtém a página de promoções conforme os filtros informados. */
    public LiveData<LoadState<CatalogPage<CatalogPromotion>>> promotions() { return promotions; }
    /** Obtém a página de lojas conforme os filtros informados. */
    public LiveData<LoadState<CatalogPage<CatalogStore>>> stores() { return stores; }
    /** Retorna o estado observado dos detalhes da promoção. */
    public LiveData<LoadState<CatalogPromotion>> detail() { return detail; }
    /** Retorna a posição disponível para as consultas próximas. */
    public LiveData<LoadState<Location>> location() { return location; }
    /** Indica se a obtenção da posição do aparelho está em andamento. */
    public boolean isLocating() {
        return location.getValue() != null && location.getValue().status == LoadState.Status.LOADING;
    }

    /** Solicita a posição atual do aparelho e acompanha seu resultado. */
    public void requestLocation() {
        if (demoMode) {
            loadNearbyStores(0, storeQuery);
            return;
        }
        if (isLocating()) return;
        locationRepository.request(state -> {
            location.setValue(state);
            if (state.status == LoadState.Status.LOADING) {
                storeLatitude = null;
                storeLongitude = null;
                storePages = null;
                storePage = 0;
                storeRequest++;
                stores.setValue(LoadState.loading());
            } else if (state.status == LoadState.Status.SUCCESS) {
                useStoreLocation(state.data.getLatitude(), state.data.getLongitude());
            } else {
                useStoreLocation(null, null);
            }
        });
    }

    /** Cancela a operação pendente e libera os recursos associados. */
    public void cancelLocationRequest() {
        locationRepository.cancel();
        if (isLocating()) {
            location.setValue(LoadState.error("Atualização de localização pausada. Tente novamente.", 0));
            useStoreLocation(null, null);
        }
    }
    /** Retorna o filtro de loja aplicado às promoções. */
    public String promotionStoreId() { return promotionStoreId; }
    /** Retorna o termo de busca aplicado às promoções. */
    public String promotionQuery() { return promotionQuery; }
    /** Retorna a página atual da consulta de promoções. */
    public int promotionPage() { return promotionPage; }
    /** Retorna a página atual da consulta de lojas. */
    public int storePage() { return storePage; }
    /** Solicita a próxima página disponível de promoções. */
    public int nextPromotionPage() { return promotionPages == null ? 0 : promotionPages.number + 1; }
    /** Solicita a próxima página disponível de lojas. */
    public int nextStorePage() { return storePages == null ? 0 : storePages.number + 1; }
    /** Retorna a coordenada de referência usada na consulta de lojas. */
    public Double storeLatitude() { return storeLatitude; }
    /** Retorna a coordenada de referência usada na consulta de lojas. */
    public Double storeLongitude() { return storeLongitude; }
    /** Retorna o raio aplicado à consulta de lojas. */
    public OfferRadius storeRadius() { return storeRadius; }

    /** Atualiza o raio e refaz a consulta de lojas com a preferência escolhida. */
    public void useStoreRadius(OfferRadius radius) {
        Objects.requireNonNull(radius, "radius");
        if (storeRadius == radius) return;
        storeRadius = radius;
        resetStores();
    }

    /** Atualiza a posição usada nas consultas de lojas próximas. */
    private void useStoreLocation(Double latitude, Double longitude) {
        if (latitude != null && !GeoCoordinates.areValid(latitude, longitude)) return;
        storeLatitude = latitude;
        storeLongitude = latitude == null ? null : longitude;
        resetStores();
    }

    /** Limpa a paginação e os filtros anteriores da consulta de lojas. */
    private void resetStores() {
        storePages = null;
        storePage = 0;
        storeRequest++;
        stores.setValue(null);
        loadNearbyStores(0, storeQuery);
    }

    /** Carrega as promoções com os filtros e a paginação atuais. */
    public void loadPromotions(int page, String storeId, String query) {
        String normalized = query == null ? "" : query.trim();
        boolean sameFilter = Objects.equals(storeId, promotionStoreId) && normalized.equals(promotionQuery);
        LoadState<?> current = promotions.getValue();
        if (sameFilter && page == promotionPage && current != null
                && current.status == LoadState.Status.LOADING) return;
        if (page > 0 && (!sameFilter || promotionPages == null
                || page != promotionPages.number + 1)) return;
        if (!sameFilter) promotionPages = null;
        promotionStoreId = storeId;
        promotionQuery = normalized;
        promotionPage = page;
        int request = ++promotionRequest;
        promotions.setValue(LoadState.loading(promotionPages));
        repository.promotions(page, storeId, normalized, state -> {
            if (request != promotionRequest) return;
            if (state.status == LoadState.Status.SUCCESS && state.data.number == page) {
                promotionPages = CatalogPages.merge(promotionPages, state.data, item -> item.id);
                promotions.setValue(LoadState.success(promotionPages));
            } else {
                promotions.setValue(LoadState.error(state.message == null
                        ? "Não foi possível carregar esta página." : state.message,
                        state.httpStatus, promotionPages));
            }
        });
    }

    /** Carrega as lojas ao redor da posição informada. */
    public void loadNearbyStores(int page, String query) {
        if (!demoMode && (storeLatitude == null || storeLongitude == null)) {
            storePages = null;
            storePage = 0;
            LoadState<Location> current = location.getValue();
            stores.setValue(isLocating() ? LoadState.loading()
                    : LoadState.error(current != null && current.message != null ? current.message
                    : "Busque sua localização para encontrar lojas próximas.", 0));
            return;
        }
        loadStores(page, query);
    }

    /** Carrega a página de lojas com os filtros selecionados. */
    private void loadStores(int page, String query) {
        String normalized = query == null ? "" : query.trim();
        LoadState<?> current = stores.getValue();
        if (normalized.equals(storeQuery) && page == storePage && current != null
                && current.status == LoadState.Status.LOADING) return;
        if (page > 0 && (storePages == null || page != storePages.number + 1
                || !normalized.equals(storeQuery))) return;
        if (!normalized.equals(storeQuery)) storePages = null;
        storeQuery = normalized;
        storePage = page;
        int request = ++storeRequest;
        stores.setValue(LoadState.loading(storePages));
        repository.stores(page, normalized, storeLatitude, storeLongitude,
                storeLatitude == null ? null : (double) storeRadius.kilometers(), state -> {
            if (request != storeRequest) return;
            if (state.status == LoadState.Status.SUCCESS && state.data.number == page) {
                storePages = CatalogPages.merge(storePages, state.data, item -> item.id);
                stores.setValue(LoadState.success(storePages));
            } else {
                stores.setValue(LoadState.error(state.message == null
                        ? "Não foi possível carregar esta página." : state.message,
                        state.httpStatus, storePages));
            }
        });
    }

    /** Carrega os detalhes da promoção escolhida. */
    public void loadPromotion(String id) {
        if (id.equals(detailId) && detail.getValue() != null
                && detail.getValue().status != LoadState.Status.ERROR) return;
        detailId = id;
        detail.setValue(LoadState.loading());
        repository.promotion(id, detail::setValue);
    }

    /** Libera os recursos mantidos pelo estado quando ele deixa de ser utilizado. */
    @Override protected void onCleared() {
        locationRepository.cancel();
        super.onCleared();
    }
}
