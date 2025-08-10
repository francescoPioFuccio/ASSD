package com.example.app1.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.app1.R;
import com.example.app1.ui.chat.ChatActivity;
import com.example.app1.ui.login.LoginActivity;
import com.example.app1.ui.musei.MuseiActivity;
import com.example.app1.ui.promotion.PuntiBonusActivity;
import com.example.app1.ui.settings.SettingsActivity;
import com.example.app1.util.ThemeHelper;
import com.google.android.material.navigation.NavigationView;
import com.example.app1.ui.profile.ProfileActivity;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private TextView welcomeTextView;

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

        // Ottieni il nome utente e l'ID passato da LoginActivity
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

        // Listener del navigation drawer
        navigationView.setNavigationItemSelectedListener(this);

        // Configura il toggle per il drawer
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Mostra l'icona hamburger nella toolbar (se ActionBar non è null)
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu); // opzionale
        }

        // Edge-to-edge layout padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Chiude il drawer quando si seleziona una voce
        drawerLayout.closeDrawers();

        int id = item.getItemId(); // Ottieni l'ID della voce selezionata

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));

        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));

        }  else if (id == R.id.nav_history) {
            // startActivity(new Intent(this, HistoryActivity.class));
            Toast.makeText(this, "History Activity not implemented yet", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_bonus) {
            startActivity(new Intent(this, PuntiBonusActivity.class));
            //Toast.makeText(this, "Bonus Points Activity not implemented yet", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_chatbot) {
            startActivity(new Intent(this, ChatActivity.class));
            //Toast.makeText(this, "ChatBot Activity not implemented yet", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_info) {
            startActivity(new Intent(this, MuseiActivity.class));
            //Toast.makeText(this, "Info Activity not implemented yet", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            // logout logica
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            // Se nessun ID corrisponde, restituisci false
            return false;
        }

        // Se una voce è stata gestita, restituisci true
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(this);
    }
}
