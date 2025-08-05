package com.example.app1.ui.promotion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app1.databinding.ActivityPuntiBonusBinding;
import com.example.app1.ui.promotion.*;

import org.json.JSONArray;

public class PuntiBonusActivity extends AppCompatActivity {

    private ActivityPuntiBonusBinding binding;
    private PuntiBonusViewModel viewModel;
    private PromotionsAdapter promotionsAdapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPuntiBonusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inizializza il ViewModel
        viewModel = new ViewModelProvider(this).get(com.example.app1.ui.promotion.PuntiBonusViewModel.class);

        // Recupera l'ID utente
        currentUserId = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("userid", null);
        Log.d("PuntiBonusActivity", "User ID recuperato: " + currentUserId);

        if (currentUserId == null) {
            Log.e("PuntiBonusActivity", "User ID non trovato nelle SharedPreferences");
            Toast.makeText(this, "Errore: User ID non trovato", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupRecyclerView();
        setupObservers();
        setupClickListeners();

        // Carica i dati
        viewModel.fetchAllData(currentUserId);
    }

    private void setupRecyclerView() {
        promotionsAdapter = new PromotionsAdapter();
        binding.promotionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.promotionsRecyclerView.setAdapter(promotionsAdapter);
    }

    private void setupObservers() {
        // Osserva i punti utente
        viewModel.getUserPoints().observe(this, points -> {
            if (points != null) {
                binding.userPointsTextView.setText(points + " Punti");
                Log.d("PuntiBonusActivity", "Punti utente aggiornati: " + points);

                // Aggiorna l'adapter se abbiamo anche le promozioni
                JSONArray currentPromotions = viewModel.getPromotionsData().getValue();
                if (currentPromotions != null) {
                    promotionsAdapter.updateData(currentPromotions, points);
                }
            }
        });

        // Osserva le promozioni
        viewModel.getPromotionsData().observe(this, promotions -> {
            if (promotions != null) {
                Log.d("PuntiBonusActivity", "Promozioni ricevute: " + promotions.length());

                // Aggiorna l'adapter con i punti correnti
                Integer currentPoints = viewModel.getUserPoints().getValue();
                int points = currentPoints != null ? currentPoints : 0;
                promotionsAdapter.updateData(promotions, points);
            }
        });

        // Osserva gli errori
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e("PuntiBonusActivity", "Errore: " + error);
            }
        });

        // Osserva lo stato di loading
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.promotionsRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void setupClickListeners() {
        // Bottone torna indietro
        binding.backButton.setOnClickListener(v -> finish());
    }
}