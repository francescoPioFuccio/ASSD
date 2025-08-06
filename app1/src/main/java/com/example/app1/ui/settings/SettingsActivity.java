package com.example.app1.ui.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.app1.R;
import com.example.app1.util.ThemeHelper;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final int REQUEST_LOCATION_PERMISSION = 1001;
        private SwitchPreferenceCompat locationPref;
        private boolean userToggledSwitch = false;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings_container, rootKey);

            // Tema scuro
            SwitchPreferenceCompat darkModePref = findPreference("dark_mode");
            if (darkModePref != null) {
                darkModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    requireActivity().recreate();
                    return true;
                });
            }

            // Lingua
            ListPreference languagePref = findPreference("language");
            if (languagePref != null) {
                languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    requireActivity().recreate();
                    return true;
                });
            }

            // Posizione
            locationPref = findPreference("enable_location");
            if (locationPref != null) {
                locationPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enable = (Boolean) newValue;
                    userToggledSwitch = true;

                    if (enable) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            // Serve richiedere permesso
                            requestPermissions(
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION
                            );
                            return false; // Aspetta risultato prima di attivare switch
                        }
                    }
                    return true; // ok disattivarlo oppure già concesso
                });
            }

            // Notifiche
            SwitchPreferenceCompat notificationsPref = findPreference("enable_notifications");
            if (notificationsPref != null) {
                notificationsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enable = (Boolean) newValue;
                    if (enable) {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                        startActivity(intent);
                    }
                    return true;
                });
            }

            // Privacy
            Preference privacyPref = findPreference("privacy_policy");
            if (privacyPref != null) {
                privacyPref.setOnPreferenceClickListener(preference -> {
                    Toast.makeText(requireContext(), "Privacy policy (da implementare)", Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == REQUEST_LOCATION_PERMISSION && locationPref != null) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Permesso posizione concesso", Toast.LENGTH_SHORT).show();
                    locationPref.setChecked(true); // ✅ Attiva switch
                } else {
                    Toast.makeText(requireContext(), "Permesso posizione negato", Toast.LENGTH_SHORT).show();
                    locationPref.setChecked(false); // ❌ Lascia disattivato
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeHelper.applyTheme(this);
    }
}