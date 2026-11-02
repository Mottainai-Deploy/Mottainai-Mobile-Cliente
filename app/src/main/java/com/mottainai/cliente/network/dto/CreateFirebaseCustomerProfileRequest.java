package com.mottainai.cliente.network.dto;

/** Representa CreateFirebaseCustomerProfileRequest, responsável por a comunicação com os serviços externos. */
public final class CreateFirebaseCustomerProfileRequest {
    public final String fullName;
    public final String cpf;
    public final String phone;
    public final String birthDate;
    public final Boolean marketingConsent;
    public final CreateAddressRequest address;

    /** Inicializa CreateFirebaseCustomerProfileRequest com os dados e as dependências recebidos. */
    public CreateFirebaseCustomerProfileRequest(String fullName, String cpf, String phone, String birthDate,
                                               Boolean marketingConsent, CreateAddressRequest address) {
        this.fullName = fullName;
        this.cpf = cpf;
        this.phone = phone;
        this.birthDate = birthDate;
        this.marketingConsent = marketingConsent;
        this.address = address;
    }
}
