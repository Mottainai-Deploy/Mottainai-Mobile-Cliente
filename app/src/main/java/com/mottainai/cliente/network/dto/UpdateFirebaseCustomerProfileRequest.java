package com.mottainai.cliente.network.dto;

public final class UpdateFirebaseCustomerProfileRequest {
    public final String fullName;
    public final String phone;
    public final Boolean marketingConsent;
    public final String avatarUrl;

    public UpdateFirebaseCustomerProfileRequest(String fullName, String phone, Boolean marketingConsent) {
        this(fullName, phone, marketingConsent, null);
    }

    public UpdateFirebaseCustomerProfileRequest(String fullName, String phone, Boolean marketingConsent,
                                                String avatarUrl) {
        this.fullName = fullName;
        this.phone = phone;
        this.marketingConsent = marketingConsent;
        this.avatarUrl = avatarUrl;
    }
}
