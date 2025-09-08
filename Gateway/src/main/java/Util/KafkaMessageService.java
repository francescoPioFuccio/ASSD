package Util;

import kafka_impl.KafkaProducerService;
import kafka_impl.KafkaConsumerService;
import org.json.JSONObject;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import jakarta.ws.rs.core.Response;

/**
 * Servizio centralizzato per la gestione dei messaggi Kafka
 */
public class KafkaMessageService {

    private static final Logger LOGGER = Logger.getLogger(KafkaMessageService.class.getName());

    // Topics Kafka
    public static final String TOPIC_USER_MODULE = "userdatamodule";
    public static final String TOPIC_QUEST_MODULE = "gestioneQuest";
    public static final String TOPIC_OPERE_MODULE = "gestioneOpere";
    public static final String TOPIC_MUSEI_MODULE = "gestioneMusei";
    public static final String TOPIC_RESPONSE = "gateway-responses";

    // Timeout per risposte asincrone (in secondi)
    private static final int ASYNC_RESPONSE_TIMEOUT = 30;

    private KafkaProducerService kafkaProducer;
    private KafkaConsumerService responseConsumer;

    // Cache per memorizzare le future delle richieste asincrone
    private static final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    private volatile boolean responseListenerRunning = false;

    public KafkaMessageService() {
        this.kafkaProducer = new KafkaProducerService();
        this.responseConsumer = new KafkaConsumerService("gateway-response-group", "earliest");
        startResponseListener();
        LOGGER.info("Kafka Producer/Consumer and Response Listener initialized successfully.");
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
            LOGGER.info("Contenuto completo della risposta ricevuta: " + responseMessage);

            JSONObject response = new JSONObject(responseMessage);
            String requestId = response.getString("requestId");

            LOGGER.info("Risposta ricevuta per RequestID: " + requestId);

            CompletableFuture<String> future = pendingResponses.get(requestId);
            if (future != null) {
                future.complete(responseMessage);
                LOGGER.info("Future completata per RequestID: " + requestId);
            } else {
                LOGGER.warning("Nessuna future in attesa per RequestID: " + requestId);
            }

        } catch (Exception e) {
            LOGGER.severe("Errore parsing risposta: " + e.getMessage() + ". Messaggio originale: " + responseMessage);
            e.printStackTrace();
        }
    }

    /**
     * Invia un messaggio Kafka e attende la risposta
     */
    public Response sendKafkaRequestAndWait(String topic, String operation, JSONObject kafkaMessage) {
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

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nell'invio della richiesta: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Invia un messaggio Kafka senza attendere la risposta (fire-and-forget)
     */
    public Response sendKafkaRequestFireAndForget(String topic, String operation, JSONObject kafkaMessage) {
        try {
            String requestId = kafkaMessage.getString("requestId");
            kafkaProducer.sendMessage(topic, requestId, kafkaMessage.toString());

            LOGGER.info("Messaggio Kafka inviato per " + operation + " - RequestID: " + requestId);

            return Response.status(Response.Status.ACCEPTED)
                    .entity("{\"message\": \"Richiesta " + operation + " in elaborazione\", \"requestId\": \"" + requestId + "\"}")
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore nell'invio della richiesta: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public boolean isResponseListenerRunning() {
        return responseListenerRunning;
    }

    public int getPendingRequestsCount() {
        return pendingResponses.size();
    }

    public Set<String> getPendingRequestIds() {
        return new HashSet<>(pendingResponses.keySet());
    }

    public void sendMessage(String topic, String key, String message) {
        kafkaProducer.sendMessage(topic, key, message);
    }

    public void shutdown() {
        LOGGER.info("Shutting down Kafka Message Service...");

        responseListenerRunning = false;

        if (responseConsumer != null) {
            responseConsumer.close();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }

        LOGGER.info("Kafka Message Service shutdown complete.");
    }
}