package Service;

import kafka_impl.KafkaProducerService;
import kafka_impl.KafkaConsumerService;
import Util.LLMSimulationService;
import org.json.JSONObject;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.ejb.Singleton;

import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Consumer Kafka per il modulo GestioneOpere
 * Gestisce le operazioni asincrone relative alle opere d'arte e LLM
 */
@Singleton
@Startup
public class GestioneOpereConsumer {

    private static final Logger LOGGER = Logger.getLogger(GestioneOpereConsumer.class.getName());
    private static final String TOPIC_OPERE_MODULE = "gestioneOpere";
    private static final String TOPIC_RESPONSE = "gateway-responses";

    private KafkaConsumerService kafkaConsumer;
    private KafkaProducerService kafkaProducer;
    private volatile boolean running = false;

    @PostConstruct
    public void init() {
        this.kafkaConsumer = new KafkaConsumerService("gestioneopere-group", "earliest");
        this.kafkaProducer = new KafkaProducerService();
        startListening();
    }

    public void startListening() {
        if (running) {
            return;
        }

        running = true;
        Thread consumerThread = new Thread(() -> {
            kafkaConsumer.consumer.subscribe(Collections.singletonList(TOPIC_OPERE_MODULE));
            LOGGER.info("GestioneOpere Consumer avviato - Topic: " + TOPIC_OPERE_MODULE);

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
        LOGGER.info("Thread Consumer Kafka avviato per GestioneOpere");
    }

    private void processMessage(String messageValue) {
        try {
            JSONObject message = new JSONObject(messageValue);
            String operation = message.getString("operation");
            String requestId = message.getString("requestId");

            LOGGER.info("GestioneOpere ricevuto: " + operation + " (RequestID: " + requestId + ")");

            switch (operation) {
                case "processaDomanda":
                    handleProcessaDomanda(message);
                    break;
                case "processaChat":
                    handleProcessaChat(message);
                    break;
                case "analizzaImmagine":
                    handleAnalizzaImmagine(message);
                    break;
                case "getInfoOpera":
                    handleGetInfoOpera(message);
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

    /**
     * Gestisce le domande inviate al servizio LLM
     */
    private void handleProcessaDomanda(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String domanda = message.getString("domanda");
            String userId = message.optString("userId", null);

            LOGGER.info("Processando domanda LLM: " + domanda + " per utente: " + userId);

            // Invoca il servizio LLM
            String rispostaLLM = LLMSimulationService.processaDomanda(domanda, userId);

            // Crea la risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "processaDomanda");
            response.put("risposta", rispostaLLM);
            response.put("domandaOriginale", domanda);
            response.put("timestamp", System.currentTimeMillis());

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore processing domanda: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante l'elaborazione della domanda: " + e.getMessage());
        }
    }

    /**
     * Gestisce le conversazioni chat con il servizio LLM
     */
    private void handleProcessaChat(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String messaggio = message.getString("messaggio");
            String userId = message.optString("userId", null);
            String conversationId = message.optString("conversationId", null);

            LOGGER.info("Processando chat: " + messaggio + " per utente: " + userId + " conversazione: " + conversationId);

            // Invoca il servizio LLM per la chat
            JSONObject chatResult = LLMSimulationService.processaChat(messaggio, userId, conversationId);

            // Crea la risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "processaChat");
            response.put("chatResult", chatResult);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore processing chat: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante l'elaborazione della chat: " + e.getMessage());
        }
    }

    /**
     * Gestisce l'analisi delle immagini inviate dall'utente
     */
    private void handleAnalizzaImmagine(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String base64Image = message.getString("base64Image");
            String fileName = message.getString("fileName");
            String userId = message.optString("userId", null);
            String descrizione = message.optString("descrizione", "");

            LOGGER.info("Analizzando immagine: " + fileName + " per utente: " + userId);

            // Validazione dimensione immagine decodificata
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                if (imageBytes.length > 10 * 1024 * 1024) { // 10MB limit
                    sendErrorResponse(requestId, "Immagine troppo grande (limite 10MB)");
                    return;
                }
            } catch (IllegalArgumentException e) {
                sendErrorResponse(requestId, "Formato immagine Base64 non valido");
                return;
            }

            // Invoca il servizio LLM per l'analisi dell'immagine
            JSONObject analisiResult = LLMSimulationService.analizzaImmagine(base64Image, fileName, userId, descrizione);

            // Crea la risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "analizzaImmagine");
            response.put("analisiResult", analisiResult);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore analisi immagine: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante l'analisi dell'immagine: " + e.getMessage());
        }
    }

    /**
     * Gestisce le richieste di informazioni su opere specifiche
     */
    private void handleGetInfoOpera(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String operaId = message.getString("operaId");
            String userId = message.optString("userId", null);

            LOGGER.info("Recuperando info opera: " + operaId + " per utente: " + userId);

            // Invoca il servizio per ottenere le informazioni dell'opera
            JSONObject infoOpera = LLMSimulationService.getInfoOpera(operaId, userId);

            // Crea la risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getInfoOpera");
            response.put("infoOpera", infoOpera);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore recupero info opera: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante il recupero delle informazioni dell'opera: " + e.getMessage());
        }
    }

    /**
     * Invia una risposta di successo via Kafka
     */
    private void sendResponse(String requestId, String responseMessage) {
        try {
            kafkaProducer.sendMessage(TOPIC_RESPONSE, requestId, responseMessage);
            LOGGER.info("Risposta inviata via Kafka - RequestID: " + requestId);
        } catch (Exception e) {
            LOGGER.severe("Errore invio risposta Kafka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Invia una risposta di errore via Kafka
     */
    private void sendErrorResponse(String requestId, String errorMessage) {
        try {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("requestId", requestId);
            errorResponse.put("status", "error");
            errorResponse.put("message", errorMessage);
            errorResponse.put("timestamp", System.currentTimeMillis());

            kafkaProducer.sendMessage(TOPIC_RESPONSE, requestId, errorResponse.toString());
            LOGGER.warning("Risposta di errore inviata - RequestID: " + requestId + ", Error: " + errorMessage);
        } catch (Exception e) {
            LOGGER.severe("Errore invio risposta di errore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Ferma il consumer quando l'applicazione viene terminata
     */
    public void stop() {
        running = false;
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
        LOGGER.info("GestioneOpere Consumer fermato");
    }
}