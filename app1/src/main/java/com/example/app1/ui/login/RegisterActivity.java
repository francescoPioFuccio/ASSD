package com.example.app1.ui.login;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.app1.R;
import com.example.app1.databinding.ActivityRegisterBinding;
import com.example.app1.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private RegisterViewModel registerViewModel;
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        registerViewModel = new ViewModelProvider(this, new RegisterViewModelFactory())
                .get(RegisterViewModel.class);

        final EditText nomeEditText = binding.nome;
        final EditText cognomeEditText = binding.cognome;
        final EditText emailEditText = binding.email;
        final EditText passwordEditText = binding.password;
        final EditText confirmPasswordEditText = binding.confirmPassword;
        final Button registerButton = binding.register;
        final ProgressBar loadingProgressBar = binding.loading;

        // Osserva stato validazione form
        registerViewModel.getRegisterFormState().observe(this, registerFormState -> {
            if (registerFormState == null) return;
            registerButton.setEnabled(registerFormState.isDataValid());

            if (registerFormState.getNomeError() != null)
                nomeEditText.setError(getString(registerFormState.getNomeError()));
            if (registerFormState.getCognomeError() != null)
                cognomeEditText.setError(getString(registerFormState.getCognomeError()));
            if (registerFormState.getEmailError() != null)
                emailEditText.setError(getString(registerFormState.getEmailError()));
            if (registerFormState.getPasswordError() != null)
                passwordEditText.setError(getString(registerFormState.getPasswordError()));
            if (registerFormState.getConfirmPasswordError() != null)
                confirmPasswordEditText.setError(getString(registerFormState.getConfirmPasswordError()));
        });

        // Osserva risultato registrazione
        registerViewModel.getRegisterResult().observe(this, registerResult -> {
            if (registerResult == null) return;
            loadingProgressBar.setVisibility(android.view.View.GONE);

            if (registerResult.getError() != null) showRegisterFailed(registerResult.getError());
            if (registerResult.getSuccess() != null) updateUiWithUser(registerResult.getSuccess());

            setResult(Activity.RESULT_OK);
            finish();
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                registerViewModel.registerDataChanged(
                        nomeEditText.getText().toString(),
                        cognomeEditText.getText().toString(),
                        emailEditText.getText().toString(),
                        passwordEditText.getText().toString(),
                        confirmPasswordEditText.getText().toString()
                );
            }
        };

        nomeEditText.addTextChangedListener(afterTextChangedListener);
        cognomeEditText.addTextChangedListener(afterTextChangedListener);
        emailEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        confirmPasswordEditText.addTextChangedListener(afterTextChangedListener);

        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister();
                return true;
            }
            return false;
        });

        registerButton.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        final ProgressBar loadingProgressBar = binding.loading;
        loadingProgressBar.setVisibility(android.view.View.VISIBLE);

        // Raccogli preferenze selezionate dai CheckBox
        List<String> selectedMuseumPreferences = new ArrayList<>();
        if (((CheckBox)findViewById(R.id.checkbox_arte)).isChecked())
            selectedMuseumPreferences.add("Arte");
        if (((CheckBox)findViewById(R.id.checkbox_scienza)).isChecked())
            selectedMuseumPreferences.add("Scienza");
        if (((CheckBox)findViewById(R.id.checkbox_storia)).isChecked())
            selectedMuseumPreferences.add("Storia");
        if (((CheckBox)findViewById(R.id.checkbox_tecnologia)).isChecked())
            selectedMuseumPreferences.add("Tecnologia");
        if (((CheckBox)findViewById(R.id.checkbox_archeologia)).isChecked())
            selectedMuseumPreferences.add("Archeologia");
        if (((CheckBox)findViewById(R.id.checkbox_naturalistica)).isChecked())
            selectedMuseumPreferences.add("Storia Naturale");
        if (((CheckBox)findViewById(R.id.checkbox_design)).isChecked())
            selectedMuseumPreferences.add("Design");
        if (((CheckBox)findViewById(R.id.checkbox_fotografia)).isChecked())
            selectedMuseumPreferences.add("Fotografia");

        if (selectedMuseumPreferences.isEmpty()) {
            loadingProgressBar.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "Seleziona almeno un tipo di museo preferito", Toast.LENGTH_SHORT).show();
            return;
        }

        String preferencesString = String.join(",", selectedMuseumPreferences);

        registerViewModel.register(
                binding.nome.getText().toString(),
                binding.cognome.getText().toString(),
                binding.email.getText().toString(),
                binding.password.getText().toString(),
                binding.confirmPassword.getText().toString(),
                preferencesString
        );
    }

    private void updateUiWithUser(RegisteredUserView model) {
        String welcome = getString(R.string.welcome) + " " + model.getDisplayName();
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showRegisterFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(this);
    }

}
