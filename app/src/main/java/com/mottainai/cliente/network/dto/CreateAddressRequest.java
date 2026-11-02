package com.mottainai.cliente.network.dto;

/** Representa CreateAddressRequest, responsável por a comunicação com os serviços externos. */
public final class CreateAddressRequest {
    public final String zipCode;
    public final String street;
    public final String number;
    public final String complement;
    public final String neighborhood;
    public final String city;
    public final String state;

    /** Inicializa CreateAddressRequest com os dados e as dependências recebidos. */
    public CreateAddressRequest(String zipCode, String street, String number, String complement,
                                String neighborhood, String city, String state) {
        this.zipCode = zipCode;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
    }
}
