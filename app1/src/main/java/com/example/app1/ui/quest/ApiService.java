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
    private static final String BASE_URL_QUEST = "http://10.0.2.2:8080/gateway/api/quest";
    private static final String BASE_URL_OPERA = "http://10.0.2.2:8080/gateway/api/opera";
    // User service (UserDataModule3) su porta 8080
    private static final String BASE_URL_USER = "http://10.0.2.2:8080/gateway/api/users";

    private static final int TIMEOUT_MS = 15000;

    /**
     * === NUOVO METODO DI TEST ===
     * Testa la connessione al server user
     */
    public String testUserConnection() throws Exception {
        String urlString = BASE_URL_USER + "/test";
        Log.d(TAG, "🔥 TEST USER CONNECTION: " + urlString);
        return executeGetRequest(urlString);
    }

    /**
     * === NUOVO METODO DI TEST PATH ===
     * Testa il path specifico per punti
     */
    public String testPuntiPath(String userId) throws Exception {
        String urlString = BASE_URL_USER + "/" + userId + "/test-punti";
        Log.d(TAG, "🔥 TEST PUNTI PATH: " + urlString);
        return executePutRequest(urlString);
    }

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
     * === METODO UPDATE PUNTI BONUS CON DEBUG MASSIMO ===
     */
    public void updetePuntiBonus(String userId, int puntiBonus) throws Exception {
        Log.d(TAG, "🔥🔥🔥 ===== INIZIO DEBUG UPDATE PUNTI BONUS ===== 🔥🔥🔥");
        Log.d(TAG, "🔥 USER ID ricevuto: '" + userId + "'");
        Log.d(TAG, "🔥 USER ID è null? " + (userId == null));
        Log.d(TAG, "🔥 USER ID è vuoto? " + (userId != null && userId.isEmpty()));
        Log.d(TAG, "🔥 USER ID lunghezza: " + (userId != null ? userId.length() : "NULL"));
        Log.d(TAG, "🔥 PUNTI BONUS da aggiungere: " + puntiBonus);
        Log.d(TAG, "🔥 BASE_URL_USER: " + BASE_URL_USER);

        // Costruzione URL con debug step-by-step
        Log.d(TAG, "🔥 Step 1: Costruzione URL...");
        String urlString = BASE_URL_USER + "/" + userId + "/aggiungi-punti" + "?punti=" + puntiBonus;
        Log.d(TAG, "🔥 URL COMPLETO COSTRUITO: " + urlString);

        // Verifica caratteri speciali nell'userId
        if (userId != null) {
            Log.d(TAG, "🔥 USER ID char-by-char:");
            for (int i = 0; i < userId.length(); i++) {
                char c = userId.charAt(i);
                Log.d(TAG, "🔥   Char[" + i + "]: '" + c + "' (ASCII: " + (int)c + ")");
            }
        }

        // Test di connettività prima della chiamata vera
        Log.d(TAG, "🔥 Step 2: Test di connettività base...");
        try {
            String testResponse = testUserConnection();
            Log.d(TAG, "🔥 Test connettività SUCCESSO: " + testResponse);
        } catch (Exception e) {
            Log.e(TAG, "❌🔥 Test connettività FALLITO: " + e.getMessage());
            Log.e(TAG, "❌🔥 Impossibile continuare se il server non risponde!");
            throw new Exception("Server non raggiungibile: " + e.getMessage(), e);
        }

        // Test path specifico
        Log.d(TAG, "🔥 Step 3: Test path specifico...");
        try {
            String testPathResponse = testPuntiPath(userId);
            Log.d(TAG, "🔥 Test path SUCCESSO: " + testPathResponse);
        } catch (Exception e) {
            Log.e(TAG, "❌🔥 Test path FALLITO: " + e.getMessage());
            Log.e(TAG, "❌🔥 Il path o l'ID potrebbero essere il problema!");
        }

        Log.d(TAG, "🔥 Step 4: Esecuzione chiamata PUT finale...");

        try {
            String result = executePutRequest(urlString);
            Log.d(TAG, "✅🔥 UPDATE PUNTI BONUS SUCCESSO!");
            Log.d(TAG, "🔥 Risposta server: " + result);
            Log.d(TAG, "✅🔥 ===== FINE DEBUG UPDATE PUNTI BONUS - SUCCESSO ===== 🔥🔥🔥");
        } catch (Exception e) {
            Log.e(TAG, "❌🔥 ===== ERRORE FATALE UPDATE PUNTI BONUS ===== 🔥🔥🔥");
            Log.e(TAG, "❌🔥 Errore durante executePutRequest: " + e.getMessage());
            Log.e(TAG, "❌🔥 Classe eccezione: " + e.getClass().getName());
            if (e.getCause() != null) {
                Log.e(TAG, "❌🔥 Causa principale: " + e.getCause().getMessage());
            }
            Log.e(TAG, "❌🔥 ===== FINE ERRORE FATALE ===== 🔥🔥🔥");
            throw e;
        }
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
        Log.d(TAG, "🔥 EXECUTE GET REQUEST: " + urlString);
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "GET Response Code: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                String response = readResponse(connection.getInputStream());
                Log.d(TAG, "GET Response Success: " + response);
                return response;
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                Log.e(TAG, "GET Error Response: " + errorResponse);
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
            Log.d(TAG, "POST Response Code: " + responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                return readResponse(connection.getInputStream());
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                Log.e(TAG, "POST Error Response: " + errorResponse);
                throw new Exception("HTTP " + responseCode + ": " + errorResponse);
            }

        } finally {
            connection.disconnect();
        }
    }

    private String executePutRequest(String urlString) throws Exception {

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            // 1. Imposta il metodo su PUT
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            // 2. NON impostiamo Content-Type o setDoOutput(true) perché non c'è body
            // connection.setRequestProperty("Content-Type", "application/json");
            // connection.setDoOutput(true);

            // 3. Invio richiesta PUT
            long startTime = System.currentTimeMillis();
            int responseCode = connection.getResponseCode();
            long endTime = System.currentTimeMillis();

            if (responseCode >= 200 && responseCode < 300) {
                // Successo
                String response = readResponse(connection.getInputStream());
                return response;
            } else {
                // Errore
                String errorResponse = readResponse(connection.getErrorStream());
                throw new Exception("HTTP " + responseCode + ": " + errorResponse);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            connection.disconnect();
        }
    }
    /**
     * Esegue una richiesta multipart per upload foto
     */
    private String executeMultipartRequest(String urlString, File fotoFile, String userId, String descrizioneQuest) throws Exception {

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
            Log.d(TAG, "🔥 WARNING: InputStream è null, ritorno stringa vuota");
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
        Log.d(TAG, "Response Read: " + responseText);
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