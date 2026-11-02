package com.mottainai.cliente.network.dto;

/** Representa UpdateFirebaseCustomerProfileRequest, responsável por a comunicação com os serviços externos. */
public final class UpdateFirebaseCustomerProfileRequest {
    public final String fullName;
    public final String phone;
    public final Boolean marketingConsent;
    public final String avatarUrl;

    /** Inicializa UpdateFirebaseCustomerProfileRequest com os dados e as dependências recebidos. */
    public UpdateFirebaseCustomerProfileRequest(String fullName, String phone, Boolean marketingConsent) {
        this(fullName, phone, marketingConsent, null);
    }

    /** Inicializa UpdateFirebaseCustomerProfileRequest com os dados e as dependências recebidos. */
    public UpdateFirebaseCustomerProfileRequest(String fullName, String phone, Boolean marketingConsent,
                                                String avatarUrl) {
        this.fullName = fullName;
        this.phone = phone;
        this.marketingConsent = marketingConsent;
        this.avatarUrl = avatarUrl;
    }
}
