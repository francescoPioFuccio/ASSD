package Service;

import Util.KafkaMessageService;
import Util.GrpcClientManager;

import Util.QuestSimulationService;

import io.grpc.StatusRuntimeException;
import it.unisannio.quest.grpc.GetQuestDisponibiliRequest;
import it.unisannio.quest.grpc.GetQuestDisponibiliResponse;
import it.unisannio.quest.grpc.GetDettaglioQuestRequest;
import it.unisannio.quest.grpc.GetDettaglioQuestResponse;
import it.unisannio.quest.grpc.GetStoricoQuestRequest;
import it.unisannio.quest.grpc.GetStoricoQuestResponse;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler per le operazioni relative alle quest
 */
public class QuestHandler {

    private static final Logger LOGGER = Logger.getLogger(QuestHandler.class.getName());

    private final KafkaMessageService kafkaMessageService;
    private final GrpcClientManager grpcClientManager;

    public QuestHandler(KafkaMessageService kafkaMessageService, GrpcClientManager grpcClientManager) {
        this.kafkaMessageService = kafkaMessageService;
        this.grpcClientManager = grpcClientManager;
    }

    public Response getQuestDisponibili(String userId, String museoId, String preferenze, String difficolta) {
        try {
            LOGGER.info("REST GetQuestDisponibili request for userId: " + userId + ", museoId: " + museoId);

            // Validazione preliminare dei campi obbligatori
            if (userId == null || userId.isEmpty() || museoId == null || museoId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId e MuseoId sono obbligatori.\"}")
                        .build();
            }

            // Costruisci la richiesta gRPC
            GetQuestDisponibiliRequest request = GetQuestDisponibiliRequest.newBuilder()
                    .setUserId(userId)
                    .setMuseoId(museoId)
                    .setPreferenze(preferenze != null ? preferenze : "")
                    .setDifficolta(difficolta != null ? difficolta : "media")
                    .build();

            // Chiamata gRPC sincrona
            GetQuestDisponibiliResponse response = grpcClientManager.getQuestStub().getQuestDisponibili(request);

            // Gestisci la risposta
            if ("success".equals(response.getStatus())) {
                LOGGER.info("gRPC GetQuestDisponibili successful for userId: " + userId + ", museoId: " + museoId);
                return Response.status(Response.Status.OK)
                        .entity(response.getJsonResponse())
                        .build();
            } else {
                // Errore nella ricerca quest
                if (response.getMessage().contains("Nessuna quest disponibile")) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(response.getJsonResponse())
                            .build();
                } else if (response.getMessage().contains("obbligatori")) {
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
            LOGGER.log(Level.SEVERE, "gRPC call failed for getQuestDisponibili: " + e.getStatus(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Failed to contact QuestService: " + e.getStatus().getDescription() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred in getQuestDisponibili", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"An internal error occurred in the Gateway.\"}")
                    .build();
        }
    }

    public Response getDettaglioQuest(String questId, String userId) {
        try {
            LOGGER.info("REST GetDettaglioQuest request for questId: " + questId + ", userId: " + userId);

            // Validazione preliminare dei campi obbligatori
            if (questId == null || questId.isEmpty() || userId == null || userId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"QuestId e UserId sono obbligatori.\"}")
                        .build();
            }

            // Costruisci la richiesta gRPC
            GetDettaglioQuestRequest request = GetDettaglioQuestRequest.newBuilder()
                    .setQuestId(questId)
                    .setUserId(userId)
                    .build();

            // Chiamata gRPC sincrona
            GetDettaglioQuestResponse response = grpcClientManager.getQuestStub().getDettaglioQuest(request);

            // Gestisci la risposta
            if ("success".equals(response.getStatus())) {
                LOGGER.info("gRPC GetDettaglioQuest successful for questId: " + questId + ", userId: " + userId);
                return Response.status(Response.Status.OK)
                        .entity(response.getJsonResponse())
                        .build();
            } else {
                // Errore nella ricerca quest
                if (response.getMessage().contains("Quest non trovata")) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(response.getJsonResponse())
                            .build();
                } else if (response.getMessage().contains("obbligatori")) {
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
            LOGGER.log(Level.SEVERE, "gRPC call failed for getDettaglioQuest: " + e.getStatus(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Failed to contact QuestService: " + e.getStatus().getDescription() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred in getDettaglioQuest", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"An internal error occurred in the Gateway.\"}")
                    .build();
        }
    }

    public Response iniziaQuest(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String userId = request.getString("userId");
            String questId = request.getString("questId");
            String museoId = request.getString("museoId");

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "iniziaQuest");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("questId", questId);
            kafkaMessage.put("museoId", museoId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Messaggio Kafka inviato per iniziare quest - RequestID: " + requestId);
            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_QUEST_MODULE, "iniziaQuest", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore avvio quest: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response completaQuest(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String userId = request.getString("userId");
            String questId = request.getString("questId");
            int tempoCompletamento = request.optInt("tempoCompletamento", 0);

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "completaQuest");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("questId", questId);
            kafkaMessage.put("tempoCompletamento", tempoCompletamento);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Messaggio Kafka inviato per completare quest - RequestID: " + requestId);

            return kafkaMessageService.sendKafkaRequestFireAndForget(KafkaMessageService.TOPIC_QUEST_MODULE, "completaQuest", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore completamento quest: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getStoricoQuest(String userId) {
        try {
            LOGGER.info("REST GetStoricoQuest request for userId: " + userId);

            // Validazione preliminare del campo obbligatorio
            if (userId == null || userId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId è obbligatorio.\"}")
                        .build();
            }

            // Costruisci la richiesta gRPC
            GetStoricoQuestRequest request = GetStoricoQuestRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            // Chiamata gRPC sincrona
            GetStoricoQuestResponse response = grpcClientManager.getQuestStub().getStoricoQuest(request);

            // Gestisci la risposta
            if ("success".equals(response.getStatus())) {
                LOGGER.info("gRPC GetStoricoQuest successful for userId: " + userId);
                return Response.status(Response.Status.OK)
                        .entity(response.getJsonResponse())
                        .build();
            } else {
                // Errore nella ricerca storico
                if (response.getMessage().contains("Nessuno storico disponibile")) {
                    return Response.status(Response.Status.OK) // 200 OK perché la richiesta è valida, solo che non ci sono dati
                            .entity(response.getJsonResponse())
                            .build();
                } else if (response.getMessage().contains("obbligatorio")) {
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
            LOGGER.log(Level.SEVERE, "gRPC call failed for getStoricoQuest: " + e.getStatus(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Failed to contact QuestService: " + e.getStatus().getDescription() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred in getStoricoQuest", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"An internal error occurred in the Gateway.\"}")
                    .build();
        }
    }

    public Response getStatisticheDettagliate(String userId) {
        // TODO: Implementare chiamata gRPC
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore statistiche: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response healthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "QuestService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("questDisponibili", QuestSimulationService.getTotaleQuestDisponibili());
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }
}