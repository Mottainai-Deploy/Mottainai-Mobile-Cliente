package com.mottainai.cliente.utils;

/** Requisitos de acesso da interface; autenticação e autorização da API continuam obrigatórias. */
public enum ClientAccessState {
    LOGIN_REQUIRED, LOCATION_REQUIRED, READY;

    /** Determina o estado de acesso conforme a sessão e a disponibilidade de localização. */
    public static ClientAccessState evaluate(boolean canBrowse, boolean debugDemo,
                                             boolean locationPermission, boolean locationEnabled) {
        if (!canBrowse) return LOGIN_REQUIRED;
        if (debugDemo) return READY;
        return locationPermission && locationEnabled ? READY : LOCATION_REQUIRED;
    }
}
