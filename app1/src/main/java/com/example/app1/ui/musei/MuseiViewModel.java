package com.example.app1.ui.musei;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MuseiViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<JSONArray> museiData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> searchInfo = new MutableLiveData<>();
    private final MutableLiveData<Boolean> locationPermissionNeeded = new MutableLiveData<>(false);

    public MuseiViewModel(Application application) {
        super(application);
    }

    // Getters per osservare i LiveData
    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<JSONArray> getMuseiData() {
        return museiData;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<String> getSearchInfo() {
        return searchInfo;
    }

    public MutableLiveData<Boolean> getLocationPermissionNeeded() {
        return locationPermissionNeeded;
    }

    // Metodo principale per recuperare musei raccomandati
    public void fetchMuseiRaccomandati(String userId, double latitudine, double longitudine,
                                       String preferenze, Integer raggio) {
        isLoading.setValue(true);
        Log.d("MuseiViewModel", "Fetching musei for user: " + userId +
                " at position: " + latitudine + ", " + longitudine);

        OkHttpClient client = new OkHttpClient();

        // Costruisci l'URL con i parametri
        StringBuilder urlBuilder = new StringBuilder("http://10.0.2.2:8085/gestionemusei/api/musei/raccomandati");
        urlBuilder.append("?userId=").append(userId);
        urlBuilder.append("&latitudine=").append(latitudine);
        urlBuilder.append("&longitudine=").append(longitudine);

        if (preferenze != null && !preferenze.isEmpty()) {
            urlBuilder.append("&preferenze=").append(preferenze);
        }

        if (raggio != null) {
            urlBuilder.append("&raggio=").append(raggio);
        }

        String url = urlBuilder.toString();
        Log.d("MuseiViewModel", "Musei URL: " + url);

        // Aggiorna info di ricerca
        searchInfo.postValue("Ricerca musei nel raggio di " + (raggio != null ? raggio : 20) + " km...");

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("MuseiViewModel", "Musei response code: " + response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("MuseiViewModel", "Musei response: " + responseBody);

                    if (responseBody == null || responseBody.isEmpty()) {
                        Log.e("MuseiViewModel", "Risposta vuota dal server");
                        errorMessage.postValue("Nessun museo trovato nella zona.");
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);

                    if (jsonResponse.getBoolean("success")) {
                        JSONArray musei = jsonResponse.getJSONArray("musei");
                        int totalFound = jsonResponse.getInt("totalFound");

                        museiData.postValue(musei);

                        // Aggiorna info ricerca
                        String info = "Trovati " + totalFound + " musei";
                        if (preferenze != null && !preferenze.isEmpty()) {
                            info += " per '" + preferenze + "'";
                        }
                        searchInfo.postValue(info);

                        Log.d("MuseiViewModel", "Caricati " + musei.length() + " musei");
                    } else {
                        errorMessage.postValue("Errore nella ricerca dei musei");
                    }

                } else {
                    Log.e("MuseiViewModel", "Errore nel recupero musei: " + response.code());
                    errorMessage.postValue("Errore nel recupero musei: " + response.code());
                }
            } catch (Exception e) {
                Log.e("MuseiViewModel", "Errore di rete nel recupero musei: " + e.getMessage());
                errorMessage.postValue("Errore di rete: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // Metodo per ottenere dettagli di un museo specifico
    public void fetchDettaglioMuseo(String museoId, String userId, MuseoDetailCallback callback) {
        Log.d("MuseiViewModel", "Fetching dettaglio museo: " + museoId);

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8085/gestionemusei/api/musei/dettaglio/" + museoId +
                "?userId=" + userId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("MuseiViewModel", "Dettaglio museo response code: " + response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject dettaglio = new JSONObject(responseBody);

                    if (callback != null) {
                        callback.onSuccess(dettaglio);
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Errore nel recupero dettagli: " + response.code());
                    }
                }
            } catch (Exception e) {
                Log.e("MuseiViewModel", "Errore dettaglio museo: " + e.getMessage());
                if (callback != null) {
                    callback.onError("Errore di rete: " + e.getMessage());
                }
            }
        }).start();
    }

    // Metodo per aggiungere museo ai preferiti
    public void aggiungiAiPreferiti(String userId, String museoId, FavoritiCallback callback) {
        Log.d("MuseiViewModel", "Aggiungendo museo ai preferiti: " + museoId);

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8085/gestionemusei/api/musei/preferiti";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("userId", userId);
            requestBody.put("museoId", museoId);
        } catch (JSONException e) {
            Log.e("MuseiViewModel", "Errore creazione JSON: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("MuseiViewModel", "Preferiti response code: " + response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject risultato = new JSONObject(responseBody);

                    if (callback != null) {
                        callback.onSuccess(risultato.getString("message"));
                    }
                } else {
                    if (callback != null) {
                        callback.onError("Errore nell'aggiunta ai preferiti: " + response.code());
                    }
                }
            } catch (Exception e) {
                Log.e("MuseiViewModel", "Errore preferiti: " + e.getMessage());
                if (callback != null) {
                    callback.onError("Errore di rete: " + e.getMessage());
                }
            }
        }).start();
    }

    // Callback interfaces
    public interface MuseoDetailCallback {
        void onSuccess(JSONObject dettaglio);
        void onError(String error);
    }

    public interface FavoritiCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // Metodo per richiedere permessi di localizzazione
    public void requestLocationPermission() {
        locationPermissionNeeded.setValue(true);
    }

    // Reset dei messaggi di errore
    public void clearError() {
        errorMessage.setValue(null);
    }
}