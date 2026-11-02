package com.mottainai.cliente.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.mottainai.cliente.activities.LocationAccessActivity;
import com.mottainai.cliente.activities.LoginClienteActivity;
import com.mottainai.cliente.repository.ForegroundLocationRepository;

/** Representa ClientAccessGuard, responsável por as operações auxiliares deste recurso. */
public final class ClientAccessGuard {
    /** Impede a criação de instâncias desta classe utilitária. */
    private ClientAccessGuard() { }

    /** Verifica se o aplicativo possui permissão para acessar a localização. */
    public static boolean hasLocationPermission(Context context) {
        return ForegroundLocationRepository.hasPermission(context);
    }

    /** Verifica se a localização do aparelho está ativada. */
    public static boolean isLocationEnabled(Context context) {
        return ForegroundLocationRepository.isEnabled(context);
    }

    /** Retorna o estado atual observado pelo fluxo. */
    public static ClientAccessState state(Context context) {
        SessionManager session = new SessionManager(context);
        if (!session.canBrowse()) return ClientAccessState.LOGIN_REQUIRED;
        if (session.isDemoMode()) return ClientAccessState.READY;
        return ClientAccessState.evaluate(true, false, hasLocationPermission(context),
                isLocationEnabled(context));
    }

    /** Retorna verdadeiro quando a chamada não deve iniciar nem exibir conteúdo protegido. */
    public static boolean redirectIfRequired(Activity activity) {
        if (activity.isFinishing()) return true;
        ClientAccessState state = state(activity);
        if (state == ClientAccessState.READY) return false;
        Class<?> target = state == ClientAccessState.LOGIN_REQUIRED
                ? LoginClienteActivity.class : LocationAccessActivity.class;
        activity.startActivity(new Intent(activity, target)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
        return true;
    }
}
