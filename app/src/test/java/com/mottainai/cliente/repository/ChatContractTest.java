package com.mottainai.cliente.repository;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mottainai.cliente.network.dto.ChatRequest;
import com.mottainai.cliente.network.dto.ChatResponse;
import com.mottainai.cliente.network.dto.ChatHistory;
import com.mottainai.cliente.network.dto.ChatSessions;
import org.junit.Test;
import static org.junit.Assert.*;

/** Reúne os cenários de teste de ChatContract. */
public class ChatContractTest {
    private final Gson gson = new Gson();

    /** Verifica se o corpo da requisição contém somente os campos do contrato. */
    @Test public void requestUsesOnlyPublishedFields() {
        JsonObject json = gson.toJsonTree(new ChatRequest(" Olá ", "session-1")).getAsJsonObject();
        assertEquals(2, json.size());
        assertEquals("Olá", json.get("message").getAsString());
        assertEquals("session-1", json.get("session_id").getAsString());
        assertFalse(json.has("role"));
        assertFalse(json.has("customer_id"));
    }

    /** Verifica a rejeição de caminhos e caracteres inválidos em identificadores de conversa. */
    @Test public void sessionIdsRejectPathsAndInvalidCharacters() {
        for (String invalid : new String[]{null, "", "../other", "abc/other", "_abc", "abc x",
                String.join("", java.util.Collections.nCopies(129, "a"))}) {
            assertFalse(ChatRequest.isValidSessionId(invalid));
        }
        assertTrue(ChatRequest.isValidSessionId("customer-session:abc.1_test"));
    }

    /** Verifica a rejeição de mensagens vazias. */
    @Test(expected = IllegalArgumentException.class) public void blankMessageIsRejected() {
        new ChatRequest("   ", "session-1");
    }

    /** Verifica a rejeição de mensagens acima do limite permitido. */
    @Test(expected = IllegalArgumentException.class) public void oversizedMessageIsRejected() {
        new ChatRequest(String.join("", java.util.Collections.nCopies(4097, "a")), "session-1");
    }

    /** Verifica se a resposta pertence à conversa solicitada. */
    @Test public void responseMustBelongToRequestedConversation() {
        ChatResponse response = gson.fromJson(
                "{\"session_id\":\"session-1\",\"response\":\"Olá\"}", ChatResponse.class);
        assertTrue(response.isValidFor("session-1"));
        assertFalse(response.isValidFor("session-2"));
        response.response = " ";
        assertFalse(response.isValidFor("session-1"));
    }

    /** Verifica os nomes dos campos e a presença da lista de mensagens no histórico. */
    @Test public void historyUsesSnakeCaseAndNeedsMessageArray() {
        ChatHistory history = gson.fromJson(
                "{\"session_id\":\"session-1\",\"status\":\"active\",\"messages\":[]}", ChatHistory.class);
        assertTrue(history.isValidFor("session-1"));
        assertFalse(history.isValidFor("session-2"));
        history.messages = null;
        assertFalse(history.isValidFor("session-1"));
    }

    /** Verifica os nomes dos campos publicados na lista de conversas. */
    @Test public void sessionListUsesPublishedCamelCase() {
        ChatSessions result = gson.fromJson(
                "{\"sessions\":[{\"sessionId\":\"session-1\",\"title\":\"Compras\","
                        + "\"lastInteraction\":\"2026-10-21T10:00:00Z\"}]}", ChatSessions.class);
        assertEquals("session-1", result.sessions.get(0).sessionId);
        assertEquals("Compras", result.sessions.get(0).title);
    }
}
