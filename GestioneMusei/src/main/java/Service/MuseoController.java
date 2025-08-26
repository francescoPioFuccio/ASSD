package Service;

import Entity.User;
import Repository.UserRepository;
import Util.MuseoSimulationService;
import Kafka.KafkaProducerService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import com.google.gson.Gson;

@Path("/musei")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MuseoController {

    @Inject
    private UserRepository userRepository;

    @Inject
    private KafkaProducerService kafkaProducer;

    private final Gson gson = new Gson();

    /**
     * Endpoint per ottenere musei raccomandati basati su posizione e preferenze
     * La app Android invia userId, posizione (lat, lon) e preferenze
     * Il sistema restituisce una lista di musei raccomandati
     */
    @GET
    @Path("/raccomandati")
    public Response getMuseiRaccomandati(
            @QueryParam("userId") String userId,
            @QueryParam("latitudine") Double latitudine,
            @QueryParam("longitudine") Double longitudine,
            @QueryParam("preferenze") String preferenze,
            @QueryParam("raggio") Integer raggio) {

        System.out.println("=== DEBUG RICERCA MUSEI ===");
        System.out.println("UserId: " + userId);
        System.out.println("Latitudine: " + latitudine);
        System.out.println("Longitudine: " + longitudine);
        System.out.println("Preferenze: " + preferenze);
        System.out.println("Raggio ricerca: " + (raggio != null ? raggio + " km" : "default"));

        try {
            // Validazione input obbligatori
            if (userId == null || userId.isEmpty()) {
                System.out.println("❌ UserId mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId è obbligatorio.\"}")
                        .build();
            }

            if (latitudine == null || longitudine == null) {
                System.out.println("❌ Coordinate mancanti");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"Latitudine e longitudine sono obbligatorie.\"}")
                        .build();
            }

            // Validazione range coordinate
            if (latitudine < -90 || latitudine > 90 || longitudine < -180 || longitudine > 180) {
                System.out.println("❌ Coordinate non valide");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"Coordinate non valide.\"}")
                        .build();
            }

            // Imposta raggio default se non specificato
            if (raggio == null) {
                raggio = 20; // 20 km di default
            }

            // Chiamata al servizio di raccomandazione musei (simulato)
            JSONObject risultato = MuseoSimulationService.trovaMuseiRaccomandati(
                    userId,
                    latitudine,
                    longitudine,
                    preferenze != null ? preferenze : "",
                    raggio
            );

            System.out.println("✅ Ricerca musei completata - trovati " + risultato.getJSONArray("musei").length() + " musei");

            return Response.status(Response.Status.OK)
                    .entity(risultato.toString())
                    .build();

        } catch (Exception e) {
            System.out.println("❌ Errore durante ricerca musei: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno del server: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per ottenere informazioni dettagliate di un museo specifico
     */
    @GET
    @Path("/dettaglio/{museoId}")
    public Response getDettaglioMuseo(
            @PathParam("museoId") String museoId,
            @QueryParam("userId") String userId) {

        System.out.println("=== DEBUG DETTAGLIO MUSEO ===");
        System.out.println("MuseoId: " + museoId);
        System.out.println("UserId: " + userId);

        try {
            if (museoId == null || museoId.isEmpty()) {
                System.out.println("❌ MuseoId mancante");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"MuseoId è obbligatorio.\"}")
                        .build();
            }

            // Chiamata al servizio per ottenere dettagli museo (simulato)
            JSONObject dettagliMuseo = MuseoSimulationService.getDettaglioMuseo(museoId, userId);

            if (!dettagliMuseo.getBoolean("found")) {
                System.out.println("❌ Museo non trovato");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Museo non trovato.\"}")
                        .build();
            }

            System.out.println("✅ Dettagli museo recuperati");
            return Response.status(Response.Status.OK)
                    .entity(dettagliMuseo.toString())
                    .build();

        } catch (Exception e) {
            System.out.println("❌ Errore recupero dettagli museo: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nel recupero dei dettagli: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per salvare un museo nei preferiti dell'utente
     */
    @POST
    @Path("/preferiti")
    public Response aggiungiAiPreferiti(String requestBody) {
        System.out.println("=== DEBUG AGGIUNGI PREFERITI ===");

        try {
            JSONObject request = new JSONObject(requestBody);

            if (!request.has("userId") || !request.has("museoId")) {
                System.out.println("❌ Parametri mancanti per aggiungere ai preferiti");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId e MuseoId sono obbligatori.\"}")
                        .build();
            }

            String userId = request.getString("userId");
            String museoId = request.getString("museoId");

            System.out.println("UserId: " + userId);
            System.out.println("MuseoId: " + museoId);

            // Simulazione salvataggio preferiti
            JSONObject risultato = MuseoSimulationService.aggiungiAiPreferiti(userId, museoId);

            System.out.println("✅ Museo aggiunto ai preferiti");
            return Response.status(Response.Status.OK)
                    .entity(risultato.toString())
                    .build();

        } catch (JSONException e) {
            System.out.println("❌ Errore parsing JSON: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Formato JSON non valido.\"}")
                    .build();
        } catch (Exception e) {
            System.out.println("❌ Errore aggiunta preferiti: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nell'aggiunta ai preferiti: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * NUOVO: Endpoint di test per inviare musei via Kafka
     */
    @POST
    @Path("/test-kafka")
    public Response testKafkaIntegration(String requestBody) {
        System.out.println("=== TEST KAFKA INTEGRATION ===");
        try {
            JSONObject request = new JSONObject(requestBody);

            String userId = request.getString("userId");
            double lat = request.getDouble("latitude");
            double lon = request.getDouble("longitude");

            // Usa il tuo servizio esistente
            JSONObject risultato = MuseoSimulationService.trovaMuseiRaccomandati(
                    userId, lat, lon, "", 10
            );

            // Invia via Kafka
            kafkaProducer.sendNearbyMuseums(userId, risultato);

            System.out.println("📤 Invio test completato per userId: " + userId);
            return Response.status(Response.Status.OK)
                    .entity("{\"message\": \"Test Kafka completato\", \"userId\": \"" + userId + "\"}")
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Endpoint per verificare lo stato del servizio musei
     */
    @GET
    @Path("/health")
    public Response healthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "MuseoService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("message", "Servizio musei operativo e pronto a ricevere richieste");
        health.put("kafka", "enabled");

        return Response.status(Response.Status.OK)
                .entity(health.toString())
                .build();
    }
}