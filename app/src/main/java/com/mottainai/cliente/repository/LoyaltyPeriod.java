package com.mottainai.cliente.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Limites mensais inclusivos na precisão de microssegundos do timestamp PostgreSQL. */
public final class LoyaltyPeriod {
    public final LocalDateTime from;
    public final LocalDateTime to;

    /** Inicializa LoyaltyPeriod com os dados e as dependências recebidos. */
    public LoyaltyPeriod(LocalDate month) {
        from = month.withDayOfMonth(1).atStartOfDay();
        to = from.plusMonths(1).minusNanos(1000);
    }
}
