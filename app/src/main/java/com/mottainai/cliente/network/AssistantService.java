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

/** Contrato existente da IA; a identidade fica apenas no cabeçalho autenticado. */
public interface AssistantService {
    /** Envia a mensagem e acompanha o resultado da consulta à assistente. */
    @POST("chat") Call<ChatResponse> send(@Body ChatRequest request, @Tag String firebaseUid);
    /** Obtém as mensagens da conversa solicitada. */
    @GET("chat/history/{sessionId}") Call<ChatHistory> history(@Path("sessionId") String sessionId,
                                                            @Tag String firebaseUid);
    /** Obtém as conversas disponíveis para a conta atual. */
    @GET("chat/sessions") Call<ChatSessions> sessions(@Tag String firebaseUid);
}
