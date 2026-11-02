package com.mottainai.cliente.network;

import com.mottainai.cliente.network.dto.CatalogPage;
import com.mottainai.cliente.network.dto.CatalogPromotion;
import com.mottainai.cliente.network.dto.CatalogStore;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Define o contrato de CustomerCatalogService, responsável por a comunicação com os serviços externos. */
public interface CustomerCatalogService {
    /** Obtém a página de lojas conforme os filtros informados. */
    @GET("api/v1/customer-catalog/stores")
    Call<CatalogPage<CatalogStore>> stores(@Query("page") int page, @Query("size") int size,
                                           @Query("query") String query,
                                           @Query("latitude") Double latitude,
                                           @Query("longitude") Double longitude,
                                           @Query("radiusKm") Double radiusKm);

    /** Obtém os dados da loja identificada na consulta. */
    @GET("api/v1/customer-catalog/stores/{id}")
    Call<CatalogStore> store(@Path("id") String id);

    /** Obtém a página de promoções conforme os filtros informados. */
    @GET("api/v1/customer-catalog/promotions")
    Call<CatalogPage<CatalogPromotion>> promotions(@Query("page") int page, @Query("size") int size,
                                                     @Query("storeId") String storeId,
                                                     @Query("query") String query);

    /** Obtém os dados da promoção identificada na consulta. */
    @GET("api/v1/customer-catalog/promotions/{id}")
    Call<CatalogPromotion> promotion(@Path("id") String id);
}
