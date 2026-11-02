package com.mottainai.cliente.network.dto;

import com.google.gson.annotations.SerializedName;

/** Representa ViaCepAddress, responsável por a comunicação com os serviços externos. */
public final class ViaCepAddress {
    public String cep;
    public String logradouro;
    public String bairro;
    public String localidade;
    public String uf;
    @SerializedName("erro") public Boolean error;
}
