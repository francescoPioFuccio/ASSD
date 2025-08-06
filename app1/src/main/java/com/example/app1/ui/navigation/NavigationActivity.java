package com.example.app1.ui.navigation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.app1.R;
import com.example.app1.databinding.ActivityNavigationBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import org.json.JSONException;
import org.json.JSONObject;

public class NavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityNavigationBinding binding;
    private NavigationViewModel viewModel;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    private String currentUserId;
    private LatLng destinationLatLng;
    private LatLng currentLocationLatLng;
    private String destinationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inizializza il ViewModel
        viewModel = new ViewModelProvider(this).get(NavigationViewModel.class);

        // Inizializza location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Recupera l'ID utente
        currentUserId = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("userid", null);
        Log.d("NavigationActivity", "User ID recuperato: " + currentUserId);

        if (currentUserId == null) {
            Log.e("NavigationActivity", "User ID non trovato nelle SharedPreferences");
            Toast.makeText(this, "Errore: User ID non trovato", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Inizializza la mappa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupObservers();
        setupClickListeners();

        // Richiedi permessi di localizzazione
        if (checkLocationPermissions()) {
            loadDestination();
        } else {
            requestLocationPermissions();
        }
    }

    private void setupObservers() {
        // Osserva i dati della destinazione
        viewModel.getDestinationData().observe(this, destinationData -> {
            if (destinationData != null) {
                try {
                    parseDestinationData(destinationData);
                    Log.d("NavigationActivity", "Destinazione ricevuta: " + destinationData.toString());
                } catch (JSONException e) {
                    Log.e("NavigationActivity", "Errore nel parsing della destinazione", e);
                    Toast.makeText(this, "Errore nel parsing della destinazione", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Osserva gli errori
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                Log.e("NavigationActivity", "Errore: " + error);
            }
        });

        // Osserva lo stato di loading
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                binding.loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.startNavigationButton.setEnabled(!isLoading && destinationLatLng != null);
            }
        });
    }

    private void parseDestinationData(JSONObject data) throws JSONException {
        // Assumendo che il JSON contenga: {"nome": "Museo XYZ", "latitudine": 40.7128, "longitudine": -74.0060}
        destinationName = data.optString("nome", "Destinazione");
        double lat = data.optDouble("latitudine", 0.0);
        double lng = data.optDouble("longitudine", 0.0);

        destinationLatLng = new LatLng(lat, lng);

        binding.destinationTextView.setText("Destinazione: " + destinationName);
        binding.startNavigationButton.setEnabled(true);

        // Aggiungi marker sulla mappa
        if (googleMap != null) {
            addDestinationMarker();
            getCurrentLocationAndShowRoute();
        }
    }

    private void setupClickListeners() {
        // Bottone per avviare la navigazione con Google Maps
        binding.startNavigationButton.setOnClickListener(v -> startGoogleMapsNavigation());

        // Bottone torna indietro
        binding.backButton.setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Configurazioni della mappa
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Abilita la posizione corrente se abbiamo i permessi
        if (checkLocationPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            googleMap.setMyLocationEnabled(true);
        }

        // Se abbiamo già la destinazione, mostrala
        if (destinationLatLng != null) {
            addDestinationMarker();
            getCurrentLocationAndShowRoute();
        }
    }

    private void addDestinationMarker() {
        if (googleMap != null && destinationLatLng != null) {
            googleMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title(destinationName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    private void getCurrentLocationAndShowRoute() {
        if (!checkLocationPermissions()) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null && destinationLatLng != null) {
                            currentLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                            // Aggiungi marker per posizione corrente
                            googleMap.addMarker(new MarkerOptions()
                                    .position(currentLocationLatLng)
                                    .title("La tua posizione")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                            // Disegna una linea dritta (per una vera navigazione servirebbero le Directions API)
                            drawStraightLine();

                            // Centra la mappa per mostrare entrambi i punti
                            centerMapOnBothPoints();
                        }
                    }
                });
    }

    private void drawStraightLine() {
        if (currentLocationLatLng != null && destinationLatLng != null) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(currentLocationLatLng)
                    .add(destinationLatLng)
                    .width(5)
                    .color(Color.BLUE);

            googleMap.addPolyline(polylineOptions);
        }
    }

    private void centerMapOnBothPoints() {
        if (currentLocationLatLng != null && destinationLatLng != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(currentLocationLatLng);
            builder.include(destinationLatLng);

            LatLngBounds bounds = builder.build();
            int padding = 100; // padding in pixels

            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }

    private void startGoogleMapsNavigation() {
        if (destinationLatLng != null) {
            // Apri Google Maps per la navigazione
            String uri = "google.navigation:q=" + destinationLatLng.latitude + "," + destinationLatLng.longitude;
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback: apri nel browser
                String url = "https://www.google.com/maps/dir/?api=1&destination="
                        + destinationLatLng.latitude + "," + destinationLatLng.longitude;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        }
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void loadDestination() {
        viewModel.fetchDestination(currentUserId);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permesso concesso
                if (googleMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    googleMap.setMyLocationEnabled(true);
                }
                loadDestination();
            } else {
                // Permesso negato
                Toast.makeText(this, "Permesso di localizzazione necessario per la navigazione", Toast.LENGTH_LONG).show();
            }
        }
    }
}