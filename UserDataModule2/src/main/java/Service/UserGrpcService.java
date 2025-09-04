package Service;

import it.unisannio.user.grpc.UserServiceGrpc;
import it.unisannio.user.grpc.RegisterRequest;
import it.unisannio.user.grpc.RegisterResponse;
import it.unisannio.user.grpc.LoginRequest;
import it.unisannio.user.grpc.LoginResponse;

import io.grpc.stub.StreamObserver;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import Repository.UserRepository;
import Entity.User;
import org.mindrot.jbcrypt.BCrypt;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

@Singleton
@Startup
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(UserGrpcService.class.getName());
    private Server grpcServer;
    private final int GRPC_PORT = 50053; // Porta diversa dal servizio Opere (50052)

    @Inject
    private UserRepository userRepository;

    @PostConstruct
    public void init() {
        try {
            grpcServer = ServerBuilder.forPort(GRPC_PORT)
                    .addService(this) // "this" è l'implementazione del servizio
                    .build()
                    .start();
            LOGGER.info("gRPC Server for UserDataModule started on port " + GRPC_PORT);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error starting gRPC server for UserDataModule", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (grpcServer != null) {
            LOGGER.info("Shutting down gRPC server for UserDataModule.");
            grpcServer.shutdown();
        }
    }

    // ==================== 1. USERGRPCSERVICE.JAVA - METODO REGISTER ====================
    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        String email = request.getEmail();
        String password = request.getPassword();
        String nome = request.getNome();
        String cognome = request.getCognome();

        // CORREZIONE: Ottieni la lista dei musei preferiti dal gRPC request
        List<String> museiPreferiti = request.getMuseoPreferitoList();

        LOGGER.info("gRPC Register request received for email: " + email +
                " with " + museiPreferiti.size() + " musei preferiti: " + museiPreferiti);

        try {
            // Validazione dei campi obbligatori
            if (email == null || email.isEmpty() ||
                    password == null || password.isEmpty() ||
                    nome == null || nome.isEmpty() ||
                    cognome == null || cognome.isEmpty()) {

                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "Email, password, nome e cognome sono obbligatori per la registrazione.");

                RegisterResponse response = RegisterResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("Email, password, nome e cognome sono obbligatori per la registrazione.")
                        .setJsonResponse(errorResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Controllo se l'email è già registrata
            if (userRepository.findByEmail(email) != null) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "Email già registrata.");

                RegisterResponse response = RegisterResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("Email già registrata.")
                        .setJsonResponse(errorResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Creazione del nuovo utente
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
            newUser.setNome(nome);
            newUser.setCognome(cognome);

            // CORREZIONE: Imposta la lista dei musei preferiti
            newUser.setMuseoPreferito(new ArrayList<>(museiPreferiti));

            // Salvataggio nel database
            userRepository.save(newUser);

            // Risposta di successo
            JSONObject successResponse = new JSONObject();
            successResponse.put("message", "Registrazione avvenuta con successo!");
            successResponse.put("userId", newUser.getId());
            successResponse.put("email", newUser.getEmail());
            successResponse.put("museiPreferiti", museiPreferiti);

            RegisterResponse response = RegisterResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Registrazione avvenuta con successo!")
                    .setJsonResponse(successResponse.toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing Register gRPC request", e);

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "Errore interno: " + e.getMessage());

            RegisterResponse response = RegisterResponse.newBuilder()
                    .setStatus("error")
                    .setMessage("Errore interno: " + e.getMessage())
                    .setJsonResponse(errorResponse.toString())
                    .build();

            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // ==================== 2. USERGRPCSERVICE.JAVA - METODO LOGIN (NUOVO) ====================
    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
        String email = request.getEmail();
        String password = request.getPassword();

        LOGGER.info("gRPC Login request received for email: " + email);

        try {
            // Validazione dei campi obbligatori
            if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "Email e password sono obbligatori per il login.");

                LoginResponse response = LoginResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("Email e password sono obbligatori per il login.")
                        .setJsonResponse(errorResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Cerca l'utente nel database
            User foundUser = userRepository.findByEmail(email);

            // Verifica credenziali
            if (foundUser == null || !BCrypt.checkpw(password, foundUser.getPassword())) {
                LOGGER.warning("gRPC Login failed for email: " + email);

                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "Credenziali non valide.");

                LoginResponse response = LoginResponse.newBuilder()
                        .setStatus("error")
                        .setMessage("Credenziali non valide.")
                        .setJsonResponse(errorResponse.toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            // Login riuscito - prepara la risposta
            foundUser.setPassword(null); // Non restituire la password hashata

            JSONObject successResponse = new JSONObject();
            successResponse.put("message", "Login riuscito!");

            // Crea l'oggetto user da restituire
            JSONObject userJson = new JSONObject();
            userJson.put("id", foundUser.getId());
            userJson.put("email", foundUser.getEmail());
            userJson.put("nome", foundUser.getNome());
            userJson.put("cognome", foundUser.getCognome());
            userJson.put("punti", foundUser.getPuntiBonus());
            userJson.put("museoPreferito", foundUser.getMuseoPreferito());

            successResponse.put("user", userJson);

            LoginResponse response = LoginResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Login riuscito!")
                    .setJsonResponse(successResponse.toString())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            LOGGER.info("gRPC Login successful for email: " + email);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing Login gRPC request", e);

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "Errore interno: " + e.getMessage());

            LoginResponse response = LoginResponse.newBuilder()
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