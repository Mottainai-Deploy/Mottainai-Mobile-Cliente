package com.mottainai.cliente.models;

import org.junit.Test;

import static org.junit.Assert.*;

public class OfferRadiusTest {
    @Test public void startsAtFiveKilometers() {
        assertEquals(5, OfferRadius.DEFAULT.kilometers());
    }

    @Test public void exposesOnlyProductApprovedOptionsInOrder() {
        assertEquals(2, OfferRadius.values().length);
        int[] expected = {5, 10};
        for (int index = 0; index < expected.length; index++) {
            assertEquals(expected[index], OfferRadius.values()[index].kilometers());
            assertSame(OfferRadius.values()[index], OfferRadius.fromKilometers(expected[index]));
        }
    }

    /** Rejeita distâncias arbitrárias e os raios antigos acima do limite atual. */
    @Test public void rejectsArbitraryRadius() {
        for (int radius : new int[]{7, 15, 20}) {
            assertThrows(IllegalArgumentException.class, () -> OfferRadius.fromKilometers(radius));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZero() { OfferRadius.fromKilometers(0); }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeRadius() { OfferRadius.fromKilometers(-5); }
}
