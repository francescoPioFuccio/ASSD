package Service;

import Entity.SimulazionePromozione;
import Entity.User;
import Repository.UserRepository;
import Util.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import it.unisannio.opere.grpc.GetInfoOperaRequest;
import it.unisannio.opere.grpc.GetInfoOperaResponse;
import it.unisannio.opere.grpc.OpereServiceGrpc;
import jakarta.annotation.PreDestroy;
import kafka_impl.KafkaProducerService;
import kafka_impl.KafkaConsumerService;
import it.unisannio.user.grpc.LoginRequest;
import it.unisannio.user.grpc.LoginResponse;
import com.google.gson.Gson;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.ejb.Singleton;
import it.unisannio.quest.grpc.GetDettaglioQuestRequest;
import it.unisannio.quest.grpc.GetDettaglioQuestResponse;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.mindrot.jbcrypt.BCrypt;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
// Aggiungi questi import all'inizio del file GatewayController.java
import it.unisannio.user.grpc.UserServiceGrpc;
import it.unisannio.user.grpc.RegisterRequest;
import it.unisannio.user.grpc.RegisterResponse;
import it.unisannio.quest.grpc.QuestServiceGrpc;
import it.unisannio.quest.grpc.GetQuestDisponibiliRequest;
import it.unisannio.quest.grpc.GetQuestDisponibiliResponse;
import it.unisannio.quest.grpc.GetStoricoQuestRequest;
import it.unisannio.quest.grpc.GetStoricoQuestResponse;
/**
 * Gateway Controller con gestione completa dei messaggi asincroni Kafka
 * Include listener per le risposte dai moduli
 */
@ApplicationScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)

public class GatewayController {

    private static final Logger LOGGER = Logger.getLogger(GatewayController.class.getName());

    // Topics Kafka
    private static final String TOPIC_USER_MODULE = "userdatamodule";
    private static final String TOPIC_QUEST_MODULE = "gestioneQuest";
    private static final String TOPIC_OPERE_MODULE = "gestioneOpere";
    private static final String TOPIC_MUSEI_MODULE = "gestioneMusei";
    private static final String TOPIC_RESPONSE = "gateway-responses";
    private static final String OPERE_GRPC_HOST = "localhost";
    private static final int OPERE_GRPC_PORT = 50052;
    private ManagedChannel opereGrpcChannel;
    // Aggiungi queste costanti dopo quelle esistenti per OPERE_GRPC
    private static final String USER_GRPC_HOST = "localhost";
    private static final int USER_GRPC_PORT = 50053;
    private static final String QUEST_GRPC_HOST = "localhost";
    private static final int QUEST_GRPC_PORT = 50054;
    // Aggiungi questi campi dopo quelli esistenti per il client gRPC delle opere
    private ManagedChannel userGrpcChannel;
    private UserServiceGrpc.UserServiceBlockingStub userGrpcStub;
    private OpereServiceGrpc.OpereServiceBlockingStub opereGrpcStub;
    private ManagedChannel questGrpcChannel;
    private QuestServiceGrpc.QuestServiceBlockingStub questGrpcStub;
    // Timeout per risposte asincrone (in secondi)
    private static final int ASYNC_RESPONSE_TIMEOUT = 30;

    @Inject
    private UserRepository userRepository;

    private KafkaProducerService kafkaProducer;
    private KafkaConsumerService responseConsumer;
    private final Gson gson = new Gson();

    // Cache per memorizzare le future delle richieste asincrone
    private static final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    private volatile boolean responseListenerRunning = false;



    public GatewayController() {
        //this.kafkaProducer = new KafkaProducerService();
        //this.responseConsumer = new KafkaConsumerService("gateway-response-group", "earliest");
        //startResponseListener();
        // Inizializza il consumer solo se necessario
    }

    @PostConstruct
    public void init() {
        try {
            // Inizializzazione di Kafka
            this.kafkaProducer = new KafkaProducerService();
            this.responseConsumer = new KafkaConsumerService("gateway-response-group", "earliest");
            startResponseListener(); // Avvia il thread del consumer Kafka
            LOGGER.info("Kafka Producer/Consumer and Response Listener initialized successfully.");

            // Inizializzazione del client gRPC
            this.opereGrpcChannel = ManagedChannelBuilder.forAddress(OPERE_GRPC_HOST, OPERE_GRPC_PORT)
                    .usePlaintext() // Per sviluppo. Usare TLS in produzione.
                    .build();
            this.opereGrpcStub = OpereServiceGrpc.newBlockingStub(opereGrpcChannel);
            LOGGER.info("gRPC Client for GestioneOpere initialized, target: " + OPERE_GRPC_HOST + ":" + OPERE_GRPC_PORT);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize GatewayController resources", e);
        }
        // Nel metodo @PostConstruct init(), aggiungi dopo l'inizializzazione del client gRPC opere:
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
    }
    /**
     * Avvia il listener per le risposte dai moduli
     */
    private void startResponseListener() {
        if (responseListenerRunning) {
            return;
        }

        responseListenerRunning = true;
        Thread responseThread = new Thread(() -> {
            responseConsumer.consumer.subscribe(Collections.singletonList(TOPIC_RESPONSE));
            LOGGER.info("Gateway Response Listener avviato - Topic: " + TOPIC_RESPONSE);

            while (responseListenerRunning) {
                try {
                    var records = responseConsumer.consumer.poll(Duration.ofMillis(500));
                    for (var record : records) {
                        try {
                            handleResponse(record.value());
                        } catch (Exception e) {
                            LOGGER.severe("Errore gestione risposta: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.severe("Errore nel polling risposte: " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        responseThread.setDaemon(false);
        responseThread.start();
        LOGGER.info("Thread Response Listener avviato per Gateway");
    }

    /**
     * Gestisce le risposte ricevute dai moduli via Kafka
     */
    private void handleResponse(String responseMessage) {
        try {
            // ===================================================================
            // === COMANDO PER STAMPARE LA RISPOSTA RICEVUTA (AGGIUNGI QUESTA RIGA) ===
            LOGGER.info("Contenuto completo della risposta ricevuta: " + responseMessage);
            // ===================================================================

            JSONObject response = new JSONObject(responseMessage);
            String requestId = response.getString("requestId");

            LOGGER.info("Risposta ricevuta per RequestID: " + requestId);

            CompletableFuture<String> future = pendingResponses.get(requestId);
            if (future != null) {
                future.complete(responseMessage);
                //pendingResponses.remove(requestId);
                LOGGER.info("Future rimossa dalla mappa per RequestID: " + requestId);
                LOGGER.info("Future completata per RequestID: " + requestId);
            } else {
                LOGGER.warning("Nessuna future in attesa per RequestID: " + requestId);
            }

        } catch (Exception e) {
            // Se c'è un errore qui, significa che il messaggio ricevuto non è un JSON valido.
            // Stampa il messaggio originale per capire perché.
            LOGGER.severe("Errore parsing risposta: " + e.getMessage() + ". Messaggio originale: " + responseMessage);
            e.printStackTrace(); // Aggiungi anche questo per più dettagli
        }
    }

    /**
     * Invia un messaggio Kafka e attende la risposta
     */
    private Response sendKafkaRequestAndWait(String topic, String operation, JSONObject kafkaMessage) {
        try {
            String requestId = kafkaMessage.getString("requestId");
            CompletableFuture<String> responseFuture = new CompletableFuture<>();

            // Aggiungiamo un'azione che rimuove la future dalla mappa QUANDO si completa (in qualsiasi modo)
            responseFuture.whenComplete((result, throwable) -> {
                pendingResponses.remove(requestId);
                LOGGER.info("Future rimossa dalla mappa per RequestID: " + requestId);
            });

            pendingResponses.put(requestId, responseFuture);

            kafkaProducer.sendMessage(topic, requestId, kafkaMessage.toString());
            LOGGER.info("Messaggio Kafka inviato per " + operation + " - RequestID: " + requestId);

            try {
                String responseMessage = responseFuture.get(ASYNC_RESPONSE_TIMEOUT, TimeUnit.SECONDS);
                JSONObject response = new JSONObject(responseMessage);
                LOGGER.info("Messaggio Kafka ricevuto completo per operazione: " + operation + " - RequestID: " + requestId + "   Contenuto: " + responseMessage);
                if ("success".equals(response.getString("status"))) {
                    // Istruzioni per fare in modo che al client venga inviata solo il contenuto del payload  e non informazioni per la gestione interna

                    // 1. Definiamo le chiavi "statiche" del nostro contenitore di risposta.
                    final Set<String> staticKeys = new HashSet<>(Arrays.asList("requestId", "operation", "status"));

                    String payloadKey = null;

                    Iterator<String> keys = response.keys();
                    while (keys.hasNext()) {
                        String currentKey = keys.next();
                        if (!staticKeys.contains(currentKey)) {
                            payloadKey = currentKey;
                            break;
                        }
                    }
                    if (payloadKey != null) {
                        Object payload = response.get(payloadKey);
                        LOGGER.info("mess invaito al client: " + payload.toString());
                        return Response.status(Response.Status.OK)
                                .entity(payload.toString())
                                .build();
                    } else {
                        String error = "{\"error\":\"Malformed success response from microservice: payload missing.\"}";
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(error)
                                .build();
                    }

                    // --- FINE MODIFICA DINAMICA ---

                } else {
                    return Response.status(Response.Status.BAD_REQUEST).entity(responseMessage).build();
                }
            } catch (java.util.concurrent.TimeoutException e) {
                LOGGER.severe("TIMEOUT scattato per " + operation + " (RequestID: " + requestId + ")");
                // La rimozione dalla mappa è già gestita da whenComplete
                return Response.status(Response.Status.REQUEST_TIMEOUT)
                        .entity("{\"message\": \"Timeout nell'elaborazione della richiesta\", \"requestId\": \"" + requestId + "\"}")
                        .build();
            }
            // Il resto del codice rimane simile

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nell'invio della richiesta: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // =================================================================================
    // === USER ENDPOINTS ===
    // =================================================================================

    @GET
    @Path("/users/test")
    public Response testUserEndpoint() {
        LOGGER.info("TEST ENDPOINT chiamato - Server funzionante!");
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Server UserController (via Gateway) funzionante!\", \"timestamp\": " + System.currentTimeMillis() + "}")
                .build();
    }

    // SINCRONO CON GRPC - Login gestito tramite gRPC per coerenza architetturale
    @POST
    @Path("/users/login")
    @Consumes(MediaType.APPLICATION_JSON)
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
            LoginResponse response = userGrpcStub.login(request);

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
    // ==================== 2. GATEWAY - METODO REGISTER ====================
    @POST
    @Path("/users/register")
    @Consumes(MediaType.APPLICATION_JSON)
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

            // CORREZIONE: Aggiungi TUTTI i musei preferiti
            if (user.getMuseoPreferito() != null && !user.getMuseoPreferito().isEmpty()) {
                LOGGER.info("Adding " + user.getMuseoPreferito().size() + " musei to gRPC request");
                requestBuilder.addAllMuseoPreferito(user.getMuseoPreferito());
            } else {
                LOGGER.info("No musei preferiti to add (lista vuota o null)");
            }

            RegisterRequest request = requestBuilder.build();

            // Chiamata gRPC sincrona
            RegisterResponse response = userGrpcStub.register(request);

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
    // ASINCRONO CON RISPOSTA - Applica promozione
    @POST
    @Path("/users/applica-promozione")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applicaPromozione(SimulazionePromozione promozione, @QueryParam("puntiUtente") int puntiUtente) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "applicaPromozione");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("promozione", new JSONObject(gson.toJson(promozione)));
            kafkaMessage.put("puntiUtente", puntiUtente);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "applicaPromozione", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO CON RISPOSTA - Aggiunta punti
    @PUT
    @Path("/users/{id}/aggiungi-punti")
    public Response aggiungiPunti(@PathParam("id") Long id, @QueryParam("punti") int punti) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "aggiungiPunti");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", id.toString());
            kafkaMessage.put("punti", punti);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "aggiungiPunti", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO CON RISPOSTA - Rimozione punti
    @PUT
    @Path("/users/{id}/rimuovi-punti")
    public Response rimuoviPunti(@PathParam("id") Long id, @QueryParam("punti") int punti) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "rimuoviPunti");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", id.toString());
            kafkaMessage.put("punti", punti);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "rimuoviPunti", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // =================================================================================
    // === QUEST ENDPOINTS (Mantenuti per compatibilità - TODO: implementare nei moduli) ===
    // =================================================================================

    // 5. SOSTITUISCI IL METODO getQuestDisponibili ESISTENTE con questo:
    @GET
    @Path("/quest/disponibili")
    public Response getQuestDisponibili(
            @QueryParam("userId") String userId,
            @QueryParam("museoId") String museoId,
            @QueryParam("preferenze") String preferenze,
            @QueryParam("difficolta") String difficolta) {

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
            GetQuestDisponibiliResponse response = questGrpcStub.getQuestDisponibili(request);

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

    @GET
    @Path("/quest/dettaglio/{questId}")
    public Response getDettaglioQuest(
            @PathParam("questId") String questId,
            @QueryParam("userId") String userId) {

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
            GetDettaglioQuestResponse response = questGrpcStub.getDettaglioQuest(request);

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

    // ASINCRONO PURO - Inizio quest
    @POST
    @Path("/quest/inizia")
    @Consumes(MediaType.APPLICATION_JSON)
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
            return sendKafkaRequestAndWait(TOPIC_QUEST_MODULE, "requestId", kafkaMessage);




            // prima si faceva return Response.status(Response.Status.OK).entity(risultato.toString()).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore avvio quest: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO PURO - Completamento quest
    @POST
    @Path("/quest/completa")
    @Consumes(MediaType.APPLICATION_JSON)
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

            kafkaProducer.sendMessage(TOPIC_QUEST_MODULE, requestId, kafkaMessage.toString());

            LOGGER.info("Messaggio Kafka inviato per completare quest - RequestID: " + requestId);

            return Response.status(Response.Status.ACCEPTED)
                    .entity("{\"message\": \"Richiesta di completamento quest in elaborazione\", \"requestId\": \"" + requestId + "\"}")
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore completamento quest: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // =================================================================================
    // === OPERE ENDPOINTS ===
    // =================================================================================

    // ASINCRONO PURO - Domande LLM
    @POST
    @Path("/opera/domanda")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response faiDomanda(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String domanda = request.getString("domanda");
            String userId = request.optString("userId", null);

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "processaDomanda");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("domanda", domanda);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            kafkaProducer.sendMessage(TOPIC_OPERE_MODULE, requestId, kafkaMessage.toString());

            LOGGER.info("Messaggio Kafka inviato per domanda LLM - RequestID: " + requestId);

            return sendKafkaRequestAndWait(TOPIC_OPERE_MODULE, "processaChat", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO PURO - Chat LLM
    @POST
    @Path("/opera/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response chat(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String messaggio = request.getString("messaggio");
            String userId = request.optString("userId", null);
            String conversationId = request.optString("conversationId", null);

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "processaChat");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("messaggio", messaggio);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("conversationId", conversationId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());



            LOGGER.info("Messaggio Kafka inviato per chat - RequestID: " + requestId);
            return sendKafkaRequestAndWait(TOPIC_OPERE_MODULE, "processaChat", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore chat: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO PURO - Analisi foto
    @POST
    @Path("/opera/analizza-foto")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response analizzaFoto(MultipartFormDataInput input) {
        try {
            Map<String, List<InputPart>> formData = input.getFormDataMap();
            String userId = formData.get("userId").get(0).getBodyAsString();
            String descrizione = formData.containsKey("descrizione") ?
                    formData.get("descrizione").get(0).getBodyAsString() : "";
            InputPart filePart = formData.get("file").get(0);
            InputStream fileInputStream = filePart.getBody(InputStream.class, null);

            String fileName = "unknown.tmp";
            String contentDisposition = filePart.getHeaders().getFirst("Content-Disposition");
            if (contentDisposition != null && contentDisposition.contains("filename=")) {
                fileName = contentDisposition.replaceFirst(".*filename=\"([^\"]+)\".*", "$1");
            }

            byte[] fileBytes = fileInputStream.readAllBytes();

            if (fileBytes.length > 10 * 1024 * 1024) {
                return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                        .entity("{\"message\": \"File troppo grande.\"}")
                        .build();
            }
            if (!isValidImageFile(fileName, fileBytes)) {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity("{\"message\": \"Formato non supportato.\"}")
                        .build();
            }

            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "analizzaImmagine");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("base64Image", base64Image);
            kafkaMessage.put("fileName", fileName);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("descrizione", descrizione);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            //kafkaProducer.sendMessage(TOPIC_OPERE_MODULE, requestId, kafkaMessage.toString());

            LOGGER.info("Messaggio Kafka inviato per analisi foto - RequestID: " + requestId);


            return sendKafkaRequestAndWait(TOPIC_OPERE_MODULE, "analizzaImmagine", kafkaMessage);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore analisi foto", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // =================================================================================
    // === MUSEI ENDPOINTS ===
    // =================================================================================

    // ASINCRONO PURO - Aggiunta ai preferiti
    @POST
    @Path("/musei/preferiti")
    @Consumes(MediaType.APPLICATION_JSON)
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

            kafkaProducer.sendMessage(TOPIC_MUSEI_MODULE, requestId, kafkaMessage.toString());

            LOGGER.info("Messaggio Kafka inviato per aggiungere ai preferiti - RequestID: " + requestId);

            return Response.status(Response.Status.ACCEPTED)
                    .entity("{\"message\": \"Richiesta di aggiunta ai preferiti in elaborazione\", \"requestId\": \"" + requestId + "\"}")
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore aggiunta preferiti: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // =================================================================================
    // === HEALTH CHECKS E MONITORAGGIO ===
    // =================================================================================

    @GET
    @Path("/health/kafka")
    public Response getKafkaStatus() {
        JSONObject status = new JSONObject();
        status.put("kafkaEnabled", true);
        status.put("responseListenerRunning", responseListenerRunning);
        status.put("pendingRequests", pendingResponses.size());
        status.put("topics", new JSONArray()
                .put(TOPIC_USER_MODULE)
                .put(TOPIC_QUEST_MODULE)
                .put(TOPIC_OPERE_MODULE)
                .put(TOPIC_MUSEI_MODULE)
                .put(TOPIC_RESPONSE));
        status.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.OK).entity(status.toString()).build();
    }

    @GET
    @Path("/health/pending-requests")
    public Response getPendingRequests() {
        JSONObject status = new JSONObject();
        status.put("totalPending", pendingResponses.size());
        status.put("requestIds", new JSONArray(pendingResponses.keySet()));
        status.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.OK).entity(status.toString()).build();
    }

    @POST
    @Path("/kafka/test-async-response")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testAsyncResponse(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String testMessage = request.optString("message", "Test message");

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getUserById");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", "1");
            kafkaMessage.put("testMessage", testMessage);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "testAsyncResponse", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore test: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // =================================================================================
    // === METODI HELPER ===
    // =================================================================================

    private boolean isValidImageFile(String fileName, byte[] fileBytes) {
        if (fileName == null || fileName.trim().isEmpty()) return false;
        String extension = getFileExtension(fileName).toLowerCase();
        boolean validExtension = extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");
        return validExtension && hasValidImageSignature(fileBytes, extension);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    private boolean hasValidImageSignature(byte[] fileBytes, String extension) {
        if (fileBytes.length < 4) return false;
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF;
        }
        if ("png".equals(extension)) {
            return fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 &&
                    fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47;
        }
        return true;
    }

    // =================================================================================
    // === CLEANUP ===
    // =================================================================================

    @PreDestroy
    public void cleanup() {
        LOGGER.info("Cleaning up GatewayController resources...");

        // Ferma il listener Kafka
        responseListenerRunning = false;
        if (responseConsumer != null) {
            responseConsumer.close();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }


        // Spegni il canale gRPC in modo pulito
        if (opereGrpcChannel != null && !opereGrpcChannel.isShutdown()) {
            try {
                opereGrpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                LOGGER.info("gRPC Channel for GestioneOpere shut down successfully.");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "gRPC Channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        if (userGrpcChannel != null && !userGrpcChannel.isShutdown()) {
            try {
                userGrpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                LOGGER.info("gRPC Channel for UserDataModule shut down successfully.");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "User gRPC Channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        // Chiusura del canale gRPC Quest
        if (questGrpcChannel != null && !questGrpcChannel.isShutdown()) {
            try {
                questGrpcChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                LOGGER.info("gRPC Channel for QuestService shut down successfully.");
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Quest gRPC Channel shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        LOGGER.info("Gateway Controller cleanup complete.");
    }


    // =================================================================================
// === ENDPOINT MANCANTI DA AGGIUNGERE ALLA SECONDA VERSIONE ===
// =================================================================================

// Questi endpoint vanno aggiunti dopo gli endpoint USER esistenti nella seconda versione

    // ASINCRONO CON RISPOSTA - Ottieni tutti gli utenti
    @GET
    @Path("/users")
    public Response getAllUsers() {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getAllUsers");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "getAllUsers", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO CON RISPOSTA - Ottieni utente per ID
    @GET
    @Path("/users/{id}")
    public Response getUserById(@PathParam("id") Long id) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getUserById");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", id.toString());
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "getUserById", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO CON RISPOSTA - Ottieni utente per email
    @GET
    @Path("/users/by-email")
    public Response getUserByEmail(@QueryParam("email") String email) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getUserByEmail");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("email", email);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "getUserByEmail", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO CON RISPOSTA - Aggiorna profilo
    @PUT
    @Path("/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProfile(@PathParam("id") Long id, User updatedUser) {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "updateProfile");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", id.toString());
            kafkaMessage.put("updatedUser", new JSONObject(gson.toJson(updatedUser)));
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "updateProfile", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ASINCRONO CON RISPOSTA - Ottieni promozioni
    @GET
    @Path("/users/promozioni")
    public Response getPromozioni() {
        try {
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getPromozioni");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return sendKafkaRequestAndWait(TOPIC_USER_MODULE, "getPromozioni", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

// =================================================================================
// === QUEST ENDPOINTS COMPLETI (da sostituire/aggiungere) ===
// =================================================================================

    @GET
    @Path("/quest/storico/{userId}")
    public Response getStoricoQuest(@PathParam("userId") String userId) {
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
            GetStoricoQuestResponse response = questGrpcStub.getStoricoQuest(request);

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



    @GET
    @Path("/quest/statistiche/{userId}")
    public Response getStatisticheDettagliate(@PathParam("userId") String userId) {
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

    @GET
    @Path("/quest/health")
    public Response questHealthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "QuestService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("questDisponibili", QuestSimulationService.getTotaleQuestDisponibili());
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }

// =================================================================================
// === OPERE ENDPOINTS MANCANTI ===
// =================================================================================

    // SINCRONO - Info opera richiede risposta immediata
    @GET
    @Path("/opera/info/{operaId}")
    public Response getInfoOpera(@PathParam("operaId") String operaId, @QueryParam("userId") String userId) {
        try {
            LOGGER.info("Calling gRPC GetInfoOpera for operaId: " + operaId);

            // 1. Costruisci la richiesta gRPC
            GetInfoOperaRequest request = GetInfoOperaRequest.newBuilder()
                    .setOperaId(operaId)
                    .setUserId(userId != null ? userId : "")
                    .build();

            // 2. Esegui la chiamata sincrona (bloccante)
            // L'errore avveniva qui perché 'opereGrpcStub' era null.
            GetInfoOperaResponse response = opereGrpcStub.getInfoOpera(request);

            // 3. Restituisci la risposta JSON al client
            return Response.status(Response.Status.OK)
                    .entity(response.getJsonResponse())
                    .build();

        } catch (StatusRuntimeException e) {
            LOGGER.log(Level.SEVERE, "gRPC call failed: " + e.getStatus(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to contact OperaService: " + e.getStatus().getDescription() + "\"}")
                    .build();
        } catch (Exception e) {
            // Cattura altre eccezioni, come il NullPointerException se l'inizializzazione è fallita
            LOGGER.log(Level.SEVERE, "An unexpected error occurred in getInfoOpera", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An internal error occurred in the Gateway.\"}")
                    .build();
        }
    }

    @GET
    @Path("/opera/health")
    public Response operaHealthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "OperaService");
        health.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }

// =================================================================================
// === MUSEI ENDPOINTS MANCANTI ===
// =================================================================================

    // SINCRONO - Raccomandazioni musei richiedono risposta immediata
    @GET
    @Path("/musei/raccomandati")
    public Response getMuseiRaccomandati(
            @QueryParam("userId") String userId,
            @QueryParam("latitudine") Double latitudine,
            @QueryParam("longitudine") Double longitudine,
            @QueryParam("preferenze") String preferenze,
            @QueryParam("raggio") Integer raggio) {

        // TODO: Implementare chiamata gRPC
        try {
            if (userId == null || latitudine == null || longitudine == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"UserId, latitudine e longitudine sono obbligatori.\"}")
                        .build();
            }
            int raggioKm = (raggio != null) ? raggio : 20;
            JSONObject risultato = MuseoSimulationService.trovaMuseiRaccomandati(userId, latitudine, longitudine,
                    preferenze != null ? preferenze : "", raggioKm);
            return Response.status(Response.Status.OK).entity(risultato.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // SINCRONO - Dettaglio museo richiede risposta immediata
   /* @GET
    @Path("/musei/dettaglio/{museoId}")
    public Response getDettaglioMuseo(
            @PathParam("museoId") String museoId,
            @QueryParam("userId") String userId) {

        // TODO: Implementare chiamata gRPC
        try {
            JSONObject dettagliMuseo = MuseoSimulationService.getDettaglioMuseo(museoId, userId);
            if (!dettagliMuseo.getBoolean("found")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Museo non trovato.\"}")
                        .build();
            }
            return Response.status(Response.Status.OK).entity(dettagliMuseo.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore recupero dettagli: " + e.getMessage() + "\"}")
                    .build();
        }
    }*/
    @GET
    @Path("/musei/dettaglio/{museoId}")
    public Response getDettaglioMuseo(
            @PathParam("museoId") String museoId,
            @QueryParam("userId") String userId) {

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
            return sendKafkaRequestAndWait(TOPIC_MUSEI_MODULE, "getDettaglioMuseo", kafkaMessage);

        } catch (Exception e) {
            // Gestisce eventuali errori nella creazione del messaggio Kafka
            LOGGER.log(Level.SEVERE, "Errore nella preparazione della richiesta di dettaglio museo: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno nel recupero dei dettagli del museo: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/musei/health")
    public Response museiHealthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "MuseoService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("kafka", "enabled");
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }

// =================================================================================
// === ENDPOINT DI TEST KAFKA AGGIUNTIVI ===
// =================================================================================

    @POST
    @Path("/kafka/test-message")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendTestMessage(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String topic = request.getString("topic");
            String message = request.getString("message");

            String testId = UUID.randomUUID().toString();
            kafkaProducer.sendMessage(topic, testId, message);

            return Response.status(Response.Status.OK)
                    .entity("{\"message\": \"Test message sent\", \"testId\": \"" + testId + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore invio test: " + e.getMessage() + "\"}")
                    .build();
        }
    }

}

// ASINCRONO