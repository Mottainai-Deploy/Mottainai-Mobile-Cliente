package com.mottainai.cliente.utils;

public enum ClientAccessState {
    LOGIN_REQUIRED, LOCATION_REQUIRED, READY;

    public static ClientAccessState evaluate(boolean canBrowse, boolean debugDemo,
                                             boolean locationPermission, boolean locationEnabled) {
        if (!canBrowse) return LOGIN_REQUIRED;
        if (debugDemo) return READY;
        return locationPermission && locationEnabled ? READY : LOCATION_REQUIRED;
    }
}
