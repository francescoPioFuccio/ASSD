package com.example.app1.ui.quest;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.app1.R;

public class QuestDetailsFragment extends DialogFragment {

    private static final String ARG_QUEST = "quest";

    public interface OnQuestActionListener {
        void onStartQuest(Quest quest);
        void onTakePhoto(Quest quest);
    }

    private Quest quest;
    private OnQuestActionListener listener;

    public static QuestDetailsFragment newInstance(Quest quest) {
        QuestDetailsFragment fragment = new QuestDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_QUEST, quest);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnQuestActionListener(OnQuestActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            quest = (Quest) getArguments().getSerializable(ARG_QUEST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quest_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (quest == null) {
            dismiss();
            return;
        }

        setupViews(view);
        populateData();
        setupButtons(view);
    }

    private void setupViews(View view) {
        // Il dialog occupa la maggior parte dello schermo
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void populateData() {
        View view = getView();
        if (view == null) return;

        // Titolo e informazioni base
        TextView titleView = view.findViewById(R.id.questDetailTitle);
        TextView operaView = view.findViewById(R.id.questDetailOpera);
        TextView autoreView = view.findViewById(R.id.questDetailAutore);
        TextView descrizioneView = view.findViewById(R.id.questDetailDescrizione);
        TextView categoriaView = view.findViewById(R.id.questDetailCategoria);
        TextView difficoltaView = view.findViewById(R.id.questDetailDifficolta);
        TextView puntiView = view.findViewById(R.id.questDetailPunti);
        TextView tempoView = view.findViewById(R.id.questDetailTempo);

        titleView.setText(quest.getTitoloQuest());
        operaView.setText(quest.getNomeOpera());
        autoreView.setText(quest.getAutoreOpera());
        descrizioneView.setText(quest.getDescrizioneQuest());

        // Categoria con colore
        categoriaView.setText(quest.getCategoria().toUpperCase());
        categoriaView.setBackgroundColor(Color.parseColor(quest.getCategoriaColore()));

        // Difficoltà con colore
        difficoltaView.setText(quest.getDifficolta().toUpperCase());
        difficoltaView.setBackgroundColor(Color.parseColor(quest.getDifficoltaColore()));

        puntiView.setText(String.format("Ricompensa: %d punti", quest.getPuntiRicompensa()));
        tempoView.setText(String.format("Tempo stimato: ~%d minuti", quest.getTempoStimato()));

        // Dettagli extra dell'opera
        setupOperaDetails(view);

        // Obiettivi
        setupObiettivi(view);

        // Indizi
        setupIndizzi(view);

        // Suggerimenti
        setupSuggerimenti(view);
    }

    private void setupOperaDetails(View view) {
        LinearLayout detailsContainer = view.findViewById(R.id.operaDetailsContainer);

        if (quest.getDimensioni() != null || quest.getAnno() != null ||
                quest.getTecnica() != null || quest.getProvenienza() != null) {

            TextView headerView = new TextView(getContext());
            headerView.setText("📋 Dettagli dell'Opera");
            headerView.setTextSize(16);
            headerView.setTextColor(Color.parseColor("#333333"));
            headerView.setTypeface(null, android.graphics.Typeface.BOLD);
            headerView.setPadding(0, 16, 0, 8);
            detailsContainer.addView(headerView);

            if (quest.getDimensioni() != null) {
                addDetailItem(detailsContainer, "Dimensioni", quest.getDimensioni());
            }

            if (quest.getAnno() != null) {
                addDetailItem(detailsContainer, "Anno", quest.getAnno());
            }

            if (quest.getTecnica() != null) {
                addDetailItem(detailsContainer, "Tecnica", quest.getTecnica());
            }

            if (quest.getProvenienza() != null) {
                addDetailItem(detailsContainer, "Provenienza", quest.getProvenienza());
            }
        }
    }

    private void setupObiettivi(View view) {
        LinearLayout obiettiviContainer = view.findViewById(R.id.obiettiviContainer);

        if (quest.getObiettivi() != null && !quest.getObiettivi().isEmpty()) {
            TextView headerView = new TextView(getContext());
            headerView.setText("🎯 Obiettivi della Quest");
            headerView.setTextSize(16);
            headerView.setTextColor(Color.parseColor("#333333"));
            headerView.setTypeface(null, android.graphics.Typeface.BOLD);
            headerView.setPadding(0, 16, 0, 8);
            obiettiviContainer.addView(headerView);

            for (int i = 0; i < quest.getObiettivi().size(); i++) {
                TextView obiettivoView = new TextView(getContext());
                obiettivoView.setText(String.format("%d. %s", i + 1, quest.getObiettivi().get(i)));
                obiettivoView.setTextSize(14);
                obiettivoView.setTextColor(Color.parseColor("#555555"));
                obiettivoView.setPadding(16, 4, 0, 4);
                obiettiviContainer.addView(obiettivoView);
            }
        }
    }

    private void setupIndizzi(View view) {
        LinearLayout indizziContainer = view.findViewById(R.id.indizziContainer);

        if (quest.getIndizi() != null && !quest.getIndizi().isEmpty()) {
            TextView headerView = new TextView(getContext());
            headerView.setText("💡 Indizi per Trovare l'Opera");
            headerView.setTextSize(16);
            headerView.setTextColor(Color.parseColor("#333333"));
            headerView.setTypeface(null, android.graphics.Typeface.BOLD);
            headerView.setPadding(0, 16, 0, 8);
            indizziContainer.addView(headerView);

            for (String indizio : quest.getIndizi()) {
                TextView indizioView = new TextView(getContext());
                indizioView.setText("• " + indizio);
                indizioView.setTextSize(14);
                indizioView.setTextColor(Color.parseColor("#666666"));
                indizioView.setPadding(16, 4, 0, 4);
                indizziContainer.addView(indizioView);
            }
        }
    }

    private void setupSuggerimenti(View view) {
        LinearLayout suggerimentiContainer = view.findViewById(R.id.suggerimentiContainer);

        if (quest.getSuggerimenti() != null && !quest.getSuggerimenti().isEmpty()) {
            TextView headerView = new TextView(getContext());
            headerView.setText("💭 Suggerimenti Utili");
            headerView.setTextSize(16);
            headerView.setTextColor(Color.parseColor("#333333"));
            headerView.setTypeface(null, android.graphics.Typeface.BOLD);
            headerView.setPadding(0, 16, 0, 8);
            suggerimentiContainer.addView(headerView);

            for (String suggerimento : quest.getSuggerimenti()) {
                TextView suggerimentoView = new TextView(getContext());
                suggerimentoView.setText("💭 " + suggerimento);
                suggerimentoView.setTextSize(14);
                suggerimentoView.setTextColor(Color.parseColor("#666666"));
                suggerimentoView.setPadding(16, 4, 0, 4);
                suggerimentiContainer.addView(suggerimentoView);
            }
        }
    }

    private void addDetailItem(LinearLayout container, String label, String value) {
        LinearLayout itemLayout = new LinearLayout(getContext());
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(0, 4, 0, 4);

        TextView labelView = new TextView(getContext());
        labelView.setText(label + ": ");
        labelView.setTextSize(14);
        labelView.setTextColor(Color.parseColor("#333333"));
        labelView.setTypeface(null, android.graphics.Typeface.BOLD);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f));

        TextView valueView = new TextView(getContext());
        valueView.setText(value);
        valueView.setTextSize(14);
        valueView.setTextColor(Color.parseColor("#555555"));
        valueView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f));

        itemLayout.addView(labelView);
        itemLayout.addView(valueView);
        container.addView(itemLayout);
    }

    private void setupButtons(View view) {
        Button startButton = view.findViewById(R.id.startQuestButtonDetail);
        Button photoButton = view.findViewById(R.id.takePhotoButtonDetail);
        Button closeButton = view.findViewById(R.id.closeButton);

        // Setup visibilità e azioni bottoni basata sullo stato
        if (quest.isCompletata()) {
            startButton.setVisibility(View.GONE);
            photoButton.setVisibility(View.GONE);

        } else if (quest.isAttiva()) {
            startButton.setVisibility(View.GONE);
            photoButton.setVisibility(View.VISIBLE);
            photoButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTakePhoto(quest);
                    dismiss();
                }
            });

        } else if (quest.isDisponibile()) {
            startButton.setVisibility(View.VISIBLE);
            photoButton.setVisibility(View.GONE);
            startButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStartQuest(quest);
                    dismiss();
                }
            });

        } else {
            startButton.setVisibility(View.GONE);
            photoButton.setVisibility(View.GONE);
        }

        closeButton.setOnClickListener(v -> dismiss());
    }
}