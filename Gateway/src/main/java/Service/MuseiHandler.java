package Service;

import Util.KafkaMessageService;
import Util.GrpcClientManager;
import it.unisannio.musei.grpc.MuseiServiceGrpc;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.unisannio.musei.grpc.GetMuseiRaccomandatiRequest;
import it.unisannio.musei.grpc.GetMuseiRaccomandatiResponse;
import io.grpc.StatusRuntimeException;

/**
 * Handler per le operazioni relative ai musei
 */
public class MuseiHandler {

    private static final Logger LOGGER = Logger.getLogger(MuseiHandler.class.getName());

    private final KafkaMessageService kafkaMessageService;
    private final GrpcClientManager grpcClientManager;


    public MuseiHandler(KafkaMessageService kafkaMessageService, GrpcClientManager grpcClientManager) {
        this.kafkaMessageService = kafkaMessageService;
        this.grpcClientManager = grpcClientManager;
    }

    public Response aggiungiAiPreferiti(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String userId = request.getString("userId");
            String museoId = request.getString("museoId");

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "aggiungiAiPreferiti");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("museoId", museoId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Messaggio Kafka inviato per aggiungere ai preferiti - RequestID: " + requestId);

            return kafkaMessageService.sendKafkaRequestFireAndForget(KafkaMessageService.TOPIC_MUSEI_MODULE, "aggiungiAiPreferiti", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore aggiunta preferiti: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getMuseiRaccomandatiGrpc(String userId, Double latitudine, Double longitudine,
                                             String preferenze, Integer raggio) {
        try {
            LOGGER.info("gRPC GetMuseiRaccomandati request for userId: " + userId +
                    " at position: " + latitudine + "," + longitudine);

            // Validazione preliminare dei campi obbligatori
            if (userId == null || userId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId è obbligatorio.\"}")
                        .build();
            }

            if (latitudine == null || longitudine == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"Coordinate latitudine e longitudine sono obbligatorie.\"}")
                        .build();
            }

            // Ottieni lo stub gRPC dal manager
            MuseiServiceGrpc.MuseiServiceBlockingStub museiStub = grpcClientManager.getMuseiStub();

            // Costruisci la richiesta gRPC
            GetMuseiRaccomandatiRequest.Builder requestBuilder = GetMuseiRaccomandatiRequest.newBuilder()
                    .setUserId(userId)
                    .setLatitudine(latitudine)
                    .setLongitudine(longitudine);

            // Aggiungi parametri opzionali se forniti
            if (preferenze != null && !preferenze.isEmpty()) {
                requestBuilder.setPreferenze(preferenze);
            }

            if (raggio != null && raggio > 0) {
                requestBuilder.setRaggio(raggio);
            } else {
                requestBuilder.setRaggio(20); // default
            }

            GetMuseiRaccomandatiRequest request = requestBuilder.build();

            // Esegui la chiamata gRPC sincrona
            GetMuseiRaccomandatiResponse response = museiStub.getMuseiRaccomandati(request);

            // Gestisci la risposta
            if ("success".equals(response.getStatus())) {
                LOGGER.info("gRPC GetMuseiRaccomandati successful for userId: " + userId);
                return Response.status(Response.Status.OK)
                        .entity(response.getJsonResponse())
                        .build();
            } else {
                // Errore nella ricerca musei
                if (response.getMessage().contains("obbligator")) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(response.getJsonResponse())
                            .build();
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(response.getJsonResponse())
                            .build();
                }
            }

        } catch (StatusRuntimeException e) {
            LOGGER.log(Level.SEVERE, "gRPC call failed for getMuseiRaccomandati: " + e.getStatus(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Failed to contact MuseiService: " + e.getStatus().getDescription() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred in getMuseiRaccomandati", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"An internal error occurred in the Gateway.\"}")
                    .build();
        }
    }
    public Response getDettaglioMuseo(String museoId, String userId) {
        try {
            // Genera un ID univoco per tracciare la richiesta e la risposta
            String requestId = UUID.randomUUID().toString();

            // Costruisce il messaggio da inviare a Kafka
            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getDettaglioMuseo");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("museoId", museoId);

            // Aggiunge lo userId se presente (potrebbe essere opzionale)
            if (userId != null && !userId.isEmpty()) {
                kafkaMessage.put("userId", userId);
            }

            kafkaMessage.put("timestamp", System.currentTimeMillis());

            // Invia la richiesta a Kafka e attende la risposta in modo bloccante (con timeout)
            // La gestione della risposta (success/error/timeout) è centralizzata nel metodo helper.
            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_MUSEI_MODULE, "getDettaglioMuseo", kafkaMessage);

        } catch (Exception e) {
            // Gestisce eventuali errori nella creazione del messaggio Kafka
            LOGGER.log(Level.SEVERE, "Errore nella preparazione della richiesta di dettaglio museo: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno nel recupero dei dettagli del museo: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response healthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "MuseoService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("kafka", "enabled");
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }
}