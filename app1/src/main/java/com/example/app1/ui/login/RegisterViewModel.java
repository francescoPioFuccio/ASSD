package com.example.app1.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Log;
import android.util.Patterns;

import com.example.app1.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterViewModel extends ViewModel {

    private static final String TAG = "RegisterViewModel";

    private MutableLiveData<RegisterFormState> registerFormState = new MutableLiveData<>();
    private MutableLiveData<RegisterResult> registerResult = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LiveData<RegisterFormState> getRegisterFormState() {
        return registerFormState;
    }

    public LiveData<RegisterResult> getRegisterResult() {
        return registerResult;
    }

    public void register(String nome, String cognome, String email,
                         String password, String confirmPassword,
                         String museumPreferencesCsv) {
        executor.execute(() -> {
            if (!password.equals(confirmPassword)) {
                registerResult.postValue(new RegisterResult(R.string.error_password_mismatch));
                return;
            }

            OkHttpClient client = new OkHttpClient();
            String url = "http://10.0.2.2:8080/usermodule3/api/users/register";

            JSONObject json = new JSONObject();
            try {
                json.put("nome", nome);
                json.put("cognome", cognome);
                json.put("email", email);
                json.put("password", password);

                // Trasforma CSV in JSONArray
                JSONArray preferencesArray = new JSONArray();
                if (museumPreferencesCsv != null && !museumPreferencesCsv.isEmpty()) {
                    List<String> preferencesList = Arrays.asList(museumPreferencesCsv.split(","));
                    for (String pref : preferencesList) {
                        preferencesArray.put(pref.trim());
                    }
                }
                json.put("museoPreferito", preferencesArray);

                Log.d(TAG, "Sending registration data: " + json);
                Log.d(TAG, "POST URL: " + url);

            } catch (JSONException e) {
                Log.e(TAG, "JSON creation error", e);
                registerResult.postValue(new RegisterResult(R.string.register_failed));
                return;
            }

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder().url(url).post(body).build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "null";
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response body: " + responseBody);

                if (response.isSuccessful()) {
                    registerResult.postValue(new RegisterResult(new RegisteredUserView(email)));
                } else {
                    registerResult.postValue(new RegisterResult(R.string.register_failed));
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error during registration", e);
                registerResult.postValue(new RegisterResult(R.string.register_failed));
            }
        });
    }

    public void registerDataChanged(String nome, String cognome,
                                    String email, String password,
                                    String confirmPassword) {
        if (nome == null || nome.trim().isEmpty()) {
            registerFormState.setValue(new RegisterFormState(R.string.invalid_name, null, null, null, null));
        } else if (cognome == null || cognome.trim().isEmpty()) {
            registerFormState.setValue(new RegisterFormState(null, R.string.invalid_surname, null, null, null));
        } else if (!isEmailValid(email)) {
            registerFormState.setValue(new RegisterFormState(null, null, R.string.invalid_email, null, null));
        } else if (!isPasswordValid(password)) {
            registerFormState.setValue(new RegisterFormState(null, null, null, R.string.invalid_password, null));
        } else if (!password.equals(confirmPassword)) {
            registerFormState.setValue(new RegisterFormState(null, null, null, null, R.string.error_password_mismatch));
        } else {
            registerFormState.setValue(new RegisterFormState(true));
        }
    }

    private boolean isEmailValid(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
}
