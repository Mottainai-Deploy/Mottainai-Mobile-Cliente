package com.mottainai.cliente.network.dto;

import java.math.BigDecimal;

/** Representa CatalogStore, responsável por a comunicação com os serviços externos. */
public class CatalogStore {
    public String id;
    public String name;
    public Address address;
    public BigDecimal latitude;
    public BigDecimal longitude;

    /** Representa Address, responsável por a comunicação com os serviços externos. */
    public static class Address {
        public String zipCode;
        public String street;
        public String number;
        public String complement;
        public String neighborhood;
        public String city;
        public String state;
    }
}
