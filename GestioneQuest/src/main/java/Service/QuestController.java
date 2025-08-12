package Service;

import Entity.User;
import Repository.UserRepository;
import Util.QuestSimulationService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import com.google.gson.Gson;

@Path("/quest")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QuestController {

    @Inject
    private UserRepository userRepository;

    private final Gson gson = new Gson();

    /**
     * Endpoint per ottenere le quest disponibili per un museo specifico
     * basate su preferenze dell'utente
     */
    @GET
    @Path("/disponibili")
    public Response getQuestDisponibili(
            @QueryParam("userId") String userId,
            @QueryParam("museoId") String museoId,
            @QueryParam("preferenze") String preferenze,
            @QueryParam("difficolta") String difficolta) {

        System.out.println("=== DEBUG QUEST DISPONIBILI ===");
        System.out.println("UserId: " + userId);
        System.out.println("MuseoId: " + museoId);
        System.out.println("Preferenze: " + preferenze);
        System.out.println("Difficoltà: " + difficolta);

        try {
            // Validazione input obbligatori
            if (userId == null || userId.isEmpty()) {
                System.out.println("❌ UserId mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId è obbligatorio.\"}")
                        .build();
            }

            if (museoId == null || museoId.isEmpty()) {
                System.out.println("❌ MuseoId mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"MuseoId è obbligatorio.\"}")
                        .build();
            }

            // Chiamata al servizio di generazione quest
            JSONObject risultato = QuestSimulationService.getQuestDisponibili(
                    userId,
                    museoId,
                    preferenze != null ? preferenze : "",
                    difficolta != null ? difficolta : "media"
            );

            if (!risultato.getBoolean("found")) {
                System.out.println("❌ Nessuna quest trovata per il museo");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Nessuna quest disponibile per questo museo.\"}")
                        .build();
            }

            System.out.println("✅ Quest disponibili recuperate - trovate " +
                    risultato.getJSONArray("quest").length() + " quest");

            return Response.status(Response.Status.OK)
                    .entity(risultato.toString())
                    .build();

        } catch (Exception e) {
            System.out.println("❌ Errore durante recupero quest: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno del server: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per ottenere i dettagli di una quest specifica
     */
    @GET
    @Path("/dettaglio/{questId}")
    public Response getDettaglioQuest(
            @PathParam("questId") String questId,
            @QueryParam("userId") String userId) {

        System.out.println("=== DEBUG DETTAGLIO QUEST ===");
        System.out.println("QuestId: " + questId);
        System.out.println("UserId: " + userId);

        try {
            if (questId == null || questId.isEmpty()) {
                System.out.println("❌ QuestId mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"QuestId è obbligatorio.\"}")
                        .build();
            }

            if (userId == null || userId.isEmpty()) {
                System.out.println("❌ UserId mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId è obbligatorio.\"}")
                        .build();
            }

            // Chiamata al servizio per dettagli quest
            JSONObject dettagliQuest = QuestSimulationService.getDettaglioQuest(questId, userId);

            if (!dettagliQuest.getBoolean("found")) {
                System.out.println("❌ Quest non trovata");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Quest non trovata.\"}")
                        .build();
            }

            System.out.println("✅ Dettagli quest recuperati");
            return Response.status(Response.Status.OK)
                    .entity(dettagliQuest.toString())
                    .build();

        } catch (Exception e) {
            System.out.println("❌ Errore recupero dettagli quest: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nel recupero dei dettagli: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per iniziare una quest
     */
    @POST
    @Path("/inizia")
    public Response iniziaQuest(String requestBody) {
        System.out.println("=== DEBUG INIZIO QUEST ===");

        try {
            JSONObject request = new JSONObject(requestBody);

            if (!request.has("userId") || !request.has("questId")) {
                System.out.println("❌ Parametri mancanti per iniziare quest");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId e QuestId sono obbligatori.\"}")
                        .build();
            }

            String userId = request.getString("userId");
            String questId = request.getString("questId");

            System.out.println("UserId: " + userId);
            System.out.println("QuestId: " + questId);

            // Simulazione avvio quest
            JSONObject risultato = QuestSimulationService.iniziaQuest(userId, questId);

            if (!risultato.getBoolean("success")) {
                System.out.println("❌ Impossibile iniziare la quest");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"" + risultato.optString("message", "Errore nell'avvio della quest") + "\"}")
                        .build();
            }

            System.out.println("✅ Quest avviata con successo");
            return Response.status(Response.Status.OK)
                    .entity(risultato.toString())
                    .build();

        } catch (JSONException e) {
            System.out.println("❌ Errore parsing JSON: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Formato JSON non valido.\"}")
                    .build();
        } catch (Exception e) {
            System.out.println("❌ Errore avvio quest: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nell'avvio della quest: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per completare una quest
     */
    @POST
    @Path("/completa")
    public Response completaQuest(String requestBody) {
        System.out.println("=== DEBUG COMPLETAMENTO QUEST ===");

        try {
            JSONObject request = new JSONObject(requestBody);

            if (!request.has("userId") || !request.has("questId")) {
                System.out.println("❌ Parametri mancanti per completare quest");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId e QuestId sono obbligatori.\"}")
                        .build();
            }

            String userId = request.getString("userId");
            String questId = request.getString("questId");
            int tempoCompletamento = request.optInt("tempoCompletamento", 0);

            System.out.println("UserId: " + userId);
            System.out.println("QuestId: " + questId);
            System.out.println("Tempo completamento: " + tempoCompletamento + " minuti");

            // Simulazione completamento quest
            JSONObject risultato = QuestSimulationService.completaQuest(userId, questId, tempoCompletamento);

            System.out.println("✅ Quest completata con successo");
            return Response.status(Response.Status.OK)
                    .entity(risultato.toString())
                    .build();

        } catch (JSONException e) {
            System.out.println("❌ Errore parsing JSON: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Formato JSON non valido.\"}")
                    .build();
        } catch (Exception e) {
            System.out.println("❌ Errore completamento quest: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nel completamento della quest: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per ottenere lo storico delle quest dell'utente
     */
    @GET
    @Path("/storico/{userId}")
    public Response getStoricoQuest(@PathParam("userId") String userId) {
        System.out.println("=== DEBUG STORICO QUEST ===");
        System.out.println("UserId: " + userId);

        try {
            if (userId == null || userId.isEmpty()) {
                System.out.println("❌ UserId mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId è obbligatorio.\"}")
                        .build();
            }

            JSONObject storico = QuestSimulationService.getStoricoQuest(userId);

            System.out.println("✅ Storico quest recuperato");
            return Response.status(Response.Status.OK)
                    .entity(storico.toString())
                    .build();

        } catch (Exception e) {
            System.out.println("❌ Errore recupero storico: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nel recupero dello storico: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per verificare lo stato del servizio quest
     */
    @GET
    @Path("/health")
    public Response healthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "QuestService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Servizio quest operativo e pronto a ricevere richieste");
        health.put("questDisponibili", QuestSimulationService.getTotaleQuestDisponibili());

        return Response.status(Response.Status.OK)
                .entity(health.toString())
                .build();
    }
}