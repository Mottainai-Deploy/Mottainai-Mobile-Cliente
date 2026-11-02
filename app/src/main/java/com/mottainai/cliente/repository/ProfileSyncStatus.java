package com.mottainai.cliente.repository;

import com.mottainai.cliente.network.dto.CustomerProfileResponse;

/** Interpreta o contrato do perfil antes de permitir acesso às telas autenticadas. */
public enum ProfileSyncStatus {
    READY, PROFILE_REQUIRED, INACTIVE, INVALID_RESPONSE, ERROR;

    /** Indica se o resultado permite iniciar a criação do perfil. */
    public boolean allowsProfileCreation(boolean createIfMissing) {
        return this == PROFILE_REQUIRED && createIfMissing;
    }

    /** Converte o resultado recebido para o estado de sincronização correspondente. */
    public static ProfileSyncStatus from(int httpStatus, CustomerProfileResponse profile) {
        if (httpStatus == 404) return PROFILE_REQUIRED;
        if (httpStatus < 200 || httpStatus >= 300) return ERROR;
        if ((httpStatus != 200 && httpStatus != 201) || profile == null
                || profile.id == null || profile.id <= 0 || profile.active == null) {
            return INVALID_RESPONSE;
        }
        return profile.active ? READY : INACTIVE;
    }
}
