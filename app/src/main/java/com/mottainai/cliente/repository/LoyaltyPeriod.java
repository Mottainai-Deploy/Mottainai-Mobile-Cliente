package com.mottainai.cliente.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class LoyaltyPeriod {
    public final LocalDateTime from;
    public final LocalDateTime to;

    public LoyaltyPeriod(LocalDate month) {
        from = month.withDayOfMonth(1).atStartOfDay();
        to = from.plusMonths(1).minusNanos(1000);
    }
}
