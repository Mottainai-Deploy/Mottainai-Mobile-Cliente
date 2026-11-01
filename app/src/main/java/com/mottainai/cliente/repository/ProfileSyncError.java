package com.mottainai.cliente.repository;

import com.google.gson.JsonParseException;
import com.google.gson.stream.MalformedJsonException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import javax.net.ssl.SSLException;

public final class ProfileSyncError {
    private ProfileSyncError() {}

    public static String message(Throwable failure, boolean emulatorAddress) {
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            if (cause instanceof JsonParseException || cause instanceof MalformedJsonException
                    || cause instanceof InterruptedIOException || cause instanceof UnknownHostException
                    || cause instanceof ConnectException || cause instanceof SSLException
                    || cause instanceof UnknownServiceException) {
                failure = cause;
                break;
            }
        }
        String message;
        boolean invalidResponse = failure instanceof JsonParseException || failure instanceof MalformedJsonException;
        if (invalidResponse) {
            message = "Não foi possível concluir agora. Tente novamente.";
        } else if (failure instanceof InterruptedIOException) {
            message = "A conexão demorou. Tente novamente.";
        } else if (failure instanceof UnknownHostException || failure instanceof ConnectException
                || failure instanceof SSLException || failure instanceof UnknownServiceException) {
            message = "Não foi possível conectar. Verifique sua internet e tente novamente.";
        } else {
            message = "Não foi possível atualizar seu perfil. Tente novamente.";
        }
        return message;
    }
}
