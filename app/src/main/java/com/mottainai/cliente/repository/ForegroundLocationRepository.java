package com.mottainai.cliente.repository;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.mottainai.cliente.utils.GeoCoordinates;

import java.util.List;
import java.util.function.Consumer;

/** Uma solicitação de posição em primeiro plano por vez. Sem rastreamento, coordenadas salvas ou tarefa em segundo plano. */
public final class ForegroundLocationRepository {
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CancellationSignal pending;

    /** Inicializa ForegroundLocationRepository com os dados e as dependências recebidos. */
    public ForegroundLocationRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Verifica se o aplicativo possui permissão para acessar a localização. */
    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Verifica se a localização do aparelho está ativada. */
    public static boolean isEnabled(Context context) {
        LocationManager manager = context.getSystemService(LocationManager.class);
        return manager != null && LocationManagerCompat.isLocationEnabled(manager);
    }

    /** Solicita a posição atual do aparelho e acompanha seu resultado. */
    public void request(Consumer<LoadState<Location>> result) {
        cancel();
        if (!hasPermission(context) || !isEnabled(context)) {
            result.accept(LoadState.error("Ative e autorize a localização para encontrar lojas próximas.", 0));
            return;
        }
        LocationManager manager = context.getSystemService(LocationManager.class);
        CancellationSignal request = new CancellationSignal();
        pending = request;
        result.accept(LoadState.loading());
        handler.postDelayed(() -> {
            if (pending != request) return;
            cancel();
            result.accept(LoadState.error("A localização demorou a responder. Tente novamente.", 0));
        }, 15000);
        try {
            List<String> providers = manager.getProviders(true);
            String provider = providers.contains(LocationManager.NETWORK_PROVIDER)
                    ? LocationManager.NETWORK_PROVIDER : LocationManager.GPS_PROVIDER;
            LocationManagerCompat.getCurrentLocation(manager, provider, request,
                    ContextCompat.getMainExecutor(context), location -> {
                        if (pending != request) return;
                        cancel();
                        if (!hasPermission(context) || !isEnabled(context) || location == null
                                || !GeoCoordinates.areValid(location.getLatitude(), location.getLongitude())) {
                            result.accept(LoadState.error("Não foi possível obter sua localização. Confira as permissões e tente novamente.", 0));
                            return;
                        }
                        result.accept(LoadState.success(location));
                    });
        } catch (SecurityException | IllegalArgumentException unavailable) {
            if (pending != request) return;
            cancel();
            result.accept(LoadState.error("Localização indisponível. Confira os serviços do aparelho.", 0));
        }
    }

    /** Cancela a operação pendente e libera os recursos associados. */
    public void cancel() {
        CancellationSignal request = pending;
        pending = null;
        handler.removeCallbacksAndMessages(null);
        if (request != null) request.cancel();
    }
}
