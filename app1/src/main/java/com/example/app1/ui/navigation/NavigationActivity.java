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
    private boolean hasArrived = false; // Flag per evitare chiamate multiple

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

        // Salva anche l'id/nome museo se disponibili nell'intent o già in SharedPreferences
        String museoIdFromIntent = intent.getStringExtra("museo_id");
        if (museoIdFromIntent != null && !museoIdFromIntent.isEmpty()) {
            getSharedPreferences("museo", MODE_PRIVATE)
                    .edit()
                    .putString("museo", museoIdFromIntent)
                    .putString("museo_nome", museoNome)
                    .apply();
        } else if (museoNome != null && !museoNome.isEmpty()) {
            // Almeno sincronizza il nome se manca l'id
            getSharedPreferences("museo", MODE_PRIVATE)
                    .edit()
                    .putString("museo_nome", museoNome)
                    .apply();
        }

        Log.d("NavigationActivity", "Destinazione: " + museoNome +
                " (" + museoLatitudine + ", " + museoLongitudine + ")");

        if (museoNome == null || (museoLatitudine == 0.0 && museoLongitudine == 0.0)) {
            Toast.makeText(this, "Errore: dati del museo non validi", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
                .setMaxUpdateAgeMillis(10000)
                .build();

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
            Intent intent = new Intent(NavigationActivity.this, QuestActivity.class);
            startActivity(intent);
        });

        // MODIFICA: Il bottone per aprire Maps è disabilitato di default.
        // Verrà abilitato solo dopo aver ottenuto la posizione iniziale dell'utente.
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        isNavigating = true;
        binding.statusTextView.setText("Rilevamento posizione in corso...");

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d("NavigationActivity", "Aggiornamenti di posizione avviati");

        // MODIFICA: Ottieni la posizione iniziale solo per ABILITARE il pulsante, non per aprire Mappe.
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateCurrentLocation(location);

                        // NUOVO: Abilita il pulsante e aggiorna l'UI per informare l'utente.
                        runOnUiThread(() -> {
                            binding.openMapsButton.setEnabled(true);
                            binding.statusTextView.setText("Posizione trovata! Premi 'Apri in Mappe' per iniziare.");
                        });

                        // RIMOSSO: La chiamata automatica a openGoogleMaps() è stata rimossa da qui.
                        // if (!hasOpenedMaps) {
                        //     openGoogleMaps();
                        // }
                    }
                })
                .addOnFailureListener(this, e -> {
                    // NUOVO: Gestione del caso in cui non si riesca a ottenere la posizione iniziale.
                    Log.e("NavigationActivity", "Errore nel recuperare la posizione iniziale", e);
                    runOnUiThread(() -> {
                        binding.statusTextView.setText("Impossibile ottenere la posizione. Controlla il GPS.");
                        Toast.makeText(this, "Impossibile ottenere la posizione iniziale.", Toast.LENGTH_LONG).show();
                    });
                });
    }

    private void updateCurrentLocation(Location location) {
        if (hasArrived) return;

        double currentLat = location.getLatitude();
        double currentLng = location.getLongitude();
        currentLatitude = currentLat;
        currentLongitude = currentLng;

        float distance = calculateDistance(currentLat, currentLng, museoLatitudine, museoLongitudine);

        runOnUiThread(() -> {
            binding.currentLocationTextView.setText(String.format(Locale.getDefault(), "Posizione: %.6f, %.6f", currentLat, currentLng));
            binding.distanceTextView.setText(String.format(Locale.getDefault(), "Distanza: %.0f metri", distance));

            // Aggiorna lo stato solo se l'utente non ha ancora aperto le mappe
            if (!hasOpenedMaps) {
                if (distance > ARRIVAL_THRESHOLD_METERS) {
                    // Lo stato viene già impostato in startLocationUpdates, non serve cambiarlo qui.
                } else {
                    binding.statusTextView.setText("Sei già molto vicino! Premi 'Apri in Mappe' se necessario.");
                }
            }
        });

        Log.d("NavigationActivity", "Posizione aggiornata. Distanza: " + distance + " metri da " + museoNome);
    }

    private void checkArrival(Location location) {
        if (hasArrived) return;

        float distance = calculateDistance(
                location.getLatitude(),
                location.getLongitude(),
                museoLatitudine,
                museoLongitudine
        );

        Log.d("NavigationActivity", "Distanza dal museo: " + distance + " metri (soglia: " + ARRIVAL_THRESHOLD_METERS + ")");

        if (distance <= ARRIVAL_THRESHOLD_METERS) {
            Log.d("NavigationActivity", "Utente arrivato a destinazione!");
            hasArrived = true;
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
        if (currentLatitude == null || currentLongitude == null) {
            Toast.makeText(this, "Posizione corrente non ancora disponibile. Riprova tra un istante.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hasOpenedMaps) {
            // Se le mappe sono già state aperte, l'utente potrebbe volerle riaprire.
            // Invece di un Toast, riapriamo l'app di navigazione.
        }

        try {
            // URI per le indicazioni da posizione corrente a destinazione
            String mapsUri = String.format(Locale.US,
                    "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=walking",
                    currentLatitude, currentLongitude, museoLatitudine, museoLongitudine);

            Log.d("NavigationActivity", "Apertura Mappe con indicazioni: " + mapsUri);

            Intent mapsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUri));
            mapsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mapsIntent.setPackage("com.google.android.apps.maps");

            if (mapsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapsIntent);
                hasOpenedMaps = true;
                updateUIAfterMapsOpen();
            } else {
                // Fallback: qualsiasi app di mappe
                mapsIntent.setPackage(null);
                if (mapsIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapsIntent);
                    hasOpenedMaps = true;
                    updateUIAfterMapsOpen();
                } else {
                    Toast.makeText(this, "Nessuna app di mappe disponibile", Toast.LENGTH_LONG).show();
                }
            }

        } catch (Exception e) {
            Log.e("NavigationActivity", "Errore nell'aprire Google Maps: " + e.getMessage());
            Toast.makeText(this, "Errore nell'aprire l'app di navigazione", Toast.LENGTH_SHORT).show();
        }
    }

    // ... il resto del codice rimane invariato ...
    // onArrivalAtDestination(), stopNavigationAndGoHome(), etc. sono corretti.
    // Li ometto per brevità ma devono rimanere nel tuo file.

    private void updateUIAfterMapsOpen() {
        runOnUiThread(() -> {
            binding.statusTextView.setText("Navigazione avviata - Seguendo il percorso...");
            Toast.makeText(this, "Navigazione avviata. Torna qui quando arrivi a destinazione", Toast.LENGTH_LONG).show();
        });
    }

    private void onArrivalAtDestination() {
        if (!isNavigating) return; // Evita chiamate multiple se la navigazione è già stata fermata

        Log.d("NavigationActivity", "Gestendo arrivo a destinazione");

        // Ferma gli aggiornamenti di posizione
        stopLocationUpdates();

        // Riporta l'app in primo piano
        bringOurAppToForeground();

        // Mostra notifica di arrivo
        showArrivalNotification();

        runOnUiThread(() -> {
            binding.statusTextView.setText("🎉 Sei arrivato a " + museoNome + "!");
            Toast.makeText(this, "Congratulazioni! Sei arrivato al " + museoNome + "!", Toast.LENGTH_LONG).show();
        });

        new Handler(Looper.getMainLooper()).postDelayed(this::goToHomeActivity, 2500); // Ritardo per permettere all'utente di leggere il messaggio
    }

    private void bringOurAppToForeground() {
        try {
            Intent intent = new Intent(this, NavigationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.d("NavigationActivity", "App riportata in primo piano");
        } catch (Exception e) {
            Log.w("NavigationActivity", "Errore nel riportare l'app in primo piano: " + e.getMessage());
        }
    }

    private void showArrivalNotification() {
        try {
            Toast.makeText(this, "🎉 ARRIVO! Tornando all'app...", Toast.LENGTH_LONG).show();
            // Vibrazione (richiede permesso VIBRATE nel manifest)
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(500);
                }
            }
        } catch (Exception e) {
            Log.w("NavigationActivity", "Errore nella notifica di arrivo: " + e.getMessage());
        }
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
        hasArrived = true;
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
        Log.d("NavigationActivity", "App in background - continuando a monitorare la posizione");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("NavigationActivity", "App tornata in foreground");
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
        if (isNavigating && !hasArrived) {
            Log.d("NavigationActivity", "Back pressed - minimizzando l'app");
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }
}