package Service;

import Entity.User;
import Repository.UserRepository;
import Util.LLMSimulationService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Base64;

import org.json.JSONObject;
import org.json.JSONException;
import com.google.gson.Gson;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("/opera")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OperaController {

    @Inject
    private UserRepository userRepository;

    private final Gson gson = new Gson();

    /**
     * Endpoint per fare una domanda al servizio LLM
     * La app Android invia una domanda e riceve una risposta dal LLM
     */
    @POST
    @Path("/domanda")
    public Response faiDomanda(String requestBody) {
        System.out.println("=== DEBUG DOMANDA LLM ===");
        System.out.println("Request body ricevuto: " + requestBody);

        try {
            JSONObject request = new JSONObject(requestBody);

            // Validazione input
            if (!request.has("domanda") || request.getString("domanda").isEmpty()) {
                System.out.println("❌ Domanda mancante o vuota");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"La domanda è obbligatoria.\"}")
                        .build();
            }

            String domanda = request.getString("domanda");
            String userId = request.optString("userId", null);

            System.out.println("Domanda: " + domanda);
            System.out.println("UserId: " + userId);

            // Chiamata al servizio LLM (simulato)
            String rispostaLLM = LLMSimulationService.processaDomanda(domanda, userId);

            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("domanda", domanda);
            response.put("risposta", rispostaLLM);
            response.put("timestamp", System.currentTimeMillis());

            System.out.println("✅ Risposta LLM generata con successo");
            return Response.status(Response.Status.OK)
                    .entity(response.toString())
                    .build();

        } catch (JSONException e) {
            System.out.println("❌ Errore parsing JSON: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Formato JSON non valido.\"}")
                    .build();
        } catch (Exception e) {
            System.out.println("❌ Errore interno: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno del server: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per l'analisi di immagini
     * La app Android invia una foto e riceve un giudizio se va bene o meno
     */
    @POST
    @Path("/analizza-foto")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response analizzaFoto(
            @FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fileMetaData,
            @FormDataParam("userId") String userId,
            @FormDataParam("descrizione") String descrizione) {

        System.out.println("=== DEBUG ANALISI FOTO ===");
        System.out.println("Nome file: " + (fileMetaData != null ? fileMetaData.getFileName() : "non disponibile"));
        System.out.println("UserId: " + userId);
        System.out.println("Descrizione: " + descrizione);

        try {
            // Validazione input
            if (fileInputStream == null) {
                System.out.println("❌ File mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"File immagine obbligatorio.\"}")
                        .build();
            }

            // Leggi il file (per la simulazione)
            byte[] fileBytes = fileInputStream.readAllBytes();
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);

            System.out.println("Dimensione file: " + fileBytes.length + " bytes");

            // Chiamata al servizio di analisi immagini (simulato)
            JSONObject risultatoAnalisi = LLMSimulationService.analizzaImmagine(
                    base64Image,
                    fileMetaData != null ? fileMetaData.getFileName() : "image.jpg",
                    userId,
                    descrizione
            );

            System.out.println("✅ Analisi immagine completata");
            return Response.status(Response.Status.OK)
                    .entity(risultatoAnalisi.toString())
                    .build();

        } catch (Exception e) {
            System.out.println("❌ Errore durante analisi foto: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore durante l'analisi della foto: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint alternativo per analizzare foto inviate come Base64
     */
    @POST
    @Path("/analizza-foto-base64")
    public Response analizzaFotoBase64(String requestBody) {
        System.out.println("=== DEBUG ANALISI FOTO BASE64 ===");

        try {
            JSONObject request = new JSONObject(requestBody);

            // Validazione input
            if (!request.has("imageBase64") || request.getString("imageBase64").isEmpty()) {
                System.out.println("❌ Immagine Base64 mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"Immagine in formato Base64 obbligatoria.\"}")
                        .build();
            }

            String imageBase64 = request.getString("imageBase64");
            String userId = request.optString("userId", null);
            String descrizione = request.optString("descrizione", "");
            String nomeFile = request.optString("nomeFile", "image.jpg");

            System.out.println("UserId: " + userId);
            System.out.println("Descrizione: " + descrizione);
            System.out.println("Nome file: " + nomeFile);

            // Chiamata al servizio di analisi immagini (simulato)
            JSONObject risultatoAnalisi = LLMSimulationService.analizzaImmagine(
                    imageBase64,
                    nomeFile,
                    userId,
                    descrizione
            );

            System.out.println("✅ Analisi immagine Base64 completata");
            return Response.status(Response.Status.OK)
                    .entity(risultatoAnalisi.toString())
                    .build();

        } catch (JSONException e) {
            System.out.println("❌ Errore parsing JSON: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Formato JSON non valido.\"}")
                    .build();
        } catch (Exception e) {
            System.out.println("❌ Errore interno: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno del server: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per ottenere informazioni su un'opera
     */
    @GET
    @Path("/info/{operaId}")
    public Response getInfoOpera(@PathParam("operaId") String operaId, @QueryParam("userId") String userId) {
        System.out.println("=== DEBUG INFO OPERA ===");
        System.out.println("Opera ID: " + operaId);
        System.out.println("User ID: " + userId);

        try {
            // Chiamata al servizio per ottenere info opera (simulato)
            JSONObject infoOpera = LLMSimulationService.getInfoOpera(operaId, userId);

            System.out.println("✅ Informazioni opera recuperate");
            return Response.status(Response.Status.OK)
                    .entity(infoOpera.toString())
                    .build();

        } catch (Exception e) {
            System.out.println("❌ Errore recupero info opera: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nel recupero delle informazioni: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per il chat conversazionale con il LLM
     */
    @POST
    @Path("/chat")
    public Response chat(String requestBody) {
        System.out.println("=== DEBUG CHAT LLM ===");

        try {
            JSONObject request = new JSONObject(requestBody);

            String messaggio = request.getString("messaggio");
            String userId = request.optString("userId", null);
            String conversationId = request.optString("conversationId", null);

            System.out.println("Messaggio: " + messaggio);
            System.out.println("UserId: " + userId);
            System.out.println("ConversationId: " + conversationId);

            // Chiamata al servizio chat LLM (simulato)
            JSONObject rispostaChat = LLMSimulationService.processaChat(messaggio, userId, conversationId);

            System.out.println("✅ Risposta chat generata");
            return Response.status(Response.Status.OK)
                    .entity(rispostaChat.toString())
                    .build();

        } catch (JSONException e) {
            System.out.println("❌ Errore parsing JSON: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Formato JSON non valido.\"}")
                    .build();
        } catch (Exception e) {
            System.out.println("❌ Errore chat: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nel servizio chat: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per verificare lo stato del servizio
     */
    @GET
    @Path("/health")
    public Response healthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "OperaService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Servizio operativo e pronto a ricevere richieste");

        return Response.status(Response.Status.OK)
                .entity(health.toString())
                .build();
    }
}