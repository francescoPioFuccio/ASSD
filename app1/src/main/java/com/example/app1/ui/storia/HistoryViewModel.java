package com.example.app1.ui.storia;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<JSONArray> historyData = new MutableLiveData<>();
    private final MutableLiveData<JSONObject> userStats = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private JSONArray originalHistoryData; // Dati originali per il filtraggio
    private String currentFilter = "all";

    public HistoryViewModel(Application application) {
        super(application);
    }

    // Getters per osservare i LiveData
    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<JSONArray> getHistoryData() {
        return historyData;
    }

    public MutableLiveData<JSONObject> getUserStats() {
        return userStats;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Metodo principale per recuperare lo storico dell'utente
    public void fetchUserHistory(String userId) {
        isLoading.setValue(true);
        Log.d("HistoryViewModel", "Fetching user history for ID: " + userId);

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8085/gestionequest/api/quest/storico/" + userId;


        Log.d("HistoryViewModel", "History URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("HistoryViewModel", "History response code: " + response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("HistoryViewModel", "History response: " + responseBody);

                    if (responseBody == null || responseBody.isEmpty()) {
                        Log.e("HistoryViewModel", "La risposta è vuota o nulla");
                        errorMessage.postValue("La risposta del server è vuota.");
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    processHistoryResponse(jsonResponse, userId);

                } else {
                    Log.e("HistoryViewModel", "Errore nel recupero storico: " + response.code());
                    errorMessage.postValue("Errore nel recupero storico: " + response.code());
                }
            } catch (Exception e) {
                Log.e("HistoryViewModel", "Errore di rete nel recupero storico: " + e.getMessage());
                errorMessage.postValue("Errore di rete: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    private void processHistoryResponse(JSONObject response, String userId) {
        try {
            JSONArray questCompletate = response.optJSONArray("questCompletate");
            JSONObject statistiche = response.optJSONObject("statistiche");

            if (questCompletate == null) {
                questCompletate = new JSONArray();
            }

            // Crea array combinato per musei e quest
            JSONArray combinedHistory = createCombinedHistory(questCompletate);
            originalHistoryData = combinedHistory;

            // Aggiorna le statistiche
            if (statistiche != null) {
                // Calcola musei unici visitati
                int museiVisitati = calcolaMuseiVisitati(questCompletate);
                statistiche.put("museiVisitati", museiVisitati);
                userStats.postValue(statistiche);
            }

            // Applica il filtro corrente
            applyCurrentFilter();

        } catch (Exception e) {
            Log.e("HistoryViewModel", "Errore nel processare la risposta: " + e.getMessage());
            errorMessage.postValue("Errore nel processare i dati: " + e.getMessage());
        }
    }

    private JSONArray createCombinedHistory(JSONArray questCompletate) {
        JSONArray combinedArray = new JSONArray();

        try {
            // Mappa per raggruppare quest per museo
            JSONObject museiMap = new JSONObject();

            // Prima passa: raccogli informazioni sui musei e le quest
            for (int i = 0; i < questCompletate.length(); i++) {
                JSONObject quest = questCompletate.getJSONObject(i);
                String museoId = quest.optString("museoId", "unknown");
                String questId = quest.optString("questId", "");

                // Aggiungi quest alla storia
                JSONObject questItem = new JSONObject();
                questItem.put("tipo", "quest");
                questItem.put("questId", questId);
                questItem.put("titoloQuest", estraiTitoloQuest(questId));
                questItem.put("museoId", museoId);
                questItem.put("nomeMuseo", estraiNomeMuseo(museoId));
                questItem.put("dataCompletamento", formatDataCompletamento(quest.optLong("dataCompletamento")));
                questItem.put("punteggio", quest.optInt("punteggio", 0));
                questItem.put("difficolta", estraiDifficoltaQuest(questId));
                questItem.put("timestamp", quest.optLong("dataCompletamento"));

                combinedArray.put(questItem);

                // Aggiungi museo se non già presente
                if (!museiMap.has(museoId)) {
                    JSONObject museoItem = new JSONObject();
                    museoItem.put("tipo", "museo");
                    museoItem.put("museoId", museoId);
                    museoItem.put("nomeMuseo", estraiNomeMuseo(museoId));
                    museoItem.put("citta", estraiCittaMuseo(museoId));
                    museoItem.put("dataVisita", formatDataVisita(quest.optLong("dataCompletamento")));
                    museoItem.put("questCompletate", 1);
                    museoItem.put("timestamp", quest.optLong("dataCompletamento"));

                    museiMap.put(museoId, museoItem);
                } else {
                    // Incrementa il contatore delle quest per questo museo
                    JSONObject museoEsistente = museiMap.getJSONObject(museoId);
                    int questAttuali = museoEsistente.optInt("questCompletate", 0);
                    museoEsistente.put("questCompletate", questAttuali + 1);

                    // Aggiorna data se più recente
                    long dataEsistente = museoEsistente.optLong("timestamp", 0);
                    long nuovaData = quest.optLong("dataCompletamento", 0);
                    if (nuovaData > dataEsistente) {
                        museoEsistente.put("dataVisita", formatDataVisita(nuovaData));
                        museoEsistente.put("timestamp", nuovaData);
                    }
                }
            }

            // Aggiungi i musei all'array combinato
            JSONArray museiKeys = museiMap.names();
            if (museiKeys != null) {
                for (int i = 0; i < museiKeys.length(); i++) {
                    String key = museiKeys.getString(i);
                    combinedArray.put(museiMap.getJSONObject(key));
                }
            }

            // Ordina per timestamp (più recenti prima)
            JSONArray sortedArray = sortByTimestamp(combinedArray);

            return sortedArray;

        } catch (JSONException e) {
            Log.e("HistoryViewModel", "Errore nella creazione della storia combinata: " + e.getMessage());
            return new JSONArray();
        }
    }

    private int calcolaMuseiVisitati(JSONArray questCompletate) {
        try {
            JSONArray museiUnici = new JSONArray();

            for (int i = 0; i < questCompletate.length(); i++) {
                JSONObject quest = questCompletate.getJSONObject(i);
                String museoId = quest.optString("museoId", "");

                // Controlla se il museo è già nella lista
                boolean trovato = false;
                for (int j = 0; j < museiUnici.length(); j++) {
                    if (museoId.equals(museiUnici.getString(j))) {
                        trovato = true;
                        break;
                    }
                }

                if (!trovato && !museoId.isEmpty()) {
                    museiUnici.put(museoId);
                }
            }

            return museiUnici.length();
        } catch (Exception e) {
            Log.e("HistoryViewModel", "Errore nel calcolo musei visitati: " + e.getMessage());
            return 0;
        }
    }

    public void filterHistory(String filterType) {
        currentFilter = filterType;
        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        if (originalHistoryData == null) {
            return;
        }

        try {
            JSONArray filteredArray = new JSONArray();

            for (int i = 0; i < originalHistoryData.length(); i++) {
                JSONObject item = originalHistoryData.getJSONObject(i);
                String tipo = item.optString("tipo", "");

                switch (currentFilter) {
                    case "all":
                        filteredArray.put(item);
                        break;
                    case "museums":
                        if ("museo".equals(tipo)) {
                            filteredArray.put(item);
                        }
                        break;
                    case "quests":
                        if ("quest".equals(tipo)) {
                            filteredArray.put(item);
                        }
                        break;
                }
            }

            historyData.postValue(filteredArray);

        } catch (JSONException e) {
            Log.e("HistoryViewModel", "Errore nel filtraggio: " + e.getMessage());
        }
    }

    // === METODI AUSILIARI ===

    private String estraiTitoloQuest(String questId) {
        // Estrae il titolo basato sull'ID della quest
        if (questId.contains("GIOCONDA")) return "Trova la Gioconda";
        if (questId.contains("ULTIMA_CENA")) return "L'Ultima Cena";
        if (questId.contains("VENERE")) return "La Nascita di Venere";
        if (questId.contains("NOTTE_STELLATA")) return "La Notte Stellata";
        if (questId.contains("T_REX")) return "Il Fossile del T-Rex";
        if (questId.contains("TAVOLA_PERIODICA")) return "La Tavola Periodica Originale";
        if (questId.contains("TELESCOPIO")) return "Il Telescopio di Galileo";
        if (questId.contains("ROSETTA")) return "La Stele di Rosetta";
        if (questId.contains("ARMATURA")) return "L'Armatura del Cavaliere";
        if (questId.contains("MANOSCRITTO")) return "Il Manoscritto Illuminato";
        if (questId.contains("COMPUTER")) return "Il Primo Computer";
        if (questId.contains("FORD")) return "L'Automobile di Ford";
        if (questId.contains("MONARCA")) return "La Farfalla Monarca";
        if (questId.contains("QUARZO")) return "Il Cristallo di Quarzo Gigante";

        return "Quest Misteriosa";
    }

    private String estraiNomeMuseo(String museoId) {
        switch (museoId) {
            case "MUS_ARTE": return "Museo d'Arte";
            case "MUS_SCIENZA": return "Museo delle Scienze";
            case "MUS_STORIA": return "Museo di Storia";
            case "MUS_TECNOLOGIA": return "Museo della Tecnologia";
            case "MUS_NATURA": return "Museo di Scienze Naturali";
            default: return "Museo Sconosciuto";
        }
    }

    private String estraiCittaMuseo(String museoId) {
        switch (museoId) {
            case "MUS_ARTE": return "Firenze";
            case "MUS_SCIENZA": return "Milano";
            case "MUS_STORIA": return "Roma";
            case "MUS_TECNOLOGIA": return "Torino";
            case "MUS_NATURA": return "Napoli";
            default: return "Città non specificata";
        }
    }

    private String estraiDifficoltaQuest(String questId) {
        if (questId.contains("001")) return "facile";
        if (questId.contains("002")) return "media";
        if (questId.contains("003")) return "difficile";
        return "media";
    }

    private String formatDataCompletamento(long timestamp) {
        if (timestamp == 0) return "Data non disponibile";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN);
        return sdf.format(new Date(timestamp));
    }

    private String formatDataVisita(long timestamp) {
        if (timestamp == 0) return "Data non disponibile";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN);
        return sdf.format(new Date(timestamp));
    }

    private JSONArray sortByTimestamp(JSONArray array) {
        try {
            // Semplice bubble sort per timestamp (più recenti prima)
            for (int i = 0; i < array.length() - 1; i++) {
                for (int j = 0; j < array.length() - 1 - i; j++) {
                    JSONObject obj1 = array.getJSONObject(j);
                    JSONObject obj2 = array.getJSONObject(j + 1);

                    long timestamp1 = obj1.optLong("timestamp", 0);
                    long timestamp2 = obj2.optLong("timestamp", 0);

                    if (timestamp1 < timestamp2) {
                        // Scambia posizioni
                        array.put(j, obj2);
                        array.put(j + 1, obj1);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("HistoryViewModel", "Errore nell'ordinamento: " + e.getMessage());
        }
        return array;
    }
}