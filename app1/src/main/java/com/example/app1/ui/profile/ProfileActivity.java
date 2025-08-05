package com.example.app1.ui.profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.app1.databinding.ActivityProfileBinding;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private String currentUserId;
    private JSONObject currentUserData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inizializza il ViewModel
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        currentUserId = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("userid", null);
        Log.d("ProfileActivity", "User ID recuperato: " + currentUserId);

        if (currentUserId != null) {
            // Chiamata al ViewModel per recuperare il profilo
            Log.e("ProfileActivity", "User ID trovato nelle SharedPreferences");
            profileViewModel.fetchProfile(currentUserId);
        } else {
            Log.e("ProfileActivity", "User ID non trovato nelle SharedPreferences");
            binding.profileNameTextView.setText("Errore: User ID non trovato");
            binding.profileEmailTextView.setVisibility(View.GONE);
            binding.profileIdTextView.setVisibility(View.GONE);
        }

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        // Osserva i dati del profilo (JSONObject)
        profileViewModel.getProfileData().observe(this, jsonResponse -> {
            try {
                currentUserData = jsonResponse;
                updateUI(jsonResponse);
                Log.d("ProfileActivity", "Profilo recuperato con successo: " + jsonResponse.toString());
            } catch (JSONException e) {
                Log.e("ProfileActivity", "Errore nel parsing del JSON", e);
            }
        });

        // Osserva eventuali errori
        profileViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        // Osserva messaggi di successo
        profileViewModel.getSuccessMessage().observe(this, success -> {
            if (success != null) {
                Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
            }
        });

        // Osserva lo stato di loading
        profileViewModel.getIsLoading().observe(this, isLoading -> {
            // Potresti aggiungere una progress bar qui se necessario
            binding.backButton.setEnabled(!isLoading);
        });
    }

    private void updateUI(JSONObject jsonResponse) throws JSONException {
        binding.profileNameTextView.setText("Nome: " + jsonResponse.getString("nome"));
        binding.profileSurnameTextView.setText("Cognome: " + jsonResponse.getString("cognome"));
        binding.profileEmailTextView.setText("Email: " + jsonResponse.getString("email"));
        binding.profileIdTextView.setText("ID Utente: " + jsonResponse.getString("id"));

        // Gestisci le preferenze (array)
        if (jsonResponse.has("museoPreferito")) {
            JSONArray preferenze = jsonResponse.getJSONArray("museoPreferito");
            StringBuilder preferencesText = new StringBuilder("Preferenze: ");
            for (int i = 0; i < preferenze.length(); i++) {
                if (i > 0) preferencesText.append(", ");
                preferencesText.append(preferenze.getString(i));
            }
            binding.profilePreferencesTextView.setText(preferencesText.toString());
        }
    }

    private void setupClickListeners() {
        // Click listener per modificare il nome
        binding.editNameIcon.setOnClickListener(v -> showEditDialog("Nome", "nome", currentUserData.optString("nome")));

        // Click listener per modificare il cognome
        binding.editSurnameIcon.setOnClickListener(v -> showEditDialog("Cognome", "cognome", currentUserData.optString("cognome")));

        // Click listener per modificare l'email
        binding.editEmailIcon.setOnClickListener(v -> showEditDialog("Email", "email", currentUserData.optString("email")));

        // Click listener per modificare le preferenze
        binding.editPreferencesIcon.setOnClickListener(v -> showPreferencesDialog());

        // Imposta il listener per il bottone "Torna Indietro"
        binding.backButton.setOnClickListener(v -> finish());
    }

    private void showEditDialog(String title, String field, String currentValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifica " + title);

        // Crea un EditText per l'input
        final EditText input = new EditText(this);
        input.setText(currentValue);
        builder.setView(input);

        builder.setPositiveButton("Salva", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty()) {
                updateUserField(field, newValue);
            } else {
                Toast.makeText(this, title + " non può essere vuoto", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showPreferencesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifica Preferenze Museo");

        // Opzioni disponibili
        String[] options = {"Arte", "Scienza", "Storia", "Tecnologia", "Natura"};

        // Ottieni le preferenze attuali
        List<String> currentPreferences = new ArrayList<>();
        try {
            if (currentUserData.has("museoPreferito")) {
                JSONArray prefs = currentUserData.getJSONArray("museoPreferito");
                for (int i = 0; i < prefs.length(); i++) {
                    currentPreferences.add(prefs.getString(i));
                }
            }
        } catch (JSONException e) {
            Log.e("ProfileActivity", "Errore nel leggere le preferenze attuali", e);
        }

        // Crea layout con checkbox
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String option : options) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(option);
            checkBox.setChecked(currentPreferences.contains(option));
            checkBoxes.add(checkBox);
            layout.addView(checkBox);
        }

        builder.setView(layout);

        builder.setPositiveButton("Salva", (dialog, which) -> {
            List<String> selectedPreferences = new ArrayList<>();
            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isChecked()) {
                    selectedPreferences.add(checkBox.getText().toString());
                }
            }

            if (!selectedPreferences.isEmpty()) {
                updateUserPreferences(selectedPreferences);
            } else {
                Toast.makeText(this, "Seleziona almeno una preferenza", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUserField(String field, String newValue) {
        if (currentUserData == null) return;

        try {
            String nome = currentUserData.getString("nome");
            String cognome = currentUserData.getString("cognome");
            String email = currentUserData.getString("email");
            JSONArray preferenze = currentUserData.optJSONArray("museoPreferito");

            // Aggiorna il campo specifico
            switch (field) {
                case "nome":
                    nome = newValue;
                    break;
                case "cognome":
                    cognome = newValue;
                    break;
                case "email":
                    email = newValue;
                    break;
            }

            // Invia la richiesta di aggiornamento
            profileViewModel.updateProfile(currentUserId, nome, cognome, email, preferenze);

        } catch (JSONException e) {
            Log.e("ProfileActivity", "Errore nell'aggiornamento del campo: " + field, e);
            Toast.makeText(this, "Errore nell'aggiornamento", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserPreferences(List<String> selectedPreferences) {
        if (currentUserData == null) return;

        try {
            String nome = currentUserData.getString("nome");
            String cognome = currentUserData.getString("cognome");
            String email = currentUserData.getString("email");

            // Converti la lista in JSONArray
            JSONArray preferencesArray = new JSONArray();
            for (String pref : selectedPreferences) {
                preferencesArray.put(pref);
            }

            // Invia la richiesta di aggiornamento
            profileViewModel.updateProfile(currentUserId, nome, cognome, email, preferencesArray);

        } catch (JSONException e) {
            Log.e("ProfileActivity", "Errore nell'aggiornamento delle preferenze", e);
            Toast.makeText(this, "Errore nell'aggiornamento delle preferenze", Toast.LENGTH_SHORT).show();
        }
    }
}