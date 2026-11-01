package com.mottainai.cliente.network;

import com.mottainai.cliente.network.dto.ChatHistory;
import com.mottainai.cliente.network.dto.ChatRequest;
import com.mottainai.cliente.network.dto.ChatResponse;
import com.mottainai.cliente.network.dto.ChatSessions;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Tag;

public interface AssistantService {
    @POST("chat") Call<ChatResponse> send(@Body ChatRequest request, @Tag String firebaseUid);
    @GET("chat/history/{sessionId}") Call<ChatHistory> history(@Path("sessionId") String sessionId,
                                                            @Tag String firebaseUid);
    @GET("chat/sessions") Call<ChatSessions> sessions(@Tag String firebaseUid);
}
