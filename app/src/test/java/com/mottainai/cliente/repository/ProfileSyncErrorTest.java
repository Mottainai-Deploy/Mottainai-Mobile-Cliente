package com.mottainai.cliente.repository;

import com.google.gson.JsonSyntaxException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProfileSyncErrorTest {
    /** Informa a falha de conexão sem expor configuração local nem texto da exceção. */
    @Test public void unavailableServiceDoesNotExposeTechnicalConfiguration() {
        String message = ProfileSyncError.message(new ConnectException("SECRET"), true);
        assertEquals("Não foi possível conectar. Verifique sua internet e tente novamente.", message);
        assertFalse(message.contains("API"));
        assertFalse(message.contains("emulador"));
        assertEquals(message, ProfileSyncError.message(new ConnectException("SECRET"), false));
        assertFalse(message.contains("SECRET"));
    }
    @Test public void timeoutDoesNotReportWrongPassword() {
        String message = ProfileSyncError.message(new SocketTimeoutException(), false);
        assertTrue(message.contains("demorou"));
        assertFalse(message.contains("senha"));
    }
    @Test public void malformedResponseIsNotReportedAsInternetFailure() {
        String message = ProfileSyncError.message(new JsonSyntaxException("SECRET"), false);
        assertEquals("Não foi possível concluir agora. Tente novamente.", message);
        assertFalse(message.contains("internet"));
        assertFalse(message.contains("SECRET"));
    }
    /** Apresenta falhas de endereço e conexão segura sem detalhes técnicos ou sensíveis. */
    @Test public void dnsAndTlsFailuresUseSafeConnectionMessage() {
        String expected = "Não foi possível conectar. Verifique sua internet e tente novamente.";
        assertEquals(expected, ProfileSyncError.message(new UnknownHostException("SECRET"), false));
        assertEquals(expected, ProfileSyncError.message(new SSLHandshakeException("SECRET"), false));
    }
    @Test public void wrappedNetworkErrorsKeepTheirClassification() {
        String message = ProfileSyncError.message(new java.io.IOException(new ConnectException()), false);
        assertEquals(ProfileSyncError.message(new ConnectException(), false), message);
    }
    @Test public void malformedJsonIsNotAnInternetError() {
        String message = ProfileSyncError.message(new com.google.gson.stream.MalformedJsonException("SECRET"), true);
        assertEquals("Não foi possível concluir agora. Tente novamente.", message);
        assertFalse(message.contains("internet"));
        assertFalse(message.contains("SECRET"));
        assertFalse(message.contains("emulador"));
    }
}
