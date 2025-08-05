package com.example.app1.ui.profile;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class ProfileViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<JSONObject> profileData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

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

    // Metodo per recuperare i dati dal server
    public void fetchProfile(String userId) {

        isLoading.setValue(true);
        Log.d("ProfileViewModel", "Fetching profile for user ID: " + userId);

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8085/usermodule3/api/users/" + userId;
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


}