package com.mottainai.cliente.repository;

import com.mottainai.cliente.network.dto.CustomerProfileResponse;

public enum ProfileSyncStatus {
    READY, PROFILE_REQUIRED, INACTIVE, INVALID_RESPONSE, ERROR;

    public boolean allowsProfileCreation(boolean createIfMissing) {
        return this == PROFILE_REQUIRED && createIfMissing;
    }

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
