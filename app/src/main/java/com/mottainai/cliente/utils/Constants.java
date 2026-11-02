package com.mottainai.cliente.utils;

/** Identificadores não sensíveis do aplicativo compartilhados por componentes de infraestrutura. */
public final class Constants {

    public static final String SESSION_PREFERENCES_NAME = "mottainai_cliente_session";
    public static final String KEY_ONBOARDING_DONE = "onboarding_done";
    public static final String KEY_CLIENT_ID = "client_id";
    public static final String KEY_FIREBASE_UID = "firebase_uid";
    public static final String KEY_SESSION_ACTIVE = "session_active";
    public static final String KEY_DEMO_MODE = "demo_mode";
    public static final String NOTIFICATION_CHANNEL_GENERAL_ID = "mottainai_general";

    /** Impede a criação de instâncias desta classe utilitária. */
    private Constants() {
    }
}
