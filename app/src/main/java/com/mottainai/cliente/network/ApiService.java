package com.mottainai.cliente.network;

import com.mottainai.cliente.network.dto.CreateFirebaseCustomerProfileRequest;
import com.mottainai.cliente.network.dto.CustomerProfileResponse;
import com.mottainai.cliente.network.dto.UpdateFirebaseCustomerProfileRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Tag;

/**
 * Contrato REST da API Mottainai.
 *
 * Os endpoints do Cliente usam tokens de ID do Firebase inseridos por ApiClient.
 */
public interface ApiService {
    /** Obtém os dados do perfil utilizado neste fluxo. */
    @GET("api/v1/client/auth/profile")
    Call<CustomerProfileResponse> profile(@Tag String firebaseUid);

    /** Declara a requisição que cria o perfil da conta autenticada. */
    @POST("api/v1/client/auth/profile")
    Call<CustomerProfileResponse> createProfile(@Body CreateFirebaseCustomerProfileRequest request,
                                                 @Tag String firebaseUid);

    /** Envia as alterações dos dados do perfil para persistência. */
    @PUT("api/v1/client/auth/profile")
    Call<CustomerProfileResponse> updateProfile(@Body UpdateFirebaseCustomerProfileRequest request,
                                                 @Tag String firebaseUid);
}
