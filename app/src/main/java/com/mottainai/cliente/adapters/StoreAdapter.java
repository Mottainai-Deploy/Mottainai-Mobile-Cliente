package com.mottainai.cliente.adapters;

import android.view.LayoutInflater;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mottainai.cliente.R;
import com.mottainai.cliente.models.PartnerStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Representa StoreAdapter, responsável por a apresentação dos itens em uma lista. */
public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.StoreViewHolder> {

    /** Define o contrato de OnStoreClickListener, responsável por a apresentação dos itens em uma lista. */
    public interface OnStoreClickListener {
        /** Notifica a seleção da loja para continuar sua navegação. */
        void onStoreClick(PartnerStore store);
    }

    private final List<PartnerStore> stores = new ArrayList<>();
    private final OnStoreClickListener listener;
    private Double latitude;
    private Double longitude;

    /** Inicializa StoreAdapter com os dados e as dependências recebidos. */
    public StoreAdapter(OnStoreClickListener listener) {
        this.listener = listener;
    }

    /** Substitui as lojas apresentadas pelo componente. */
    public void setStores(List<PartnerStore> newStores) {
        stores.clear();
        stores.addAll(newStores);
        notifyDataSetChanged();
    }

    /** Atualiza a posição de referência usada no cálculo das distâncias. */
    public void setOrigin(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Cria o componente visual usado para exibir um item da lista. */
    @NonNull
    @Override
    public StoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store, parent, false);
        return new StoreViewHolder(view);
    }

    /** Preenche o componente da lista com os dados do item na posição solicitada. */
    @Override
    public void onBindViewHolder(@NonNull StoreViewHolder holder, int position) {
        PartnerStore store = stores.get(position);
        holder.name.setText(store.getName());
        String address = store.getAddressLabel().isEmpty() ? "Endereço indisponível" : store.getAddressLabel();
        if (latitude != null && longitude != null && store.hasValidLocation()) {
            float[] distance = new float[1];
            Location.distanceBetween(latitude, longitude, store.getLatitude(), store.getLongitude(), distance);
            address = String.format(Locale.forLanguageTag("pt-BR"), "Aprox. %.1f km · %s", distance[0] / 1000, address);
        }
        holder.meta.setText(address);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStoreClick(store);
            }
        });
    }

    /** Retorna a quantidade de itens que a lista pode exibir. */
    @Override
    public int getItemCount() {
        return stores.size();
    }

    /** Representa StoreViewHolder, responsável por a apresentação dos itens em uma lista. */
    static class StoreViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView meta;

        /** Inicializa StoreViewHolder com os dados e as dependências recebidos. */
        StoreViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_store_name);
            meta = itemView.findViewById(R.id.tv_store_meta);
        }
    }
}
