package com.example.app1; // o il tuo package name

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Questa riga funzionerà perché il nuovo modulo ha res/layout/activity_main.xml
        setContentView(R.layout.activity_main);
    }
}