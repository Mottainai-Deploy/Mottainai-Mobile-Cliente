package com.mottainai.cliente.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Representa ChatHistory, responsável por a comunicação com os serviços externos. */
public final class ChatHistory {
    @SerializedName("session_id") public String sessionId;
    public String status;
    public List<Message> messages;

    /** Representa Message, responsável por a comunicação com os serviços externos. */
    public static final class Message {
        public String role;
        public String content;
    }

    /** Confere se a resposta corresponde à conversa solicitada. */
    public boolean isValidFor(String expectedSession) {
        return expectedSession != null && expectedSession.equals(sessionId) && messages != null;
    }
}
