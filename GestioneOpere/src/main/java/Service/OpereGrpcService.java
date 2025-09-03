package Service;

import it.unisannio.opere.grpc.OpereServiceGrpc;
import it.unisannio.opere.grpc.GetInfoOperaRequest; // Importa le classi generate
import it.unisannio.opere.grpc.GetInfoOperaResponse;
import Util.LLMSimulationService;

import io.grpc.stub.StreamObserver;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.json.JSONObject; // Assicurati di avere l'import corretto

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class OpereGrpcService extends OpereServiceGrpc.OpereServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(OpereGrpcService.class.getName());
    private Server grpcServer;
    private final int GRPC_PORT = 50052; // Porta per questo servizio

    @PostConstruct
    public void init() {
        try {
            grpcServer = ServerBuilder.forPort(GRPC_PORT)
                    .addService(this) // "this" è l'implementazione del servizio
                    .build()
                    .start();
            LOGGER.info("gRPC Server for GestioneOpere started on port " + GRPC_PORT);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error starting gRPC server for GestioneOpere", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (grpcServer != null) {
            LOGGER.info("Shutting down gRPC server for GestioneOpere.");
            grpcServer.shutdown();
        }
    }

    @Override
    public void getInfoOpera(GetInfoOperaRequest request,
                             StreamObserver<GetInfoOperaResponse> responseObserver) {
        String operaId = request.getOperaId();
        String userId = request.getUserId();

        LOGGER.info("gRPC GetInfoOpera request received for operaId: " + operaId);

        try {
            // Chiamata alla tua logica di business esistente
            JSONObject infoOperaJson = LLMSimulationService.getInfoOpera(operaId, userId);

            // Costruisci la risposta gRPC
            GetInfoOperaResponse response = GetInfoOperaResponse.newBuilder()
                    .setJsonResponse(infoOperaJson.toString())
                    .build();

            responseObserver.onNext(response); // Invia la risposta
            responseObserver.onCompleted(); // Completa la chiamata

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing GetInfoOpera gRPC request", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}