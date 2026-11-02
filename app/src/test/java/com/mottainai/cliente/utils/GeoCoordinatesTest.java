package com.mottainai.cliente.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/** Reúne os cenários de teste de GeoCoordinates. */
public class GeoCoordinatesTest {
    /** Verifica a aceitação de zero e dos limites geográficos válidos. */
    @Test public void acceptsZeroAndBothGeographicBoundaries() {
        assertTrue(GeoCoordinates.areValid(0.0, 0.0));
        assertTrue(GeoCoordinates.areValid(-90.0, -180.0));
        assertTrue(GeoCoordinates.areValid(90.0, 180.0));
        assertTrue(GeoCoordinates.areValid(-23.55, -46.63));
    }

    /** Verifica a rejeição de pares incompletos de coordenadas. */
    @Test public void rejectsMissingPairs() {
        assertFalse(GeoCoordinates.areValid(null, null));
        assertFalse(GeoCoordinates.areValid(0.0, null));
        assertFalse(GeoCoordinates.areValid(null, 0.0));
    }

    /** Verifica a rejeição de latitudes fora dos limites permitidos. */
    @Test public void rejectsLatitudeOutsideBounds() {
        assertFalse(GeoCoordinates.areValid(-90.001, 0.0));
        assertFalse(GeoCoordinates.areValid(90.001, 0.0));
    }

    /** Verifica a rejeição de longitudes fora dos limites permitidos. */
    @Test public void rejectsLongitudeOutsideBounds() {
        assertFalse(GeoCoordinates.areValid(0.0, -180.001));
        assertFalse(GeoCoordinates.areValid(0.0, 180.001));
    }

    /** Verifica a rejeição de coordenadas que não representam números finitos. */
    @Test public void rejectsNonFiniteValues() {
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertFalse(GeoCoordinates.areValid(value, 0.0));
            assertFalse(GeoCoordinates.areValid(0.0, value));
        }
    }
}
