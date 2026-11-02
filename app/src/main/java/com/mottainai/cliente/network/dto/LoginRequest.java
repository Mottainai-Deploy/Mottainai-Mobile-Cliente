package com.mottainai.cliente.network.dto;

/** Representa LoginRequest, responsável por a comunicação com os serviços externos. */
public final class LoginRequest {
    public final String email;
    public final String password;

    /** Inicializa LoginRequest com os dados e as dependências recebidos. */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
