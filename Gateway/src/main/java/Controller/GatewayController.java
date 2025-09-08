package Controller;

import Entity.User;
import Service.*;
import Util.KafkaMessageService;
import Util.GrpcClientManager;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gateway Controller principale - Orchestratore delle richieste
 */
@ApplicationScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class GatewayController {

    private static final Logger LOGGER = Logger.getLogger(GatewayController.class.getName());

    private KafkaMessageService kafkaMessageService;
    private GrpcClientManager grpcClientManager;

    // Handler per ogni dominio
    private UserHandler userHandler;
    private QuestHandler questHandler;
    private OpereHandler opereHandler;
    private MuseiHandler museiHandler;
    private HealthHandler healthHandler;

    public GatewayController() {
        // Costruttore vuoto per CDI
    }

    @PostConstruct
    public void init() {
        try {
            // Inizializza servizi di base
            this.kafkaMessageService = new KafkaMessageService();
            this.grpcClientManager = new GrpcClientManager();

            // Inizializza handler con dipendenze
            this.userHandler = new UserHandler(kafkaMessageService, grpcClientManager);
            this.questHandler = new QuestHandler(kafkaMessageService, grpcClientManager);
            this.opereHandler = new OpereHandler(kafkaMessageService, grpcClientManager);
            this.museiHandler = new MuseiHandler(kafkaMessageService, grpcClientManager);
            this.healthHandler = new HealthHandler(kafkaMessageService);

            LOGGER.info("Gateway Controller initialized successfully");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize GatewayController", e);
        }
    }

    // =================================================================================
    // === USER ENDPOINTS ===
    // =================================================================================

    @GET
    @Path("/users/test")
    public Response testUserEndpoint() {
        return userHandler.testEndpoint();
    }

    @POST
    @Path("/users/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(User user) {
        return userHandler.login(user);
    }

    @POST
    @Path("/users/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        return userHandler.register(user);
    }

    @POST
    @Path("/users/applica-promozione")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response applicaPromozione(Object promozione, @QueryParam("puntiUtente") int puntiUtente) {
        return userHandler.applicaPromozione(promozione, puntiUtente);
    }

    @PUT
    @Path("/users/{id}/aggiungi-punti")
    public Response aggiungiPunti(@PathParam("id") Long id, @QueryParam("punti") int punti) {
        return userHandler.aggiungiPunti(id, punti);
    }

    @PUT
    @Path("/users/{id}/rimuovi-punti")
    public Response rimuoviPunti(@PathParam("id") Long id, @QueryParam("punti") int punti) {
        return userHandler.rimuoviPunti(id, punti);
    }

    @GET
    @Path("/users")
    public Response getAllUsers() {
        return userHandler.getAllUsers();
    }

    @GET
    @Path("/users/{id}")
    public Response getUserById(@PathParam("id") Long id) {
        return userHandler.getUserById(id);
    }

    @GET
    @Path("/users/by-email")
    public Response getUserByEmail(@QueryParam("email") String email) {
        return userHandler.getUserByEmail(email);
    }

    @PUT
    @Path("/users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProfile(@PathParam("id") Long id, User updatedUser) {
        return userHandler.updateProfile(id, updatedUser);
    }

    @GET
    @Path("/users/promozioni")
    public Response getPromozioni() {
        return userHandler.getPromozioni();
    }

    // =================================================================================
    // === QUEST ENDPOINTS ===
    // =================================================================================

    @GET
    @Path("/quest/disponibili")
    public Response getQuestDisponibili(
            @QueryParam("userId") String userId,
            @QueryParam("museoId") String museoId,
            @QueryParam("preferenze") String preferenze,
            @QueryParam("difficolta") String difficolta) {
        return questHandler.getQuestDisponibili(userId, museoId, preferenze, difficolta);
    }

    @GET
    @Path("/quest/dettaglio/{questId}")
    public Response getDettaglioQuest(
            @PathParam("questId") String questId,
            @QueryParam("userId") String userId) {
        return questHandler.getDettaglioQuest(questId, userId);
    }

    @POST
    @Path("/quest/inizia")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response iniziaQuest(String requestBody) {
        return questHandler.iniziaQuest(requestBody);
    }

    @POST
    @Path("/quest/completa")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response completaQuest(String requestBody) {
        return questHandler.completaQuest(requestBody);
    }

    @GET
    @Path("/quest/storico/{userId}")
    public Response getStoricoQuest(@PathParam("userId") String userId) {
        return questHandler.getStoricoQuest(userId);
    }

    @GET
    @Path("/quest/statistiche/{userId}")
    public Response getStatisticheDettagliate(@PathParam("userId") String userId) {
        return questHandler.getStatisticheDettagliate(userId);
    }

    @GET
    @Path("/quest/health")
    public Response questHealthCheck() {
        return questHandler.healthCheck();
    }

    // =================================================================================
    // === OPERE ENDPOINTS ===
    // =================================================================================

    @POST
    @Path("/opera/domanda")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response faiDomanda(String requestBody) {
        return opereHandler.faiDomanda(requestBody);
    }

    @POST
    @Path("/opera/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response chat(String requestBody) {
        return opereHandler.chat(requestBody);
    }

    @POST
    @Path("/opera/analizza-foto")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response analizzaFoto(MultipartFormDataInput input) {
        return opereHandler.analizzaFoto(input);
    }

    @GET
    @Path("/opera/info/{operaId}")
    public Response getInfoOpera(@PathParam("operaId") String operaId, @QueryParam("userId") String userId) {
        return opereHandler.getInfoOpera(operaId, userId);
    }

    @GET
    @Path("/opera/health")
    public Response operaHealthCheck() {
        return opereHandler.healthCheck();
    }

    // =================================================================================
    // === MUSEI ENDPOINTS ===
    // =================================================================================

    @POST
    @Path("/musei/preferiti")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response aggiungiAiPreferiti(String requestBody) {
        return museiHandler.aggiungiAiPreferiti(requestBody);
    }

    @GET
    @Path("/musei/raccomandati")
    public Response getMuseiRaccomandati(
            @QueryParam("userId") String userId,
            @QueryParam("latitudine") Double latitudine,
            @QueryParam("longitudine") Double longitudine,
            @QueryParam("preferenze") String preferenze,
            @QueryParam("raggio") Integer raggio) {
        return museiHandler.getMuseiRaccomandatiGrpc(userId, latitudine, longitudine, preferenze, raggio);
    }

    @GET
    @Path("/musei/dettaglio/{museoId}")
    public Response getDettaglioMuseo(
            @PathParam("museoId") String museoId,
            @QueryParam("userId") String userId) {
        return museiHandler.getDettaglioMuseo(museoId, userId);
    }

    @GET
    @Path("/musei/health")
    public Response museiHealthCheck() {
        return museiHandler.healthCheck();
    }

    // =================================================================================
    // === HEALTH CHECKS E MONITORAGGIO ===
    // =================================================================================

    @GET
    @Path("/health/kafka")
    public Response getKafkaStatus() {
        return healthHandler.getKafkaStatus();
    }

    @GET
    @Path("/health/pending-requests")
    public Response getPendingRequests() {
        return healthHandler.getPendingRequests();
    }

    @POST
    @Path("/kafka/test-async-response")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testAsyncResponse(String requestBody) {
        return healthHandler.testAsyncResponse(requestBody);
    }

    @POST
    @Path("/kafka/test-message")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendTestMessage(String requestBody) {
        return healthHandler.sendTestMessage(requestBody);
    }

    // =================================================================================
    // === CLEANUP ===
    // =================================================================================

    @PreDestroy
    public void cleanup() {
        LOGGER.info("Cleaning up GatewayController resources...");

        if (grpcClientManager != null) {
            grpcClientManager.shutdown();
        }

        if (kafkaMessageService != null) {
            kafkaMessageService.shutdown();
        }

        LOGGER.info("Gateway Controller cleanup complete.");
    }
}