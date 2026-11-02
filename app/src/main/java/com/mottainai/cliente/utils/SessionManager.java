package com.mottainai.cliente.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.mottainai.cliente.BuildConfig;

/**
 * Armazena o estado local mínimo necessário para direcionar o cliente às telas protegidas.
 * Senhas e outros dados sensíveis do perfil nunca podem ser persistidos aqui.
 */
public class SessionManager {

    private static final String PREFS_NAME = Constants.SESSION_PREFERENCES_NAME;

    private final Context context;
    private final SharedPreferences preferences;

    /** Inicializa SessionManager com os dados e as dependências recebidos. */
    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove("auth_token").apply();
    }

    /** Informa se a apresentação inicial já foi concluída. */
    public boolean isOnboardingDone() {
        return preferences.getBoolean(Constants.KEY_ONBOARDING_DONE, false);
    }

    /** Salva a conclusão da apresentação inicial. */
    public void setOnboardingDone() {
        preferences.edit().putBoolean(Constants.KEY_ONBOARDING_DONE, true).apply();
    }

    /** Salva os dados necessários para identificar a sessão autenticada do cliente. */
    public void saveClientSession(@NonNull String clientId, @NonNull String firebaseUid) {
        if (clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("O ID do cliente é obrigatório para salvar a sessão.");
        }
        preferences.edit()
                .putString(Constants.KEY_CLIENT_ID, clientId)
                .putString(Constants.KEY_FIREBASE_UID, firebaseUid)
                .putBoolean(Constants.KEY_SESSION_ACTIVE, true)
                .remove(Constants.KEY_DEMO_MODE)
                .apply();
    }

    /** Verifica se existe uma sessão de cliente válida no armazenamento local. */
    public boolean isLoggedIn() {
        FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
        return auth != null && auth.getCurrentUser() != null
                && auth.getCurrentUser().getUid().equals(
                preferences.getString(Constants.KEY_FIREBASE_UID, null))
                && preferences.getBoolean(Constants.KEY_SESSION_ACTIVE, false)
                && !isBlank(getClientId());
    }

    /** Informa se o estado local está marcado como demonstração. */
    public boolean isDemoMode() {
        return BuildConfig.DEBUG && preferences.getBoolean(Constants.KEY_DEMO_MODE, false);
    }

    /** Confere se a sessão permite acessar as funcionalidades protegidas. */
    public boolean canBrowse() {
        return isLoggedIn();
    }

    /** Atualiza a marcação local de demonstração. */
    public void enterDemoMode() {
        if (!BuildConfig.DEBUG) return;
        clearClientSession();
        preferences.edit().putBoolean(Constants.KEY_DEMO_MODE, true).apply();
    }

    /** Atualiza a marcação local de demonstração. */
    public void exitDemoMode() {
        preferences.edit().remove(Constants.KEY_DEMO_MODE).apply();
    }

    /** Retorna o identificador do cliente associado à sessão. */
    @Nullable
    public String getClientId() {
        return preferences.getString(Constants.KEY_CLIENT_ID, null);
    }

    /** Limpa todo o estado de autenticação em uma única transação de preferências. */
    public void clearSession() {
        FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
        if (auth != null) auth.signOut();
        clearClientSession();
    }

    /** Limpa a associação do perfil SQL sem alterar a identidade Firebase. */
    public void clearClientSession() {
        preferences.edit()
                .remove(Constants.KEY_CLIENT_ID)
                .remove(Constants.KEY_FIREBASE_UID)
                .remove(Constants.KEY_SESSION_ACTIVE)
                .remove(Constants.KEY_DEMO_MODE)
                .apply();
    }

    /** Verifica se o valor está ausente ou contém apenas espaços. */
    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }
}
