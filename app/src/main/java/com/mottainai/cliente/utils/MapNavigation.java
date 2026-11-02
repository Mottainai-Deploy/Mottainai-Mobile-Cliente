package com.mottainai.cliente.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.mottainai.cliente.models.PartnerStore;

/** Representa MapNavigation, responsável por as operações auxiliares deste recurso. */
public final class MapNavigation {
    /** Impede a criação de instâncias desta classe utilitária. */
    private MapNavigation() { }

    /** Abre a rota para a loja selecionada no aplicativo de mapas disponível. */
    public static void openRoute(Context context, PartnerStore store) {
        String url = GoogleMapsUrl.forStore(store);
        if (url == null) {
            Toast.makeText(context, "Esta loja ainda não possui coordenadas ou endereço completo para abrir no mapa.", Toast.LENGTH_LONG).show();
            return;
        }
        Uri destination = Uri.parse(url);
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, destination)
                    .setPackage("com.google.android.apps.maps"));
        } catch (ActivityNotFoundException | SecurityException exception) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, destination));
            } catch (ActivityNotFoundException | SecurityException unavailable) {
                Toast.makeText(context, "Não foi possível abrir o Google Maps. Confira se há um navegador disponível.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
