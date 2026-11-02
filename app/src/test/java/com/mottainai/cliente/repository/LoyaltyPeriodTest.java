package com.mottainai.cliente.repository;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Reúne os cenários de teste de LoyaltyPeriod. */
public class LoyaltyPeriodTest {
    /** Verifica se as janelas inclusivas consecutivas não repetem o mesmo instante. */
    @Test public void adjacentInclusiveWindowsDoNotShareMidnight() {
        LoyaltyPeriod december = new LoyaltyPeriod(LocalDate.of(2025, 12, 17));
        LoyaltyPeriod january = new LoyaltyPeriod(LocalDate.of(2026, 1, 1));
        assertEquals(LocalDateTime.of(2025, 12, 1, 0, 0), december.from);
        assertEquals(january.from, december.to.plusNanos(1000));
        assertEquals(999999000, december.to.getNano());
        assertTrue(december.to.isBefore(january.from));
    }

    /** Verifica a inclusão do último dia de fevereiro em ano bissexto. */
    @Test public void leapYearFebruaryKeepsTheLastDay() {
        LoyaltyPeriod february = new LoyaltyPeriod(LocalDate.of(2024, 2, 15));
        assertEquals(LocalDate.of(2024, 2, 29), february.to.toLocalDate());
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0), february.to.plusNanos(1000));
    }
}
