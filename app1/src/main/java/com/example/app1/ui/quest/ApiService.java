package com.example.app1.ui.quest;

import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class ApiService {

    private static final String TAG = "ApiService";
    private static final String BASE_URL_QUEST = "http://10.0.2.2:8085/gestionequest/api/quest";
    private static final String BASE_URL_OPERA = "http://10.0.2.2:8085/gestioneopere/api";
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
    public String iniziaQuest(String userId, String questId) throws Exception {
        String urlString = BASE_URL_QUEST + "/inizia";
        Log.d(TAG, "POST Inizio Quest: " + urlString);

        JSONObject requestBody = new JSONObject();
        requestBody.put("userId", userId);
        requestBody.put("questId", questId);

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
    private String executeMultipartRequest(String urlString, File fotoFile,
                                           String userId, String descrizioneQuest) throws Exception {

        String boundary = "----FormBoundary" + System.currentTimeMillis();
        String LINE_FEED = "\r\n";

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");

            OutputStream outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

            // Aggiungi campo userId
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"userId\"").append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(userId).append(LINE_FEED);
            writer.flush();

            // Aggiungi campo descrizione
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"descrizione\"").append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(descrizioneQuest != null ? descrizioneQuest : "").append(LINE_FEED);
            writer.flush();

            // Aggiungi file foto
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(fotoFile.getName()).append("\"").append(LINE_FEED);
            writer.append("Content-Type: image/jpeg").append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            // Copia il file
            try (FileInputStream inputStream = new FileInputStream(fotoFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

            writer.append(LINE_FEED);
            writer.append("--").append(boundary).append("--").append(LINE_FEED);
            writer.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Upload Response Code: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                return readResponse(connection.getInputStream());
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                Log.e(TAG, "Upload Error Response: " + errorResponse);
                throw new Exception("HTTP " + responseCode + ": " + errorResponse);
            }

        } finally {
            connection.disconnect();
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