package com.example.app1.ui.quest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.app1.R;

public class QuestActivity extends AppCompatActivity {

    private static final String TAG = "QuestActivity";

    private TextView welcomeTextView;
    private TextView museumNameTextView;
    private TextView userIdTextView;
    private Button startQuestButton;
    private Button backButton;

    private String museumName;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest);

        Log.d(TAG, "QuestActivity avviata");

        // Inizializza le view
        initViews();

        // Recupera i dati dall'intent
        getIntentData();

        // Configura i listener
        setupClickListeners();

        // Mostra i dati
        displayQuestInfo();
    }

    private void initViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        museumNameTextView = findViewById(R.id.museumNameTextView);
        userIdTextView = findViewById(R.id.userIdTextView);
        startQuestButton = findViewById(R.id.startQuestButton);
        backButton = findViewById(R.id.backButton);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            museumName = intent.getStringExtra("museum_name");
            userId = intent.getStringExtra("user_id");

            Log.d(TAG, "Ricevuti dati: Museo=" + museumName + ", UserId=" + userId);
        }
    }

    private void displayQuestInfo() {
        // Mostra messaggio di benvenuto
        welcomeTextView.setText("🎉 Benvenuto al Museo! 🎉");

        // Mostra il nome del museo
        if (museumName != null && !museumName.isEmpty()) {
            museumNameTextView.setText("📍 " + museumName);
        } else {
            museumNameTextView.setText("📍 Museo");
        }

        // Mostra l'ID utente (per debug)
        if (userId != null && !userId.isEmpty()) {
            userIdTextView.setText("👤 User ID: " + userId);
        } else {
            userIdTextView.setText("👤 Utente");
        }

        // Toast di conferma
        Toast.makeText(this, "Geofence funzionante! Sei arrivato al museo! 🎯", Toast.LENGTH_LONG).show();
    }

    private void setupClickListeners() {
        // Bottone per iniziare le quest
        startQuestButton.setOnClickListener(v -> {
            Toast.makeText(this, "Quest avviate! (Funzionalità fittizia)", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Quest simulate avviate per museo: " + museumName);

            // Qui potrai aggiungere la logica reale per avviare le quest
            // Per ora mostriamo solo un messaggio
            simulateQuestStart();
        });

        // Bottone per tornare indietro
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Tornando alla schermata precedente");
            finish(); // Chiude l'activity e torna alla precedente
        });
    }

    private void simulateQuestStart() {
        // Simula l'avvio di una quest fittizia
        String questTitle = "Quest: Esplora il " + (museumName != null ? museumName : "museo");
        String questDescription = "Benvenuto! La tua avventura inizia ora.\n\n" +
                "🔍 Esplora le sale del museo\n" +
                "🎯 Trova gli oggetti nascosti\n" +
                "📚 Scopri la storia dietro ogni reperto\n" +
                "🏆 Guadagna punti bonus!\n\n" +
                "Questa è solo una demo - qui implementerai le tue quest reali!";

        // Per ora mostra un dialogo con le info della quest
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(questTitle)
                .setMessage(questDescription)
                .setPositiveButton("Inizia Avventura!", (dialog, which) -> {
                    Toast.makeText(this, "Avventura iniziata! 🚀", Toast.LENGTH_SHORT).show();
                    // Qui potresti aprire un'altra activity o fragment per le quest reali
                })
                .setNegativeButton("Non ora", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Gestisce il caso in cui l'activity è già aperta e riceve un nuovo intent
        Log.d(TAG, "Nuovo intent ricevuto in QuestActivity");
        setIntent(intent);
        getIntentData();
        displayQuestInfo();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button premuto");
        super.onBackPressed();
    }
}