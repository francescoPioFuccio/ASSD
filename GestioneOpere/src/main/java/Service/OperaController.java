package Service;

import Entity.User;
import Repository.UserRepository;
import Util.LLMSimulationService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.HttpServletRequest;

// CAMBIA: Usa RESTEasy invece di Jersey per multipart
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;
import org.json.JSONException;
import com.google.gson.Gson;

@Path("/opera")
@Produces(MediaType.APPLICATION_JSON)
public class OperaController {

    private static final Logger LOGGER = Logger.getLogger(OperaController.class.getName());

    @Inject
    private UserRepository userRepository;

    private final Gson gson = new Gson();

    // =================================================================================
    // ENDPOINT CON RESTEASY MULTIPART
    // =================================================================================
    @POST
    @Path("/analizza-foto")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response analizzaFoto(MultipartFormDataInput input, @Context HttpHeaders headers) {

        LOGGER.info("=== [ANALIZZA FOTO] Richiesta ricevuta sull'endpoint /analizza-foto ===");

        // Log degli header ricevuti per debug
        LOGGER.info("[ANALIZZA FOTO] Header ricevuti dal client:");
        for (String header : headers.getRequestHeaders().keySet()) {
            List<String> values = headers.getRequestHeaders().get(header);
            LOGGER.info("  > " + header + ": " + String.join(", ", values));
        }

        try {
            // Estrai i form data
            Map<String, List<InputPart>> formData = input.getFormDataMap();

            // Estrai userId
            String userId = null;
            if (formData.containsKey("userId")) {
                userId = formData.get("userId").get(0).getBodyAsString();
                LOGGER.info("[ANALIZZA FOTO] ✅ Parametro 'userId' ricevuto: " + userId);
            }

            // Estrai descrizione
            String descrizione = null;
            if (formData.containsKey("descrizione")) {
                descrizione = formData.get("descrizione").get(0).getBodyAsString();
                LOGGER.info("[ANALIZZA FOTO] ✅ Parametro 'descrizione' ricevuto: " + (descrizione != null ? descrizione : "N/A"));
            }

            // Validazione userId
            if (userId == null || userId.trim().isEmpty()) {
                LOGGER.warning("[ANALIZZA FOTO] ❌ UserId mancante o vuoto.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId è obbligatorio.\"}")
                        .build();
            }

            // Estrai il file
            InputStream fileInputStream = null;
            String fileName = "unknown.tmp";

            if (formData.containsKey("file")) {
                InputPart filePart = formData.get("file").get(0);
                fileInputStream = filePart.getBody(InputStream.class, null);

                // Estrai il nome del file dagli header
                String contentDisposition = filePart.getHeaders().getFirst("Content-Disposition");
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    fileName = contentDisposition.replaceFirst(".*filename=\"([^\"]+)\".*", "$1");
                }

                LOGGER.info("[ANALIZZA FOTO] ✅ File ricevuto: " + fileName);
            }

            if (fileInputStream == null) {
                LOGGER.severe("[ANALIZZA FOTO] ❌ ERRORE: File mancante.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"File immagine obbligatorio.\"}")
                        .build();
            }

            // Leggi il file
            byte[] fileBytes = fileInputStream.readAllBytes();
            LOGGER.info("[ANALIZZA FOTO] Lettura del file completata. Dimensione: " + fileBytes.length + " bytes.");

            // Validazioni
            if (fileBytes.length > 10 * 1024 * 1024) {
                LOGGER.warning("[ANALIZZA FOTO] ❌ File troppo grande: " + fileBytes.length + " bytes.");
                return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                        .entity("{\"message\": \"File troppo grande. Dimensione massima: 10MB\"}")
                        .build();
            }

            if (!isValidImageFile(fileName, fileBytes)) {
                LOGGER.warning("[ANALIZZA FOTO] ❌ Tipo file non valido: " + fileName);
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity("{\"message\": \"Formato file non supportato. Usa JPG, PNG o JPEG.\"}")
                        .build();
            }

            // Chiama il servizio di analisi
            LOGGER.info("[ANALIZZA FOTO] Inizio chiamata a LLMSimulationService.analizzaImmagine...");
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
            JSONObject risultatoAnalisi = LLMSimulationService.analizzaImmagine(
                    base64Image,
                    fileName,
                    userId,
                    descrizione != null ? descrizione : ""
            );
            LOGGER.info("[ANALIZZA FOTO] ✅ Chiamata a LLMSimulationService completata.");

            // Aggiungi informazioni tecniche alla risposta
            risultatoAnalisi.put("fileDimensioneBytes", fileBytes.length);
            risultatoAnalisi.put("fileName", fileName);
            risultatoAnalisi.put("analisiCompletataIl", System.currentTimeMillis());

            LOGGER.info("[ANALIZZA FOTO] ✅ Analisi immagine completata con successo. Risultato: " + risultatoAnalisi.optString("status", "UNKNOWN"));

            return Response.status(Response.Status.OK)
                    .entity(risultatoAnalisi.toString())
                    .build();

        } catch (OutOfMemoryError e) {
            LOGGER.log(Level.SEVERE, "[ANALIZZA FOTO] ❌ OutOfMemoryError durante l'elaborazione del file.", e);
            return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                    .entity("{\"message\": \"Immagine troppo grande per essere processata dal server.\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ANALIZZA FOTO] ❌ Errore imprevisto durante l'analisi della foto.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno del server durante l'analisi della foto: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // =================================================================================
    // ENDPOINT DI TEST PER RESTEASY
    // =================================================================================
    @POST
    @Path("/test-upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response testUpload(MultipartFormDataInput input) {
        LOGGER.info("--- [TEST UPLOAD] Endpoint /test-upload RAGGIUNTO ---");

        try {
            Map<String, List<InputPart>> formData = input.getFormDataMap();

            JSONObject response = new JSONObject();
            response.put("status", "OK");
            response.put("message", "Endpoint di test RESTEasy raggiunto con successo.");
            response.put("form_fields_count", formData.size());

            for (String fieldName : formData.keySet()) {
                LOGGER.info("[TEST UPLOAD] Campo ricevuto: " + fieldName);
                response.put("field_" + fieldName, "received");
            }

            return Response.ok(response.toString()).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[TEST UPLOAD] Errore nel test", e);
            return Response.serverError()
                    .entity("{\"message\": \"Errore nel test: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // Altri endpoint rimangono uguali...
    @GET
    @Path("/health")
    public Response healthCheck() {
        LOGGER.finest("=== [HEALTH CHECK] Richiesta ricevuta ===");
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "OperaService");
        health.put("framework", "RESTEasy");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Servizio operativo e pronto a ricevere richieste");
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }

    // Metodi helper
    private boolean isValidImageFile(String fileName, byte[] fileBytes) {
        // Implementa la validazione del file immagine
        return true; // Semplificato per l'esempio
    }
}