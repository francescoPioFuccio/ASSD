package Util;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import it.unisannio.opere.grpc.OpereServiceGrpc;
import it.unisannio.user.grpc.UserServiceGrpc;
import it.unisannio.quest.grpc.QuestServiceGrpc;
import it.unisannio.musei.grpc.MuseiServiceGrpc;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager centralizzato per i client gRPC
 */
public class GrpcClientManager {

    private static final Logger LOGGER = Logger.getLogger(GrpcClientManager.class.getName());
/*
    // Configurazioni gRPC con supporto per variabili di ambiente
    private static final String OPERE_GRPC_HOST = System.getenv("OPERE_GRPC_HOST") != null ?
            System.getenv("OPERE_GRPC_HOST") : "localhost";
    private static final int OPERE_GRPC_PORT = 50052;

    private static final String USER_GRPC_HOST = System.getenv("USER_GRPC_HOST") != null ?
            System.getenv("USER_GRPC_HOST") : "localhost";
    private static final int USER_GRPC_PORT = 50053;

    private static final String QUEST_GRPC_HOST = System.getenv("QUEST_GRPC_HOST") != null ?
            System.getenv("QUEST_GRPC_HOST") : "localhost";
    private static final int QUEST_GRPC_PORT = 50054;

    private static final String MUSEI_GRPC_HOST = System.getenv("MUSEI_GRPC_HOST") != null ?
            System.getenv("MUSEI_GRPC_HOST") : "localhost";
    private static final int MUSEI_GRPC_PORT = 50055;
*/
private static final String OPERE_GRPC_HOST = "gestioneopere-backend";
    private static final int OPERE_GRPC_PORT = 50052;

    private static final String USER_GRPC_HOST = "userdatamodule2-backend";
    private static final int USER_GRPC_PORT = 50053;

    private static final String QUEST_GRPC_HOST = "gestionequest-backend";
    private static final int QUEST_GRPC_PORT = 50054;

    private static final String MUSEI_GRPC_HOST = "gestionemusei-backend";
    private static final int MUSEI_GRPC_PORT = 50055;

    // Canali gRPC
    private ManagedChannel opereGrpcChannel;
    private ManagedChannel userGrpcChannel;
    private ManagedChannel questGrpcChannel;
    private ManagedChannel museiGrpcChannel;

    // Stub gRPC
    private OpereServiceGrpc.OpereServiceBlockingStub opereGrpcStub;
    private UserServiceGrpc.UserServiceBlockingStub userGrpcStub;
    private QuestServiceGrpc.QuestServiceBlockingStub questGrpcStub;
    private MuseiServiceGrpc.MuseiServiceBlockingStub museiGrpcStub;

    public GrpcClientManager() {
        initializeClients();
    }

    private void initializeClients() {
        int maxRetries = 5;
        int retryDelay = 2000; // 2 secondi

        // Inizializzazione del client gRPC per User Service con retry
        initializeUserGrpcClient(maxRetries, retryDelay);

        // Inizializzazione del client gRPC per Opere con retry
        initializeOpereGrpcClient(maxRetries, retryDelay);

        // Inizializzazione del client gRPC per Quest con retry
        initializeQuestGrpcClient(maxRetries, retryDelay);

        // Inizializzazione del client gRPC per Musei con retry
        initializeMuseiGrpcClient(maxRetries, retryDelay);
    }

    private void initializeUserGrpcClient(int maxRetries, int retryDelay) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                this.userGrpcChannel = ManagedChannelBuilder.forAddress(USER_GRPC_HOST, USER_GRPC_PORT)
                        .usePlaintext()
                        .keepAliveTime(30, TimeUnit.SECONDS)
                        .keepAliveTimeout(5, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .build();
                this.userGrpcStub = UserServiceGrpc.newBlockingStub(userGrpcChannel);
                LOGGER.info("gRPC Client for UserDataModule initialized, target: " + USER_GRPC_HOST + ":" + USER_GRPC_PORT);
                break; // Successo, esci dal loop

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Attempt " + attempt + "/" + maxRetries + " failed to initialize UserDataModule gRPC client", e);

                if (attempt == maxRetries) {
                    LOGGER.log(Level.SEVERE, "Failed to initialize UserDataModule gRPC client after " + maxRetries + " attempts", e);
                    break;
                }

                try {
                    Thread.sleep(retryDelay * attempt); // Backoff esponenziale
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void initializeOpereGrpcClient(int maxRetries, int retryDelay) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                this.opereGrpcChannel = ManagedChannelBuilder.forAddress(OPERE_GRPC_HOST, OPERE_GRPC_PORT)
                        .usePlaintext()
                        .keepAliveTime(30, TimeUnit.SECONDS)
                        .keepAliveTimeout(5, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .build();
                this.opereGrpcStub = OpereServiceGrpc.newBlockingStub(opereGrpcChannel);
                LOGGER.info("gRPC Client for GestioneOpere initialized, target: " + OPERE_GRPC_HOST + ":" + OPERE_GRPC_PORT);
                break;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Attempt " + attempt + "/" + maxRetries + " failed to initialize GestioneOpere gRPC client", e);

                if (attempt == maxRetries) {
                    LOGGER.log(Level.SEVERE, "Failed to initialize GestioneOpere gRPC client after " + maxRetries + " attempts", e);
                    break;
                }

                try {
                    Thread.sleep(retryDelay * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void initializeQuestGrpcClient(int maxRetries, int retryDelay) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                this.questGrpcChannel = ManagedChannelBuilder.forAddress(QUEST_GRPC_HOST, QUEST_GRPC_PORT)
                        .usePlaintext()
                        .keepAliveTime(30, TimeUnit.SECONDS)
                        .keepAliveTimeout(5, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .build();
                this.questGrpcStub = QuestServiceGrpc.newBlockingStub(questGrpcChannel);
                LOGGER.info("gRPC Client for QuestService initialized, target: " + QUEST_GRPC_HOST + ":" + QUEST_GRPC_PORT);
                break;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Attempt " + attempt + "/" + maxRetries + " failed to initialize QuestService gRPC client", e);

                if (attempt == maxRetries) {
                    LOGGER.log(Level.SEVERE, "Failed to initialize QuestService gRPC client after " + maxRetries + " attempts", e);
                    break;
                }

                try {
                    Thread.sleep(retryDelay * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void initializeMuseiGrpcClient(int maxRetries, int retryDelay) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                this.museiGrpcChannel = ManagedChannelBuilder.forAddress(MUSEI_GRPC_HOST, MUSEI_GRPC_PORT)
                        .usePlaintext()
                        .keepAliveTime(30, TimeUnit.SECONDS)
                        .keepAliveTimeout(5, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .build();
                this.museiGrpcStub = MuseiServiceGrpc.newBlockingStub(museiGrpcChannel);
                LOGGER.info("gRPC Client for MuseiService initialized, target: " + MUSEI_GRPC_HOST + ":" + MUSEI_GRPC_PORT);
                break;

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Attempt " + attempt + "/" + maxRetries + " failed to initialize MuseiService gRPC client", e);

                if (attempt == maxRetries) {
                    LOGGER.log(Level.SEVERE, "Failed to initialize MuseiService gRPC client after " + maxRetries + " attempts", e);
                    break;
                }

                try {
                    Thread.sleep(retryDelay * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
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

        // Spegni il canale gRPC Musei
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