package Service;

import it.unisannio.quest.grpc.QuestServiceGrpc;
import it.unisannio.quest.grpc.GetQuestDisponibiliRequest;
import it.unisannio.quest.grpc.GetQuestDisponibiliResponse;
import it.unisannio.quest.grpc.GetStoricoQuestRequest;
import it.unisannio.quest.grpc.GetStoricoQuestResponse;
import it.unisannio.quest.grpc.GetDettaglioQuestRequest;
import it.unisannio.quest.grpc.GetDettaglioQuestResponse;
// NUOVO IMPORT


import io.grpc.stub.StreamObserver;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import org.json.JSONObject;
import Util.QuestSimulationService;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class QuestGrpcService extends QuestServiceGrpc.QuestServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(QuestGrpcService.class.getName());
    private Server grpcServer;
    private final int GRPC_PORT = 50054; // Porta diversa dai servizi User (50053) e Opere (50052)

    @PostConstruct
    public void init() {
        try {
            grpcServer = ServerBuilder.forPort(GRPC_PORT)
                    .addService(this) // "this" è l'implementazione del servizio
                    .build()
                    .start();
            LOGGER.info("gRPC Server for QuestService started on port " + GRPC_PORT);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error starting gRPC server for QuestService", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (grpcServer != null) {
            LOGGER.info("Shutting down gRPC server for QuestService.");
            grpcServer.shutdown();
        }
    }

    // ==================== IMPLEMENTAZIONE METODO GET QUEST DISPONIBILI ====================
    @Override
    public void getQuestDisponibili(GetQuestDisponibiliRequest request, StreamObserver<GetQuestDisponibiliResponse> responseObserver) {
        String userId = request.getUserId();
        String museoId = request.getMuseoId();
        String preferenze = request.getPreferenze();
        String difficolta = request.getDifficolta();

        LOGGER.info("gRPC GetQuestDisponibili request received for userId: " + userId + ", museoId: " + museoId);

        try {
            // Validazione dei campi obbligatori
            if (userId == null || userId.isEmpty() || museoId == null || museoId.isEmpty()) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "UserId e MuseoId sono obbligatori.");

                GetQuestDisponibiliResponse response = GetQuestDisponibiliResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("UserId e MuseoId sono obbligatori.")
                        .setJsonResponse(errorResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Chiamata al servizio di simulazione delle quest
            String preferenceFinal = (preferenze != null && !preferenze.isEmpty()) ? preferenze : "";
            String difficoltaFinal = (difficolta != null && !difficolta.isEmpty()) ? difficolta : "media";

            JSONObject risultato = QuestSimulationService.getQuestDisponibili(userId, museoId, preferenceFinal, difficoltaFinal);

            // Controlla se ci sono quest disponibili
            if (!risultato.getBoolean("found")) {
                JSONObject notFoundResponse = new JSONObject();
                notFoundResponse.put("message", "Nessuna quest disponibile per i criteri specificati.");
                notFoundResponse.put("found", false);
                notFoundResponse.put("questDisponibili", new org.json.JSONArray());

                GetQuestDisponibiliResponse response = GetQuestDisponibiliResponse.newBuilder()
                        .setStatus("success")
                        .setMessage("Nessuna quest disponibile per i criteri specificati.")
                        .setJsonResponse(notFoundResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Quest disponibili trovate - risposta di successo
            GetQuestDisponibiliResponse response = GetQuestDisponibiliResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Quest disponibili trovate con successo.")
                    .setJsonResponse(risultato.toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            LOGGER.info("gRPC GetQuestDisponibili successful for userId: " + userId + ", museoId: " + museoId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing GetQuestDisponibili gRPC request", e);

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "Errore interno: " + e.getMessage());

            GetQuestDisponibiliResponse response = GetQuestDisponibiliResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Errore interno: " + e.getMessage())
                    .setJsonResponse(errorResponse.toString())
                    .build();

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== IMPLEMENTAZIONE METODO GET STORICO QUEST ====================
    @Override
    public void getStoricoQuest(GetStoricoQuestRequest request, StreamObserver<GetStoricoQuestResponse> responseObserver) {
        String userId = request.getUserId();
        LOGGER.info("gRPC GetStoricoQuest request received for userId: " + userId);

        try {
            if (userId == null || userId.isEmpty()) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "UserId è obbligatorio.");

                GetStoricoQuestResponse response = GetStoricoQuestResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("UserId è obbligatorio.")
                        .setJsonResponse(errorResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Recupera direttamente lo storico dal simulatore
            JSONObject storico = QuestSimulationService.getStoricoQuest(userId);

            GetStoricoQuestResponse response = GetStoricoQuestResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Storico quest recuperato con successo.")
                    .setJsonResponse(storico.toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            LOGGER.info("gRPC GetStoricoQuest successful for userId: " + userId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing GetStoricoQuest gRPC request", e);

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "Errore interno: " + e.getMessage());

            GetStoricoQuestResponse response = GetStoricoQuestResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Errore interno: " + e.getMessage())
                    .setJsonResponse(errorResponse.toString())
                    .build();

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== IMPLEMENTAZIONE METODO GET DETTAGLIO QUEST ====================
    @Override
    public void getDettaglioQuest(GetDettaglioQuestRequest request, StreamObserver<GetDettaglioQuestResponse> responseObserver) {
        String questId = request.getQuestId();
        String userId = request.getUserId();

        LOGGER.info("gRPC GetDettaglioQuest request received for questId: " + questId + ", userId: " + userId);

        try {
            // Validazione dei campi obbligatori
            if (questId == null || questId.isEmpty() || userId == null || userId.isEmpty()) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "QuestId e UserId sono obbligatori.");

                GetDettaglioQuestResponse response = GetDettaglioQuestResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("QuestId e UserId sono obbligatori.")
                        .setJsonResponse(errorResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Chiamata al servizio di simulazione per ottenere i dettagli della quest
            JSONObject dettagliQuest = QuestSimulationService.getDettaglioQuest(questId, userId);

            // Controlla se la quest è stata trovata
            if (!dettagliQuest.getBoolean("found")) {
                JSONObject notFoundResponse = new JSONObject();
                notFoundResponse.put("message", "Quest non trovata.");
                notFoundResponse.put("found", false);
                notFoundResponse.put("questId", questId);

                GetDettaglioQuestResponse response = GetDettaglioQuestResponse.newBuilder()
                        .setStatus("success")
                        .setMessage("Quest non trovata.")
                        .setJsonResponse(notFoundResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Quest trovata - risposta di successo
            GetDettaglioQuestResponse response = GetDettaglioQuestResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Dettagli quest recuperati con successo.")
                    .setJsonResponse(dettagliQuest.toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            LOGGER.info("gRPC GetDettaglioQuest successful for questId: " + questId + ", userId: " + userId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing GetDettaglioQuest gRPC request", e);

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "Errore interno: " + e.getMessage());

            GetDettaglioQuestResponse response = GetDettaglioQuestResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Errore interno: " + e.getMessage())
                    .setJsonResponse(errorResponse.toString())
                    .build();

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }


}