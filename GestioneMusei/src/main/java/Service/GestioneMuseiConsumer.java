package Service;

import kafka_impl.KafkaProducerService;
import kafka_impl.KafkaConsumerService;
import Util.MuseoSimulationService;
import org.json.JSONObject;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.ejb.Singleton;

import java.time.Duration;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Consumer Kafka per il modulo GestioneMusei
 * Gestisce le operazioni asincrone sui musei
 */
@Singleton
@Startup
public class GestioneMuseiConsumer {

    private static final Logger LOGGER = Logger.getLogger(GestioneMuseiConsumer.class.getName());
    private static final String TOPIC_MUSEI_MODULE = "gestioneMusei";
    private static final String TOPIC_RESPONSE = "gateway-responses";

    private KafkaConsumerService kafkaConsumer;
    private KafkaProducerService kafkaProducer;
    private volatile boolean running = false;

    @PostConstruct
    public void init() {
        this.kafkaConsumer = new KafkaConsumerService("gestionemusei-group", "earliest");
        this.kafkaProducer = new KafkaProducerService();
        startListening();
    }

    public void startListening() {
        if (running) {
            return;
        }

        running = true;
        Thread consumerThread = new Thread(() -> {
            kafkaConsumer.consumer.subscribe(Collections.singletonList(TOPIC_MUSEI_MODULE));
            LOGGER.info("GestioneMusei Consumer avviato - Topic: " + TOPIC_MUSEI_MODULE);

            while (running) {
                try {
                    var records = kafkaConsumer.consumer.poll(Duration.ofMillis(500));
                    for (var record : records) {
                        try {
                            processMessage(record.value());
                        } catch (Exception e) {
                            LOGGER.severe("Errore elaborazione messaggio: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.severe("Errore nel polling Kafka: " + e.getMessage());
                    // Breve pausa prima di riprovare
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        consumerThread.setDaemon(false);
        consumerThread.start();
        LOGGER.info("Thread Consumer Kafka avviato per GestioneMusei");
    }

    private void processMessage(String messageValue) {
        try {
            JSONObject message = new JSONObject(messageValue);
            String operation = message.getString("operation");
            String requestId = message.getString("requestId");

            LOGGER.info("GestioneMusei ricevuto: " + operation + " (RequestID: " + requestId + ")");

            switch (operation) {
                case "aggiungiAiPreferiti":
                    handleAggiungiAiPreferiti(message);
                    break;
                case "trovaMuseiRaccomandati":
                    handleTrovaMuseiRaccomandati(message);
                    break;
                case "getDettaglioMuseo":
                    handleGetDettaglioMuseo(message);
                    break;
                case "rimuoviDaiPreferiti":
                    handleRimuoviDaiPreferiti(message);
                    break;
                case "getMuseiPreferiti":
                    handleGetMuseiPreferiti(message);
                    break;
                case "getMuseiVicini":
                    handleGetMuseiVicini(message);
                    break;
                default:
                    LOGGER.warning("Operazione non riconosciuta: " + operation);
                    sendErrorResponse(message.getString("requestId"), "Operazione non supportata: " + operation);
            }
        } catch (Exception e) {
            LOGGER.severe("Errore parsing messaggio Kafka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAggiungiAiPreferiti(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            String museoId = message.getString("museoId");

            LOGGER.info("Aggiungendo museo " + museoId + " ai preferiti di " + userId);

            JSONObject risultato = MuseoSimulationService.aggiungiAiPreferiti(userId, museoId);

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "aggiungiAiPreferiti");
            response.put("risultato", risultato);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore aggiunta preferiti: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante l'aggiunta ai preferiti: " + e.getMessage());
        }
    }

    private void handleTrovaMuseiRaccomandati(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            Double latitudine = message.getDouble("latitudine");
            Double longitudine = message.getDouble("longitudine");
            String preferenze = message.optString("preferenze", "");
            Integer raggio = message.optInt("raggio", 20);

            LOGGER.info("Cercando musei raccomandati per " + userId + " - Pos: " + latitudine + "," + longitudine);

            JSONObject risultato = MuseoSimulationService.trovaMuseiRaccomandati(userId, latitudine, longitudine, preferenze, raggio);

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "trovaMuseiRaccomandati");
            response.put("musei", risultato);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore ricerca musei raccomandati: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno nella ricerca musei: " + e.getMessage());
        }
    }

    private void handleGetDettaglioMuseo(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String museoId = message.getString("museoId");
            String userId = message.optString("userId", "");

            LOGGER.info("Recuperando dettagli museo " + museoId + " per utente " + userId);

            JSONObject dettaglio = MuseoSimulationService.getDettaglioMuseo(museoId, userId);

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getDettaglioMuseo");
            response.put("museo", dettaglio);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore recupero dettaglio museo: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno nel recupero dettagli museo: " + e.getMessage());
        }
    }

    private void handleRimuoviDaiPreferiti(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            String museoId = message.getString("museoId");

            LOGGER.info("Rimuovendo museo " + museoId + " dai preferiti di " + userId);

            // Simulazione rimozione dai preferiti
            JSONObject risultato = new JSONObject();
            risultato.put("success", true);
            risultato.put("message", "Museo rimosso dai preferiti con successo");
            risultato.put("action", "removed");
            risultato.put("userId", userId);
            risultato.put("museoId", museoId);
            risultato.put("timestamp", System.currentTimeMillis());

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "rimuoviDaiPreferiti");
            response.put("risultato", risultato);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore rimozione preferiti: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante la rimozione dai preferiti: " + e.getMessage());
        }
    }

    private void handleGetMuseiPreferiti(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");

            LOGGER.info("Recuperando musei preferiti per utente " + userId);

            // Simulazione recupero preferiti
            JSONObject risultato = new JSONObject();
            risultato.put("success", true);
            risultato.put("userId", userId);
            // Per semplicità, creiamo una lista vuota - in produzione ci sarebbe la logica per recuperare i preferiti
            risultato.put("musei", new org.json.JSONArray());
            risultato.put("totalCount", 0);
            risultato.put("timestamp", System.currentTimeMillis());

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getMuseiPreferiti");
            response.put("preferiti", risultato);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore recupero preferiti: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno nel recupero preferiti: " + e.getMessage());
        }
    }

    private void handleGetMuseiVicini(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.optString("userId", "");
            Double latitudine = message.getDouble("latitudine");
            Double longitudine = message.getDouble("longitudine");
            Integer raggio = message.optInt("raggio", 10);

            LOGGER.info("Cercando musei vicini - Pos: " + latitudine + "," + longitudine + " Raggio: " + raggio + "km");

            // Riutilizza il metodo trovaMuseiRaccomandati ma senza filtri preferenze
            JSONObject risultato = MuseoSimulationService.trovaMuseiRaccomandati(userId, latitudine, longitudine, "", raggio);

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getMuseiVicini");
            response.put("musei", risultato);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore ricerca musei vicini: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno nella ricerca musei vicini: " + e.getMessage());
        }
    }

    private void sendResponse(String requestId, String responseMessage) {
        try {
            kafkaProducer.sendMessage(TOPIC_RESPONSE, requestId, responseMessage);
            LOGGER.info("Risposta inviata via Kafka - RequestID: " + requestId);
        } catch (Exception e) {
            LOGGER.severe("Errore invio risposta Kafka: " + e.getMessage());
        }
    }

    private void sendErrorResponse(String requestId, String errorMessage) {
        try {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("requestId", requestId);
            errorResponse.put("status", "error");
            errorResponse.put("message", errorMessage);

            kafkaProducer.sendMessage(TOPIC_RESPONSE, requestId, errorResponse.toString());
            LOGGER.warning("Risposta di errore inviata - RequestID: " + requestId + ", Error: " + errorMessage);
        } catch (Exception e) {
            LOGGER.severe("Errore invio risposta di errore: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
        LOGGER.info("GestioneMusei Consumer fermato");
    }
}