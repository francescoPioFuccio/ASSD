package com.example.app1.ui.profile;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProfileViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<JSONObject> profileData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public ProfileViewModel(Application application) {
        super(application);
    }

    // Getter per osservare i LiveData
    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<JSONObject> getProfileData() {
        return profileData;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<String> getSuccessMessage() {
        return successMessage;
    }

    // Metodo per recuperare i dati dal server
    public void fetchProfile(String userId) {
        isLoading.setValue(true);
        Log.d("ProfileViewModel", "Fetching profile for user ID: " + userId);

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8080/usermodule3/api/users/" + userId;
        Log.d("ProfileViewModel", "Request URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("ProfileViewModel", "Response code: " + response.code());
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("ProfileViewModel", "Response body: " + responseBody);
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    profileData.postValue(jsonResponse);
                } else {
                    Log.e("ProfileViewModel", "Errore nel recupero dei dati: " + response.code());
                    errorMessage.postValue("Errore nel recupero dei dati: " + response.code());
                }
            } catch (Exception e) {
                Log.e("ProfileViewModel", "Errore di rete: " + e.getMessage());
                errorMessage.postValue("Errore di rete: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    // Nuovo metodo per aggiornare il profilo
    public void updateProfile(String userId, String nome, String cognome, String email, JSONArray preferenze) {
        isLoading.setValue(true);
        Log.d("ProfileViewModel", "Updating profile for user ID: " + userId);

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8080/usermodule3/api/users/" + userId;
        Log.d("ProfileViewModel", "Update URL: " + url);

        try {
            // Crea il JSON per la richiesta PUT
            JSONObject updateData = new JSONObject();
            updateData.put("nome", nome);
            updateData.put("cognome", cognome);
            updateData.put("email", email);
            if (preferenze != null) {
                updateData.put("museoPreferito", preferenze);
            }

            Log.d("ProfileViewModel", "Update data: " + updateData.toString());

            RequestBody body = RequestBody.create(
                    updateData.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {
                    Log.d("ProfileViewModel", "Update response code: " + response.code());
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d("ProfileViewModel", "Update response: " + responseBody);

                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.has("user")) {
                            JSONObject updatedUser = jsonResponse.getJSONObject("user");
                            profileData.postValue(updatedUser);
                        }

                        String message = jsonResponse.optString("message", "Profilo aggiornato con successo!");
                        successMessage.postValue(message);
                    } else {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        Log.e("ProfileViewModel", "Errore nell'aggiornamento: " + response.code() + " - " + responseBody);

                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMsg = errorJson.optString("message", "Errore nell'aggiornamento: " + response.code());
                            errorMessage.postValue(errorMsg);
                        } catch (JSONException e) {
                            errorMessage.postValue("Errore nell'aggiornamento: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e("ProfileViewModel", "Errore di rete nell'aggiornamento: " + e.getMessage());
                    errorMessage.postValue("Errore di rete: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            }).start();

        } catch (JSONException e) {
            Log.e("ProfileViewModel", "Errore nella creazione del JSON: " + e.getMessage());
            errorMessage.postValue("Errore nella preparazione dei dati");
            isLoading.postValue(false);
        }
    }
}