package com.example.app1.ui.chat;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private String conversationId = null;
    private final OkHttpClient client = new OkHttpClient();
    private static final String BASE_URL = "http://10.0.2.2:8085/usermodule3/api/opera";

    public ChatViewModel(Application application) {
        super(application);
    }

    // Getter per osservare i LiveData
    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<List<ChatMessage>> getChatMessages() {
        return chatMessages;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Metodo per inviare una domanda semplice
    public void sendQuestion(String question, String userId) {
        if (question == null || question.trim().isEmpty()) {
            errorMessage.setValue("La domanda non può essere vuota");
            return;
        }

        isLoading.setValue(true);

        // Aggiungi il messaggio dell'utente alla lista
        addUserMessage(question);

        Log.d("ChatViewModel", "Sending question: " + question);

        String url = BASE_URL + "/domanda";
        Log.d("ChatViewModel", "Request URL: " + url);  // Log dell'URL completo

        try {
            JSONObject requestData = new JSONObject();
            requestData.put("domanda", question.trim());
            if (userId != null) {
                requestData.put("userId", userId);
            }

            Log.d("ChatViewModel", "Request data: " + requestData.toString());  // Log del corpo della richiesta

            RequestBody body = RequestBody.create(
                    requestData.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {
                    Log.d("ChatViewModel", "Response code: " + response.code());
                    Log.d("ChatViewModel", "Response body: " + (response.body() != null ? response.body().string() : "null"));  // Log della risposta

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d("ChatViewModel", "Response body: " + responseBody);

                        JSONObject jsonResponse = new JSONObject(responseBody);

                        String botResponse = jsonResponse.getString("risposta");
                        addBotMessage(botResponse);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        Log.e("ChatViewModel", "Error response: " + response.code() + " - " + errorBody);
                        errorMessage.postValue("Errore del server: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e("ChatViewModel", "Network error: " + e.getMessage());
                    errorMessage.postValue("Errore di rete: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            }).start();

        } catch (JSONException e) {
            Log.e("ChatViewModel", "JSON error: " + e.getMessage());
            errorMessage.setValue("Errore nella preparazione della richiesta");
            isLoading.setValue(false);
        }
    }


    // Metodo per chat conversazionale (mantiene il contesto)
    public void sendChatMessage(String message, String userId) {
        if (message == null || message.trim().isEmpty()) {
            errorMessage.setValue("Il messaggio non può essere vuoto");
            return;
        }

        isLoading.setValue(true);

        // Aggiungi il messaggio dell'utente alla lista
        addUserMessage(message);

        Log.d("ChatViewModel", "Sending chat message: " + message);

        String url = BASE_URL + "/chat";

        try {
            JSONObject requestData = new JSONObject();
            requestData.put("messaggio", message.trim());
            if (userId != null) {
                requestData.put("userId", userId);
            }
            if (conversationId != null) {
                requestData.put("conversationId", conversationId);
            }

            Log.d("ChatViewModel", "Chat request data: " + requestData.toString());

            RequestBody body = RequestBody.create(
                    requestData.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {
                    Log.d("ChatViewModel", "Chat response code: " + response.code());

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d("ChatViewModel", "Chat response body: " + responseBody);

                        JSONObject jsonResponse = new JSONObject(responseBody);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        Log.e("ChatViewModel", "Chat error response: " + response.code() + " - " + errorBody);
                        errorMessage.postValue("Errore del server: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e("ChatViewModel", "Chat network error: " + e.getMessage());
                    errorMessage.postValue("Errore di rete: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            }).start();

        } catch (JSONException e) {
            Log.e("ChatViewModel", "Chat JSON error: " + e.getMessage());
            errorMessage.setValue("Errore nella preparazione della richiesta");
            isLoading.setValue(false);
        }
    }

    // Metodo per ottenere informazioni su un'opera
    public void getOperaInfo(String operaId, String userId) {
        if (operaId == null || operaId.trim().isEmpty()) {
            errorMessage.setValue("ID opera non valido");
            return;
        }

        isLoading.setValue(true);

        // Aggiungi il messaggio dell'utente alla lista
        addUserMessage("Informazioni sull'opera: " + operaId);

        Log.d("ChatViewModel", "Getting opera info for ID: " + operaId);

        String url = BASE_URL + "/info/" + operaId.trim();
        if (userId != null) {
            url += "?userId=" + userId;
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("ChatViewModel", "Opera info response code: " + response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("ChatViewModel", "Opera info response: " + responseBody);

                    JSONObject operaInfo = new JSONObject(responseBody);

                    // Formatta le informazioni dell'opera per la chat
                    StringBuilder infoMessage = new StringBuilder();
                    infoMessage.append("🎨 **").append(operaInfo.getString("titolo")).append("**\n\n");
                    infoMessage.append("👨‍🎨 **Artista:** ").append(operaInfo.getString("artista")).append("\n");
                    infoMessage.append("📅 **Anno:** ").append(operaInfo.getInt("anno")).append("\n");
                    infoMessage.append("🏛️ **Museo:** ").append(operaInfo.getString("museo")).append("\n");
                    infoMessage.append("🎭 **Periodo:** ").append(operaInfo.getString("periodo")).append("\n");

                    if (operaInfo.has("descrizione")) {
                        infoMessage.append("\n📝 **Descrizione:**\n").append(operaInfo.getString("descrizione"));
                    }

                    if (operaInfo.has("prezzo_biglietto")) {
                        infoMessage.append("\n💰 **Prezzo biglietto:** €").append(operaInfo.getInt("prezzo_biglietto"));
                    }

                    addBotMessage(infoMessage.toString());
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    Log.e("ChatViewModel", "Opera info error: " + response.code() + " - " + errorBody);
                    addBotMessage("❌ Spiacente, non ho trovato informazioni per quest'opera. Prova con un altro ID.");
                }
            } catch (Exception e) {
                Log.e("ChatViewModel", "Opera info network error: " + e.getMessage());
                addBotMessage("❌ Errore nel recuperare le informazioni dell'opera. Riprova più tardi.");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // Metodo per verificare lo stato del servizio
    public void checkServiceHealth() {
        isLoading.setValue(true);

        Log.d("ChatViewModel", "Checking service health");

        String url = BASE_URL + "/health";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject health = new JSONObject(responseBody);

                    addBotMessage("✅ Servizio operativo! " + health.getString("message"));
                } else {
                    addBotMessage("⚠️ Il servizio potrebbe avere dei problemi. Codice: " + response.code());
                }
            } catch (Exception e) {
                Log.e("ChatViewModel", "Health check error: " + e.getMessage());
                addBotMessage("❌ Impossibile verificare lo stato del servizio.");
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // Metodo per pulire la chat
    public void clearChat() {
        List<ChatMessage> messages = chatMessages.getValue();
        if (messages != null) {
            messages.clear();
            chatMessages.setValue(messages);
        }
        conversationId = null;
        Log.d("ChatViewModel", "Chat cleared and conversation reset");
    }

    // Metodi privati per gestire i messaggi
    private void addUserMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true, System.currentTimeMillis());
        addMessageToList(chatMessage);
    }

    private void addBotMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, false, System.currentTimeMillis());
        addMessageToList(chatMessage);
    }

    private void addMessageToList(ChatMessage message) {
        List<ChatMessage> currentMessages = chatMessages.getValue();
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }
        currentMessages.add(message);
        chatMessages.postValue(new ArrayList<>(currentMessages));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d("ChatViewModel", "ViewModel cleared");
    }
}