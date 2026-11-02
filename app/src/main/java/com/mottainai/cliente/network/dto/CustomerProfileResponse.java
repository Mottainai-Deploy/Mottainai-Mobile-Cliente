package com.mottainai.cliente.network.dto;

/** Representa CustomerProfileResponse, responsável por a comunicação com os serviços externos. */
public final class CustomerProfileResponse {
    public Integer id;
    public Short birthYear;
    public Boolean active;
    public String fullName;
    public String email;
    public String phone;
    public Boolean marketingConsent;
    public String avatarUrl;
}
