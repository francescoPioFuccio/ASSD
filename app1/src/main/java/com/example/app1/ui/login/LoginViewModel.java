package com.example.app1.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Patterns;

import com.example.app1.R;
import com.example.app1.data.LoginRepository;
import com.example.app1.data.model.LoggedInUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginViewModel extends ViewModel {

    private final MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private final LoginRepository loginRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LoginViewModel(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        executor.execute(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "http://10.0.2.2:8080/usermodule3/api/users/login";

            JSONObject json = new JSONObject();
            try {
                json.put("email", username);
                json.put("password", password);
            } catch (JSONException e) {
                loginResult.postValue(LoginResult.error(R.string.login_failed));
                return;
            }

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    JSONObject userObj = jsonResponse.getJSONObject("user");
                    String userId = userObj.getString("id");
                    String displayName = userObj.getString("nome");

                    LoggedInUserView userView = new LoggedInUserView(displayName,userId);
                    loginResult.postValue(LoginResult.success(userView));
                } else {
                    loginResult.postValue(LoginResult.error(R.string.login_failed));
                }
            } catch (Exception e) {
                loginResult.postValue(LoginResult.error(R.string.login_failed));
            }
        });
    }

    public void loginDataChanged(String username, String password) {
        if (!isUserNameValid(username)) {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password)) {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    private boolean isUserNameValid(String username) {
        if (username == null) {
            return false;
        }
        if (username.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else {
            return !username.trim().isEmpty();
        }
    }

    private boolean isPasswordValid(String password) {
        return password != null && password.trim().length() > 5;
    }
}
