package com.mottainai.cliente.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.mottainai.cliente.BuildConfig;

public class SessionManager {

    private static final String PREFS_NAME = Constants.SESSION_PREFERENCES_NAME;

    private final Context context;
    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove("auth_token").apply();
    }

    public boolean isOnboardingDone() {
        return preferences.getBoolean(Constants.KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone() {
        preferences.edit().putBoolean(Constants.KEY_ONBOARDING_DONE, true).apply();
    }

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

    public boolean isLoggedIn() {
        FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
        return auth != null && auth.getCurrentUser() != null
                && auth.getCurrentUser().getUid().equals(
                preferences.getString(Constants.KEY_FIREBASE_UID, null))
                && preferences.getBoolean(Constants.KEY_SESSION_ACTIVE, false)
                && !isBlank(getClientId());
    }

    public boolean isDemoMode() {
        return BuildConfig.DEBUG && preferences.getBoolean(Constants.KEY_DEMO_MODE, false);
    }

    public boolean canBrowse() {
        return isLoggedIn();
    }

    public void enterDemoMode() {
        if (!BuildConfig.DEBUG) return;
        clearClientSession();
        preferences.edit().putBoolean(Constants.KEY_DEMO_MODE, true).apply();
    }

    public void exitDemoMode() {
        preferences.edit().remove(Constants.KEY_DEMO_MODE).apply();
    }

    @Nullable
    public String getClientId() {
        return preferences.getString(Constants.KEY_CLIENT_ID, null);
    }

    public void clearSession() {
        FirebaseAuth auth = FirebaseSupport.authIfConfigured(context);
        if (auth != null) auth.signOut();
        clearClientSession();
    }

    public void clearClientSession() {
        preferences.edit()
                .remove(Constants.KEY_CLIENT_ID)
                .remove(Constants.KEY_FIREBASE_UID)
                .remove(Constants.KEY_SESSION_ACTIVE)
                .remove(Constants.KEY_DEMO_MODE)
                .apply();
    }

    private boolean isBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }
}
