package com.mottainai.cliente.repository;

import android.content.Context;
import android.location.Location;

import com.mottainai.cliente.BuildConfig;
import com.mottainai.cliente.network.ApiClient;
import com.mottainai.cliente.network.CustomerCatalogService;
import com.mottainai.cliente.network.dto.CatalogPage;
import com.mottainai.cliente.network.dto.CatalogPromotion;
import com.mottainai.cliente.network.dto.CatalogStore;
import com.mottainai.cliente.utils.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Representa CatalogRepository, responsável por o acesso e o tratamento dos dados do aplicativo. */
public class CatalogRepository {
    /** Define o contrato de Result, responsável por o acesso e o tratamento dos dados do aplicativo. */
    public interface Result<T> {
        /** Recebe os dados resolvidos pela operação e encaminha seu processamento. */
        void accept(LoadState<T> state);
    }

    private final CustomerCatalogService service;
    private final SessionManager session;

    /** Inicializa CatalogRepository com os dados e as dependências recebidos. */
    public CatalogRepository(Context context) {
        session = new SessionManager(context);
        service = BuildConfig.API_BASE_URL.trim().isEmpty() ? null
                : ApiClient.create(context.getApplicationContext(), BuildConfig.API_BASE_URL,
                CustomerCatalogService.class);
    }

    /** Obtém a página de lojas conforme os filtros informados. */
    public void stores(int page, String query, Double latitude, Double longitude, Double radiusKm,
                         Result<CatalogPage<CatalogStore>> result) {
        if (session.isDemoMode()) {
            String normalized = normalize(query);
            List<CatalogStore> stores = new ArrayList<>();
            for (CatalogStore store : DemoData.stores()) {
                if (latitude != null && longitude != null && radiusKm != null) {
                    if (store.latitude == null || store.longitude == null) continue;
                    float[] distance = new float[1];
                    Location.distanceBetween(latitude, longitude, store.latitude.doubleValue(),
                            store.longitude.doubleValue(), distance);
                    if (distance[0] > radiusKm * 1000) continue;
                }
                String address = store.address == null ? "" : store.address.street + " " + store.address.city;
                if (normalized.isEmpty() || (store.name + " " + address)
                        .toLowerCase(Locale.ROOT).contains(normalized)) stores.add(store);
            }
            result.accept(LoadState.success(page(stores, page, 20)));
            return;
        }
        if (unavailable(result)) return;
        enqueue(service.stores(page, 20, blankToNull(query), latitude, longitude, radiusKm), result);
    }

    /** Obtém os dados da loja identificada na consulta. */
    public void store(String id, Result<CatalogStore> result) {
        if (session.isDemoMode()) {
            for (CatalogStore store : DemoData.stores()) {
                if (store.id.equals(id)) {
                    result.accept(LoadState.success(store));
                    return;
                }
            }
            result.accept(LoadState.error("Conteúdo indisponível.", 404));
            return;
        }
        if (unavailable(result)) return;
        enqueue(service.store(id), result);
    }

    /** Obtém a página de promoções conforme os filtros informados. */
    public void promotions(int page, String storeId, String query,
                           Result<CatalogPage<CatalogPromotion>> result) {
        if (session.isDemoMode()) {
            String normalized = normalize(query);
            List<CatalogPromotion> promotions = new ArrayList<>();
            for (CatalogPromotion promotion : DemoData.promotions()) {
                boolean matchesStore = storeId == null || storeId.equals(promotion.store.id);
                String searchable = promotion.name + " " + promotion.description + " "
                        + promotion.store.name + " " + promotion.items.get(0).name;
                boolean matchesQuery = normalized.isEmpty()
                        || searchable.toLowerCase(Locale.ROOT).contains(normalized);
                if (matchesStore && matchesQuery) promotions.add(promotion);
            }
            result.accept(LoadState.success(page(promotions, page, 20)));
            return;
        }
        if (unavailable(result)) return;
        enqueue(service.promotions(page, 20, blankToNull(storeId), blankToNull(query)), result);
    }

    /** Obtém os dados da promoção identificada na consulta. */
    public void promotion(String id, Result<CatalogPromotion> result) {
        if (session.isDemoMode()) {
            CatalogPromotion promotion = DemoData.promotion(id);
            result.accept(promotion == null ? LoadState.error("Conteúdo indisponível.", 404)
                    : LoadState.success(promotion));
            return;
        }
        if (unavailable(result)) return;
        enqueue(service.promotion(id), result);
    }

    /** Informa que o serviço necessário à consulta não está disponível. */
    private <T> boolean unavailable(Result<T> result) {
        if (service != null) return false;
        result.accept(LoadState.error("Serviço indisponível. Tente mais tarde.", 0));
        return true;
    }

    /** Converte o filtro vazio em ausência de valor para a requisição. */
    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** Normaliza os dados antes de utilizá-los na consulta ou apresentação. */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /** Prepara a página utilizada no carregamento ou na verificação do catálogo. */
    private static <T> CatalogPage<T> page(List<T> items, int requestedPage, int size) {
        CatalogPage<T> page = new CatalogPage<>();
        int number = Math.max(0, requestedPage);
        long start = (long) number * size;
        int from = (int) Math.min(start, items.size());
        int to = Math.min(from + size, items.size());
        page.content = new ArrayList<>(items.subList(from, to));
        page.number = number;
        page.size = size;
        page.totalElements = items.size();
        page.totalPages = (items.size() + size - 1) / size;
        return page;
    }

    /** Executa a requisição assíncrona e encaminha a resposta ou a falha. */
    private static <T> void enqueue(Call<T> call, Result<T> result) {
        call.enqueue(new Callback<T>() {
            /** Trata a resposta da requisição e encaminha o resultado ao fluxo que a solicitou. */
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.accept(LoadState.success(response.body()));
                } else {
                    result.accept(LoadState.error(response.code() == 404
                            ? "Conteúdo indisponível." : "Não foi possível carregar os dados.",
                            response.code()));
                }
            }

            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            @Override
            public void onFailure(Call<T> call, Throwable throwable) {
                result.accept(LoadState.error(throwable instanceof IOException
                        ? "Sem conexão. Verifique sua internet." : "Não foi possível carregar os dados.", 0));
            }
        });
    }
}
