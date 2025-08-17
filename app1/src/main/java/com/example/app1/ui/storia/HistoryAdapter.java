package com.example.app1.ui.storia;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app1.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private JSONArray historyArray;
    private OnHistoryItemClickListener clickListener;

    public interface OnHistoryItemClickListener {
        void onMuseumClick(String museoId, String nomeMuseo);
        void onQuestClick(String questId, JSONObject questData);
    }

    public HistoryAdapter() {
        this.historyArray = new JSONArray();
    }

    public void updateData(JSONArray history) {
        this.historyArray = history != null ? history : new JSONArray();
        notifyDataSetChanged();
    }

    public void setOnHistoryItemClickListener(OnHistoryItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        try {
            JSONObject historyItem = historyArray.getJSONObject(position);

            String tipo = historyItem.optString("tipo", "unknown");

            if ("museo".equals(tipo)) {
                bindMuseumItem(holder, historyItem);
            } else if ("quest".equals(tipo)) {
                bindQuestItem(holder, historyItem);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void bindMuseumItem(HistoryViewHolder holder, JSONObject museo) {
        try {
            String nomeMuseo = museo.optString("nomeMuseo", "Museo Sconosciuto");
            String dataVisita = museo.optString("dataVisita", "Data non disponibile");
            String citta = museo.optString("citta", "");
            int questCompletate = museo.optInt("questCompletate", 0);

            holder.titleTextView.setText(nomeMuseo);
            holder.subtitleTextView.setText(citta);
            holder.dateTextView.setText("Visitato il: " + dataVisita);
            holder.detailsTextView.setText(questCompletate + " quest completate");

            // Icona museo
            holder.iconImageView.setImageResource(R.drawable.ic_museum);
            holder.iconImageView.setVisibility(View.VISIBLE);

            // Background per musei
            holder.itemView.setBackgroundResource(R.drawable.museum_item_background);

            // Click listener per museo
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    String museoId = museo.optString("museoId", "");
                    clickListener.onMuseumClick(museoId, nomeMuseo);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindQuestItem(HistoryViewHolder holder, JSONObject quest) {
        try {
            String titoloQuest = quest.optString("titoloQuest", "Quest Sconosciuta");
            String nomeMuseo = quest.optString("nomeMuseo", "Museo non specificato");
            String dataCompletamento = quest.optString("dataCompletamento", "Data non disponibile");
            int punteggio = quest.optInt("punteggio", 0);
            String difficolta = quest.optString("difficolta", "media");

            holder.titleTextView.setText(titoloQuest);
            holder.subtitleTextView.setText("presso " + nomeMuseo);
            holder.dateTextView.setText("Completata il: " + dataCompletamento);
            holder.detailsTextView.setText(punteggio + " punti • Difficoltà: " + difficolta);

            // Icona quest con colore basato sulla difficoltà
            holder.iconImageView.setVisibility(View.VISIBLE);
            switch (difficolta.toLowerCase()) {
                case "facile":
                    holder.iconImageView.setImageResource(R.drawable.ic_quest_easy);
                    break;
                case "media":
                    holder.iconImageView.setImageResource(R.drawable.ic_quest_medium);
                    break;
                case "difficile":
                    holder.iconImageView.setImageResource(R.drawable.ic_quest_hard);
                    break;
                default:
                    holder.iconImageView.setImageResource(R.drawable.ic_quest_default);
            }

            // Background per quest
            holder.itemView.setBackgroundResource(R.drawable.quest_item_background);

            // Click listener per quest
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    String questId = quest.optString("questId", "");
                    clickListener.onQuestClick(questId, quest);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return historyArray.length();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView subtitleTextView;
        TextView dateTextView;
        TextView detailsTextView;
        ImageView iconImageView;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.historyTitle);
            subtitleTextView = itemView.findViewById(R.id.historySubtitle);
            dateTextView = itemView.findViewById(R.id.historyDate);
            detailsTextView = itemView.findViewById(R.id.historyDetails);
            iconImageView = itemView.findViewById(R.id.historyIcon);
        }
    }
}