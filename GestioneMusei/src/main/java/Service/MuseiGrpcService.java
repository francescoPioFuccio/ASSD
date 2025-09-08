package Service;

import it.unisannio.musei.grpc.MuseiServiceGrpc;
import it.unisannio.musei.grpc.GetMuseiRaccomandatiRequest;
import it.unisannio.musei.grpc.GetMuseiRaccomandatiResponse;
import it.unisannio.musei.grpc.GetDettaglioMuseoRequest;
import it.unisannio.musei.grpc.GetDettaglioMuseoResponse;
import Util.MuseoSimulationService;

import io.grpc.stub.StreamObserver;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class MuseiGrpcService extends MuseiServiceGrpc.MuseiServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(MuseiGrpcService.class.getName());
    private Server grpcServer;
    private final int GRPC_PORT = 50055; // Porta per il servizio musei

    @PostConstruct
    public void init() {
        try {
            grpcServer = ServerBuilder.forPort(GRPC_PORT)
                    .addService(this) // "this" è l'implementazione del servizio
                    .build()
                    .start();
            LOGGER.info("gRPC Server for GestioneMusei started on port " + GRPC_PORT);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error starting gRPC server for GestioneMusei", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (grpcServer != null) {
            LOGGER.info("Shutting down gRPC server for GestioneMusei.");
            grpcServer.shutdown();
        }
    }

    @Override
    public void getMuseiRaccomandati(GetMuseiRaccomandatiRequest request,
                                     StreamObserver<GetMuseiRaccomandatiResponse> responseObserver) {
        String userId = request.getUserId();
        double latitudine = request.getLatitudine();
        double longitudine = request.getLongitudine();
        String preferenze = request.getPreferenze();
        int raggio = request.getRaggio();

        LOGGER.info("gRPC GetMuseiRaccomandati request received for userId: " + userId +
                " at position: " + latitudine + "," + longitudine);

        try {
            // Validazione parametri obbligatori
            if (userId == null || userId.isEmpty()) {
                GetMuseiRaccomandatiResponse errorResponse = GetMuseiRaccomandatiResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("UserId è obbligatorio.")
                        .setJsonResponse("{\"message\": \"UserId è obbligatorio.\"}")
                        .build();
                responseObserver.onNext(errorResponse);
                responseObserver.onCompleted();
                return;
            }

            if (latitudine == 0.0 && longitudine == 0.0) {
                GetMuseiRaccomandatiResponse errorResponse = GetMuseiRaccomandatiResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("Coordinate latitudine e longitudine sono obbligatorie.")
                        .setJsonResponse("{\"message\": \"Coordinate latitudine e longitudine sono obbligatorie.\"}")
                        .build();
                responseObserver.onNext(errorResponse);
                responseObserver.onCompleted();
                return;
            }

            // Imposta valori di default se non forniti
            if (raggio <= 0) {
                raggio = 20; // default 20 km
            }
            if (preferenze == null) {
                preferenze = ""; // nessuna preferenza specifica
            }

            // Chiamata alla logica di business esistente
            JSONObject museiRaccomandatiJson = MuseoSimulationService.trovaMuseiRaccomandati(
                    userId, latitudine, longitudine, preferenze, raggio
            );

            // Costruisci la risposta gRPC di successo
            GetMuseiRaccomandatiResponse response = GetMuseiRaccomandatiResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Musei raccomandati recuperati con successo")
                    .setJsonResponse(museiRaccomandatiJson.toString())
                    .build();

            responseObserver.onNext(response); // Invia la risposta
            responseObserver.onCompleted(); // Completa la chiamata

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing GetMuseiRaccomandati gRPC request", e);

            GetMuseiRaccomandatiResponse errorResponse = GetMuseiRaccomandatiResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Errore interno nel recupero dei musei raccomandati: " + e.getMessage())
                    .setJsonResponse("{\"message\": \"Errore interno nel recupero dei musei raccomandati: " + e.getMessage() + "\"}")
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getDettaglioMuseo(GetDettaglioMuseoRequest request,
                                  StreamObserver<GetDettaglioMuseoResponse> responseObserver) {
        String museoId = request.getMuseoId();
        String userId = request.getUserId();

        LOGGER.info("gRPC GetDettaglioMuseo request received for museoId: " + museoId);

        try {
            // Validazione parametri obbligatori
            if (museoId == null || museoId.isEmpty()) {
                GetDettaglioMuseoResponse errorResponse = GetDettaglioMuseoResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("MuseoId è obbligatorio.")
                        .setJsonResponse("{\"message\": \"MuseoId è obbligatorio.\"}")
                        .build();
                responseObserver.onNext(errorResponse);
                responseObserver.onCompleted();
                return;
            }

            // Chiamata alla logica di business esistente
            JSONObject dettaglioMuseoJson = MuseoSimulationService.getDettaglioMuseo(museoId, userId);

            // Verifica se il museo è stato trovato
            if (!dettaglioMuseoJson.optBoolean("found", true)) {
                GetDettaglioMuseoResponse notFoundResponse = GetDettaglioMuseoResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("Museo non trovato.")
                        .setJsonResponse("{\"message\": \"Museo non trovato.\"}")
                        .build();
                responseObserver.onNext(notFoundResponse);
                responseObserver.onCompleted();
                return;
            }

            // Costruisci la risposta gRPC di successo
            GetDettaglioMuseoResponse response = GetDettaglioMuseoResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Dettagli museo recuperati con successo")
                    .setJsonResponse(dettaglioMuseoJson.toString())
                    .build();

            responseObserver.onNext(response); // Invia la risposta
            responseObserver.onCompleted(); // Completa la chiamata

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing GetDettaglioMuseo gRPC request", e);

            GetDettaglioMuseoResponse errorResponse = GetDettaglioMuseoResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Errore interno nel recupero dei dettagli del museo: " + e.getMessage())
                    .setJsonResponse("{\"message\": \"Errore interno nel recupero dei dettagli del museo: " + e.getMessage() + "\"}")
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }
}