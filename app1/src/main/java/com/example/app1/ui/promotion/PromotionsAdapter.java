package com.example.app1.ui.promotion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app1.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PromotionsAdapter extends RecyclerView.Adapter<PromotionsAdapter.PromotionViewHolder> {

    private JSONArray promotionsArray;
    private int userPoints;

    public PromotionsAdapter() {
        this.promotionsArray = new JSONArray();
        this.userPoints = 0;
    }

    public void updateData(JSONArray promotions, int userPoints) {
        this.promotionsArray = promotions != null ? promotions : new JSONArray();
        this.userPoints = userPoints;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        try {
            JSONObject promotion = promotionsArray.getJSONObject(position);

            // Estrai dati dal JSON
            String titolo = promotion.optString("titolo", "Promozione");
            int puntiNecessari = promotion.optInt("puntiNecessari", 0);
            String sconto = promotion.optString("sconto", "Sconto disponibile");

            // Imposta i dati nelle view
            holder.titleTextView.setText(titolo);
            holder.pointsRequiredTextView.setText(String.valueOf(puntiNecessari));
            holder.discountTextView.setText("Sconto: " + sconto);

            // Gestione disponibilità della promozione
            if (userPoints >= puntiNecessari) {
                // Promozione disponibile
                holder.pointsMissingTextView.setVisibility(View.GONE);
                holder.redeemButton.setVisibility(View.VISIBLE);
                holder.redeemButton.setText("Riscatta");
                holder.redeemButton.setEnabled(true);
                holder.pointsRequiredTextView.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                // Promozione non disponibile
                int puntiMancanti = puntiNecessari - userPoints;
                holder.pointsMissingTextView.setText("Ti mancano " + puntiMancanti);
                holder.pointsMissingTextView.setVisibility(View.VISIBLE);
                holder.redeemButton.setVisibility(View.GONE);
                holder.pointsRequiredTextView.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
            }

            // Click listener per il bottone riscatta
            holder.redeemButton.setOnClickListener(v -> {
                // Qui puoi implementare la logica per riscattare la promozione
                // Per esempio, fare una chiamata al server per riscattare
                // onPromotionRedeem(promotion);
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return promotionsArray.length();
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView discountTextView;
        TextView pointsRequiredTextView;
        TextView pointsMissingTextView;
        Button redeemButton;
        ImageView typeIcon;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.promotionTitle);
            discountTextView = itemView.findViewById(R.id.promotionDiscount);
            pointsRequiredTextView = itemView.findViewById(R.id.promotionPointsRequired);
            pointsMissingTextView = itemView.findViewById(R.id.pointsMissingText);
            redeemButton = itemView.findViewById(R.id.redeemButton);
            typeIcon = itemView.findViewById(R.id.promotionTypeIcon);
        }
    }
}