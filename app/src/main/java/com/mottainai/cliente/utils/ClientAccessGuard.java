package com.mottainai.cliente.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.mottainai.cliente.activities.LocationAccessActivity;
import com.mottainai.cliente.activities.LoginClienteActivity;
import com.mottainai.cliente.repository.ForegroundLocationRepository;

public final class ClientAccessGuard {
    private ClientAccessGuard() { }

    public static boolean hasLocationPermission(Context context) {
        return ForegroundLocationRepository.hasPermission(context);
    }

    public static boolean isLocationEnabled(Context context) {
        return ForegroundLocationRepository.isEnabled(context);
    }

    public static ClientAccessState state(Context context) {
        SessionManager session = new SessionManager(context);
        if (!session.canBrowse()) return ClientAccessState.LOGIN_REQUIRED;
        if (session.isDemoMode()) return ClientAccessState.READY;
        return ClientAccessState.evaluate(true, false, hasLocationPermission(context),
                isLocationEnabled(context));
    }

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
