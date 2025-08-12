package com.example.app1.ui.navigation;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.app1.ui.home.HomeActivity;
import com.example.app1.databinding.ActivityNavigationBinding;
import com.example.app1.ui.quest.QuestActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class NavigationActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 3001;
    private static final float ARRIVAL_THRESHOLD_METERS = 200.0f; // Raggio di 200 metri per considerare l'arrivo
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5 secondi
    private static final long LOCATION_FASTEST_INTERVAL = 2000; // 2 secondi

    private ActivityNavigationBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Dati del museo di destinazione
    private String museoNome;
    private double museoLatitudine;
    private double museoLongitudine;
    private String museoIndirizzo;
    private Double currentLatitude = null;
    private Double currentLongitude = null;

    // Stato della navigazione
    private boolean isNavigating = false;
    private boolean hasOpenedMaps = false;
    private boolean hasArrived = false; // Nuovo flag per evitare chiamate multiple

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Recupera i dati del museo dall'intent
        getMuseumDataFromIntent();

        // Inizializza i servizi di localizzazione
        initializeLocationServices();

        // Setup UI
        setupUI();

        // Controlla permessi e avvia navigazione
        checkLocationPermissionAndStartNavigation();
    }

    private void getMuseumDataFromIntent() {
        Intent intent = getIntent();
        museoNome = intent.getStringExtra("museo_nome");
        museoLatitudine = intent.getDoubleExtra("museo_latitudine", 0.0);
        museoLongitudine = intent.getDoubleExtra("museo_longitudine", 0.0);
        museoIndirizzo = intent.getStringExtra("museo_indirizzo");

        Log.d("NavigationActivity", "Destinazione: " + museoNome +
                " (" + museoLatitudine + ", " + museoLongitudine + ")");

        if (museoNome == null || (museoLatitudine == 0.0 && museoLongitudine == 0.0)) {
            Toast.makeText(this, "Errore: dati del museo non validi", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configura la richiesta di localizzazione
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
                .setMaxUpdateAgeMillis(10000)
                .build();

        // Callback per gli aggiornamenti di posizione
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || hasArrived) return;

                for (Location location : locationResult.getLocations()) {
                    updateCurrentLocation(location);
                    checkArrival(location);
                }
            }
        };
    }

    private void setupUI() {
        // Imposta le informazioni del museo
        binding.museoNomeTextView.setText(museoNome);
        if (museoIndirizzo != null && !museoIndirizzo.isEmpty()) {
            binding.museoIndirizzoTextView.setText(museoIndirizzo);
        } else {
            binding.museoIndirizzoTextView.setText("Coordinate: " + museoLatitudine + ", " + museoLongitudine);
        }

        // Setup bottoni
        binding.openMapsButton.setOnClickListener(v -> openGoogleMaps());
        binding.stopNavigationButton.setOnClickListener(v -> stopNavigationAndGoHome());
        binding.backButton.setOnClickListener(v -> stopNavigationAndGoHome());
        binding.museumQuestButton.setOnClickListener(v -> {
            // Avvia QuestActivity quando il pulsante viene premuto
            Intent intent = new Intent(NavigationActivity.this, QuestActivity.class);
            startActivity(intent);
        });
        // Inizialmente disabilita il bottone per aprire Maps
        binding.openMapsButton.setEnabled(false);
    }

    private void checkLocationPermissionAndStartNavigation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        isNavigating = true;
        binding.statusTextView.setText("Rilevamento posizione in corso...");

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d("NavigationActivity", "Aggiornamenti di posizione avviati");

        // Ottieni la posizione corrente per aprire subito Maps
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateCurrentLocation(location);
                        // Abilita il bottone per aprire Maps
                        binding.openMapsButton.setEnabled(true);
                        // Apri automaticamente Google Maps solo se non è già stato aperto
                        if (!hasOpenedMaps) {
                            openGoogleMaps();
                        }
                    }
                });
    }

    private void updateCurrentLocation(Location location) {
        if (hasArrived) return; // Non aggiornare se siamo già arrivati

        double currentLat = location.getLatitude();
        double currentLng = location.getLongitude();
        currentLatitude = currentLat;
        currentLongitude = currentLng;

        // Calcola la distanza dalla destinazione
        float distance = calculateDistance(currentLat, currentLng, museoLatitudine, museoLongitudine);

        // Aggiorna l'UI con la posizione corrente e distanza
        runOnUiThread(() -> {
            binding.currentLocationTextView.setText(
                    String.format(Locale.getDefault(), "Posizione: %.6f, %.6f", currentLat, currentLng));
            binding.distanceTextView.setText(
                    String.format(Locale.getDefault(), "Distanza: %.0f metri", distance));

            if (distance > ARRIVAL_THRESHOLD_METERS) {
                binding.statusTextView.setText("In viaggio verso " + museoNome);
            } else {
                binding.statusTextView.setText("Stai arrivando a destinazione!");
            }
        });

        Log.d("NavigationActivity", "Posizione aggiornata. Distanza: " + distance + " metri da " + museoNome);
    }

    private void checkArrival(Location location) {
        if (hasArrived) return; // Evita chiamate multiple

        float distance = calculateDistance(
                location.getLatitude(),
                location.getLongitude(),
                museoLatitudine,
                museoLongitudine
        );

        // Log per debug
        Log.d("NavigationActivity", "Distanza dal museo: " + distance + " metri (soglia: " + ARRIVAL_THRESHOLD_METERS + ")");

        if (distance <= ARRIVAL_THRESHOLD_METERS) {
            Log.d("NavigationActivity", "Utente arrivato a destinazione!");
            hasArrived = true; // Imposta il flag per evitare chiamate multiple
            onArrivalAtDestination();
        }
    }

    private float calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        Location location1 = new Location("");
        location1.setLatitude(lat1);
        location1.setLongitude(lng1);

        Location location2 = new Location("");
        location2.setLatitude(lat2);
        location2.setLongitude(lng2);

        return location1.distanceTo(location2);
    }

    private void openGoogleMaps() {
        if (hasOpenedMaps) {
            Toast.makeText(this, "Google Maps già aperto", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String mapsUri;

            // Se abbiamo la posizione corrente, usa le indicazioni complete
            if (currentLatitude != null && currentLongitude != null) {
                // URI per le indicazioni da posizione corrente a destinazione
                mapsUri = String.format(Locale.US,
                        "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=walking",
                        currentLatitude, currentLongitude, museoLatitudine, museoLongitudine);

                Log.d("NavigationActivity", "Apertura Maps con indicazioni: " + mapsUri);
            } else {
                // Fallback: naviga solo verso la destinazione
                mapsUri = String.format(Locale.US,
                        "https://www.google.com/maps/dir/?api=1&destination=%f,%f&travelmode=walking",
                        museoLatitudine, museoLongitudine);

                Log.d("NavigationActivity", "Apertura Maps senza origine: " + mapsUri);
            }

            Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUri));
            mapsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Prova prima con Google Maps specifico
            mapsIntent.setPackage("com.google.android.apps.maps");
            if (mapsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapsIntent);
                hasOpenedMaps = true;
                updateUIAfterMapsOpen();
                Log.d("NavigationActivity", "Google Maps aperto con successo");
                return;
            }

            // Fallback: qualsiasi app di mappe
            mapsIntent.setPackage(null);
            if (mapsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapsIntent);
                hasOpenedMaps = true;
                updateUIAfterMapsOpen();
                Log.d("NavigationActivity", "App di mappe alternativa aperta");
                return;
            }

            // Ultimo fallback: URI geo
            String geoUri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                    museoLatitudine, museoLongitudine, museoLatitudine, museoLongitudine,
                    Uri.encode(museoNome));

            Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
            geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (geoIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(geoIntent);
                hasOpenedMaps = true;
                updateUIAfterMapsOpen();
                Log.d("NavigationActivity", "Apertura con URI geo");
            } else {
                Toast.makeText(this, "Nessuna app di mappe disponibile", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e("NavigationActivity", "Errore nell'aprire Google Maps: " + e.getMessage());
            Toast.makeText(this, "Errore nell'aprire l'app di navigazione", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIAfterMapsOpen() {
        runOnUiThread(() -> {
            binding.statusTextView.setText("Navigazione avviata - Seguendo il percorso...");
            Toast.makeText(this, "Navigazione avviata. Torna qui quando arrivi a destinazione", Toast.LENGTH_LONG).show();
        });
    }

    // NUOVA FUNZIONE: Tenta di chiudere Google Maps e riportare l'app in primo piano
    private void handleMapsAndBringAppToFront() {
        try {
            // Metodo 1: Tenta di minimizzare/uscire da Maps usando l'Intent HOME
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);

            // Piccola pausa per permettere al sistema di processare
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Metodo 2: Riporta immediatamente la nostra app in primo piano
                bringOurAppToForeground();
            }, 500);

            Log.d("NavigationActivity", "Tentativo di chiusura Maps e ritorno app completato");

        } catch (Exception e) {
            Log.w("NavigationActivity", "Errore nel gestire Maps: " + e.getMessage());
            // Fallback: riporta almeno la nostra app in primo piano
            bringOurAppToForeground();
        }
    }

    // NUOVA FUNZIONE: Forza il ritorno della nostra app in primo piano
    private void bringOurAppToForeground() {
        try {
            // Metodo più diretto per riportare la nostra app in primo piano
            Intent intent = new Intent(this, NavigationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            Log.d("NavigationActivity", "App riportata in primo piano");

        } catch (Exception e) {
            Log.w("NavigationActivity", "Errore nel riportare l'app in primo piano: " + e.getMessage());
        }
    }

    // NUOVA FUNZIONE: Notifica persistente per avvisare dell'arrivo
    private void showArrivalNotification() {
        try {
            // Crea una notifica che avvisa dell'arrivo
            // Nota: Questa funzione richiede l'implementazione del NotificationManager
            // Per ora, usiamo una combinazione di Toast e vibrazione

            Toast.makeText(this, "🎉 ARRIVO! Tornando all'app principale...", Toast.LENGTH_LONG).show();

            // Vibrazione per attirare l'attenzione (se il permesso è disponibile)
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(1000);
                    }
                }
            } catch (Exception e) {
                Log.w("NavigationActivity", "Impossibile vibrare: " + e.getMessage());
            }

        } catch (Exception e) {
            Log.w("NavigationActivity", "Errore nella notifica di arrivo: " + e.getMessage());
        }
    }

    private void onArrivalAtDestination() {
        if (!isNavigating || hasArrived) return; // Evita chiamate multiple

        Log.d("NavigationActivity", "Gestendo arrivo a destinazione");

        // NUOVO: Gestisci Maps e riporta l'app in primo piano
        handleMapsAndBringAppToFront();

        // Mostra notifica di arrivo con vibrazione
        showArrivalNotification();

        runOnUiThread(() -> {
            binding.statusTextView.setText("🎉 Sei arrivato a " + museoNome + "!");
            Toast.makeText(this, "Congratulazioni! Sei arrivato al " + museoNome + "!", Toast.LENGTH_LONG).show();
        });

        // Ferma gli aggiornamenti di posizione
        stopLocationUpdates();

        // Aumentiamo il tempo di attesa per dare tempo al sistema di gestire il cambio app
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) { // Controlla se l'activity è ancora valida
                // Riporta nuovamente l'app in primo piano prima di andare alla Home
                bringOurAppToForeground();

                // Piccola pausa aggiuntiva prima di andare alla HomeActivity
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isFinishing()) {
                        goToHomeActivity();
                    }
                }, 1000);
            }
        }, 2000);
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        isNavigating = false;
        Log.d("NavigationActivity", "Aggiornamenti di posizione fermati");
    }

    private void stopNavigationAndGoHome() {
        stopLocationUpdates();
        hasArrived = true; // Impedisce ulteriori aggiornamenti

        // NUOVO: Gestisci Maps anche quando si ferma manualmente la navigazione
        if (hasOpenedMaps) {
            handleMapsAndBringAppToFront();
        }

        Toast.makeText(this, "Navigazione fermata", Toast.LENGTH_SHORT).show();
        goToHomeActivity();
    }

    private void goToHomeActivity() {
        Log.d("NavigationActivity", "Tornando alla HomeActivity");
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("NavigationActivity", "Permessi di localizzazione concessi");
                startLocationUpdates();
            } else {
                Log.w("NavigationActivity", "Permessi di localizzazione negati");
                Toast.makeText(this, "Permessi di localizzazione necessari per la navigazione", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Continua il monitoraggio anche quando l'app va in background
        Log.d("NavigationActivity", "App in background - continuando a monitorare la posizione");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("NavigationActivity", "App tornata in foreground");

        // Aggiorna l'UI se Maps è stato aperto
        if (hasOpenedMaps && !hasArrived) {
            binding.statusTextView.setText("Navigazione attiva - Torna qui quando arrivi");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        Log.d("NavigationActivity", "NavigationActivity distrutta");
    }

    @Override
    public void onBackPressed() {
        // Minimizza l'app invece di chiuderla durante la navigazione
        if (isNavigating && !hasArrived) {
            Log.d("NavigationActivity", "Back pressed - minimizzando l'app");
            moveTaskToBack(true);
        } else {
            // Se non stiamo navigando o siamo arrivati, comportamento normale
            super.onBackPressed();
        }
    }
}