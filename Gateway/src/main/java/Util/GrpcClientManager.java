package Util;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import it.unisannio.opere.grpc.OpereServiceGrpc;
import it.unisannio.user.grpc.UserServiceGrpc;
import it.unisannio.quest.grpc.QuestServiceGrpc;
// AGGIUNTA: Import per il servizio Musei
import it.unisannio.musei.grpc.MuseiServiceGrpc;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager centralizzato per i client gRPC
 */
public class GrpcClientManager {

    private static final Logger LOGGER = Logger.getLogger(GrpcClientManager.class.getName());

    // Configurazioni gRPC
    private static final String OPERE_GRPC_HOST = "localhost";
    private static final int OPERE_GRPC_PORT = 50052;
    private static final String USER_GRPC_HOST = "localhost";
    private static final int USER_GRPC_PORT = 50053;
    private static final String QUEST_GRPC_HOST = "localhost";
    private static final int QUEST_GRPC_PORT = 50054;
    // AGGIUNTA: Configurazione per il servizio Musei
    private static final String MUSEI_GRPC_HOST = "localhost";
    private static final int MUSEI_GRPC_PORT = 50055;

    // Canali gRPC
    private ManagedChannel opereGrpcChannel;
    private ManagedChannel userGrpcChannel;
    private ManagedChannel questGrpcChannel;
    // AGGIUNTA: Canale per il servizio Musei
    private ManagedChannel museiGrpcChannel;

    // Stub gRPC
    private OpereServiceGrpc.OpereServiceBlockingStub opereGrpcStub;
    private UserServiceGrpc.UserServiceBlockingStub userGrpcStub;
    private QuestServiceGrpc.QuestServiceBlockingStub questGrpcStub;
    // AGGIUNTA: Stub per il servizio Musei
    private MuseiServiceGrpc.MuseiServiceBlockingStub museiGrpcStub;

    public GrpcClientManager() {
        initializeClients();
    }

    private void initializeClients() {
        try {
            // Inizializzazione del client gRPC per Opere
            this.opereGrpcChannel = ManagedChannelBuilder.forAddress(OPERE_GRPC_HOST, OPERE_GRPC_PORT)
                    .usePlaintext() // Per sviluppo. Usare TLS in produzione.
                    .build();
            this.opereGrpcStub = OpereServiceGrpc.newBlockingStub(opereGrpcChannel);
            LOGGER.info("gRPC Client for GestioneOpere initialized, target: " + OPERE_GRPC_HOST + ":" + OPERE_GRPC_PORT);

            // Inizializzazione del client gRPC per User Service
            this.userGrpcChannel = ManagedChannelBuilder.forAddress(USER_GRPC_HOST, USER_GRPC_PORT)
                    .usePlaintext() // Per sviluppo. Usare TLS in produzione.
                    .build();
            this.userGrpcStub = UserServiceGrpc.newBlockingStub(userGrpcChannel);
            LOGGER.info("gRPC Client for UserDataModule initialized, target: " + USER_GRPC_HOST + ":" + USER_GRPC_PORT);

            // Inizializzazione del client gRPC per Quest Service
            this.questGrpcChannel = ManagedChannelBuilder.forAddress(QUEST_GRPC_HOST, QUEST_GRPC_PORT)
                    .usePlaintext() // Per sviluppo. Usare TLS in produzione.
                    .build();
            this.questGrpcStub = QuestServiceGrpc.newBlockingStub(questGrpcChannel);
            LOGGER.info("gRPC Client for QuestService initialized, target: " + QUEST_GRPC_HOST + ":" + QUEST_GRPC_PORT);

            // AGGIUNTA: Inizializzazione del client gRPC per Musei Service
            this.museiGrpcChannel = ManagedChannelBuilder.forAddress(MUSEI_GRPC_HOST, MUSEI_GRPC_PORT)
                    .usePlaintext() // Per sviluppo. Usare TLS in produzione.
                    .build();
            this.museiGrpcStub = MuseiServiceGrpc.newBlockingStub(museiGrpcChannel);
            LOGGER.info("gRPC Client for MuseiService initialized, target: " + MUSEI_GRPC_HOST + ":" + MUSEI_GRPC_PORT);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize gRPC clients", e);
        }
    }

    // Getter per gli stub
    public OpereServiceGrpc.OpereServiceBlockingStub getOpereStub() {
        return opereGrpcStub;
    }

    public UserServiceGrpc.UserServiceBlockingStub getUserStub() {
        return userGrpcStub;
    }

    public QuestServiceGrpc.QuestServiceBlockingStub getQuestStub() {
        return questGrpcStub;
    }

    // AGGIUNTA: Getter per il servizio Musei
    public MuseiServiceGrpc.MuseiServiceBlockingStub getMuseiStub() {
        return museiGrpcStub;
    }

    public void shutdown() {
        LOGGER.info("Shutting down gRPC clients...");

        // Spegni il canale gRPC Opere
        if (opereGrpcChannel != null && !opereGrpcChannel.isShutdown()) {
            try {
                opereGrpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                LOGGER.info("gRPC Channel for GestioneOpere shut down successfully.");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "gRPC Opere Channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        // Spegni il canale gRPC User
        if (userGrpcChannel != null && !userGrpcChannel.isShutdown()) {
            try {
                userGrpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                LOGGER.info("gRPC Channel for UserDataModule shut down successfully.");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "User gRPC Channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        // Spegni il canale gRPC Quest
        if (questGrpcChannel != null && !questGrpcChannel.isShutdown()) {
            try {
                questGrpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                LOGGER.info("gRPC Channel for QuestService shut down successfully.");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Quest gRPC Channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        // AGGIUNTA: Spegni il canale gRPC Musei
        if (museiGrpcChannel != null && !museiGrpcChannel.isShutdown()) {
            try {
                museiGrpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                LOGGER.info("gRPC Channel for MuseiService shut down successfully.");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Musei gRPC Channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        LOGGER.info("gRPC Client Manager shutdown complete.");
    }
}