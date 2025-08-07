package com.example.app1.ui.promotion;

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

public class PuntiBonusViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> userPoints = new MutableLiveData<>(0);
    private final MutableLiveData<JSONArray> promotionsData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public PuntiBonusViewModel(Application application) {
        super(application);
    }

    // Getters per osservare i LiveData
    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<Integer> getUserPoints() {
        return userPoints;
    }

    public MutableLiveData<JSONArray> getPromotionsData() {
        return promotionsData;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Metodo per recuperare i punti dell'utente
    public void fetchUserPoints(String userId) {
        isLoading.setValue(true);
        Log.d("PuntiBonusViewModel", "Fetching user points for ID: " + userId);

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8080/usermodule3/api/users/" + userId;

        Log.d("PuntiBonusViewModel", "User points URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("PuntiBonusViewModel", "User points response code: " + response.code());
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("PuntiBonusViewModel", "User points response: " + responseBody);

                    // Controlla se la risposta è vuota
                    if (responseBody == null || responseBody.isEmpty()) {
                        Log.e("PuntiBonusViewModel", "La risposta è vuota o nulla");
                        errorMessage.postValue("La risposta del server è vuota.");
                        return;
                    }

                    // Parsing del JSON
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    int points = jsonResponse.optInt("punti", 0);
                    userPoints.postValue(points);

                } else {
                    Log.e("PuntiBonusViewModel", "Errore nel recupero punti utente: " + response.code());
                    errorMessage.postValue("Errore nel recupero punti: " + response.code());
                }
            } catch (Exception e) {
                Log.e("PuntiBonusViewModel", "Errore di rete nel recupero punti: " + e.getMessage());
                errorMessage.postValue("Errore di rete: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // Metodo per recuperare le promozioni
    public void fetchPromotions() {
        isLoading.setValue(true);
        Log.d("PuntiBonusViewModel", "Fetching promotions...");

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8085/usermodule3/api/users/promozioni";
        Log.d("PuntiBonusViewModel", "Promotions URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("PuntiBonusViewModel", "Promotions response code: " + response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("PuntiBonusViewModel", "Promotions response body: " + responseBody);

                    try {
                        // Ora trattiamo la risposta come un JSONArray
                        JSONArray promotionsArray = new JSONArray(responseBody);
                        Log.d("PuntiBonusViewModel", "Promotions JSON array length: " + promotionsArray.length());

                        // Elenco di promozioni
                        for (int i = 0; i < promotionsArray.length(); i++) {
                            JSONObject promotionObject = promotionsArray.getJSONObject(i);

                            // Estrai i dati dalla promozione
                            String titolo = promotionObject.optString("titolo", "N/A");
                            int puntiNecessari = promotionObject.optInt("puntiNecessari", 0);
                            double sconto = promotionObject.optDouble("sconto", 0.0);

                            // Crea l'oggetto della promozione (se vuoi salvarlo o usarlo)
                            Log.d("PuntiBonusViewModel", "Promozione " + i + ": " + titolo + ", Punti necessari: " + puntiNecessari + ", Sconto: " + sconto);
                        }

                        // Aggiorna la LiveData con l'array delle promozioni
                        promotionsData.postValue(promotionsArray);

                    } catch (JSONException e) {
                        Log.e("PuntiBonusViewModel", "Errore durante il parsing JSON delle promozioni: " + e.getMessage());
                        errorMessage.postValue("Errore nel parsing dei dati: " + e.getMessage());
                    }

                } else {
                    Log.e("PuntiBonusViewModel", "Errore nella risposta del server: " + response.code());
                    errorMessage.postValue("Errore nel recupero promozioni: " + response.code());
                }
            } catch (Exception e) {
                Log.e("PuntiBonusViewModel", "Errore di rete nel recupero promozioni: " + e.getMessage());
                errorMessage.postValue("Errore di rete: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }


    // Metodo combinato per recuperare sia punti che promozioni
    public void fetchAllData(String userId) {
        fetchUserPoints(userId);
        fetchPromotions();
    }
}
