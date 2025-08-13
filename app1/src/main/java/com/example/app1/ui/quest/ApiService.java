package com.example.app1.ui.quest;

import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.*;
import org.json.JSONObject;

public class ApiService {

    private static final String TAG = "ApiService";
    private static final String BASE_URL_QUEST = "http://10.0.2.2:8085/gestionequest/api/quest";
    private static final String BASE_URL_OPERA = "http://10.0.2.2:8085/gestioneopere/api/opera";
    private static final int TIMEOUT_MS = 15000;

    /**
     * Ottiene le quest disponibili per un museo
     */
    public String getQuestDisponibili(String userId, String museoId, String preferenze, String difficolta)
            throws Exception {

        StringBuilder urlBuilder = new StringBuilder(BASE_URL_QUEST + "/disponibili?");
        urlBuilder.append("userId=").append(URLEncoder.encode(userId, "UTF-8"));
        urlBuilder.append("&museoId=").append(URLEncoder.encode(museoId, "UTF-8"));

        if (preferenze != null && !preferenze.trim().isEmpty()) {
            urlBuilder.append("&preferenze=").append(URLEncoder.encode(preferenze, "UTF-8"));
        }

        if (difficolta != null && !difficolta.trim().isEmpty()) {
            urlBuilder.append("&difficolta=").append(URLEncoder.encode(difficolta, "UTF-8"));
        }

        String urlString = urlBuilder.toString();
        Log.d(TAG, "GET Quest Disponibili: " + urlString);

        return executeGetRequest(urlString);
    }

    /**
     * Avvia una quest specifica
     */
    public String iniziaQuest(String userId, String questId, String museoId) throws Exception {
        String urlString = BASE_URL_QUEST + "/inizia";
        Log.d(TAG, "POST Inizio Quest: " + urlString);

        JSONObject requestBody = new JSONObject();
        requestBody.put("userId", userId);
        requestBody.put("questId", questId);
        requestBody.put("museoId", museoId);
        return executePostRequest(urlString, requestBody.toString());
    }

    /**
     * Completa una quest
     */
    public String completaQuest(String userId, String questId, int tempoCompletamento) throws Exception {
        String urlString = BASE_URL_QUEST + "/completa";
        Log.d(TAG, "POST Completa Quest: " + urlString);

        JSONObject requestBody = new JSONObject();
        requestBody.put("userId", userId);
        requestBody.put("questId", questId);
        requestBody.put("tempoCompletamento", tempoCompletamento);

        return executePostRequest(urlString, requestBody.toString());
    }

    /**
     * Analizza una foto per verificare se completa una quest
     */
    public String analizzaFoto(File fotoFile, String userId, String descrizioneQuest) throws Exception {
        String urlString = BASE_URL_OPERA + "/analizza-foto";
        Log.d(TAG, "POST Analizza Foto: " + urlString);

        return executeMultipartRequest(urlString, fotoFile, userId, descrizioneQuest);
    }

    /**
     * Esegue una richiesta GET
     */
    private String executeGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                return readResponse(connection.getInputStream());
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                Log.e(TAG, "Error Response: " + errorResponse);
                throw new Exception("HTTP " + responseCode + ": " + errorResponse);
            }

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Esegue una richiesta POST con JSON
     */
    private String executePostRequest(String urlString, String jsonBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Scrivi il body della richiesta
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                return readResponse(connection.getInputStream());
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                Log.e(TAG, "Error Response: " + errorResponse);
                throw new Exception("HTTP " + responseCode + ": " + errorResponse);
            }

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Esegue una richiesta multipart per upload foto
     */
    private String executeMultipartRequest(String urlString, File fotoFile,String userId, String descrizioneQuest) throws Exception {

        // 1. Crea un client OkHttp
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();

        try {
            // === DEBUG ESTENSIONE E MEDIATYPE ===
            String fileName = fotoFile.getName();
            String extension = "";
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                extension = fileName.substring(i + 1).toLowerCase();
            }

            MediaType mediaType;
            switch (extension) {
                case "png":
                    mediaType = MediaType.parse("image/png");
                    break;
                case "jpg":
                case "jpeg":
                    mediaType = MediaType.parse("image/jpeg");
                    break;
                default:
                    // Un tipo generico se l'estensione non è riconosciuta
                    mediaType = MediaType.parse("application/octet-stream");
                    break;
            }
            Log.d(TAG, "DEBUG: Nome file: " + fileName + ", Estensione rilevata: '" + extension + "', MediaType impostato: '" + mediaType + "'");
            // === FINE DEBUG ===


            // 3. Costruisci il corpo della richiesta multipart
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("userId", userId)
                    .addFormDataPart("descrizione", descrizioneQuest != null ? descrizioneQuest : "")
                    .addFormDataPart("file", fileName, // Usa il nome file che abbiamo già
                            RequestBody.create(fotoFile, mediaType))
                    .build();

            // 4. Costruisci la richiesta POST
            Request request = new Request.Builder()
                    .url(urlString)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Invio richiesta multipart a: " + urlString);
            Log.d(TAG, "Con userId: " + userId);

            // === DEBUG HEADERS ===
            Headers headers = request.headers();
            Log.d(TAG, "===== INIZIO HEADERS RICHIESTA OKHTTP (DEBUG) =====");
            for (int j = 0; j < headers.size(); j++) {
                Log.d(TAG, "Header: " + headers.name(j) + ": " + headers.value(j));
            }
            Log.d(TAG, "===== FINE HEADERS RICHIESTA OKHTTP (DEBUG) =====");
            // === FINE DEBUG ===


            // 5. Esegui la chiamata e ottieni la risposta
            try (Response response = client.newCall(request).execute()) {

                int responseCode = response.code();
                String responseBodyString = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "Upload Response Code: " + responseCode);
                Log.d(TAG, "Upload Response Body: " + responseBodyString);

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Upload Error Response: " + responseBodyString);
                    throw new IOException("HTTP " + responseCode + ": " + responseBodyString);
                }

                return responseBodyString;
            }

        } catch (Exception e) {
            Log.e(TAG, "Errore fatale durante l'invio della richiesta OkHttp", e);
            throw e;
        }
    }
    /**
     * Legge la risposta da un InputStream
     */
    private String readResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        String responseText = response.toString();
        Log.d(TAG, "Response: " + responseText);
        return responseText;
    }

    /**
     * Test di connettività
     */
    public boolean testConnection() {
        try {
            String response = executeGetRequest(BASE_URL_QUEST + "/health");
            return response.contains("OK");
        } catch (Exception e) {
            Log.e(TAG, "Test connessione fallito: " + e.getMessage());
            return false;
        }
    }
}