package com.example.app1.ui.quest;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app1.ui.quest.Quest;
import com.example.app1.ui.quest.ApiService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

public class QuestViewModel extends ViewModel {

    private static final String TAG = "QuestViewModel";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // LiveData per l'UI
    private final MutableLiveData<List<Quest>> questList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> allQuestsCompleted = new MutableLiveData<>(false);
    private final MutableLiveData<Quest> questAnalysisResult = new MutableLiveData<>();

    // Servizio API
    private final ApiService apiService = new ApiService();

    // Dati per le chiamate API
    private String currentUserId;
    private String currentMuseoId;

    // Getters per LiveData
    public LiveData<List<Quest>> getQuestList() {
        return questList;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<Boolean> getAllQuestsCompleted() {
        return allQuestsCompleted;
    }

    public LiveData<Quest> getQuestAnalysisResult() {
        return questAnalysisResult;
    }

    /**
     * Carica le quest disponibili per un museo
     */
    public void loadQuestDisponibili(String userId, String museoId, String preferenze, String difficolta) {
        this.currentUserId = userId;
        this.currentMuseoId = museoId;

        Log.d(TAG, "Loading quest per museo: " + museoId + ", user: " + userId);

        isLoading.postValue(true);
        errorMessage.postValue(null);

        executor.execute(() -> {
            try {
                String response = apiService.getQuestDisponibili(userId, museoId, preferenze, difficolta);
                Log.d(TAG, "Response quest: " + response);

                List<Quest> quest = parseQuestResponse(response);

                if (quest.isEmpty()) {
                    allQuestsCompleted.postValue(true);
                    errorMessage.postValue("Nessuna quest disponibile per questo museo.");
                } else {
                    questList.postValue(quest);
                    allQuestsCompleted.postValue(false);
                }

            } catch (Exception e) {
                Log.e(TAG, "Errore caricamento quest: " + e.getMessage(), e);
                errorMessage.postValue("Errore nel caricamento delle quest: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Avvia una quest specifica
     */
    public void iniziaQuest(String questId) {
        Log.d(TAG, "Avvio quest: " + questId);

        isLoading.postValue(true);

        executor.execute(() -> {
            try {
                String response = apiService.iniziaQuest(currentUserId, questId);
                Log.d(TAG, "Response inizio quest: " + response);

                JSONObject jsonResponse = new JSONObject(response);

                if (jsonResponse.optBoolean("success", false)) {
                    successMessage.postValue("Quest avviata con successo!");

                    // Aggiorna lo stato della quest nella lista
                    updateQuestStatus(questId, true, false, true);

                } else {
                    String message = jsonResponse.optString("message", "Errore nell'avvio della quest");
                    errorMessage.postValue(message);
                }

            } catch (Exception e) {
                Log.e(TAG, "Errore avvio quest: " + e.getMessage(), e);
                errorMessage.postValue("Errore nell'avvio della quest: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Analizza una foto per verificare se completa una quest
     */
    public void analizzaFotoQuest(Quest quest, File fotoFile) {
        Log.d(TAG, "Analisi foto per quest: " + quest.getIdQuest());

        isLoading.postValue(true);
        errorMessage.postValue(null);
        successMessage.postValue(null);

        executor.execute(() -> {
            try {
                String response = apiService.analizzaFoto(
                        fotoFile,
                        currentUserId,
                        quest.getDescrizioneQuest()
                );

                Log.d(TAG, "Response analisi foto: " + response);

                JSONObject jsonResponse = new JSONObject(response);
                boolean questApprovata = jsonResponse.optBoolean("questApprovata", false);
                String status = jsonResponse.optString("status", "");
                String message = jsonResponse.optString("message", "");

                if (questApprovata && "APPROVATA".equals(status)) {
                    // Quest completata con successo
                    completaQuest(quest, jsonResponse);
                    successMessage.postValue(message);

                } else {
                    // Quest non approvata
                    String suggerimento = jsonResponse.optString("suggerimento", "");
                    String errorMsg = message;
                    if (!suggerimento.isEmpty()) {
                        errorMsg += "\n\nSuggerimento: " + suggerimento;
                    }
                    errorMessage.postValue(errorMsg);
                }

                // Comunica il risultato dell'analisi
                quest.setCompletata(questApprovata);
                questAnalysisResult.postValue(quest);

            } catch (Exception e) {
                Log.e(TAG, "Errore analisi foto: " + e.getMessage(), e);
                errorMessage.postValue("Errore nell'analisi della foto: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Completa una quest (chiamata interna dopo analisi positiva)
     */
    private void completaQuest(Quest quest, JSONObject analisiResponse) {
        try {
            // Estrai informazioni dall'analisi
            int punteggioBonus = analisiResponse.optInt("punteggioBonus", 0);

            // Chiamata API per completare la quest
            String response = apiService.completaQuest(
                    currentUserId,
                    quest.getIdQuest(),
                    quest.getTempoStimato()
            );

            Log.d(TAG, "Quest completata: " + response);

            // Rimuovi la quest dalla lista (è stata completata)
            removeQuestFromList(quest.getIdQuest());

            // Controlla se tutte le quest sono state completate
            checkAllQuestsCompleted();

        } catch (Exception e) {
            Log.e(TAG, "Errore completamento quest: " + e.getMessage(), e);
        }
    }

    /**
     * Rimuove una quest dalla lista corrente
     */
    private void removeQuestFromList(String questId) {
        List<Quest> currentList = questList.getValue();
        if (currentList != null) {
            List<Quest> updatedList = new ArrayList<>(currentList);
            updatedList.removeIf(quest -> questId.equals(quest.getIdQuest()));
            questList.postValue(updatedList);
        }
    }

    /**
     * Controlla se tutte le quest sono state completate
     */
    private void checkAllQuestsCompleted() {
        List<Quest> currentList = questList.getValue();
        if (currentList == null || currentList.isEmpty()) {
            allQuestsCompleted.postValue(true);
            successMessage.postValue("🎉 Congratulazioni! Hai completato tutte le quest disponibili!");
        }
    }

    /**
     * Aggiorna lo stato di una quest specifica
     */
    private void updateQuestStatus(String questId, boolean attiva, boolean completata, boolean disponibile) {
        List<Quest> currentList = questList.getValue();
        if (currentList != null) {
            List<Quest> updatedList = new ArrayList<>(currentList);
            for (Quest quest : updatedList) {
                if (questId.equals(quest.getIdQuest())) {
                    quest.setAttiva(attiva);
                    quest.setCompletata(completata);
                    quest.setDisponibile(disponibile);
                    break;
                }
            }
            questList.postValue(updatedList);
        }
    }

    /**
     * Parse della risposta JSON delle quest
     */
    private List<Quest> parseQuestResponse(String response) throws Exception {
        List<Quest> questList = new ArrayList<>();

        JSONObject jsonResponse = new JSONObject(response);

        if (!jsonResponse.optBoolean("found", false)) {
            return questList; // Lista vuota se non trovate quest
        }

        JSONArray questArray = jsonResponse.getJSONArray("quest");

        for (int i = 0; i < questArray.length(); i++) {
            JSONObject questJson = questArray.getJSONObject(i);

            Quest quest = new Quest();
            quest.setIdQuest(questJson.optString("idQuest"));
            quest.setTitoloQuest(questJson.optString("titoloQuest"));
            quest.setDescrizioneQuest(questJson.optString("descrizioneQuest"));
            quest.setNomeOpera(questJson.optString("nomeOpera"));
            quest.setAutoreOpera(questJson.optString("autoreOpera"));
            quest.setCategoria(questJson.optString("categoria"));
            quest.setDifficolta(questJson.optString("difficolta"));
            quest.setPuntiRicompensa(questJson.optInt("puntiRicompensa", 100));
            quest.setTempoStimato(questJson.optInt("tempoStimato", 20));

            // Parse degli array
            quest.setIndizi(parseStringArray(questJson.optJSONArray("indizi")));
            quest.setObiettivi(parseStringArray(questJson.optJSONArray("obiettivi")));
            quest.setSuggerimenti(parseStringArray(questJson.optJSONArray("suggerimenti")));

            // Stati
            quest.setCompletata(questJson.optBoolean("completata", false));
            quest.setAttiva(questJson.optBoolean("attiva", false));
            quest.setDisponibile(questJson.optBoolean("disponibile", true));

            // Dettagli extra se presenti
            JSONObject dettagliOpera = questJson.optJSONObject("dettagliOpera");
            if (dettagliOpera != null) {
                quest.setDimensioni(dettagliOpera.optString("dimensioni"));
                quest.setAnno(dettagliOpera.optString("anno"));
                quest.setTecnica(dettagliOpera.optString("tecnica"));
                quest.setProvenienza(dettagliOpera.optString("provenienza"));
            }

            questList.add(quest);
        }

        return questList;
    }

    /**
     * Converte un JSONArray in List<String>
     */
    private List<String> parseStringArray(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.optString(i));
            }
        }
        return list;
    }

    /**
     * Ricarica le quest (utile per refresh)
     */
    public void refreshQuest() {
        if (currentUserId != null && currentMuseoId != null) {
            loadQuestDisponibili(currentUserId, currentMuseoId, null, null);
        }
    }

    /**
     * Pulisce i messaggi di errore/successo
     */
    public void clearMessages() {
        errorMessage.postValue(null);
        successMessage.postValue(null);
    }

    /**
     * Ottiene il numero di quest disponibili
     */
    public int getQuestCount() {
        List<Quest> currentList = questList.getValue();
        return currentList != null ? currentList.size() : 0;
    }

    /**
     * Verifica se ci sono quest attive
     */
    public boolean hasActiveQuest() {
        List<Quest> currentList = questList.getValue();
        if (currentList != null) {
            for (Quest quest : currentList) {
                if (quest.isAttiva()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Ottiene la quest attiva corrente
     */
    public Quest getActiveQuest() {
        List<Quest> currentList = questList.getValue();
        if (currentList != null) {
            for (Quest quest : currentList) {
                if (quest.isAttiva()) {
                    return quest;
                }
            }
        }
        return null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}