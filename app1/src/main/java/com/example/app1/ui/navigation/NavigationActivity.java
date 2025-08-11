package com.example.app1.ui.navigation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.app1.ui.home.HomeActivity;
import com.example.app1.databinding.ActivityNavigationBinding;
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
                if (locationResult == null) return;

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
        
        // Aggiungi bottone per confermare manualmente l'arrivo (temporaneo)
        // binding.confirmArrivalButton.setOnClickListener(v -> onArrivalAtDestination());

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
                        // Apri automaticamente Google Maps
                        if (!hasOpenedMaps) {
                            openGoogleMaps();
                        }
                    }
                });
    }

    private void updateCurrentLocation(Location location) {
        double currentLat = location.getLatitude();
        double currentLng = location.getLongitude();
        currentLatitude = currentLat;
        currentLongitude = currentLng;

        // Calcola la distanza dalla destinazione
        float distance = calculateDistance(currentLat, currentLng, museoLatitudine, museoLongitudine);

        // Aggiorna l'UI con la posizione corrente e distanza
        runOnUiThread(() -> {
            binding.currentLocationTextView.setText(
                    String.format("Posizione: %.6f, %.6f", currentLat, currentLng));
            binding.distanceTextView.setText(
                    String.format("Distanza: %.0f metri", distance));

            if (distance > ARRIVAL_THRESHOLD_METERS) {
                binding.statusTextView.setText("In viaggio verso " + museoNome);
            } else {
                binding.statusTextView.setText("Stai arrivando a destinazione!");
            }
        });

        Log.d("NavigationActivity", "Posizione aggiornata. Distanza: " + distance + " metri da " + museoNome);
    }

    private void checkArrival(Location location) {
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
        try {
            // Usa coordinate destinazione eventualmente corrette se coincidono con la posizione attuale
            double destLat = museoLatitudine;
            double destLng = museoLongitudine;
            if (currentLatitude != null && currentLongitude != null) {
                float samePointMeters = calculateDistance(currentLatitude, currentLongitude, destLat, destLng);
                if (samePointMeters < 15f) { // quasi identiche: applica un piccolo offset (circa 300-500m)
                    double offsetMeters = 400.0;
                    // ~0.000009 deg di lat per metro
                    double degPerMeterLat = 1.0 / 111_111.0;
                    double cosLat = Math.cos(Math.toRadians(currentLatitude));
                    if (cosLat < 0.1) cosLat = 0.1;
                    double degPerMeterLon = 1.0 / (111_111.0 * cosLat);
                    destLat = currentLatitude + offsetMeters * degPerMeterLat;
                    destLng = currentLongitude + offsetMeters * degPerMeterLon;
                    Log.w("NavigationActivity", String.format(Locale.US,
                            "Dest uguale all'origine: applico offset. New dest=(%f,%f)", destLat, destLng));
                }
            }
            // Prova prima con l'URI di navigazione
            // Se abbiamo la posizione corrente, esplicitiamo sia origin che destination per evitare
            // che Maps interpreti la stessa posizione come destinazione
            if (currentLatitude != null && currentLongitude != null) {
                String directionsUri = String.format(Locale.US,
                        "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=walking",
                        currentLatitude, currentLongitude, destLat, destLng);
                Intent directionsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(directionsUri));
                // Preferisci Google Maps se presente, altrimenti lascia scegliere
                directionsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (directionsIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(directionsIntent);
                    hasOpenedMaps = true;
                    binding.statusTextView.setText("Indicazioni aperte - Seguendo il percorso...");
                    Toast.makeText(this, "Indicazioni aperte in Maps", Toast.LENGTH_LONG).show();
                    Log.d("NavigationActivity", "Directions con origin esplicito: " + directionsUri);
                    return;
                }
            }

            // Fallback: schema di navigazione rapido
            String uri = String.format(Locale.US, "google.navigation:q=%f,%f&mode=walking",
                    destLat, destLng);

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Verifica se Google Maps è installato
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
                hasOpenedMaps = true;
                binding.statusTextView.setText("Google Maps aperto - Seguendo il percorso...");
                
                // Mostra un messaggio informativo
                Toast.makeText(this, "Google Maps aperto. Torna qui per fermare la navigazione", Toast.LENGTH_LONG).show();
                
                // Log per debug
                Log.d("NavigationActivity", "Google Maps aperto con successo per: " + museoNome);
            } else {
                // Fallback 1: prova con URI diverso
                try {
                    String fallbackUri;
                    if (currentLatitude != null && currentLongitude != null) {
                        // Usa lo schema saddr/daddr esplicito
                        fallbackUri = String.format(Locale.US,
                                "http://maps.google.com/maps?saddr=%f,%f&daddr=%f,%f",
                                currentLatitude, currentLongitude, destLat, destLng);
                    } else {
                        // Pin su mappa
                        fallbackUri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                                destLat, destLng, destLat, destLng, museoNome);
                    }
                    Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri));
                    fallbackIntent.setPackage("com.google.android.apps.maps");
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    if (fallbackIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(fallbackIntent);
                        hasOpenedMaps = true;
                        binding.statusTextView.setText("Google Maps aperto (fallback) - Seguendo il percorso...");
                        Toast.makeText(this, "Google Maps aperto. Torna qui per fermare la navigazione", Toast.LENGTH_LONG).show();
                        Log.d("NavigationActivity", "Google Maps aperto con fallback per: " + museoNome + ", uri=" + fallbackUri);
                        return;
                    }
                } catch (Exception e) {
                    Log.w("NavigationActivity", "Fallback 1 fallito: " + e.getMessage());
                }
                
                // Fallback 2: apri nel browser web
                String webUri;
                if (currentLatitude != null && currentLongitude != null) {
                    webUri = String.format(Locale.US,
                            "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=walking",
                            currentLatitude, currentLongitude, destLat, destLng);
                } else {
                    webUri = String.format(Locale.US,
                            "https://www.google.com/maps/dir/?api=1&destination=%f,%f&travelmode=walking",
                            destLat, destLng);
                }
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webIntent);
                hasOpenedMaps = true;
                Toast.makeText(this, "Google Maps non installato, apertura nel browser", Toast.LENGTH_SHORT).show();
                Log.d("NavigationActivity", "Apertura nel browser web per: " + museoNome + ", uri=" + webUri);
            }
        } catch (Exception e) {
            Log.e("NavigationActivity", "Errore nell'aprire Google Maps: " + e.getMessage());
            Toast.makeText(this, "Errore nell'aprire Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    private void onArrivalAtDestination() {
        if (!isNavigating) return; // Evita chiamate multiple

        runOnUiThread(() -> {
            binding.statusTextView.setText("🎉 Sei arrivato a " + museoNome + "!");
            Toast.makeText(this, "Benvenuto al " + museoNome + "!", Toast.LENGTH_LONG).show();
        });

        // Ferma gli aggiornamenti di posizione
        stopLocationUpdates();

        // Torna alla HomeActivity dopo 3 secondi
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            goToHomeActivity();
        }, 3000);
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
        Toast.makeText(this, "Navigazione fermata", Toast.LENGTH_SHORT).show();
        goToHomeActivity();
    }

    private void goToHomeActivity() {
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
        // Non fermare gli aggiornamenti di posizione quando l'app va in background
        // per continuare a monitorare l'arrivo
        Log.d("NavigationActivity", "App in background - continuando a monitorare la posizione");
        
        // Se Google Maps è stato aperto, non fare nulla di speciale
        if (hasOpenedMaps) {
            Log.d("NavigationActivity", "Google Maps è aperto, app in background");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("NavigationActivity", "App tornata in foreground");
        
        // Se Google Maps è stato aperto, aggiorna lo stato
        if (hasOpenedMaps) {
            binding.statusTextView.setText("Google Maps aperto - Torna qui per fermare la navigazione");
            Log.d("NavigationActivity", "App tornata in foreground dopo apertura Google Maps");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    @Override
    public void onBackPressed() {
        // Non fermare la navigazione quando si preme back, solo minimizza l'app
        Log.d("NavigationActivity", "Back pressed - minimizzando l'app");
        moveTaskToBack(true);
    }
}