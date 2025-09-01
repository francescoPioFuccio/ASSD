package com.example.app1.ui.home;
import android.view.View;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.app1.R;
import com.example.app1.ui.chat.ChatActivity;
import com.example.app1.ui.login.LoginActivity;
import com.example.app1.ui.musei.MuseiActivity;
import com.example.app1.ui.profile.ProfileActivity;
import com.example.app1.ui.promotion.PuntiBonusActivity;
import com.example.app1.ui.settings.SettingsActivity;
import com.example.app1.util.ThemeHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private TextView welcomeTextView;
    private Button startGameButton;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Inizializza la toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Inizializza il drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        welcomeTextView = findViewById(R.id.welcomeText);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        String userId = intent.getStringExtra("userid");

        if (username != null && !username.isEmpty()) {
            welcomeTextView.setText("Benvenuto, " + username + "!");
        }

        // Aggiorna il nome utente nell'header del NavigationView
        View headerView = navigationView.getHeaderView(0); // Ottieni l'header del NavigationView
        TextView userNameTextView = headerView.findViewById(R.id.nav_username); // Trova il TextView nell'header
        if (username != null && !username.isEmpty()) {
            userNameTextView.setText(username); // Imposta il nome utente
        }

        // Salva l'userId in SharedPreferences
        if (userId != null && !userId.isEmpty()) {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("userid", userId)
                    .apply();
        }

        navigationView.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        startGameButton = findViewById(R.id.startGameButton);
        startGameButton.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getLocationAndShowDialog();
            } else {
                requestLocationPermission();
            }
        });
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndShowDialog();
        } else {
            Toast.makeText(this, "Permesso di localizzazione negato", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLocationAndShowDialog() {
        if (!checkLocationPermission()) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        showTimeLimitDialog(location);
                    } else {
                        Toast.makeText(this, "Posizione non trovata", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTimeLimitDialog(Location currentLocation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tempo a disposizione");
        builder.setMessage("Inserisci il numero di ore disponibili:");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Es. 3");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 20, 50, 0);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Avvia", (dialog, which) -> {
            String timeLimitStr = input.getText().toString();
            if (!timeLimitStr.isEmpty()) {
                int timeLimit = Integer.parseInt(timeLimitStr);
                fetchMusei(currentLocation, timeLimit);
            } else {
                Toast.makeText(this, "Inserisci un tempo valido.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void fetchMusei(Location location, int timeLimitHours) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        int timeLimitMinutes = timeLimitHours * 60;

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("latitude", latitude);
            jsonBody.put("longitude", longitude);
            jsonBody.put("timeLimit", timeLimitMinutes);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String backendUrl = "http://10.0.2.2:8080/Museum/api/location/search"; // <-- sostituiscilo!

        new Thread(() -> {
            try {
                URL url = new URL(backendUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream())) {
                        while (scanner.hasNextLine()) sb.append(scanner.nextLine());
                    }

                    JSONObject jsonResponse = new JSONObject(sb.toString());
                    JSONArray museumsArray = jsonResponse.getJSONArray("museums");

                    ArrayList<HashMap<String, Object>> musei = new ArrayList<>();
                    for (int i = 0; i < museumsArray.length(); i++) {
                        JSONObject obj = museumsArray.getJSONObject(i);
                        HashMap<String, Object> museo = new HashMap<>();
                        museo.put("name", obj.getString("name"));
                        museo.put("latitude", obj.getDouble("latitude"));
                        museo.put("longitude", obj.getDouble("longitude"));
                        museo.put("estimatedVisitTime", obj.getInt("estimatedVisitTime"));
                        musei.add(museo);
                    }

                    runOnUiThread(() -> {
                        Intent intent = new Intent(this, MuseiActivity.class);
                        intent.putExtra("musei_json", museumsArray.toString());
                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Errore: " + responseCode, Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Errore di rete", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();

        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_bonus) {
            startActivity(new Intent(this, PuntiBonusActivity.class));
        } else if (id == R.id.nav_chatbot) {
            startActivity(new Intent(this, ChatActivity.class));
        } else if (id == R.id.nav_logout) {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(this);
    }
}
