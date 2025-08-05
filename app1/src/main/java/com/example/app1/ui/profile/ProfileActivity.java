package com.example.app1.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.app1.databinding.ActivityProfileBinding;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private ProfileViewModel profileViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inizializza il ViewModel
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        String userId = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("userid", null);
        Log.d("ProfileActivity", "User ID recuperato: " + userId);

        if (userId != null) {
            // Chiamata al ViewModel per recuperare il profilo
            Log.e("ProfileActivity", "User ID  trovato nelle SharedPreferences");
            profileViewModel.fetchProfile(userId);
        } else {
            Log.e("ProfileActivity", "User ID non trovato nelle SharedPreferences");
            binding.profileNameTextView.setText("Errore: User ID non trovato");
            binding.profileEmailTextView.setVisibility(View.GONE);
            binding.profileIdTextView.setVisibility(View.GONE);
        }

        // Osserva i cambiamenti di loading


        // Osserva i dati del profilo (JSONObject)
        profileViewModel.getProfileData().observe(this, jsonResponse -> {
            try {

                binding.profileNameTextView.setText("Nome: " + jsonResponse.getString("nome"));
                binding.profileEmailTextView.setText("Email: " + jsonResponse.getString("email"));
                binding.profileIdTextView.setText("ID Utente: " + jsonResponse.getString("id"));

                Log.d("ProfileActivity", "Profilo recuperato con successo: " + jsonResponse.toString());
                Log.d("ProfileActivity", "Nome utente: " + jsonResponse.getString("nome"));
                Log.d("ProfileActivity", "Email utente: " + jsonResponse.getString("email"));
            } catch (JSONException e) {
                Log.e("ProfileActivity", "Errore nel parsing del JSON", e);
            }
        });

        // Osserva eventuali errori


        // Imposta il listener per il bottone "Torna Indietro"
        binding.backButton.setOnClickListener(v -> finish());
    }
}
