package com.example.app1.ui.quest;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app1.R;
import com.example.app1.ui.quest.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.QuestViewHolder> {

    private static final String TAG = "QuestAdapter";
    private List<Quest> questList;
    private final Context context;
    private final OnQuestActionListener listener;

    public interface OnQuestActionListener {
        void onStartQuest(Quest quest);
        void onTakePhoto(Quest quest);
        void onViewDetails(Quest quest);
    }

    public QuestAdapter(Context context, OnQuestActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.questList = new ArrayList<>();
    }

    public void updateQuestList(List<Quest> newQuestList) {
        this.questList = newQuestList != null ? new ArrayList<>(newQuestList) : new ArrayList<>();
        notifyDataSetChanged();
        Log.d(TAG, "Quest list aggiornata: " + questList.size() + " quest");
    }

    @NonNull
    @Override
    public QuestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quest_card, parent, false);
        return new QuestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestViewHolder holder, int position) {
        Quest quest = questList.get(position);
        holder.bind(quest);
    }

    @Override
    public int getItemCount() {
        return questList.size();
    }

    class QuestViewHolder extends RecyclerView.ViewHolder {

        // Views dal layout
        private final ImageView questTypeIcon;
        private final TextView questTitolo;
        private final TextView questOpera;
        private final TextView questAutore;
        private final TextView questCategoria;
        private final TextView questDifficolta;
        private final TextView questDescrizione;
        private final TextView questPunti;
        private final TextView questTempo;
        private final TextView questStato;
        private final LinearLayout indizziContainer;
        private final Button startQuestButton;
        private final ImageButton takePhotoButton;
        private final Button viewDetailsButton;
        private final LinearLayout actionContainer;

        public QuestViewHolder(@NonNull View itemView) {
            super(itemView);

            // Inizializza le views
            questTypeIcon = itemView.findViewById(R.id.questTipoIcon);
            questTitolo = itemView.findViewById(R.id.questTitolo);
            questOpera = itemView.findViewById(R.id.questOpera);
            questAutore = itemView.findViewById(R.id.questAutore);
            questCategoria = itemView.findViewById(R.id.questCategoria);
            questDifficolta = itemView.findViewById(R.id.questDifficolta);
            questDescrizione = itemView.findViewById(R.id.questDescrizione);
            questPunti = itemView.findViewById(R.id.questPunti);
            questTempo = itemView.findViewById(R.id.questTempo);
            questStato = itemView.findViewById(R.id.questStato);
            indizziContainer = itemView.findViewById(R.id.indizziContainer);
            startQuestButton = itemView.findViewById(R.id.startQuestButton);
            takePhotoButton = itemView.findViewById(R.id.takePhotoButton);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            actionContainer = itemView.findViewById(R.id.actionContainer);
        }

        public void bind(Quest quest) {
            // Informazioni base
            questTitolo.setText(quest.getTitoloQuest());
            questOpera.setText(quest.getNomeOpera());
            questAutore.setText(quest.getAutoreOpera());
            questDescrizione.setText(quest.getDescrizioneQuest());

            // Categoria con colore
            questCategoria.setText(quest.getCategoria().toUpperCase());
            questCategoria.setBackgroundColor(Color.parseColor(quest.getCategoriaColore()));

            // Difficoltà con colore
            questDifficolta.setText(quest.getDifficolta().toUpperCase());
            questDifficolta.setBackgroundColor(Color.parseColor(quest.getDifficoltaColore()));

            // Punti e tempo
            questPunti.setText(String.format(Locale.getDefault(), "%d pt", quest.getPuntiRicompensa()));
            questTempo.setText(String.format(Locale.getDefault(), "~%d min", quest.getTempoStimato()));

            // Icona tipo quest
            setQuestTypeIcon(quest);

            // Stato della quest
            setupQuestStatus(quest);

            // Indizi (mostra solo i primi 2 per salvare spazio)
            setupIndizzi(quest);

            // Setup bottoni azioni
            setupActionButtons(quest);
        }

        private void setQuestTypeIcon(Quest quest) {
            // Imposta icona basata sulla categoria
            int iconRes;
            switch (quest.getCategoria().toLowerCase()) {
                case "arte":
                case "rinascimento":
                case "impressionismo":
                    iconRes = android.R.drawable.star_on; // Icona arte
                    break;
                case "scienza":
                case "paleontologia":
                    iconRes = android.R.drawable.ic_dialog_info; // Icona scienza
                    break;
                case "storia":
                case "archeologia":
                    iconRes = android.R.drawable.ic_dialog_dialer; // Icona storia
                    break;
                case "tecnologia":
                    iconRes = android.R.drawable.ic_menu_manage; // Icona tech
                    break;
                default:
                    iconRes = android.R.drawable.ic_dialog_map; // Icona generica
                    break;
            }

            questTypeIcon.setImageResource(iconRes);
            questTypeIcon.setBackgroundColor(Color.parseColor(quest.getCategoriaColore()));
        }

        private void setupQuestStatus(Quest quest) {
            if (quest.isCompletata()) {
                questStato.setText("COMPLETATA");
                questStato.setBackgroundColor(Color.parseColor("#4CAF50")); // Verde
                questStato.setTextColor(Color.WHITE);
            } else if (quest.isAttiva()) {
                questStato.setText("ATTIVA");
                questStato.setBackgroundColor(Color.parseColor("#2196F3")); // Blu
                questStato.setTextColor(Color.WHITE);
            } else if (quest.isDisponibile()) {
                questStato.setText("DISPONIBILE");
                questStato.setBackgroundColor(Color.parseColor("#FF9800")); // Arancione
                questStato.setTextColor(Color.WHITE);
            } else {
                questStato.setText("NON DISPONIBILE");
                questStato.setBackgroundColor(Color.parseColor("#9E9E9E")); // Grigio
                questStato.setTextColor(Color.WHITE);
            }
        }

        private void setupIndizzi(Quest quest) {
            indizziContainer.removeAllViews();

            if (quest.getIndizi() != null && !quest.getIndizi().isEmpty()) {
                // Mostra massimo 2 indizi per non appesantire la card
                int maxIndizi = Math.min(2, quest.getIndizi().size());

                for (int i = 0; i < maxIndizi; i++) {
                    TextView indizioView = new TextView(context);
                    indizioView.setText("💡 " + quest.getIndizi().get(i));
                    indizioView.setTextSize(12);
                    indizioView.setTextColor(Color.parseColor("#666666"));
                    indizioView.setPadding(0, 4, 0, 4);
                    indizziContainer.addView(indizioView);
                }

                // Se ci sono più indizi, mostra un hint
                if (quest.getIndizi().size() > 2) {
                    TextView moreHint = new TextView(context);
                    moreHint.setText(String.format(Locale.getDefault(),
                            "... e altri %d indizi", quest.getIndizi().size() - 2));
                    moreHint.setTextSize(10);
                    moreHint.setTextColor(Color.parseColor("#999999"));
                    moreHint.setTypeface(null, android.graphics.Typeface.ITALIC);
                    indizziContainer.addView(moreHint);
                }
            }
        }

        private void setupActionButtons(Quest quest) {
            // Reset visibilità
            startQuestButton.setVisibility(View.GONE);
            takePhotoButton.setVisibility(View.GONE);
            viewDetailsButton.setVisibility(View.VISIBLE); // Sempre visibile

            if (quest.isCompletata()) {
                // Quest completata - solo dettagli
                viewDetailsButton.setText("COMPLETATA");
                viewDetailsButton.setEnabled(false);
                viewDetailsButton.setBackgroundColor(Color.parseColor("#4CAF50"));

            } else if (quest.isAttiva()) {
                // Quest attiva - mostra foto e dettagli
                takePhotoButton.setVisibility(View.VISIBLE);
                viewDetailsButton.setText("Dettagli");
                viewDetailsButton.setEnabled(true);

                // Setup foto button
                takePhotoButton.setOnClickListener(v -> {
                    Log.d(TAG, "Foto clicked per quest: " + quest.getIdQuest());
                    if (listener != null) {
                        listener.onTakePhoto(quest);
                    }
                });

            } else if (quest.isDisponibile()) {
                // Quest disponibile - mostra start e dettagli
                startQuestButton.setVisibility(View.VISIBLE);
                viewDetailsButton.setText("Dettagli");
                viewDetailsButton.setEnabled(true);

                // Setup start button
                startQuestButton.setOnClickListener(v -> {
                    Log.d(TAG, "Start clicked per quest: " + quest.getIdQuest());
                    if (listener != null) {
                        listener.onStartQuest(quest);
                    }
                });

            } else {
                // Quest non disponibile
                viewDetailsButton.setText("Non Disponibile");
                viewDetailsButton.setEnabled(false);
                viewDetailsButton.setBackgroundColor(Color.parseColor("#9E9E9E"));
            }

            // Setup dettagli button (sempre attivo se la quest non è completata)
            if (!quest.isCompletata()) {
                viewDetailsButton.setOnClickListener(v -> {
                    Log.d(TAG, "Dettagli clicked per quest: " + quest.getIdQuest());
                    if (listener != null) {
                        listener.onViewDetails(quest);
                    }
                });
            }
        }
    }
}