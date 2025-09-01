package Service;

import Util.LLMSimulationService;
import kafka_impl.KafkaConsumerService;
import kafka_impl.KafkaProducerService;

import org.json.JSONObject;
import java.time.Duration;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumer Kafka per il modulo Gestione Opere
 * Ascolta il topic "gestioneOpere" e gestisce le operazioni relative alle opere d'arte
 */
public class GestioneOpereConsumer {

    private static final Logger LOGGER = Logger.getLogger(GestioneOpereConsumer.class.getName());

    // Topics Kafka
    private static final String TOPIC_OPERE_INPUT = "gestioneOpere";
    private static final String TOPIC_GATEWAY_RESPONSE = "gateway-responses";

    private KafkaConsumerService consumer;
    private KafkaProducerService producer;
    private volatile boolean running = false;

    public GestioneOpereConsumer() {
        this.consumer = new KafkaConsumerService("gestione-opere-group", "latest");
        this.producer = new KafkaProducerService();
    }

    /**
     * Avvia il consumer per ascoltare i messaggi dal topic gestioneOpere
     */
    public void start() {
        if (running) {
            LOGGER.warning("GestioneOpereConsumer è già in esecuzione");
            return;
        }

        running = true;
        consumer.consumer.subscribe(Collections.singletonList(TOPIC_OPERE_INPUT));

        LOGGER.info("GestioneOpereConsumer avviato - Topic: " + TOPIC_OPERE_INPUT);

        Thread consumerThread = new Thread(() -> {
            while (running) {
                try {
                    var records = consumer.consumer.poll(Duration.ofMillis(500));

                    for (var record : records) {
                        try {
                            LOGGER.info("Messaggio ricevuto: " + record.value());
                            processMessage(record.value());
                        } catch (Exception e) {
                            LOGGER.severe("Errore elaborazione messaggio: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.severe("Errore nel polling consumer opere: " + e.getMessage());
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
    }

    /**
     * Processa i messaggi ricevuti dal topic Kafka
     */
    private void processMessage(String message) {
        try {
            JSONObject request = new JSONObject(message);
            String operation = request.getString("operation");
            String requestId = request.getString("requestId");

            LOGGER.info("Processando operazione: " + operation + " (RequestID: " + requestId + ")");

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("operation", operation);

            switch (operation) {
                case "processaDomanda":
                    handleProcessaDomanda(request, response);
                    break;

                case "processaChat":
                    handleProcessaChat(request, response);
                    break;

                case "analizzaImmagine":
                    handleAnalizzaImmagine(request, response);
                    break;

                default:
                    LOGGER.warning("Operazione non supportata: " + operation);
                    response.put("status", "error");
                    response.put("message", "Operazione non supportata: " + operation);
            }

            // Invia la risposta al gateway
            sendResponseToGateway(response);

        } catch (Exception e) {
            LOGGER.severe("Errore processamento messaggio: " + e.getMessage());
            e.printStackTrace();

            // Invia risposta di errore se possibile
            try {
                JSONObject errorResponse = new JSONObject();
                JSONObject originalRequest = new JSONObject(message);
                errorResponse.put("requestId", originalRequest.optString("requestId", "unknown"));
                errorResponse.put("operation", originalRequest.optString("operation", "unknown"));
                errorResponse.put("status", "error");
                errorResponse.put("message", "Errore interno del servizio: " + e.getMessage());

                sendResponseToGateway(errorResponse);
            } catch (Exception ex) {
                LOGGER.severe("Impossibile inviare risposta di errore: " + ex.getMessage());
            }
        }
    }

    /**
     * Gestisce l'operazione di processare una domanda LLM
     */
    private void handleProcessaDomanda(JSONObject request, JSONObject response) {
        try {
            String domanda = request.getString("domanda");
            String userId = request.optString("userId", null);

            LOGGER.info("Processando domanda per utente " + userId + ": " + domanda);

            // Chiama il servizio LLM per processare la domanda
            String rispostaLLM = LLMSimulationService.processaDomanda(domanda, userId);

            // Costruisce la risposta di successo
            response.put("status", "success");
            response.put("risposta", rispostaLLM);
            response.put("domanda", domanda);
            response.put("userId", userId);
            response.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Domanda processata con successo per RequestID: " + request.getString("requestId"));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore processamento domanda", e);
            response.put("status", "error");
            response.put("message", "Errore nel processamento della domanda: " + e.getMessage());
        }
    }

    /**
     * Gestisce l'operazione di chat con il LLM
     */
    private void handleProcessaChat(JSONObject request, JSONObject response) {
        try {
            String messaggio = request.getString("messaggio");
            String userId = request.optString("userId", null);
            String conversationId = request.optString("conversationId", null);

            LOGGER.info("Processando chat per utente " + userId + " (conversation: " + conversationId + ")");

            // Chiama il servizio LLM per la chat
            JSONObject chatResponse = LLMSimulationService.processaChat(messaggio, userId, conversationId);

            // Costruisce la risposta di successo
            response.put("status", "success");
            response.put("chatResponse", chatResponse);
            response.put("userId", userId);
            response.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Chat processata con successo per RequestID: " + request.getString("requestId"));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore processamento chat", e);
            response.put("status", "error");
            response.put("message", "Errore nel processamento della chat: " + e.getMessage());
        }
    }

    /**
     * Gestisce l'operazione di analisi immagine
     */
    private void handleAnalizzaImmagine(JSONObject request, JSONObject response) {
        try {
            String base64Image = request.getString("base64Image");
            String fileName = request.getString("fileName");
            String userId = request.getString("userId");
            String descrizione = request.optString("descrizione", "");

            LOGGER.info("Analizzando immagine " + fileName + " per utente " + userId);

            // Chiama il servizio LLM per analizzare l'immagine
            JSONObject analisiResponse = LLMSimulationService.analizzaImmagine(
                    base64Image, fileName, userId, descrizione
            );

            // Costruisce la risposta di successo
            response.put("status", "success");
            response.put("analisiResult", analisiResponse);
            response.put("userId", userId);
            response.put("fileName", fileName);
            response.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Analisi immagine completata per RequestID: " + request.getString("requestId"));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore analisi immagine", e);
            response.put("status", "error");
            response.put("message", "Errore nell'analisi dell'immagine: " + e.getMessage());
        }
    }

    /**
     * Invia la risposta al gateway tramite Kafka
     */
    private void sendResponseToGateway(JSONObject response) {
        try {
            String requestId = response.getString("requestId");
            producer.sendMessage(TOPIC_GATEWAY_RESPONSE, requestId, response.toString());

            LOGGER.info("Risposta inviata al gateway per RequestID: " + requestId);
            LOGGER.info("Contenuto risposta: " + response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore invio risposta al gateway: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ferma il consumer
     */
    public void stop() {
        LOGGER.info("Fermando GestioneOpereConsumer...");
        running = false;

        if (consumer != null) {
            consumer.close();
        }
        if (producer != null) {
            producer.close();
        }

        LOGGER.info("GestioneOpereConsumer fermato");
    }

    /**
     * Verifica se il consumer è in esecuzione
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Main method per testare il consumer in standalone
     */
    public static void main(String[] args) {
        GestioneOpereConsumer consumer = new GestioneOpereConsumer();

        // Aggiungi shutdown hook per chiusura pulita
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook attivato, fermando consumer...");
            consumer.stop();
        }));

        try {
            consumer.start();
            LOGGER.info("GestioneOpereConsumer avviato. Premere Ctrl+C per fermare.");

            // Mantieni il processo attivo
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            LOGGER.info("Consumer interrotto");
            consumer.stop();
        } catch (Exception e) {
            LOGGER.severe("Errore avvio consumer: " + e.getMessage());
            e.printStackTrace();
            consumer.stop();
        }
    }
}