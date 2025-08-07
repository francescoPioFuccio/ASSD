package com.example.app1.ui.chat;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.app1.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private ChatViewModel chatViewModel;
    private ChatAdapter chatAdapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inizializza il ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Recupera l'ID utente
        currentUserId = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("userid", null);

        Log.d("ChatActivity", "User ID recuperato: " + currentUserId);

        setupRecyclerView();
        setupObservers();
        setupClickListeners();

        // Messaggio di benvenuto
        showWelcomeMessage();
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Inizia dal fondo

        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupObservers() {
        // Osserva i messaggi della chat
        chatViewModel.getChatMessages().observe(this, messages -> {
            if (messages != null && !messages.isEmpty()) {
                chatAdapter.updateMessages(messages);
                // Scorri automaticamente all'ultimo messaggio
                binding.chatRecyclerView.scrollToPosition(messages.size() - 1);
            }
        });

        // Osserva eventuali errori
        chatViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Osserva lo stato di loading
        chatViewModel.getIsLoading().observe(this, isLoading -> {
            binding.sendButton.setEnabled(!isLoading);
            binding.menuButton.setEnabled(!isLoading);

            if (isLoading) {
                binding.loadingIndicator.setVisibility(View.VISIBLE);
                binding.sendButton.setText("...");
            } else {
                binding.loadingIndicator.setVisibility(View.GONE);
                binding.sendButton.setText("Invia");
            }
        });
    }

    private void setupClickListeners() {
        // Click listener per il bottone di invio
        binding.sendButton.setOnClickListener(v -> sendMessage());

        // Click listener per il bottone menu
        binding.menuButton.setOnClickListener(v -> showMenuDialog());

        // Click listener per il bottone back
        binding.backButton.setOnClickListener(v -> finish());

        // Listener per l'EditText (invio con Enter)
        binding.messageEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        String message = binding.messageEditText.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(this, "Scrivi un messaggio prima di inviare", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pulisci il campo di input
        binding.messageEditText.setText("");

        // Invia il messaggio usando la chat conversazionale (mantiene il contesto)
        chatViewModel.sendChatMessage(message, currentUserId);
    }

    private void showMenuDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opzioni Chat");

        String[] options = {
                "💭 Domanda semplice",
                "🎨 Info su un'opera",
                "🔍 Stato del servizio",
                "🧹 Pulisci chat",
                "❓ Aiuto"
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showQuestionDialog();
                    break;
                case 1:
                    showOperaInfoDialog();
                    break;
                case 2:
                    chatViewModel.checkServiceHealth();
                    break;
                case 3:
                    showClearChatDialog();
                    break;
                case 4:
                    showHelpDialog();
                    break;
            }
        });

        builder.show();
    }

    private void showQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Fai una domanda");

        final EditText input = new EditText(this);
        input.setHint("Es: Chi ha dipinto la Gioconda?");
        builder.setView(input);

        builder.setPositiveButton("Invia", (dialog, which) -> {
            String question = input.getText().toString().trim();
            if (!question.isEmpty()) {
                chatViewModel.sendQuestion(question, currentUserId);
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showOperaInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Informazioni Opera");
        builder.setMessage("Inserisci l'ID o il nome dell'opera di cui vuoi sapere di più:");

        final EditText input = new EditText(this);
        input.setHint("Es: gioconda, notte-stellata, etc.");
        builder.setView(input);

        builder.setPositiveButton("Cerca", (dialog, which) -> {
            String operaId = input.getText().toString().trim();
            if (!operaId.isEmpty()) {
                chatViewModel.getOperaInfo(operaId, currentUserId);
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showClearChatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pulisci Chat");
        builder.setMessage("Sei sicuro di voler cancellare tutti i messaggi della chat?");

        builder.setPositiveButton("Sì", (dialog, which) -> {
            chatViewModel.clearChat();
            Toast.makeText(this, "Chat pulita!", Toast.LENGTH_SHORT).show();
            showWelcomeMessage();
        });

        builder.setNegativeButton("No", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Aiuto - Come usare la chat");

        String helpText = "🤖 **Assistente AI per l'Arte**\n\n" +
                "**Cosa posso fare:**\n" +
                "• Rispondere a domande sull'arte\n" +
                "• Fornire informazioni su opere e artisti\n" +
                "• Conversazioni su musei e mostre\n" +
                "• Consigli per visite culturali\n\n" +
                "**Come usare:**\n" +
                "• Scrivi direttamente nella chat per una conversazione\n" +
                "• Usa il menu per opzioni specifiche\n" +
                "• Puoi fare domande su qualsiasi opera d'arte\n\n" +
                "**Esempi di domande:**\n" +
                "• \"Chi ha dipinto la Gioconda?\"\n" +
                "• \"Raccontami del Rinascimento\"\n" +
                "• \"Quali musei visitare a Roma?\"";

        builder.setMessage(helpText);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showWelcomeMessage() {
        // Aggiungi un messaggio di benvenuto dopo un breve delay
        binding.chatRecyclerView.postDelayed(() -> {
            String welcomeMessage = "👋 Ciao! Sono il tuo assistente per l'arte e i musei.\n\n" +
                    "Puoi farmi domande su:\n" +
                    "🎨 Opere d'arte e artisti\n" +
                    "🏛️ Musei e mostre\n" +
                    "📚 Storia dell'arte\n" +
                    "🖼️ Analisi di immagini\n\n" +
                    "Come posso aiutarti oggi?";

            // Simula un messaggio del bot
            ChatMessage welcomeMsg = new ChatMessage(welcomeMessage, false, System.currentTimeMillis());
            chatAdapter.addMessage(welcomeMsg);
            binding.chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        }, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ChatActivity", "ChatActivity destroyed");
    }
}