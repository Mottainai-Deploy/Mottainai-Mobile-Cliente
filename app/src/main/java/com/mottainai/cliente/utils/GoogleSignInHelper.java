package com.mottainai.cliente.utils;

import android.app.Activity;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.credentials.*;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.core.content.ContextCompat;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import java.util.concurrent.atomic.AtomicBoolean;

/** Representa GoogleSignInHelper, responsável por as operações auxiliares deste recurso. */
public final class GoogleSignInHelper {
    /** Define o contrato de Callback, responsável por as operações auxiliares deste recurso. */
    public interface Callback {
        /** Recebe o token do Google e encaminha a autenticação para o fluxo da conta. */
        void onToken(String idToken);
        /** Trata a falha recebida e informa o erro ao fluxo responsável. */
        void onError(String message);
    }
    /** Impede a criação de instâncias desta classe utilitária. */
    private GoogleSignInHelper() {}

    /** Monta a solicitação de credencial do botão de entrada com Google. */
    static GetCredentialRequest buttonRequest(String webClientId) {
        return new GetCredentialRequest.Builder().addCredentialOption(
                new GetSignInWithGoogleOption.Builder(webClientId).build()).build();
    }

    /** Abre a seleção da conta Google e encaminha o resultado da autenticação. */
    public static CancellationSignal signIn(Activity activity, Callback callback) {
        CancellationSignal cancellation = new CancellationSignal();
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean completed = new AtomicBoolean();
        Runnable timeout = () -> {
            if (completed.compareAndSet(false, true)) {
                cancellation.cancel();
                callback.onError("O Google demorou para responder. Confira a internet e tente novamente.");
            }
        };
        Callback once = new Callback() {
            /** Recebe o token do Google e encaminha a autenticação para o fluxo da conta. */
            public void onToken(String token) {
                if (completed.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeout);
                    callback.onToken(token);
                }
            }
            /** Trata a falha recebida e informa o erro ao fluxo responsável. */
            public void onError(String message) {
                if (completed.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeout);
                    callback.onError(message);
                }
            }
        };
        int clientIdResource = activity.getResources().getIdentifier(
                "default_web_client_id", "string", activity.getPackageName());
        if (clientIdResource == 0 || activity.getString(clientIdResource).trim().isEmpty()) {
            once.onError("O login com Google não está disponível no momento.");
            return cancellation;
        }
        handler.postDelayed(timeout, 90000);
        try {
            CredentialManager.create(activity).getCredentialAsync(
                    activity, buttonRequest(activity.getString(clientIdResource)), cancellation,
                    ContextCompat.getMainExecutor(activity),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        /** Recebe o resultado da seleção de credenciais do Google. */
                        @Override public void onResult(@NonNull GetCredentialResponse response) {
                            Credential credential = response.getCredential();
                            if (!(credential instanceof CustomCredential)
                                    || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                                once.onError("O Google não retornou uma credencial compatível. Tente novamente.");
                                return;
                            }
                            try {
                                GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(
                                        ((CustomCredential) credential).getData());
                                once.onToken(google.getIdToken());
                            } catch (Exception invalid) {
                                once.onError("Não foi possível validar a conta Google. Tente novamente.");
                            }
                        }
                        /** Trata a falha recebida e informa o erro ao fluxo responsável. */
                        @Override public void onError(@NonNull GetCredentialException error) {
                            if (error instanceof GetCredentialCancellationException)
                                once.onError("Login com Google cancelado. Você pode tentar novamente.");
                            else if (error instanceof NoCredentialException)
                                once.onError("Nenhuma conta Google disponível no aparelho.");
                            else if (error instanceof GetCredentialProviderConfigurationException)
                                once.onError("O login com Google não está disponível no momento.");
                            else once.onError("Não foi possível entrar com Google. Verifique sua conexão e tente novamente.");
                        }
                    });
        } catch (Exception unavailable) {
            once.onError("Não foi possível iniciar o login com Google. Tente novamente.");
        }
        return cancellation;
    }
}
