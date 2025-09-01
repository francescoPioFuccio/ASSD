package com.example.app1.ui.storia;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app1.databinding.ActivityHistoryBinding;
import org.json.JSONArray;
import org.json.JSONObject;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryItemClickListener {

    private ActivityHistoryBinding binding;
    private HistoryViewModel viewModel;
    private HistoryAdapter historyAdapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inizializza il ViewModel
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        // Recupera l'ID utente
        currentUserId = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("userid", null);
        Log.d("HistoryActivity", "User ID recuperato: " + currentUserId);

        if (currentUserId == null) {
            Log.e("HistoryActivity", "User ID non trovato nelle SharedPreferences");
            Toast.makeText(this, "Errore: User ID non trovato", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupRecyclerView();
        setupObservers();
        setupClickListeners();
        setupTabs();

        // Carica i dati
        viewModel.fetchUserHistory(currentUserId);
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter();
        historyAdapter.setOnHistoryItemClickListener(this);
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.historyRecyclerView.setAdapter(historyAdapter);
    }

    private void setupObservers() {
        // Osserva i dati storici
        viewModel.getHistoryData().observe(this, historyData -> {
            if (historyData != null) {
                Log.d("HistoryActivity", "Dati storici ricevuti: " + historyData.length());
                historyAdapter.updateData(historyData);

                // Mostra messaggio se non ci sono dati
                if (historyData.length() == 0) {
                    binding.emptyStateTextView.setVisibility(View.VISIBLE);
                    binding.historyRecyclerView.setVisibility(View.GONE);
                } else {
                    binding.emptyStateTextView.setVisibility(View.GONE);
                    binding.historyRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        // Osserva le statistiche utente
        viewModel.getUserStats().observe(this, stats -> {
            if (stats != null) {
                try {
                    int questTotali = stats.optInt("questTotali", 0);
                    int punteggioTotale = stats.optInt("punteggioTotale", 0);
                    int museiVisitati = stats.optInt("museiVisitati", 0);

                    binding.totalQuestsTextView.setText(String.valueOf(questTotali));
                    binding.totalPointsTextView.setText(String.valueOf(punteggioTotale));
                    binding.museumsVisitedTextView.setText(String.valueOf(museiVisitati));

                    Log.d("HistoryActivity", "Statistiche aggiornate - Quest: " + questTotali +
                            ", Punti: " + punteggioTotale + ", Musei: " + museiVisitati);
                } catch (Exception e) {
                    Log.e("HistoryActivity", "Errore nell'aggiornamento statistiche: " + e.getMessage());
                }
            }
        });

        // Osserva gli errori
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e("HistoryActivity", "Errore: " + error);
            }
        });

        // Osserva lo stato di loading
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.historyRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
                binding.statsContainer.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void setupClickListeners() {
        // Bottone torna indietro
        binding.backButton.setOnClickListener(v -> finish());

        // Bottone refresh
        binding.refreshButton.setOnClickListener(v -> {
            viewModel.fetchUserHistory(currentUserId);
        });
    }

    private void setupTabs() {
        // Setup tab per filtrare tra musei e quest
        binding.allTab.setOnClickListener(v -> {
            selectTab("all");
            viewModel.filterHistory("all");
        });

        binding.museumsTab.setOnClickListener(v -> {
            selectTab("museums");
            viewModel.filterHistory("museums");
        });

        binding.questsTab.setOnClickListener(v -> {
            selectTab("quests");
            viewModel.filterHistory("quests");
        });

        // Seleziona il tab "Tutti" per default
        selectTab("all");
    }

    private void selectTab(String selectedTab) {
        // Reset tutti i tab
        binding.allTab.setSelected(false);
        binding.museumsTab.setSelected(false);
        binding.questsTab.setSelected(false);

        // Seleziona il tab corrente
        switch (selectedTab) {
            case "all":
                binding.allTab.setSelected(true);
                break;
            case "museums":
                binding.museumsTab.setSelected(true);
                break;
            case "quests":
                binding.questsTab.setSelected(true);
                break;
        }
    }

    @Override
    public void onMuseumClick(String museoId, String nomeMuseo) {
        Log.d("HistoryActivity", "Click su museo: " + nomeMuseo + " (ID: " + museoId + ")");

        // Qui puoi aprire un'activity di dettaglio del museo o mostrare più informazioni
        Toast.makeText(this, "Dettagli museo: " + nomeMuseo, Toast.LENGTH_SHORT).show();

        // Esempio: Intent verso MuseumDetailActivity
        // Intent intent = new Intent(this, MuseumDetailActivity.class);
        // intent.putExtra("museum_id", museoId);
        // intent.putExtra("museum_name", nomeMuseo);
        // startActivity(intent);
    }

    @Override
    public void onQuestClick(String questId, JSONObject questData) {
        Log.d("HistoryActivity", "Click su quest: " + questId);

        try {
            String titoloQuest = questData.optString("titoloQuest", "Quest");
            int punteggio = questData.optInt("punteggio", 0);

            // Mostra dettagli quest
            Toast.makeText(this, "Quest: " + titoloQuest + " (" + punteggio + " punti)",
                    Toast.LENGTH_SHORT).show();

            // Esempio: Intent verso QuestDetailActivity
            // Intent intent = new Intent(this, QuestDetailActivity.class);
            // intent.putExtra("quest_id", questId);
            // intent.putExtra("quest_data", questData.toString());
            // startActivity(intent);

        } catch (Exception e) {
            Log.e("HistoryActivity", "Errore nel click quest: " + e.getMessage());
        }
    }
}