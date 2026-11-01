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

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    public interface OnOfferClickListener {
        void onOfferClick(Offer offer);
    }

    private final List<Offer> offers = new ArrayList<>();
    private final OnOfferClickListener listener;
    private final boolean compact;

    public OfferAdapter(OnOfferClickListener listener) {
        this(listener, false);
    }

    public OfferAdapter(OnOfferClickListener listener, boolean compact) {
        this.listener = listener;
        this.compact = compact;
    }

    public void setOffers(List<Offer> newOffers) {
        offers.clear();
        offers.addAll(newOffers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                compact ? R.layout.item_offer_compact : R.layout.item_offer, parent, false);
        return new OfferViewHolder(view);
    }

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

    @Override
    public void onViewRecycled(@NonNull OfferViewHolder holder) {
        ProductImages.clear(holder.itemView.findViewById(R.id.view_offer_thumb));
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return offers.size();
    }

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

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        OfferViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
