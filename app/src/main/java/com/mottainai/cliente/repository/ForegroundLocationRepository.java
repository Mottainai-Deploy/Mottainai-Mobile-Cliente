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
import java.util.ArrayList;
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
        try {
            boolean precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            List<String> providers = orderedProviders(manager.getProviders(true), precise);
            if (providers.isEmpty()) {
                result.accept(LoadState.error("Ative a localização precisa ou os serviços de localização do aparelho.", 0));
                return;
            }
            result.accept(LoadState.loading());
            requestProvider(manager, providers, 0, result);
        } catch (SecurityException | IllegalArgumentException unavailable) {
            cancel();
            result.accept(LoadState.error("Localização indisponível. Confira os serviços do aparelho.", 0));
        }
    }

    /** Prioriza GPS autorizado e mantém a rede como alternativa compatível com localização aproximada. */
    static List<String> orderedProviders(List<String> enabled, boolean precise) {
        List<String> providers = new ArrayList<>();
        if (enabled == null) return providers;
        if (precise && enabled.contains(LocationManager.GPS_PROVIDER)) providers.add(LocationManager.GPS_PROVIDER);
        if (enabled.contains(LocationManager.NETWORK_PROVIDER)) providers.add(LocationManager.NETWORK_PROVIDER);
        return providers;
    }

    /** Tenta cada fonte habilitada, dividindo o limite de quinze segundos e descartando respostas antigas. */
    private void requestProvider(LocationManager manager, List<String> providers, int index,
                                 Consumer<LoadState<Location>> result) {
        cancel();
        if (index >= providers.size()) {
            result.accept(LoadState.error("Não foi possível obter sua localização. Tente novamente em um local com melhor sinal.", 0));
            return;
        }
        CancellationSignal request = new CancellationSignal();
        pending = request;
        handler.postDelayed(() -> {
            if (pending == request) requestProvider(manager, providers, index + 1, result);
        }, 15000L / providers.size());
        try {
            LocationManagerCompat.getCurrentLocation(manager, providers.get(index), request,
                    ContextCompat.getMainExecutor(context), location -> {
                        if (pending != request) return;
                        if (!hasPermission(context) || !isEnabled(context)) {
                            cancel();
                            result.accept(LoadState.error("Ative e autorize a localização para encontrar lojas próximas.", 0));
                            return;
                        }
                        if (location == null || !GeoCoordinates.areValid(location.getLatitude(), location.getLongitude())) {
                            requestProvider(manager, providers, index + 1, result);
                            return;
                        }
                        cancel();
                        result.accept(LoadState.success(location));
                    });
        } catch (SecurityException | IllegalArgumentException unavailable) {
            if (pending != request) return;
            requestProvider(manager, providers, index + 1, result);
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
