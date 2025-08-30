package Service;

import Entity.SimulazionePromozione;
import Entity.User;
import Repository.UserRepository;
import Util.*;
//import Kafka.KafkaProducerService;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.mindrot.jbcrypt.BCrypt;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Questa classe agisce come un API Gateway unificato, consolidando tutti gli endpoint
 * dell'applicazione in un unico punto di ingresso.
 * Combina la logica di UserController, QuestController, OperaController e MuseoController.
 * Il path di base per tutti gli endpoint è /api.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class GatewayController {

    // === INJECTIONS E CAMPI COMUNI ===
    private static final Logger LOGGER = Logger.getLogger(GatewayController.class.getName());

    @Inject
    private UserRepository userRepository;

    //@Inject
    //private KafkaProducerService kafkaProducer;

    private final Gson gson = new Gson();


    // =================================================================================
    // === ENDPOINTS DA UserController (path base: /api/users) ===
    // =================================================================================

    @GET
    @Path("/users/test")
    public Response testUserEndpoint() {
        System.out.println("🔥 TEST ENDPOINT (da Gateway) CHIAMATO - Server funzionante!");
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Server UserController (via Gateway) funzionante!\", \"timestamp\": " + System.currentTimeMillis() + "}")
                .build();
    }

    @PUT
    @Path("/users/{id}/test-punti")
    public Response testPuntiPath(@PathParam("id") String id) {
        System.out.println("🔥 TEST PUNTI PATH (da Gateway) CHIAMATO con ID: " + id);
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Path punti funzionante per ID: " + id + "\"}")
                .build();
    }

    @POST
    @Path("/users/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(User user) {
        User foundUser = userRepository.findByEmail(user.getEmail());
        if (foundUser == null || !BCrypt.checkpw(user.getPassword(), foundUser.getPassword())) {
            System.out.println("❌ Tentativo di login fallito per email: " + user.getEmail());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"message\": \"Credenziali non valide.\"}")
                    .build();
        }
        foundUser.setPassword(null);
        System.out.println("✅ Login riuscito per: " + foundUser.getEmail());
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Login riuscito!\", \"user\": " + gson.toJson(foundUser) + "}")
                .build();
    }

    @POST
    @Path("/users/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        if (user.getEmail() == null || user.getEmail().isEmpty() ||
                user.getPassword() == null || user.getPassword().isEmpty() ||
                user.getNome() == null || user.getNome().isEmpty() ||
                user.getCognome() == null || user.getCognome().isEmpty() ||
                user.getMuseoPreferito() == null || user.getMuseoPreferito().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Tutti i campi (email, password, nome, cognome, almeno una preferenza museo) sono obbligatori per la registrazione.\"}")
                    .build();
        }

        if (userRepository.findByEmail(user.getEmail()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"message\": \"Email già registrata. Prova un'altra email.\"}")
                    .build();
        }

        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        try {
            userRepository.save(user);
            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Registrazione avvenuta con successo!\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno durante la registrazione: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GET
    @Path("/users/{id}")
    public User getUserById(@PathParam("id") Long id) {
        return userRepository.findById(id);
    }

    @GET
    @Path("/users/by-email")
    public User getUserByEmail(@QueryParam("email") String email) {
        return userRepository.findByEmail(email);
    }

    @PUT
    @Path("/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProfile(@PathParam("id") Long id, User updatedUser) {
        try {
            String requestId = UUID.randomUUID().toString();
            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("operation", "UPDATE_PROFILE");
            kafkaMessage.put("userId", id);
            kafkaMessage.put("userData", gson.toJson(updatedUser));
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            kafkaProducer.sendMessage("user-operations", requestId, kafkaMessage.toString());

            return Response.status(Response.Status.ACCEPTED)
                    .entity("{\"message\": \"Richiesta aggiornamento profilo inviata via Kafka\", \"requestId\": \"" + requestId + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nell'invio via Kafka: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/users/promozioni")
    public Response getPromozioni() {
        String simulatedResponse = SimulazioneService.getPromozioni();
        return Response.status(Response.Status.OK).entity(simulatedResponse).build();
    }

    @POST
    @Path("/users/applica-promozione")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applicaPromozione(SimulazionePromozione promozione, @QueryParam("puntiUtente") int puntiUtente) {
        String result = SimulazioneService.applicaPromozione(promozione, puntiUtente);
        return Response.status(Response.Status.OK).entity("{\"message\": \"" + result + "\"}").build();
    }

    @PUT
    @Path("/users/{id}/aggiungi-punti")
    public Response aggiungiPunti(@PathParam("id") Long id, @QueryParam("punti") int punti) {
        try {
            User user = userRepository.findById(id);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"message\": \"Utente non trovato con ID: " + id + "\"}").build();
            }
            user.addPuntiBonus(punti);
            userRepository.save(user);
            String responseMessage = "{\"message\": \"Punti bonus aggiunti con successo.\", \"nuoviPunti\": " + user.getPuntiBonus() + "}";
            return Response.status(Response.Status.OK).entity(responseMessage).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore interno durante l'aggiunta dei punti: " + e.getMessage() + "\"}").build();
        }
    }

    @PUT
    @Path("/users/{id}/rimuovi-punti")
    public Response rimuoviPunti(@PathParam("id") Long id, @QueryParam("punti") int punti) {
        try {
            User user = userRepository.findById(id);
            if (user == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"message\": \"Utente non trovato.\"}").build();
            }
            if (user.getPuntiBonus() < punti) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\": \"Punti bonus insufficienti.\"}").build();
            }
            user.removePuntiBonus(punti);
            userRepository.save(user);
            return Response.status(Response.Status.OK).entity("{\"message\": \"Punti bonus rimossi con successo.\", \"puntiResidui\": " + user.getPuntiBonus() + "}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore interno durante la rimozione dei punti: " + e.getMessage() + "\"}").build();
        }
    }


    // =================================================================================
    // === ENDPOINTS DA QuestController (path base: /api/quest) ===
    // =================================================================================

    @GET
    @Path("/quest/disponibili")
    public Response getQuestDisponibili(
            @QueryParam("userId") String userId,
            @QueryParam("museoId") String museoId,
            @QueryParam("preferenze") String preferenze,
            @QueryParam("difficolta") String difficolta) {
        try {
            if (userId == null || userId.isEmpty() || museoId == null || museoId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\": \"UserId e MuseoId sono obbligatori.\"}").build();
            }
            JSONObject risultato = QuestSimulationService.getQuestDisponibili(userId, museoId, preferenze != null ? preferenze : "", difficolta != null ? difficolta : "media");
            if (!risultato.getBoolean("found")) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"message\": \"Nessuna quest disponibile per questo museo.\"}").build();
            }
            return Response.status(Response.Status.OK).entity(risultato.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/quest/dettaglio/{questId}")
    public Response getDettaglioQuest(
            @PathParam("questId") String questId,
            @QueryParam("userId") String userId) {
        try {
            if (questId == null || questId.isEmpty() || userId == null || userId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\": \"QuestId e UserId sono obbligatori.\"}").build();
            }
            JSONObject dettagliQuest = QuestSimulationService.getDettaglioQuest(questId, userId);
            if (!dettagliQuest.getBoolean("found")) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"message\": \"Quest non trovata.\"}").build();
            }
            return Response.status(Response.Status.OK).entity(dettagliQuest.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore nel recupero dettagli: " + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/quest/inizia")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response iniziaQuest(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String userId = request.getString("userId");
            String questId = request.getString("questId");
            String museoId = request.getString("museoId");
            JSONObject risultato = QuestSimulationService.iniziaQuest(userId, questId, museoId);
            if (!risultato.getBoolean("success")) {
                return Response.status(Response.Status.BAD_REQUEST).entity(risultato.toString()).build();
            }
            return Response.status(Response.Status.OK).entity(risultato.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore avvio quest: " + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/quest/completa")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response completaQuest(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String userId = request.getString("userId");
            String questId = request.getString("questId");
            int tempoCompletamento = request.optInt("tempoCompletamento", 0);
            JSONObject risultato = QuestSimulationService.completaQuest(userId, questId, tempoCompletamento);
            return Response.status(Response.Status.OK).entity(risultato.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore completamento quest: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/quest/storico/{userId}")
    public Response getStoricoQuest(@PathParam("userId") String userId) {
        try {
            JSONObject storico = QuestSimulationService.getStoricoQuest(userId);
            return Response.status(Response.Status.OK).entity(storico.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore recupero storico: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/quest/musei-visitati/{userId}")
    public Response getMuseiVisitati(@PathParam("userId") String userId) {
        try {
            JSONObject museiVisitati = QuestSimulationService.getMuseiVisitati(userId);
            return Response.status(Response.Status.OK).entity(museiVisitati.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore recupero musei visitati: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/quest/statistiche/{userId}")
    public Response getStatisticheDettagliate(@PathParam("userId") String userId) {
        try {
            JSONObject storico = QuestSimulationService.getStoricoQuest(userId);
            JSONObject statistiche = new JSONObject();
            JSONObject stats = storico.optJSONObject("statistiche");
            JSONArray questCompletate = storico.optJSONArray("questCompletate");

            if (stats != null) {
                statistiche.put("questTotali", stats.optInt("questTotali", 0));
                statistiche.put("punteggioTotale", stats.optInt("punteggioTotale", 0));
                statistiche.put("museiVisitati", stats.optInt("museiVisitati", 0));

                if (questCompletate != null && questCompletate.length() > 0) {
                    int tempoTotale = 0;
                    Map<String, Integer> difficoltaCount = new HashMap<>();
                    Map<String, Integer> categoriaCount = new HashMap<>();
                    for (int i = 0; i < questCompletate.length(); i++) {
                        JSONObject quest = questCompletate.getJSONObject(i);
                        tempoTotale += quest.optInt("tempoCompletamento", 0);
                        String difficolta = quest.optString("difficolta", "media");
                        difficoltaCount.put(difficolta, difficoltaCount.getOrDefault(difficolta, 0) + 1);
                        String categoria = quest.optString("categoriaQuest", "generale");
                        categoriaCount.put(categoria, categoriaCount.getOrDefault(categoria, 0) + 1);
                    }
                    statistiche.put("tempoMedio", tempoTotale / questCompletate.length());
                    statistiche.put("distribuzioneDifficolta", new JSONObject(difficoltaCount));
                    statistiche.put("distribuzioneCategorie", new JSONObject(categoriaCount));
                }
            }
            return Response.status(Response.Status.OK).entity(statistiche.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore nelle statistiche: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/quest/health")
    public Response questHealthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "QuestService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("questDisponibili", QuestSimulationService.getTotaleQuestDisponibili());
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }


    // =================================================================================
    // === ENDPOINTS DA OperaController (path base: /api/opera) ===
    // =================================================================================

    @POST
    @Path("/opera/domanda")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response faiDomanda(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String domanda = request.getString("domanda");
            String userId = request.optString("userId", null);
            String rispostaLLM = LLMSimulationService.processaDomanda(domanda, userId);
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("risposta", rispostaLLM);
            return Response.status(Response.Status.OK).entity(response.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/opera/info/{operaId}")
    public Response getInfoOpera(@PathParam("operaId") String operaId, @QueryParam("userId") String userId) {
        try {
            JSONObject infoOpera = LLMSimulationService.getInfoOpera(operaId, userId);
            return Response.status(Response.Status.OK).entity(infoOpera.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore recupero info opera: " + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/opera/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response chat(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String messaggio = request.getString("messaggio");
            String userId = request.optString("userId", null);
            String conversationId = request.optString("conversationId", null);
            JSONObject rispostaChat = LLMSimulationService.processaChat(messaggio, userId, conversationId);
            return Response.status(Response.Status.OK).entity(rispostaChat.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore nel servizio chat: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/opera/health")
    public Response operaHealthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "OperaService");
        health.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }

    @POST
    @Path("/opera/analizza-foto")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response analizzaFoto(MultipartFormDataInput input) {
        try {
            Map<String, List<InputPart>> formData = input.getFormDataMap();
            String userId = formData.get("userId").get(0).getBodyAsString();
            String descrizione = formData.containsKey("descrizione") ? formData.get("descrizione").get(0).getBodyAsString() : "";
            InputPart filePart = formData.get("file").get(0);
            InputStream fileInputStream = filePart.getBody(InputStream.class, null);

            String fileName = "unknown.tmp";
            String contentDisposition = filePart.getHeaders().getFirst("Content-Disposition");
            if (contentDisposition != null && contentDisposition.contains("filename=")) {
                fileName = contentDisposition.replaceFirst(".*filename=\"([^\"]+)\".*", "$1");
            }

            byte[] fileBytes = fileInputStream.readAllBytes();

            if (fileBytes.length > 10 * 1024 * 1024) { // 10MB limit
                return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE).entity("{\"message\": \"File troppo grande.\"}").build();
            }
            if (!isValidImageFile(fileName, fileBytes)) {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).entity("{\"message\": \"Formato file non supportato.\"}").build();
            }

            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
            JSONObject risultatoAnalisi = LLMSimulationService.analizzaImmagine(base64Image, fileName, userId, descrizione);

            return Response.status(Response.Status.OK).entity(risultatoAnalisi.toString()).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[ANALIZZA FOTO] Errore imprevisto", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore interno del server: " + e.getMessage() + "\"}").build();
        }
    }


    // =================================================================================
    // === ENDPOINTS DA MuseoController (path base: /api/musei) ===
    // =================================================================================

    @GET
    @Path("/musei/raccomandati")
    public Response getMuseiRaccomandati(
            @QueryParam("userId") String userId,
            @QueryParam("latitudine") Double latitudine,
            @QueryParam("longitudine") Double longitudine,
            @QueryParam("preferenze") String preferenze,
            @QueryParam("raggio") Integer raggio) {
        try {
            if (userId == null || latitudine == null || longitudine == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"message\": \"UserId, latitudine e longitudine sono obbligatori.\"}").build();
            }
            int raggioKm = (raggio != null) ? raggio : 20;
            JSONObject risultato = MuseoSimulationService.trovaMuseiRaccomandati(userId, latitudine, longitudine, preferenze != null ? preferenze : "", raggioKm);
            return Response.status(Response.Status.OK).entity(risultato.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("/musei/dettaglio/{museoId}")
    public Response getDettaglioMuseo(
            @PathParam("museoId") String museoId,
            @QueryParam("userId") String userId) {
        try {
            JSONObject dettagliMuseo = MuseoSimulationService.getDettaglioMuseo(museoId, userId);
            if (!dettagliMuseo.getBoolean("found")) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"message\": \"Museo non trovato.\"}").build();
            }
            return Response.status(Response.Status.OK).entity(dettagliMuseo.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore nel recupero dettagli: " + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/musei/preferiti")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response aggiungiAiPreferiti(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String userId = request.getString("userId");
            String museoId = request.getString("museoId");
            JSONObject risultato = MuseoSimulationService.aggiungiAiPreferiti(userId, museoId);
            return Response.status(Response.Status.OK).entity(risultato.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"message\": \"Errore aggiunta preferiti: " + e.getMessage() + "\"}").build();
        }
    }
    /*
    @POST
    @Path("/musei/test-kafka")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testKafkaIntegration(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String userId = request.getString("userId");
            double lat = request.getDouble("latitude");
            double lon = request.getDouble("longitude");
            JSONObject risultato = MuseoSimulationService.trovaMuseiRaccomandati(userId, lat, lon, "", 10);
           // kafkaProducer.sendNearbyMuseums(userId, risultato);
            return Response.status(Response.Status.OK).entity("{\"message\": \"Test Kafka completato\"}").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }*/

    @GET
    @Path("/musei/health")
    public Response museiHealthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "MuseoService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("kafka", "enabled");
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }


    // =================================================================================
    // === METODI HELPER PRIVATI (DA OperaController) ===
    // =================================================================================

    private boolean isValidImageFile(String fileName, byte[] fileBytes) {
        if (fileName == null || fileName.trim().isEmpty()) return false;
        String extension = getFileExtension(fileName).toLowerCase();
        boolean validExtension = extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");
        return validExtension && hasValidImageSignature(fileBytes, extension);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    private boolean hasValidImageSignature(byte[] fileBytes, String extension) {
        if (fileBytes.length < 4) return false;
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF;
        }
        if ("png".equals(extension)) {
            return fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 && fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47;
        }
        return true;
    }
}