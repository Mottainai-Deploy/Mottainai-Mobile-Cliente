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

public interface ApiService {
    @GET("api/v1/client/auth/profile")
    Call<CustomerProfileResponse> profile(@Tag String firebaseUid);

    @POST("api/v1/client/auth/profile")
    Call<CustomerProfileResponse> createProfile(@Body CreateFirebaseCustomerProfileRequest request,
                                                 @Tag String firebaseUid);

    @PUT("api/v1/client/auth/profile")
    Call<CustomerProfileResponse> updateProfile(@Body UpdateFirebaseCustomerProfileRequest request,
                                                 @Tag String firebaseUid);
}
