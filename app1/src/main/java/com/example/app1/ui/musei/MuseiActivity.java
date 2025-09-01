package com.example.app1.ui.musei;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app1.R;
import com.example.app1.data.Entities.Museo;
import com.example.app1.util.MuseoAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MuseiActivity extends AppCompatActivity {
    private static final String TAG = "MuseiActivity";
    private RecyclerView recyclerView;
    private MuseoAdapter museoAdapter;
    private List<Museo> museoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musei);

        initializeViews();
        loadMuseumData();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewMusei);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        museoAdapter = new MuseoAdapter(museoList);
        recyclerView.setAdapter(museoAdapter);
    }

    private void loadMuseumData() {
        String museiJson = getIntent().getStringExtra("musei_json");
        Log.d(TAG, "JSON ricevuto: " + museiJson);

        if (museiJson == null || museiJson.isEmpty()) {
            Log.w(TAG, "JSON vuoto o null");
            showError("Nessun museo trovato.");
            return;
        }

        try {
            // Parse del JSON array
            JSONArray jsonArray = new JSONArray(museiJson);
            Log.d(TAG, "Array length: " + jsonArray.length());

            // Converti il JSONArray in lista di oggetti Museo
            List<Museo> parsedMusei = parseJsonToMusei(jsonArray);

            if (parsedMusei.isEmpty()) {
                Log.w(TAG, "Lista musei vuota dopo il parsing");
                showError("Nessun museo disponibile.");
                return;
            }

            // Aggiorna l'adapter
            updateMuseiList(parsedMusei);
            Log.d(TAG, "Musei caricati con successo: " + parsedMusei.size());

        } catch (JSONException e) {
            Log.e(TAG, "Errore parsing JSON", e);
            showError("Errore nel caricamento dei musei: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Errore generico", e);
            showError("Errore imprevisto: " + e.getMessage());
        }
    }

    private List<Museo> parseJsonToMusei(JSONArray jsonArray) {
        List<Museo> musei = new ArrayList<>();

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject museumObj = jsonArray.getJSONObject(i);

                String name = museumObj.optString("name", "Nome non disponibile");
                double latitude = museumObj.optDouble("latitude", 0.0);
                double longitude = museumObj.optDouble("longitude", 0.0);
                int estimatedVisitTime = museumObj.optInt("estimatedVisitTime", 0);

                Museo museo = new Museo(name, latitude, longitude, estimatedVisitTime);
                musei.add(museo);

                Log.d(TAG, "Museo parsato: " + name + " (" + latitude + "," + longitude + ")");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Errore nel parsing del singolo museo", e);
        }

        return musei;
    }

    private void updateMuseiList(List<Museo> newMusei) {
        museoList.clear();
        museoList.addAll(newMusei);
        museoAdapter.notifyDataSetChanged();

        // Alternativa se l'adapter ha il metodo updateData
        // museoAdapter.updateData(newMusei);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Errore mostrato all'utente: " + message);
    }
}