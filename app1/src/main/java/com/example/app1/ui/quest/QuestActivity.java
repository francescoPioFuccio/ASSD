package com.example.app1.ui.quest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.app.AlertDialog;

import android.content.Context;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.app1.R;
import com.example.app1.databinding.ActivityQuestBinding;
import com.example.app1.ui.quest.Quest;
import com.example.app1.ui.home.HomeActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuestActivity extends AppCompatActivity implements QuestAdapter.OnQuestActionListener, QuestDetailsFragment.OnQuestActionListener {

    private static final String TAG = "QuestActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final int REQUEST_IMAGE_CAPTURE = 1002;

    private ActivityQuestBinding binding;
    private QuestViewModel viewModel;
    private QuestAdapter questAdapter;

    // Dati passati dall'attività precedente
    private String userId;
    private String museoId;
    private String museoNome;

    // Gestione foto
    private String currentPhotoPath;
    private Quest currentQuestForPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Recupera dati dall'intent
        getDataFromIntent();

        // Inizializza ViewModel
        viewModel = new ViewModelProvider(this).get(QuestViewModel.class);

        // Setup UI
        setupUI();
        setupRecyclerView();
        setupObservers();
        setupSwipeRefresh();

        // Carica le quest
        loadQuest();
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();

        userId = getSharedPreferences("user", MODE_PRIVATE).getString("userId", "");
        museoId = getSharedPreferences("museo", MODE_PRIVATE).getString("museo", "");
        museoNome = getSharedPreferences("museo", MODE_PRIVATE).getString("museo_nome", "");
        Log.d(TAG, "QuestActivity: " + userId);
        Log.d(TAG, "QuestActivity: " + museoId);
        Log.d(TAG, "QuestActivity: " + museoNome);
        // Valori di default per testing se mancanti
        if (userId == null) userId = "user_test_123";
        if (museoId == null) museoId = "MUS_ARTE";
        if (museoNome == null) museoNome = "Museo d'Arte";

        Log.d(TAG, "Dati ricevuti - UserId: " + userId + ", MuseoId: " + museoId + ", Nome: " + museoNome);
    }

    private void setupUI() {
        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Quest - " + museoNome);
        }

        // Setup bottone back
        binding.backButton.setOnClickListener(v -> finish());

        // Inizialmente nascondi il messaggio di quest completate
        binding.allQuestsCompletedLayout.setVisibility(View.GONE);

        // Setup bottone torna alla home
        binding.goToHomeButton.setOnClickListener(v -> goToHome());
    }

    private void setupRecyclerView() {
        questAdapter = new QuestAdapter(this, this);
        binding.questRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.questRecyclerView.setAdapter(questAdapter);
    }

    private void setupObservers() {
        // Osserva la lista delle quest
        viewModel.getQuestList().observe(this, questList -> {
            questAdapter.updateQuestList(questList);

            if (questList == null || questList.isEmpty()) {
                binding.questRecyclerView.setVisibility(View.GONE);
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
            } else {
                binding.questRecyclerView.setVisibility(View.VISIBLE);
                binding.emptyStateLayout.setVisibility(View.GONE);
            }

            updateQuestCounter(questList != null ? questList.size() : 0);
        });

        // Osserva lo stato di caricamento
        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.swipeRefresh.setRefreshing(isLoading);
        });

        // Osserva gli errori
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                viewModel.clearMessages();
            }
        });

        // Osserva i messaggi di successo
        viewModel.getSuccessMessage().observe(this, successMessage -> {
            if (successMessage != null) {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });

        // Osserva il completamento di tutte le quest
        viewModel.getAllQuestsCompleted().observe(this, allCompleted -> {
            if (allCompleted) {
                showAllQuestsCompletedMessage();
            } else {
                binding.allQuestsCompletedLayout.setVisibility(View.GONE);
            }
        });

        // Osserva i risultati dell'analisi delle foto
        viewModel.getQuestAnalysisResult().observe(this, quest -> {
            if (quest != null && quest.isCompletata()) {
                // Quest completata con successo, aggiorna la UI
                questAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshQuest();
        });

        // Colori del refresh indicator
        binding.swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void loadQuest() {
        Log.d(TAG, "Caricamento quest per museo: " + museoId);
        viewModel.loadQuestDisponibili(userId, museoId, null, null);
    }

    private void updateQuestCounter(int count) {
        String counterText = count == 1 ? count + " quest disponibile" : count + " quest disponibili";
        binding.questCounterText.setText(counterText);
    }

    private void showAllQuestsCompletedMessage() {
        binding.questRecyclerView.setVisibility(View.GONE);
        binding.emptyStateLayout.setVisibility(View.GONE);
        binding.allQuestsCompletedLayout.setVisibility(View.VISIBLE);

        // Auto-ritorno alla home dopo 3 secondi
        binding.allQuestsCompletedLayout.postDelayed(() -> {
            if (!isFinishing()) {
                goToHome();
            }
        }, 3000);
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // === IMPLEMENTAZIONE INTERFACCE ADAPTER E FRAGMENT ===

    @Override
    public void onStartQuest(Quest quest) {
        Log.d(TAG, "Avvio quest: " + quest.getIdQuest());
        viewModel.iniziaQuest(quest.getIdQuest());
    }

    @Override
    public void onTakePhoto(Quest quest) {
        Log.d(TAG, "Richiesta foto per quest: " + quest.getIdQuest());
        currentQuestForPhoto = quest;

        if (checkCameraPermission()) {
            // Prima prova con intent normale
            debugCameraSystem();

            // Se non ci sono app camera, usa implementazione diretta
            PackageManager pm = getPackageManager();
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(pm) != null) {
                openCamera(); // Metodo originale
            } else {
                Log.d(TAG, "Nessuna app camera trovata, uso implementazione diretta");

            }
        } else {
            requestCameraPermission();
        }
    }

    @Override
    public void onViewDetails(Quest quest) {
        Log.d(TAG, "Visualizza dettagli quest: " + quest.getIdQuest());
        showQuestDetails(quest);
    }

    private void showQuestDetails(Quest quest) {
        QuestDetailsFragment detailsFragment = QuestDetailsFragment.newInstance(quest);
        detailsFragment.setOnQuestActionListener(this);
        detailsFragment.show(getSupportFragmentManager(), "quest_details");
    }

    // === GESTIONE CAMERA E PERMESSI ===

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                REQUEST_CAMERA_PERMISSION);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Verifica se c'è un'app che può gestire l'intent
        PackageManager packageManager = getPackageManager();
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Crea file per salvare la foto
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Errore creazione file foto: " + ex.getMessage());
                Toast.makeText(this, "Errore nella creazione del file foto", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.app1.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (Exception e) {
                    Log.e(TAG, "Errore avvio camera: " + e.getMessage());
                    showCameraAlternatives();
                }
            }
        } else {
            Log.w(TAG, "Nessuna app camera trovata");
            showCameraAlternatives();
        }
    }

    private void showCameraAlternatives() {
        // Mostra dialog con alternative
        new AlertDialog.Builder(this)
                .setTitle("Camera non disponibile")
                .setMessage("Non è stata trovata un'app camera compatibile. " +
                        "Vuoi provare con un'altra app o installare Google Camera?")
                .setPositiveButton("Prova altre app", (dialog, which) -> {
                    tryAlternativeCameraIntent();
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void tryAlternativeCameraIntent() {
        // Prova con intent generico
        Intent cameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.app1.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            } catch (Exception e) {
                Toast.makeText(this, "Impossibile avviare la camera", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this,
                    "Camera non disponibile. Installa un'app camera dal Play Store",
                    Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        // Crea nome file univoco
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "QUEST_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefisso */
                ".jpg",         /* suffisso */
                storageDir      /* directory */
        );

        // Salva il percorso per l'uso nella callback
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "File foto creato: " + currentPhotoPath);

        return image;
    }

    // === CALLBACK E RISULTATI ===

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permessi camera concessi");
                openCamera();
            } else {
                Log.w(TAG, "Permessi camera negati");
                Toast.makeText(this, "Permessi camera necessari per completare le quest",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d(TAG, "Foto scattata con successo: " + currentPhotoPath);

            if (currentPhotoPath != null && currentQuestForPhoto != null) {
                File fotoFile = new File(currentPhotoPath);
                if (fotoFile.exists()) {
                    // Avvia analisi foto
                    Toast.makeText(this, "Analizzando la foto...", Toast.LENGTH_SHORT).show();
                    viewModel.analizzaFotoQuest(currentQuestForPhoto, fotoFile);
                } else {
                    Log.e(TAG, "File foto non trovato: " + currentPhotoPath);
                    Toast.makeText(this, "Errore: file foto non trovato", Toast.LENGTH_SHORT).show();
                }
            }

            // Reset variabili
            currentPhotoPath = null;
            currentQuestForPhoto = null;

        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Scatto foto annullato dall'utente");
            currentPhotoPath = null;
            currentQuestForPhoto = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Pulizia file temporanei
        if (currentPhotoPath != null) {
            File tempFile = new File(currentPhotoPath);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private void debugCameraSystem() {
        Log.d(TAG, "=== DEBUG CAMERA SYSTEM ===");

        PackageManager pm = getPackageManager();

        // 1. Verifica se il sistema ha la feature camera
        boolean hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        boolean hasCameraAny = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        Log.d(TAG, "Sistema ha CAMERA: " + hasCamera);
        Log.d(TAG, "Sistema ha CAMERA_ANY: " + hasCameraAny);

        // 2. Verifica intent MediaStore.ACTION_IMAGE_CAPTURE
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> cameraApps = pm.queryIntentActivities(takePictureIntent, 0);
        Log.d(TAG, "App per MediaStore.ACTION_IMAGE_CAPTURE: " + cameraApps.size());

        for (ResolveInfo info : cameraApps) {
            String packageName = info.activityInfo.packageName;
            String activityName = info.activityInfo.name;
            Log.d(TAG, "  - " + packageName + "/" + activityName);
        }



        // 4. Verifica intent alternativi
        Intent altIntent1 = new Intent("android.media.action.IMAGE_CAPTURE");
        List<ResolveInfo> altApps1 = pm.queryIntentActivities(altIntent1, 0);
        Log.d(TAG, "App per android.media.action.IMAGE_CAPTURE: " + altApps1.size());

        Intent altIntent2 = new Intent(Intent.ACTION_MAIN);

    }
}

