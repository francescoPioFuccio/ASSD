package com.example.app1.ui.musei;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app1.databinding.FragmentMuseiBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

/**
 * Fragment che mostra la lista dei musei, riutilizzando la logica di MuseiActivity.
 */
public class MuseiFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;

    private FragmentMuseiBinding binding;
    private MuseiViewModel viewModel;
    private MuseiAdapter museiAdapter;
    private String currentUserId;
    private FusedLocationProviderClient fusedLocationClient;

    // Coordinate di default (Roma) se la localizzazione non è disponibile
    private final double defaultLatitude = 41.9028;
    private final double defaultLongitude = 12.4964;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMuseiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MuseiViewModel.class);
        Context context = requireContext();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        currentUserId = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("userid", null);
        Log.d("MuseiFragment", "User ID recuperato: " + currentUserId);
        if (currentUserId == null) {
            Log.e("MuseiFragment", "User ID non trovato nelle SharedPreferences");
            Toast.makeText(context, "Errore: User ID non trovato", Toast.LENGTH_LONG).show();
            return;
        }

        setupRecyclerView();
        setupObservers();
        setupClickListeners();

        checkLocationPermissionAndLoadData();
    }

    private void setupRecyclerView() {
        museiAdapter = new MuseiAdapter();
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
                // Se le coordinate non sono fornite nell'elenco, recupera i dettagli prima della navigazione
                if (lat == 0.0 && lon == 0.0) {
                    viewModel.fetchDettaglioMuseo(museoId, currentUserId, new MuseiViewModel.MuseoDetailCallback() {
                        @Override
                        public void onSuccess(org.json.JSONObject dettaglio) {
                            if (!isAdded()) return;
                            double[] coords = extractCoordinatesFromDetail(dettaglio);
                            final double dLat = coords[0];
                            final double dLon = coords[1];
                            requireActivity().runOnUiThread(() -> {
                                if (dLat != 0.0 || dLon != 0.0) {
                                    startNavigationTo(dLat, dLon, museoNome);
                                } else {
                                    Toast.makeText(requireContext(), "Coordinate non disponibili per questo museo", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Errore nel recupero coordinate", Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    startNavigationTo(lat, lon, museoNome);
                }
            }
        });
        binding.museiRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.museiRecyclerView.setAdapter(museiAdapter);
    }

    private void startNavigationTo(double lat, double lon, String label) {
        try {
            String uri = "google.navigation:q=" + lat + "," + lon + "&mode=d";
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
                // registra geofence per rilevare l'arrivo
                com.example.app1.ui.navigation.NavigationGeofenceHelper.registerArrivalGeofence(requireContext(), label, lat, lon);
            } else {
                // fallback a geo: URI generico
                android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + label + ")"));
                startActivity(mapIntent);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Impossibile avviare la navigazione", Toast.LENGTH_SHORT).show();
        }
    }

    private double[] extractCoordinatesFromDetail(org.json.JSONObject dettaglio) {
        try {
            // tenta vari possibili nomi di campo e strutture
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
        viewModel.getMuseiData().observe(getViewLifecycleOwner(), musei -> {
            if (musei != null) {
                museiAdapter.updateData(musei);
                binding.emptyStateTextView.setVisibility(musei.length() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getSearchInfo().observe(getViewLifecycleOwner(), info -> {
            if (info != null) {
                binding.searchInfoTextView.setText(info);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                binding.emptyStateTextView.setVisibility(View.VISIBLE);
                binding.emptyStateTextView.setText("Errore nel caricamento dei musei");
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                binding.loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                binding.museiRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void setupClickListeners() {
        // Nascondi il bottone back in contesto Home
        binding.backButton.setVisibility(View.GONE);

        binding.refreshButton.setOnClickListener(v -> {
            viewModel.clearError();
            checkLocationPermissionAndLoadData();
        });

        binding.filterButton.setOnClickListener(v -> showFilterDialog());
    }

    private void checkLocationPermissionAndLoadData() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocationAndLoadMusei();
        }
    }

    private void getCurrentLocationAndLoadMusei() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            loadMuseiWithDefaultLocation();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        loadMusei(location.getLatitude(), location.getLongitude());
                    } else {
                        loadMuseiWithDefaultLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    loadMuseiWithDefaultLocation();
                });
    }

    private void loadMuseiWithDefaultLocation() {
        loadMusei(defaultLatitude, defaultLongitude);
    }

    private void loadMusei(double latitude, double longitude) {
        String preferenze = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("preferenze_musei", "");
        Integer raggio = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getInt("raggio_ricerca", 20);
        viewModel.fetchMuseiRaccomandati(currentUserId, latitude, longitude, preferenze, raggio);
    }

    private void showMuseoDetails(String museoId) {
        viewModel.fetchDettaglioMuseo(museoId, currentUserId, new MuseiViewModel.MuseoDetailCallback() {
            @Override
            public void onSuccess(org.json.JSONObject dettaglio) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> showMuseoDetailDialog(dettaglio));
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Errore nel caricamento dettagli: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void addToFavorites(String museoId, String museoNome) {
        viewModel.aggiungiAiPreferiti(currentUserId, museoId, new MuseiViewModel.FavoritiCallback() {
            @Override
            public void onSuccess(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Errore: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showMuseoDetailDialog(org.json.JSONObject dettaglio) {
        try {
            String nome = dettaglio.getString("nome");
            String tipologia = dettaglio.getString("tipologia");
            String descrizione = dettaglio.getString("descrizione");
            String info = nome + "\n" + tipologia + "\n" + descrizione;
            Toast.makeText(requireContext(), info, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Errore nella visualizzazione dettagli", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFilterDialog() {
        Toast.makeText(requireContext(), "Filtri disponibili: Arte Contemporanea, Archeologia, Scienze Naturali", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndLoadMusei();
            } else {
                Toast.makeText(requireContext(), "Permessi di localizzazione negati. Uso posizione di default (Roma).", Toast.LENGTH_LONG).show();
                loadMuseiWithDefaultLocation();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


