package Service;

import Entity.User;
import Util.KafkaMessageService;
import Util.GrpcClientManager;
import com.google.gson.Gson;

import io.grpc.StatusRuntimeException;
import it.unisannio.user.grpc.LoginRequest;
import it.unisannio.user.grpc.LoginResponse;
import it.unisannio.user.grpc.RegisterRequest;
import it.unisannio.user.grpc.RegisterResponse;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler per le operazioni relative agli utenti
 */
public class UserHandler {

    private static final Logger LOGGER = Logger.getLogger(UserHandler.class.getName());

    private final KafkaMessageService kafkaMessageService;
    private final GrpcClientManager grpcClientManager;
    private final Gson gson = new Gson();

    public UserHandler(KafkaMessageService kafkaMessageService, GrpcClientManager grpcClientManager) {
        this.kafkaMessageService = kafkaMessageService;
        this.grpcClientManager = grpcClientManager;
    }

    public Response testEndpoint() {
        LOGGER.info("TEST ENDPOINT chiamato - Server funzionante!");
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Server UserController (via Gateway) funzionante!\", \"timestamp\": " + System.currentTimeMillis() + "}")
                .build();
    }

    public Response login(User user) {
        try {
            LOGGER.info("REST Login request for email: " + user.getEmail());

            // Validazione preliminare dei campi
            if (user.getEmail() == null || user.getEmail().isEmpty() ||
                    user.getPassword() == null || user.getPassword().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"Email e password sono obbligatori.\"}")
                        .build();
            }

            // Costruisci la richiesta gRPC
            LoginRequest request = LoginRequest.newBuilder()
                    .setEmail(user.getEmail())
                    .setPassword(user.getPassword())
                    .build();

            // Chiamata gRPC sincrona
            LoginResponse response = grpcClientManager.getUserStub().login(request);

            // Gestisci la risposta
            if ("success".equals(response.getStatus())) {
                LOGGER.info("gRPC Login successful for: " + user.getEmail());
                return Response.status(Response.Status.OK)
                        .entity(response.getJsonResponse())
                        .build();
            } else {
                // Login fallito
                if (response.getMessage().contains("Credenziali non valide")) {
                    LOGGER.warning("gRPC Login failed for email: " + user.getEmail());
                    return Response.status(Response.Status.UNAUTHORIZED)
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
            LOGGER.log(Level.SEVERE, "gRPC call failed for login: " + e.getStatus(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Failed to contact UserService: " + e.getStatus().getDescription() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred in login", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"An internal error occurred in the Gateway.\"}")
                    .build();
        }
    }

    public Response register(User user) {
        try {
            LOGGER.info("REST Register request for email: " + user.getEmail() +
                    " with musei: " + user.getMuseoPreferito());

            // Costruisci la richiesta gRPC
            RegisterRequest.Builder requestBuilder = RegisterRequest.newBuilder()
                    .setEmail(user.getEmail() != null ? user.getEmail() : "")
                    .setPassword(user.getPassword() != null ? user.getPassword() : "")
                    .setNome(user.getNome() != null ? user.getNome() : "")
                    .setCognome(user.getCognome() != null ? user.getCognome() : "");

            // Aggiungi TUTTI i musei preferiti
            if (user.getMuseoPreferito() != null && !user.getMuseoPreferito().isEmpty()) {
                LOGGER.info("Adding " + user.getMuseoPreferito().size() + " musei to gRPC request");
                requestBuilder.addAllMuseoPreferito(user.getMuseoPreferito());
            } else {
                LOGGER.info("No musei preferiti to add (lista vuota o null)");
            }

            RegisterRequest request = requestBuilder.build();

            // Chiamata gRPC sincrona
            RegisterResponse response = grpcClientManager.getUserStub().register(request);

            // Gestisci la risposta
            if ("success".equals(response.getStatus())) {
                return Response.status(Response.Status.CREATED)
                        .entity(response.getJsonResponse())
                        .build();
            } else {
                if (response.getMessage().contains("già registrata")) {
                    return Response.status(Response.Status.CONFLICT)
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
            LOGGER.log(Level.SEVERE, "gRPC call failed: " + e.getStatus(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Failed to contact UserService: " + e.getStatus().getDescription() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred in register", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"An internal error occurred in the Gateway.\"}")
                    .build();
        }
    }

    public Response applicaPromozione(Object promozione, int puntiUtente) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "applicaPromozione");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("promozione", new JSONObject(gson.toJson(promozione)));
            kafkaMessage.put("puntiUtente", puntiUtente);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "applicaPromozione", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response aggiungiPunti(Long id, int punti) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "aggiungiPunti");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", id.toString());
            kafkaMessage.put("punti", punti);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "aggiungiPunti", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response rimuoviPunti(Long id, int punti) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "rimuoviPunti");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", id.toString());
            kafkaMessage.put("punti", punti);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "rimuoviPunti", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getAllUsers() {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getAllUsers");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "getAllUsers", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getUserById(Long id) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getUserById");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", id.toString());
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "getUserById", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getUserByEmail(String email) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getUserByEmail");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("email", email);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "getUserByEmail", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response updateProfile(Long id, User updatedUser) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "updateProfile");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", id.toString());
            kafkaMessage.put("updatedUser", new JSONObject(gson.toJson(updatedUser)));
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "updateProfile", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getPromozioni() {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getPromozioni");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "getPromozioni", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}