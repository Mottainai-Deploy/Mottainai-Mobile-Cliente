package com.mottainai.cliente.network.dto;

import com.google.gson.annotations.SerializedName;

/** Representa ChatResponse, responsável por a comunicação com os serviços externos. */
public final class ChatResponse {
    @SerializedName("session_id") public String sessionId;
    public String response;

    /** Confere se a resposta corresponde à conversa solicitada. */
    public boolean isValidFor(String expectedSession) {
        return expectedSession != null && expectedSession.equals(sessionId)
                && response != null && !response.trim().isEmpty();
    }
}
