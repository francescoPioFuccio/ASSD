package com.example.app1.ui.profilo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.app1.R;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Callback;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvFullName, tvEmail, tvPreferencesList;
    private LinearLayout preferencesContainer;
    private SharedPreferences prefs;


    // Sostituisci la parte di recupero dati utente in onCreate:
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPreferencesList = findViewById(R.id.tvPreferencesList);
        preferencesContainer = findViewById(R.id.preferencesContainer);
        Button btnEditFullName = findViewById(R.id.btnEditFullName);
        Button btnEditEmail = findViewById(R.id.btnEditEmail);
        Button btnEditPreferences = findViewById(R.id.btnEditPreferences);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        Long userId = prefs.getLong("user_id", -1);
        if (userId != -1) {
            loadUserFromApi(userId);
        }

        updatePreferencesList();

        btnEditFullName.setOnClickListener(v -> showEditDialog("Modifica Nome", "Nome", tvFullName, "nome"));
        btnEditEmail.setOnClickListener(v -> showEditDialog("Modifica Email", "Email", tvEmail, "email"));
        btnEditPreferences.setOnClickListener(v -> showPreferencesDialog());
    }

    // Metodo per caricare i dati utente dal backend
    private void loadUserFromApi(Long userId) {
        String url = "http://10.0.2.2:8085/usermodule3/api/users/" + userId;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).get().build();

        Log.d("ProfileActivity", "Richiesta GET inviata a: " + url);  // Log della richiesta

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("ProfileActivity", "Errore nel caricamento dati utente: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Errore nel caricamento dati utente", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("ProfileActivity", "Risposta API ricevuta: " + responseBody);  // Log della risposta
                    try {
                        JSONObject obj = new JSONObject(responseBody);
                        String nome = obj.getString("nome");
                        String cognome = obj.getString("cognome");
                        String email = obj.getString("email");

                        // Salva anche su SharedPreferences
                        prefs.edit()
                                .putString("nome", nome)
                                .putString("cognome", cognome)
                                .putString("email", email)
                                .apply();

                        runOnUiThread(() -> {
                            tvFullName.setText(nome + " " + cognome);
                            tvEmail.setText(email);
                            updatePreferencesList();  // Rinnova l'elenco delle preferenze
                        });
                    } catch (Exception ex) {
                        Log.e("ProfileActivity", "Errore nel parsing dei dati utente", ex);
                        runOnUiThread(() -> {
                            Toast.makeText(ProfileActivity.this, "Errore nel parsing dei dati utente", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e("ProfileActivity", "Risposta non riuscita, codice: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "Errore nella risposta dell'API", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // Metodo per aggiornare l'elenco delle preferenze visibili
    private void updatePreferencesList() {
        boolean[] savedPrefs = getSavedPreferences();
        StringBuilder preferencesText = new StringBuilder();

        // Aggiungi le preferenze selezionate
        if (savedPrefs[0]) preferencesText.append("Arte, ");
        if (savedPrefs[1]) preferencesText.append("Scienza, ");
        if (savedPrefs[2]) preferencesText.append("Storia, ");
        if (savedPrefs[3]) preferencesText.append("Tecnologia, ");
        if (savedPrefs[4]) preferencesText.append("Archeologia, ");
        if (savedPrefs[5]) preferencesText.append("Storia Naturale, ");
        if (savedPrefs[6]) preferencesText.append("Design, ");
        if (savedPrefs[7]) preferencesText.append("Fotografia");

        // Rimuovi l'ultima virgola e spazio
        if (preferencesText.length() > 0) {
            preferencesText.setLength(preferencesText.length() - 2);
        }

        // Impostiamo il testo nella TextView delle preferenze
        tvPreferencesList.setText(preferencesText.toString());
    }

    // Metodo per mostrare il dialogo di modifica (Nome, Email)
    private void showEditDialog(String title, String hint, TextView targetView, String prefKey) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setView(editText)
                .setPositiveButton("Salva", (dialog, which) -> {
                    String newText = editText.getText().toString();
                    targetView.setText(newText);
                    prefs.edit().putString(prefKey, newText).apply();  // Salva nei SharedPreferences
                    Toast.makeText(this, "Modifica salvata!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    // Metodo per mostrare il dialogo di modifica delle preferenze
    private void showPreferencesDialog() {
        View preferencesView = getLayoutInflater().inflate(R.layout.item_preference, null);

        // CheckBox per ogni preferenza
        CheckBox checkBoxArte = preferencesView.findViewById(R.id.checkbox_arte);
        CheckBox checkBoxScienza = preferencesView.findViewById(R.id.checkbox_scienza);
        CheckBox checkBoxStoria = preferencesView.findViewById(R.id.checkbox_storia);
        CheckBox checkBoxTecnologia = preferencesView.findViewById(R.id.checkbox_tecnologia);
        CheckBox checkBoxArcheologia = preferencesView.findViewById(R.id.checkbox_archeologia);
        CheckBox checkBoxNaturalistica = preferencesView.findViewById(R.id.checkbox_naturalistica);
        CheckBox checkBoxDesign = preferencesView.findViewById(R.id.checkbox_design);
        CheckBox checkBoxFotografia = preferencesView.findViewById(R.id.checkbox_fotografia);

        // Preimposta i checkbox in base alle preferenze salvate
        boolean[] savedPrefs = getSavedPreferences();
        checkBoxArte.setChecked(savedPrefs[0]);
        checkBoxScienza.setChecked(savedPrefs[1]);
        checkBoxStoria.setChecked(savedPrefs[2]);
        checkBoxTecnologia.setChecked(savedPrefs[3]);
        checkBoxArcheologia.setChecked(savedPrefs[4]);
        checkBoxNaturalistica.setChecked(savedPrefs[5]);
        checkBoxDesign.setChecked(savedPrefs[6]);
        checkBoxFotografia.setChecked(savedPrefs[7]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifica Preferenze")
                .setView(preferencesView)
                .setPositiveButton("Salva", (dialog, which) -> {
                    // Salva le preferenze selezionate
                    savePreferences(new boolean[]{
                            checkBoxArte.isChecked(),
                            checkBoxScienza.isChecked(),
                            checkBoxStoria.isChecked(),
                            checkBoxTecnologia.isChecked(),
                            checkBoxArcheologia.isChecked(),
                            checkBoxNaturalistica.isChecked(),
                            checkBoxDesign.isChecked(),
                            checkBoxFotografia.isChecked()
                    });
                    updatePreferencesList(); // Rinnova l'elenco delle preferenze
                    Toast.makeText(this, "Preferenze salvate!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    // Recupera le preferenze salvate
    private boolean[] getSavedPreferences() {
        return new boolean[]{
                prefs.getBoolean("pref_arte", false),
                prefs.getBoolean("pref_scienza", false),
                prefs.getBoolean("pref_storia", false),
                prefs.getBoolean("pref_tecnologia", false),
                prefs.getBoolean("pref_archeologia", false),
                prefs.getBoolean("pref_naturalistica", false),
                prefs.getBoolean("pref_design", false),
                prefs.getBoolean("pref_fotografia", false)
        };
    }

    // Salva le preferenze selezionate
    private void savePreferences(boolean[] preferences) {
        prefs.edit()
                .putBoolean("pref_arte", preferences[0])
                .putBoolean("pref_scienza", preferences[1])
                .putBoolean("pref_storia", preferences[2])
                .putBoolean("pref_tecnologia", preferences[3])
                .putBoolean("pref_archeologia", preferences[4])
                .putBoolean("pref_naturalistica", preferences[5])
                .putBoolean("pref_design", preferences[6])
                .putBoolean("pref_fotografia", preferences[7])
                .apply();
    }
}
