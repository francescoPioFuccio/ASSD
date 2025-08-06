package com.example.app1.ui.navigation;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class NavigationViewModel extends AndroidViewModel {

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<JSONObject> destinationData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public NavigationViewModel(Application application) {
        super(application);
    }

    // Getters per osservare i LiveData
    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<JSONObject> getDestinationData() {
        return destinationData;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Metodo per recuperare la destinazione dal server
    public void fetchDestination(String userId) {
        isLoading.setValue(true);
        Log.d("NavigationViewModel", "Fetching destination for user ID: " + userId);

        OkHttpClient client = new OkHttpClient();
        String url = "http://10.0.2.2:8085/usermodule3/api/users/navigation/" + userId;
        Log.d("NavigationViewModel", "Navigation URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                Log.d("NavigationViewModel", "Navigation response code: " + response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("NavigationViewModel", "Navigation response body: " + responseBody);

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    destinationData.postValue(jsonResponse);

                } else {
                    Log.e("NavigationViewModel", "Errore nel recupero destinazione: " + response.code());
                    errorMessage.postValue("Errore nel recupero destinazione: " + response.code());
                }
            } catch (Exception e) {
                Log.e("NavigationViewModel", "Errore di rete: " + e.getMessage());
                errorMessage.postValue("Errore di rete: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

}