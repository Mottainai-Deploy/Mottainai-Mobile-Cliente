package com.mottainai.cliente.network.dto;

/** Representa CustomerTokenResponse, responsável por a comunicação com os serviços externos. */
public final class CustomerTokenResponse {
    public String accessToken;
    public String tokenType;
    public long expiresIn;
}
