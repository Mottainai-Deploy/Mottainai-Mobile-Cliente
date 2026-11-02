package com.mottainai.cliente.network.dto;

import java.util.List;

/** Representa ChatSessions, responsável por a comunicação com os serviços externos. */
public final class ChatSessions {
    public List<Session> sessions;

    /** A listagem usa camelCase, diferente dos envelopes de envio e histórico. */
    public static final class Session {
        public String sessionId;
        public String title;
        public String status;
        public String lastInteraction;
    }
}
