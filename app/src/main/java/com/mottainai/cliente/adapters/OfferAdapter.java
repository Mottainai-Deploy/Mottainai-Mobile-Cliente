package com.mottainai.cliente.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mottainai.cliente.R;
import com.mottainai.cliente.models.Offer;
import com.mottainai.cliente.utils.ProductImages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Representa OfferAdapter, responsável por a apresentação dos itens em uma lista. */
public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    /** Define o contrato de OnOfferClickListener, responsável por a apresentação dos itens em uma lista. */
    public interface OnOfferClickListener {
        /** Notifica a seleção da oferta para abrir seus detalhes. */
        void onOfferClick(Offer offer);
    }

    private final List<Offer> offers = new ArrayList<>();
    private final OnOfferClickListener listener;
    private final boolean compact;

    /** Inicializa OfferAdapter com os dados e as dependências recebidos. */
    public OfferAdapter(OnOfferClickListener listener) {
        this(listener, false);
    }

    /** Inicializa OfferAdapter com os dados e as dependências recebidos. */
    public OfferAdapter(OnOfferClickListener listener, boolean compact) {
        this.listener = listener;
        this.compact = compact;
    }

    /** Substitui as ofertas exibidas pela lista. */
    public void setOffers(List<Offer> newOffers) {
        offers.clear();
        offers.addAll(newOffers);
        notifyDataSetChanged();
    }

    /** Cria o componente visual usado para exibir um item da lista. */
    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                compact ? R.layout.item_offer_compact : R.layout.item_offer, parent, false);
        return new OfferViewHolder(view);
    }

    /** Preenche o componente da lista com os dados do item na posição solicitada. */
    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer offer = offers.get(position);
        bind(holder.itemView, offer);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOfferClick(offer);
            }
        });
    }

    /** Limpa os recursos do item antes de reutilizá-lo em outra posição da lista. */
    @Override
    public void onViewRecycled(@NonNull OfferViewHolder holder) {
        ProductImages.clear(holder.itemView.findViewById(R.id.view_offer_thumb));
        super.onViewRecycled(holder);
    }

    /** Retorna a quantidade de itens que a lista pode exibir. */
    @Override
    public int getItemCount() {
        return offers.size();
    }

    /** As duas apresentações vinculam o mesmo modelo da API e as visualizações de metadados compartilhadas. */
    public static void bind(View itemView, Offer offer) {
        ImageView thumb = itemView.findViewById(R.id.view_offer_thumb);
        TextView badgeLabel = itemView.findViewById(R.id.tv_offer_badge_label);
        TextView discount = itemView.findViewById(R.id.tv_offer_discount);
        TextView title = itemView.findViewById(R.id.tv_offer_title);
        TextView store = itemView.findViewById(R.id.tv_offer_store);
        TextView expiry = itemView.findViewById(R.id.tv_offer_expiry);

        ProductImages.load(thumb, offer, itemView.findViewById(R.id.product_image_attribution));

        if (offer.getBadgeLabel() != null) {
            badgeLabel.setVisibility(View.VISIBLE);
            badgeLabel.setText(offer.getBadgeLabel());
        } else {
            badgeLabel.setVisibility(View.GONE);
        }

        if (offer.isCatalogOffer()) {
            discount.setVisibility(offer.getPriceLabel() == null ? View.GONE : View.VISIBLE);
            discount.setText(offer.getPriceLabel());
        } else {
            discount.setVisibility(View.VISIBLE);
            discount.setText(String.format(Locale.getDefault(), "-%d%%", offer.getDiscountPercent()));
        }
        title.setText(offer.getTitle());
        store.setText(offer.isCatalogOffer() ? offer.getStoreName()
                : String.format(Locale.getDefault(), "%s · %.1f km",
                offer.getStoreName(), offer.getDistanceKm()));
        expiry.setText(offer.getExpiryLabel());
    }

    /** Representa OfferViewHolder, responsável por a apresentação dos itens em uma lista. */
    static class OfferViewHolder extends RecyclerView.ViewHolder {
        /** Inicializa OfferViewHolder com os dados e as dependências recebidos. */
        OfferViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
