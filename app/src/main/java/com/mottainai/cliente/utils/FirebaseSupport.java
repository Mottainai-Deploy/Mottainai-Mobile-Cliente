package com.mottainai.cliente.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

/** Representa FirebaseSupport, responsável por as operações auxiliares deste recurso. */
public final class FirebaseSupport {

    /** Impede a criação de instâncias desta classe utilitária. */
    private FirebaseSupport() {
    }

    /** Obtém a autenticação Firebase quando o aplicativo possui configuração válida. */
    @Nullable
    public static FirebaseAuth authIfConfigured(Context context) {
        if (FirebaseApp.getApps(context.getApplicationContext()).isEmpty()) return null;
        try {
            return FirebaseAuth.getInstance();
        } catch (IllegalStateException exception) {
            return null;
        }
    }
}
