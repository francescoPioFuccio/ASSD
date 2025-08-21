package com.example.app1.ui.login;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
        Log.d("RegisterActivity", "=== INIZIO ATTEMPT REGISTER ===");

        final ProgressBar loadingProgressBar = binding.loading;
        loadingProgressBar.setVisibility(android.view.View.VISIBLE);

        // Debug dei valori dei campi
        String nome = binding.nome.getText().toString();
        String cognome = binding.cognome.getText().toString();
        String email = binding.email.getText().toString();
        String password = binding.password.getText().toString();
        String confirmPassword = binding.confirmPassword.getText().toString();

        Log.d("RegisterActivity", "Nome: '" + nome + "'");
        Log.d("RegisterActivity", "Cognome: '" + cognome + "'");
        Log.d("RegisterActivity", "Email: '" + email + "'");
        Log.d("RegisterActivity", "Password length: " + (password != null ? password.length() : "null"));
        Log.d("RegisterActivity", "ConfirmPassword length: " + (confirmPassword != null ? confirmPassword.length() : "null"));
        Log.d("RegisterActivity", "Password match: " + (password != null && password.equals(confirmPassword)));

        // Raccogli preferenze selezionate dai CheckBox CON GLI ID CORRETTI
        List<String> selectedMuseumPreferences = new ArrayList<>();

        // USANDO GLI ID DAL LAYOUT XML
        CheckBox checkboxArt = findViewById(R.id.checkbox_art);
        if (checkboxArt != null && checkboxArt.isChecked()) {
            selectedMuseumPreferences.add("Arte");
            Log.d("RegisterActivity", "✅ Arte selezionata");
        }

        CheckBox checkboxScience = findViewById(R.id.checkbox_science);
        if (checkboxScience != null && checkboxScience.isChecked()) {
            selectedMuseumPreferences.add("Scienza");
            Log.d("RegisterActivity", "✅ Scienza selezionata");
        }

        CheckBox checkboxHistory = findViewById(R.id.checkbox_history);
        if (checkboxHistory != null && checkboxHistory.isChecked()) {
            selectedMuseumPreferences.add("Storia");
            Log.d("RegisterActivity", "✅ Storia selezionata");
        }

        CheckBox checkboxTech = findViewById(R.id.checkbox_tech);
        if (checkboxTech != null && checkboxTech.isChecked()) {
            selectedMuseumPreferences.add("Tecnologia");
            Log.d("RegisterActivity", "✅ Tecnologia selezionata");
        }

        CheckBox checkboxArchaeology = findViewById(R.id.checkbox_archaeology);
        if (checkboxArchaeology != null && checkboxArchaeology.isChecked()) {
            selectedMuseumPreferences.add("Archeologia");
            Log.d("RegisterActivity", "✅ Archeologia selezionata");
        }

        CheckBox checkboxNature = findViewById(R.id.checkbox_nature);
        if (checkboxNature != null && checkboxNature.isChecked()) {
            selectedMuseumPreferences.add("Storia Naturale");
            Log.d("RegisterActivity", "✅ Natura selezionata");
        }

        Log.d("RegisterActivity", "Preferenze selezionate: " + selectedMuseumPreferences);
        Log.d("RegisterActivity", "Numero preferenze: " + selectedMuseumPreferences.size());

        if (selectedMuseumPreferences.isEmpty()) {
            Log.e("RegisterActivity", "BLOCCO: Nessuna preferenza museo selezionata");
            loadingProgressBar.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "Seleziona almeno un tipo di museo preferito", Toast.LENGTH_SHORT).show();
            return;
        }

        String preferencesString = String.join(",", selectedMuseumPreferences);
        Log.d("RegisterActivity", "Preferences CSV: '" + preferencesString + "'");

        Log.d("RegisterActivity", "=== CHIAMATA REGISTER VIEW MODEL ===");
        registerViewModel.register(
                nome,
                cognome,
                email,
                password,
                confirmPassword,
                preferencesString
        );
        Log.d("RegisterActivity", "=== REGISTER VIEW MODEL CHIAMATO ===");
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
