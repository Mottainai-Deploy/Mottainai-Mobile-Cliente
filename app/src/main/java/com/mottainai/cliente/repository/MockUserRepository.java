package com.mottainai.cliente.repository;

import com.mottainai.cliente.models.UserProfile;

/** Implementação temporária para o futuro endpoint /users/me. Consulte {@link MockOfferRepository}. */
public class MockUserRepository {

    /** Retorna o perfil local utilizado na apresentação de exemplo. */
    public UserProfile getCurrentUser() {
        return new UserProfile("Camila Oliveira", "CO", "Broto", "55% para Planta",
                320, 55, 4.8, 12, 86.0);
    }
}
