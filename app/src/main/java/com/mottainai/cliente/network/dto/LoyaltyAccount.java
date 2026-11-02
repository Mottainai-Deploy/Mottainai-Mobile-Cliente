package com.mottainai.cliente.network.dto;

/** Representa LoyaltyAccount, responsável por a comunicação com os serviços externos. */
public class LoyaltyAccount {
    public String id;
    public String customerId;
    public Integer pointsBalance;
    // Extensão opcional do contrato mobile; valores ausentes não podem ser calculados localmente.
    public String statusName;
    public Integer pointsToNextStatus;
}
