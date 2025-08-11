package com.example.app1.ui.musei;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.app1.databinding.ActivityMuseiBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import org.json.JSONArray;

public class MuseiActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityMuseiBinding binding;
    private MuseiViewModel viewModel;
    private MuseiAdapter museiAdapter;
    private String currentUserId;
    private FusedLocationProviderClient fusedLocationClient;

    // Coordinate di default (Roma) se la localizzazione non è disponibile
    private double defaultLatitude = 41.9028;
    private double defaultLongitude = 12.4964;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMuseiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inizializza il ViewModel
        viewModel = new ViewModelProvider(this).get(MuseiViewModel.class);

        // Inizializza il client di localizzazione
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Recupera l'ID utente
        currentUserId = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("userid", null);
        Log.d("MuseiActivity", "User ID recuperato: " + currentUserId);

        if (currentUserId == null) {
            Log.e("MuseiActivity", "User ID non trovato nelle SharedPreferences");
            Toast.makeText(this, "Errore: User ID non trovato", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupRecyclerView();
        setupObservers();
        setupClickListeners();

        // Richiedi permessi di localizzazione e carica i dati
        checkLocationPermissionAndLoadData();
    }

    private void setupRecyclerView() {
        museiAdapter = new MuseiAdapter();

        // Imposta il callback per le azioni dell'adapter
        museiAdapter.setOnMuseoActionListener(new MuseiAdapter.OnMuseoActionListener() {
            @Override
            public void onMuseoClick(String museoId) {
                showMuseoDetails(museoId);
            }

            @Override
            public void onFavoritiClick(String museoId, String museoNome) {
                addToFavorites(museoId, museoNome);
            }

            @Override
            public void onVaiAlMuseoClick(String museoId, String museoNome, double lat, double lon) {
                if (lat == 0.0 && lon == 0.0) {
                    viewModel.fetchDettaglioMuseo(museoId, currentUserId, new MuseiViewModel.MuseoDetailCallback() {
                        @Override
                        public void onSuccess(org.json.JSONObject dettaglio) {
                            double[] coords = extractCoordinatesFromDetail(dettaglio);
                            runOnUiThread(() -> {
                                if (coords[0] != 0.0 || coords[1] != 0.0) {
                                    startNavigationTo(coords[0], coords[1], museoNome);
                                } else {
                                    Toast.makeText(MuseiActivity.this, "Coordinate non disponibili per questo museo", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> Toast.makeText(MuseiActivity.this, "Errore nel recupero coordinate", Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    startNavigationTo(lat, lon, museoNome);
                }
            }
        });

        binding.museiRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.museiRecyclerView.setAdapter(museiAdapter);
    }

    private void startNavigationTo(double lat, double lon, String label) {
        try {
            String uri = "google.navigation:q=" + lat + "," + lon + "&mode=d";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                com.example.app1.ui.navigation.NavigationGeofenceHelper.registerArrivalGeofence(this, label, lat, lon);
            } else {
                Intent mapIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + label + ")"));
                startActivity(mapIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Impossibile avviare la navigazione", Toast.LENGTH_SHORT).show();
        }
    }

    private double[] extractCoordinatesFromDetail(org.json.JSONObject dettaglio) {
        try {
            org.json.JSONObject src = dettaglio;
            if (dettaglio.has("location")) {
                src = dettaglio.getJSONObject("location");
            }
            double lat = firstPresentDouble(src, new String[]{"lat", "latitude", "latitudine"});
            double lon = firstPresentDouble(src, new String[]{"lon", "lng", "longitude", "longitudine"});
            return new double[]{lat, lon};
        } catch (Exception e) {
            return new double[]{0.0, 0.0};
        }
    }

    private double firstPresentDouble(org.json.JSONObject obj, String[] keys) {
        for (String k : keys) {
            if (obj.has(k)) {
                try {
                    return obj.getDouble(k);
                } catch (Exception ignored) {}
            }
        }
        return 0.0;
    }

    private void setupObservers() {
        // Osserva i musei
        viewModel.getMuseiData().observe(this, musei -> {
            if (musei != null) {
                Log.d("MuseiActivity", "Musei ricevuti: " + musei.length());
                museiAdapter.updateData(musei);

                // Mostra/nascondi messaggio vuoto
                binding.emptyStateTextView.setVisibility(musei.length() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        // Osserva le informazioni di ricerca
        viewModel.getSearchInfo().observe(this, info -> {
            if (info != null) {
                binding.searchInfoTextView.setText(info);
            }
        });

        // Osserva gli errori
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e("MuseiActivity", "Errore: " + error);
                binding.emptyStateTextView.setVisibility(View.VISIBLE);
                binding.emptyStateTextView.setText("Errore nel caricamento dei musei");
            }
        });

        // Osserva lo stato di loading
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.museiRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            }
        });

        // Osserva la richiesta di permessi di localizzazione
        viewModel.getLocationPermissionNeeded().observe(this, needed -> {
            if (needed != null && needed) {
                requestLocationPermissions();
            }
        });
    }

    private void setupClickListeners() {
        // Bottone torna indietro
        binding.backButton.setOnClickListener(v -> finish());

        // Bottone refresh
        binding.refreshButton.setOnClickListener(v -> {
            viewModel.clearError();
            checkLocationPermissionAndLoadData();
        });

        // Bottone filtri (opzionale)
        binding.filterButton.setOnClickListener(v -> {
            // Qui puoi implementare un dialog per scegliere le preferenze
            showFilterDialog();
        });
    }

    private void checkLocationPermissionAndLoadData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Richiedi permessi
            requestLocationPermissions();
        } else {
            // Permessi già concessi, ottieni la posizione
            getCurrentLocationAndLoadMusei();
        }
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void getCurrentLocationAndLoadMusei() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            loadMuseiWithDefaultLocation();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d("MuseiActivity", "Posizione ottenuta: " +
                                location.getLatitude() + ", " + location.getLongitude());
                        loadMusei(location.getLatitude(), location.getLongitude());
                    } else {
                        Log.w("MuseiActivity", "Posizione non disponibile, uso coordinate di default");
                        loadMuseiWithDefaultLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MuseiActivity", "Errore nell'ottenere la posizione: " + e.getMessage());
                    loadMuseiWithDefaultLocation();
                });
    }

    private void loadMuseiWithDefaultLocation() {
        Log.d("MuseiActivity", "Carico musei con posizione di default: " +
                defaultLatitude + ", " + defaultLongitude);
        loadMusei(defaultLatitude, defaultLongitude);
    }

    private void loadMusei(double latitude, double longitude) {
        // Per ora usiamo preferenze di default, ma puoi implementare un sistema per salvarle
        String preferenze = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("preferenze_musei", "");

        Integer raggio = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getInt("raggio_ricerca", 20);

        viewModel.fetchMuseiRaccomandati(currentUserId, latitude, longitude, preferenze, raggio);
    }

    private void showMuseoDetails(String museoId) {
        Log.d("MuseiActivity", "Mostrando dettagli per museo: " + museoId);

        viewModel.fetchDettaglioMuseo(museoId, currentUserId, new MuseiViewModel.MuseoDetailCallback() {
            @Override
            public void onSuccess(org.json.JSONObject dettaglio) {
                runOnUiThread(() -> {
                    // Qui puoi implementare un dialog o una nuova activity per i dettagli
                    showMuseoDetailDialog(dettaglio);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(MuseiActivity.this, "Errore nel caricamento dettagli: " + error,
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void addToFavorites(String museoId, String museoNome) {
        Log.d("MuseiActivity", "Aggiungendo ai preferiti: " + museoNome);

        viewModel.aggiungiAiPreferiti(currentUserId, museoId, new MuseiViewModel.FavoritiCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() ->
                        Toast.makeText(MuseiActivity.this, message, Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(MuseiActivity.this, "Errore: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showMuseoDetailDialog(org.json.JSONObject dettaglio) {
        // Implementa un dialog o activity per mostrare i dettagli
        // Per ora mostra solo un Toast con le info principali
        try {
            String nome = dettaglio.getString("nome");
            String tipologia = dettaglio.getString("tipologia");
            String descrizione = dettaglio.getString("descrizione");

            String info = nome + "\n" + tipologia + "\n" + descrizione;
            Toast.makeText(this, info, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Errore nella visualizzazione dettagli", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFilterDialog() {
        // Implementa un dialog per scegliere le preferenze
        // Per ora mostra solo un Toast
        Toast.makeText(this, "Filtri disponibili: Arte Contemporanea, Archeologia, Scienze Naturali",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MuseiActivity", "Permessi di localizzazione concessi");
                getCurrentLocationAndLoadMusei();
            } else {
                Log.w("MuseiActivity", "Permessi di localizzazione negati, uso posizione di default");
                Toast.makeText(this, "Permessi di localizzazione negati. Uso posizione di default (Roma).",
                        Toast.LENGTH_LONG).show();
                loadMuseiWithDefaultLocation();
            }
        }
    }
}