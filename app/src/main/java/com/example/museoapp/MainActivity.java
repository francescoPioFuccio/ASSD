package com.example.museoapp;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    /**
     * Il metodo onCreate() viene chiamato quando l'Activity viene creata per la prima volta.
     * È l'equivalente del costruttore per le Activity.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Questa riga è la più importante:
        // Dice all'Activity di usare il file 'R.layout.activity_main' come sua interfaccia utente.
        // Quel file XML, a sua volta, contiene il NavHostFragment che gestirà
        // la visualizzazione del LoginFragment e degli altri.
        setContentView(R.layout.activity_main);
    }
}