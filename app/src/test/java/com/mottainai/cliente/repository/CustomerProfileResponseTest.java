package com.mottainai.cliente.repository;

import com.google.gson.Gson;
import com.mottainai.cliente.network.dto.CustomerProfileResponse;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CustomerProfileResponseTest {
    @Test public void readsProtectedProfileWithoutExpectingPlaintextContactFields() {
        CustomerProfileResponse profile = new Gson().fromJson(
                "{\"id\":7,\"birthYear\":2000,\"active\":true,\"marketingConsent\":false}",
                CustomerProfileResponse.class);
        assertEquals(Integer.valueOf(7), profile.id);
        assertEquals(Short.valueOf((short) 2000), profile.birthYear);
        assertTrue(profile.active);
        assertNull(profile.fullName);
        assertNull(profile.email);
    }

    /** Lê os dados de contato e a referência da foto publicados pelo contrato atual. */
    @Test public void readsContactFieldsAndAvatarFromCurrentProfileContract() {
        CustomerProfileResponse profile = new Gson().fromJson(
                "{\"id\":7,\"fullName\":\"Cliente Exemplo\",\"email\":\"cliente@example.invalid\",\"phone\":\"11999990000\",\"avatarUrl\":\"https://res.cloudinary.com/example/avatar.jpg\"}",
                CustomerProfileResponse.class);
        assertEquals("Cliente Exemplo", profile.fullName);
        assertEquals("cliente@example.invalid", profile.email);
        assertEquals("11999990000", profile.phone);
        assertEquals("https://res.cloudinary.com/example/avatar.jpg", profile.avatarUrl);
    }

    /** Preserva no JSON os campos de perfil agora publicados pela API Cliente. */
    @Test public void profileFieldsSurviveJsonRoundTrip() {
        CustomerProfileResponse profile = new CustomerProfileResponse();
        profile.id = 7;
        profile.active = true;
        profile.fullName = "Nome confirmado pela atualização SQL";
        profile.email = "login@example.invalid";
        profile.phone = "telefone de apresentação";
        profile.avatarUrl = "https://res.cloudinary.com/example/avatar.jpg";

        CustomerProfileResponse restored = new Gson().fromJson(
                new Gson().toJson(profile), CustomerProfileResponse.class);
        assertEquals(profile.id, restored.id);
        assertTrue(restored.active);
        assertEquals(profile.fullName, restored.fullName);
        assertEquals(profile.email, restored.email);
        assertEquals(profile.phone, restored.phone);
        assertEquals(profile.avatarUrl, restored.avatarUrl);
    }
}
