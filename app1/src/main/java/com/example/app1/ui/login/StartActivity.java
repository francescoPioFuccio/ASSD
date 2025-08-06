// File: app1/src/main/java/com/example/app1/ui/start/StartActivity.java
package com.example.app1.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.app1.R; // Assicurati che R sia corretto per il tuo modulo app1
import com.example.app1.util.ThemeHelper;


public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start); // Collega al layout XML

        // Questa parte gestisce gli insets di sistema (barra di stato, barra di navigazione)
        // È utile per evitare che l'UI si sovrapponga a queste barre
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_start_layout), (v, insets) -> { // ID della root view del tuo layout activity_start.xml
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inizializza i bottoni usando gli ID dal layout activity_start.xml
        Button loginButton = findViewById(R.id.button_login);
        Button registerButton = findViewById(R.id.button_register);

        // Listener per il bottone Login
        if (loginButton != null) { // Controllo per sicurezza
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(StartActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Listener per il bottone Registrazione
        if (registerButton != null) { // Controllo per sicurezza
            registerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(StartActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(this);
    }

}