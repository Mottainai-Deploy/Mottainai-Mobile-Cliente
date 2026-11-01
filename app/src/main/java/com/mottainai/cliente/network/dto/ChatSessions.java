package com.mottainai.cliente.network.dto;

import java.util.List;

public final class ChatSessions {
    public List<Session> sessions;

    public static final class Session {
        public String sessionId;
        public String title;
        public String status;
        public String lastInteraction;
    }
}
