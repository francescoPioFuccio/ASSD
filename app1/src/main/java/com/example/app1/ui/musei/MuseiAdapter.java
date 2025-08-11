package com.example.app1.ui.musei;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app1.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MuseiAdapter extends RecyclerView.Adapter<MuseiAdapter.MuseoViewHolder> {

    private JSONArray museiArray;
    private OnMuseoActionListener actionListener;

    public MuseiAdapter() {
        this.museiArray = new JSONArray();
    }

    public void updateData(JSONArray musei) {
        this.museiArray = musei != null ? musei : new JSONArray();
        notifyDataSetChanged();
    }

    public void setOnMuseoActionListener(OnMuseoActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public MuseoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_museo, parent, false);
        return new MuseoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MuseoViewHolder holder, int position) {
        try {
            JSONObject museo = museiArray.getJSONObject(position);

            // Estrai dati dal JSON
            String id = museo.optString("id", "");
            String nome = museo.optString("nome", "Museo");
            String tipologia = museo.optString("tipologia", "");
            String descrizione = museo.optString("descrizione", "");
            double distanza = museo.optDouble("distanza", 0.0);
            double rating = museo.optDouble("rating", 0.0);
            boolean aperto = museo.optBoolean("aperto", true);
            boolean ingressoGratuito = museo.optBoolean("ingressoGratuito", false);

            // Imposta i dati nelle view
            holder.nomeTextView.setText(nome);
            holder.tipologiaTextView.setText(tipologia);
            holder.descrizioneTextView.setText(descrizione);

            // Formatta la distanza
            holder.distanzaTextView.setText(String.format("%.1f km", distanza));

            // Formatta il rating
            holder.ratingTextView.setText(String.format("%.1f", rating));

            // Stato apertura
            holder.statoTextView.setText(aperto ? "Aperto" : "Chiuso");
            holder.statoTextView.setTextColor(holder.itemView.getContext().getColor(
                    aperto ? android.R.color.holo_green_dark : android.R.color.holo_red_dark
            ));

            // Prezzo ingresso
            if (ingressoGratuito) {
                holder.prezzoTextView.setText("Gratuito");
                holder.prezzoTextView.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                int prezzo = museo.optInt("prezzoIngresso", 0);
                holder.prezzoTextView.setText("€" + prezzo);
                holder.prezzoTextView.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
            }

            // Imposta l'icona basata sulla tipologia
            setIconByType(holder.tipoIcon, tipologia);

            // Click listener per l'item intero (dettagli)
            holder.itemView.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onMuseoClick(id);
                }
            });

            // Click listener per il bottone preferiti
            holder.favoritiButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onFavoritiClick(id, nome);
                }
            });

            // Click listener per il bottone dettagli
            holder.vaimuseo.setOnClickListener(v -> {
                if (actionListener != null) {
                    try {
                        // Estrai coordinate dal museo se disponibili
                        double lat = museo.optDouble("latitudine", 0.0);
                        double lng = museo.optDouble("longitudine", 0.0);
                        String indirizzo = museo.optString("indirizzo", "");

                        if (lat != 0.0 && lng != 0.0) {
                            actionListener.onNavigateClick(id, nome, lat, lng, indirizzo);
                        } else {
                            // Fallback ai dettagli se non ci sono coordinate
                            actionListener.onMuseoClick(id);
                        }
                    } catch (Exception e) {
                        actionListener.onMuseoClick(id);
                    }
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setIconByType(ImageView iconView, String tipologia) {
        // Imposta icone diverse basate sulla tipologia del museo
        int iconResource;
        int colorTint;

        switch (tipologia.toLowerCase()) {
            case "arte contemporanea":
            case "arte moderna":
                iconResource = android.R.drawable.ic_menu_gallery;
                colorTint = 0xFF9C27B0; // Purple
                break;
            case "arte classica":
            case "pinacoteca":
                iconResource = android.R.drawable.star_on;
                colorTint = 0xFFFF9800; // Orange
                break;
            case "archeologia":
            case "storia":
                iconResource = android.R.drawable.ic_menu_agenda;
                colorTint = 0xFF795548; // Brown
                break;
            case "scienze naturali":
            case "natura":
                iconResource = android.R.drawable.ic_menu_compass;
                colorTint = 0xFF4CAF50; // Green
                break;
            case "mostre temporanee":
                iconResource = android.R.drawable.ic_menu_recent_history;
                colorTint = 0xFF2196F3; // Blue
                break;
            default:
                iconResource = android.R.drawable.ic_menu_info_details;
                colorTint = 0xFF607D8B; // Blue Grey
                break;
        }

        iconView.setImageResource(iconResource);
        iconView.setColorFilter(colorTint);
    }

    @Override
    public int getItemCount() {
        return museiArray.length();
    }

    // ViewHolder per gli elementi della lista
    static class MuseoViewHolder extends RecyclerView.ViewHolder {
        TextView nomeTextView;
        TextView tipologiaTextView;
        TextView descrizioneTextView;
        TextView distanzaTextView;
        TextView ratingTextView;
        TextView statoTextView;
        TextView prezzoTextView;
        ImageButton favoritiButton;
        Button vaimuseo;
        ImageView tipoIcon;


        public MuseoViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeTextView = itemView.findViewById(R.id.museoNome);
            tipologiaTextView = itemView.findViewById(R.id.museoTipologia);
            descrizioneTextView = itemView.findViewById(R.id.museoDescrizione);
            distanzaTextView = itemView.findViewById(R.id.museoDistanza);
            ratingTextView = itemView.findViewById(R.id.museoRating);
            statoTextView = itemView.findViewById(R.id.museoStato);
            prezzoTextView = itemView.findViewById(R.id.museoPrezzo);
            favoritiButton = itemView.findViewById(R.id.favoritiButton);
            vaimuseo = itemView.findViewById(R.id.vaimuseoButton);
            tipoIcon = itemView.findViewById(R.id.museoTipoIcon);
        }
    }

    // Interface per gestire le azioni sui musei
    public interface OnMuseoActionListener {
        void onMuseoClick(String museoId);
        void onFavoritiClick(String museoId, String museoNome);

        void onNavigateClick(String museoId, String museoNome, double latitudine, double longitudine, String indirizzo);
    }
}