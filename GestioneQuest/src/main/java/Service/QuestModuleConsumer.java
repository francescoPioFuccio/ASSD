package Service;

import kafka_impl.KafkaProducerService;
import kafka_impl.KafkaConsumerService;
import Util.QuestSimulationService;
import org.json.JSONArray;
import org.json.JSONObject;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.ejb.Singleton;

import java.time.Duration;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Consumer Kafka per il modulo GestioneQuest
 * Gestisce le operazioni asincrone relative alle quest
 */
@Singleton
@Startup
public class QuestModuleConsumer {

    private static final Logger LOGGER = Logger.getLogger(QuestModuleConsumer.class.getName());
    private static final String TOPIC_QUEST_MODULE = "gestioneQuest";
    private static final String TOPIC_RESPONSE = "gateway-responses";

    private KafkaConsumerService kafkaConsumer;
    private KafkaProducerService kafkaProducer;
    private volatile boolean running = false;

    @PostConstruct
    public void init() {
        this.kafkaConsumer = new KafkaConsumerService("gestioneQuest-group", "earliest");
        this.kafkaProducer = new KafkaProducerService();
        startListening();
    }

    public void startListening() {
        if (running) {
            return;
        }

        running = true;
        Thread consumerThread = new Thread(() -> {
            kafkaConsumer.consumer.subscribe(Collections.singletonList(TOPIC_QUEST_MODULE));
            LOGGER.info("QuestModule Consumer avviato - Topic: " + TOPIC_QUEST_MODULE);

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
        LOGGER.info("Thread Consumer Kafka avviato per QuestModule");
    }

    private void processMessage(String messageValue) {
        try {
            JSONObject message = new JSONObject(messageValue);
            String operation = message.getString("operation");
            String requestId = message.getString("requestId");

            LOGGER.info("QuestModule ricevuto: " + operation + " (RequestID: " + requestId + ")");

            switch (operation) {
                case "getQuestDisponibili":
                    handleGetQuestDisponibili(message);
                    break;
                case "getDettaglioQuest":
                    handleGetDettaglioQuest(message);
                    break;
                case "iniziaQuest":
                    handleIniziaQuest(message);
                    break;
                case "completaQuest":
                    handleCompletaQuest(message);
                    break;
                case "getStoricoQuest":
                    handleGetStoricoQuest(message);
                    break;
                case "getMuseiVisitati":
                    handleGetMuseiVisitati(message);
                    break;
                case "getStatisticheDettagliate":
                    handleGetStatisticheDettagliate(message);
                    break;
                case "debugQuestUtente":
                    handleDebugQuestUtente(message);
                    break;
                default:
                    LOGGER.warning("Operazione non riconosciuta: " + operation);
                    sendErrorResponse(requestId, "Operazione non supportata: " + operation);
            }
        } catch (Exception e) {
            LOGGER.severe("Errore parsing messaggio Kafka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handler per debug delle quest utente
     */
    private void handleDebugQuestUtente(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            LOGGER.info("Debug quest utente: " + userId);

            // Chiama il metodo di debug (non restituisce nulla, solo logging)
            //QuestSimulationService.debugQuestUtente(userId);

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "debugQuestUtente");
            response.put("message", "Debug eseguito - controlla i log");

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore debug quest utente: " + e.getMessage());
            sendErrorResponse(requestId, "Errore debug: " + e.getMessage());
        }
    }

    /**
     * Handler per reset quest di un utente specifico
     */


    /**
     * Handler per reset completo del sistema quest
     */


    /**
     * Gestisce la richiesta delle quest attive dell'utente
     */


    /**
     * Gestisce la richiesta di quest disponibili per un museo
     */
    private void handleGetQuestDisponibili(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            String museoId = message.getString("museoId");
            String preferenze = message.optString("preferenze", "");
            String difficolta = message.optString("difficolta", "media");

            LOGGER.info("Recupero quest disponibili - Museo: " + museoId + ", User: " + userId);

            JSONObject questDisponibili = QuestSimulationService.getQuestDisponibili(
                    userId, museoId, preferenze, difficolta);

            // Invia risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getQuestDisponibili");
            response.put("questDisponibili", questDisponibili);

            sendResponse(requestId, response.toString());
            LOGGER.info("Quest disponibili inviate con successo per museo: " + museoId);

        } catch (Exception e) {
            LOGGER.severe("Errore recupero quest disponibili: " + e.getMessage());
            sendErrorResponse(requestId, "Errore nel recupero delle quest disponibili: " + e.getMessage());
        }
    }

    /**
     * Gestisce la richiesta di dettagli di una quest specifica
     */
    private void handleGetDettaglioQuest(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String questId = message.getString("questId");
            String userId = message.getString("userId");

            LOGGER.info("Recupero dettaglio quest: " + questId + " per user: " + userId);

            JSONObject dettaglioQuest = QuestSimulationService.getDettaglioQuest(questId, userId);

            if (!dettaglioQuest.getBoolean("found")) {
                sendErrorResponse(requestId, "Quest non trovata con ID: " + questId);
                return;
            }

            // Invia risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getDettaglioQuest");
            response.put("dettaglio", dettaglioQuest);

            sendResponse(requestId, response.toString());
            LOGGER.info("Dettaglio quest inviato con successo: " + questId);

        } catch (Exception e) {
            LOGGER.severe("Errore recupero dettaglio quest: " + e.getMessage());
            sendErrorResponse(requestId, "Errore nel recupero dei dettagli della quest: " + e.getMessage());
        }
    }

    /**
     * Gestisce l'inizio di una quest
     * Questa operazione non prevede risposta sincrona al gateway (ASINCRONO PURO)
     */
    private void handleIniziaQuest(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            String questId = message.getString("questId");
            String museoId = message.getString("museoId");

            LOGGER.info("Inizio quest: " + questId + " per user: " + userId + " nel museo: " + museoId);

            JSONObject risultato = QuestSimulationService.iniziaQuest(userId, questId, museoId);

            // Per le operazioni asincrone pure, possiamo comunque inviare una notifica di conferma
            // oppure semplicemente loggare l'operazione. In base al gateway, questa sembra asincrona pura.

            if (risultato.getBoolean("success")) {
                LOGGER.info("Quest iniziata con successo: " + questId + " per user: " + userId);

                // Invia risposta di successo con payload corretto
                JSONObject response = new JSONObject();
                response.put("requestId", requestId);
                response.put("status", "success");
                response.put("operation", "iniziaQuest");
                response.put("message", risultato.getString("message"));
                response.put("questData", risultato); // Tutto il payload della quest

                sendResponse(requestId, response.toString());
            } else {
                String errorMsg = risultato.optString("message", "Errore sconosciuto nell'avvio della quest");
                LOGGER.warning("Errore inizio quest: " + errorMsg);
                sendErrorResponse(requestId, errorMsg);
            }

        } catch (Exception e) {
            LOGGER.severe("Errore inizio quest: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno nell'avvio della quest: " + e.getMessage());
        }
    }

    /**
     * Gestisce il completamento di una quest
     * Questa operazione non prevede risposta sincrona al gateway (ASINCRONO PURO)
     */
    private void handleCompletaQuest(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            String questId = message.getString("questId");
            int tempoCompletamento = message.optInt("tempoCompletamento", 0);

            LOGGER.info("Completamento quest: " + questId + " per user: " + userId +
                    " (tempo: " + tempoCompletamento + " min)");

            JSONObject risultato = QuestSimulationService.completaQuest(userId, questId, tempoCompletamento);

            if (risultato.getBoolean("success")) {
                LOGGER.info("Quest completata con successo: " + questId + " per user: " + userId);

                // Invia risposta di successo con struttura corretta
                JSONObject response = new JSONObject();
                response.put("requestId", requestId);
                response.put("status", "success");
                response.put("operation", "completaQuest");
                response.put("message", risultato.getString("message"));
                response.put("completamento", risultato); // Payload con punteggio e ricompense

                sendResponse(requestId, response.toString());
            } else {
                String errorMsg = risultato.optString("message", "Errore sconosciuto nel completamento della quest");
                LOGGER.warning("Errore completamento quest: " + errorMsg);
                sendErrorResponse(requestId, errorMsg);
            }

        } catch (Exception e) {
            LOGGER.severe("Errore completamento quest: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno nel completamento della quest: " + e.getMessage());
        }
    }

    /**
     * Gestisce la richiesta dello storico quest dell'utente
     */
    private void handleGetStoricoQuest(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");

            LOGGER.info("Recupero storico quest per user: " + userId);

            JSONObject storicoQuest = QuestSimulationService.getStoricoQuest(userId);

            // Invia risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getStoricoQuest");
            response.put("storico", storicoQuest);

            sendResponse(requestId, response.toString());
            LOGGER.info("Storico quest inviato con successo per user: " + userId);

        } catch (Exception e) {
            LOGGER.severe("Errore recupero storico quest: " + e.getMessage());
            sendErrorResponse(requestId, "Errore nel recupero dello storico quest: " + e.getMessage());
        }
    }

    /**
     * Gestisce la richiesta dei musei visitati dall'utente
     */
    private void handleGetMuseiVisitati(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");

            LOGGER.info("Recupero musei visitati per user: " + userId);

            JSONObject museiVisitati = QuestSimulationService.getMuseiVisitati(userId);

            // Invia risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getMuseiVisitati");
            response.put("musei", museiVisitati);

            sendResponse(requestId, response.toString());
            LOGGER.info("Musei visitati inviati con successo per user: " + userId);

        } catch (Exception e) {
            LOGGER.severe("Errore recupero musei visitati: " + e.getMessage());
            sendErrorResponse(requestId, "Errore nel recupero dei musei visitati: " + e.getMessage());
        }
    }

    /**
     * Gestisce la richiesta delle statistiche dettagliate dell'utente
     */
    private void handleGetStatisticheDettagliate(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");

            LOGGER.info("Recupero statistiche dettagliate per user: " + userId);

            // Utilizza il metodo del gateway che calcola statistiche basate sullo storico
            JSONObject storico = QuestSimulationService.getStoricoQuest(userId);
            JSONObject statistiche = calcolaStatisticheDettagliate(storico);

            // Invia risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getStatisticheDettagliate");
            response.put("statistiche", statistiche);

            sendResponse(requestId, response.toString());
            LOGGER.info("Statistiche dettagliate inviate con successo per user: " + userId);

        } catch (Exception e) {
            LOGGER.severe("Errore recupero statistiche dettagliate: " + e.getMessage());
            sendErrorResponse(requestId, "Errore nel recupero delle statistiche dettagliate: " + e.getMessage());
        }
    }

    /**
     * Calcola statistiche dettagliate basate sullo storico quest
     * (Replica la logica dal GatewayController)
     */
    private JSONObject calcolaStatisticheDettagliate(JSONObject storico) {
        JSONObject statistiche = new JSONObject();

        try {
            JSONObject stats = storico.optJSONObject("statistiche");
            JSONArray questCompletate = storico.optJSONArray("questCompletate");

            if (stats != null) {
                statistiche.put("questTotali", stats.optInt("questTotali", 0));
                statistiche.put("punteggioTotale", stats.optInt("punteggioTotale", 0));
                statistiche.put("museiVisitati", stats.optInt("museiVisitati", 0));

                if (questCompletate != null && questCompletate.length() > 0) {
                    int tempoTotale = 0;
                    java.util.Map<String, Integer> difficoltaCount = new java.util.HashMap<>();
                    java.util.Map<String, Integer> categoriaCount = new java.util.HashMap<>();

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
        } catch (Exception e) {
            LOGGER.warning("Errore calcolo statistiche dettagliate: " + e.getMessage());
            // Restituisci statistiche vuote in caso di errore
            statistiche.put("questTotali", 0);
            statistiche.put("punteggioTotale", 0);
            statistiche.put("museiVisitati", 0);
        }

        return statistiche;
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

            kafkaProducer.sendMessage(TOPIC_RESPONSE, requestId, errorResponse.toString());
            LOGGER.warning("Risposta di errore inviata - RequestID: " + requestId + ", Error: " + errorMessage);
        } catch (Exception e) {
            LOGGER.severe("Errore invio risposta di errore: " + e.getMessage());
        }
    }

    /**
     * Ferma il consumer Kafka
     */
    public void stop() {
        running = false;
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }
        LOGGER.info("QuestModule Consumer fermato");
    }
}