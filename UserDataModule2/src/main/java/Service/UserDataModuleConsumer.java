package Service;

import Entity.User;
import Entity.SimulazionePromozione;

import kafka_impl.KafkaProducerService;
import kafka_impl.KafkaConsumerService;
import Repository.UserRepository;
import Util.SimulazioneService;
import org.json.JSONArray;
import org.json.JSONObject;
import jakarta.inject.Inject;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Startup;
import jakarta.ejb.Singleton;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Consumer Kafka per il modulo UserDataModule2
 * Gestisce le operazioni asincrone sui dati utente
 */
@Singleton
@Startup
public class UserDataModuleConsumer {

    private static final Logger LOGGER = Logger.getLogger(UserDataModuleConsumer.class.getName());
    private static final String TOPIC_USER_MODULE = "userdatamodule";
    private static final String TOPIC_RESPONSE = "gateway-responses";

    @Inject
    private UserRepository userRepository;

    private KafkaConsumerService kafkaConsumer;
    private KafkaProducerService kafkaProducer;
    private volatile boolean running = false;

    @PostConstruct
    public void init() {
        this.kafkaConsumer = new KafkaConsumerService("userdatamodule-group", "earliest");
        this.kafkaProducer = new KafkaProducerService();
        startListening();
    }

    public void startListening() {
        if (running) {
            return;
        }

        running = true;
        Thread consumerThread = new Thread(() -> {
            kafkaConsumer.consumer.subscribe(Collections.singletonList(TOPIC_USER_MODULE));
            LOGGER.info("UserDataModule Consumer avviato - Topic: " + TOPIC_USER_MODULE);

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
        LOGGER.info("Thread Consumer Kafka avviato per UserDataModule");
    }

    private void processMessage(String messageValue) {
        try {
            JSONObject message = new JSONObject(messageValue);
            String operation = message.getString("operation");
            String requestId = message.getString("requestId");

            LOGGER.info("UserDataModule ricevuto: " + operation + " (RequestID: " + requestId + ")");

            switch (operation) {
                case "aggiungiPunti":
                    handleAggiungiPunti(message);
                    break;
                case "rimuoviPunti":
                    handleRimuoviPunti(message);
                    break;
                case "getUserById":
                    handleGetUserById(message);
                    break;
                case "getUserByEmail":
                    handleGetUserByEmail(message);
                    break;
                case "getAllUsers":
                    handleGetAllUsers(message);
                    break;
                case "updateProfile":
                    handleUpdateProfile(message);
                    break;
                case "getPromozioni":
                    handleGetPromozioni(message);
                    break;
                case "applicaPromozione":
                    handleApplicaPromozione(message);
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

    private void handleAggiungiPunti(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            int punti = message.getInt("punti");

            User user = userRepository.findById((Long) Long.parseLong(userId));
            if (user == null) {
                sendErrorResponse(requestId, "Utente non trovato con ID: " + userId);
                return;
            }

            user.addPuntiBonus(punti);
            userRepository.save(user);

            LOGGER.info("Punti aggiunti con successo per utente " + userId + " (+" + punti + " punti)");

            // Invia risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "aggiungiPunti");
            response.put("message", "Punti bonus aggiunti con successo.");
            response.put("nuoviPunti", user.getPuntiBonus());

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore aggiunta punti: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante l'aggiunta dei punti: " + e.getMessage());
        }
    }

    private void handleRimuoviPunti(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            int punti = message.getInt("punti");

            User user = userRepository.findById((Long) Long.parseLong(userId));
            if (user == null) {
                sendErrorResponse(requestId, "Utente non trovato.");
                return;
            }

            if (user.getPuntiBonus() < punti) {
                sendErrorResponse(requestId, "Punti bonus insufficienti.");
                return;
            }

            user.removePuntiBonus(punti);
            userRepository.save(user);

            LOGGER.info("Punti rimossi con successo per utente " + userId + " (-" + punti + " punti)");

            // Invia risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "rimuoviPunti");
            response.put("message", "Punti bonus rimossi con successo.");
            response.put("puntiResidui", user.getPuntiBonus());

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore rimozione punti: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante la rimozione dei punti: " + e.getMessage());
        }
    }

    private void handleGetUserById(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            User user = userRepository.findById((Long) Long.parseLong(userId));

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getUserById");

            if (user != null) {
                // Rimuovi password per sicurezza
                user.setPassword(null);
                response.put("user", new JSONObject(user));
            } else {
                response.put("user", JSONObject.NULL);
            }

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore recupero utente per ID: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno: " + e.getMessage());
        }
    }

    private void handleGetUserByEmail(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String email = message.getString("email");
            User user = userRepository.findByEmail(email);

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getUserByEmail");

            if (user != null) {
                // Rimuovi password per sicurezza
                user.setPassword(null);
                response.put("user", new JSONObject(user));
            } else {
                response.put("user", JSONObject.NULL);
            }

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore recupero utente per email: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno: " + e.getMessage());
        }
    }

    private void handleGetAllUsers(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            var users = userRepository.findAll();

            // Rimuovi password da tutti gli utenti per sicurezza
            users.forEach(user -> user.setPassword(null));

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getAllUsers");
            response.put("users", users);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore recupero tutti gli utenti: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno: " + e.getMessage());
        }
    }

    private void handleUpdateProfile(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            String userId = message.getString("userId");
            JSONObject updatedUserJson = message.getJSONObject("updatedUser");

            User existingUser = userRepository.findById((Long) Long.parseLong(userId));
            if (existingUser == null) {
                sendErrorResponse(requestId, "Utente non trovato.");
                return;
            }

            // Validazione campi obbligatori
            String newEmail = updatedUserJson.optString("email");
            String newNome = updatedUserJson.optString("nome");
            String newCognome = updatedUserJson.optString("cognome");

            if (newEmail.isEmpty() || newNome.isEmpty() || newCognome.isEmpty()) {
                sendErrorResponse(requestId, "Email, nome e cognome sono obbligatori.");
                return;
            }

            // Controllo email duplicata
            if (!newEmail.equals(existingUser.getEmail())) {
                User userWithSameEmail = userRepository.findByEmail(newEmail);
                if (userWithSameEmail != null && !userWithSameEmail.getId().equals(existingUser.getId())) {
                    sendErrorResponse(requestId, "Email già utilizzata da un altro utente.");
                    return;
                }
            }

            // Aggiorna i campi
            existingUser.setEmail(newEmail);
            existingUser.setNome(newNome);
            existingUser.setCognome(newCognome);

            if (updatedUserJson.has("museoPreferito")) {
                // Gestisci la lista di musei preferiti se presente
                // Per semplicità, assumo che sia una stringa per ora
                // existingUser.setMuseoPreferito(updatedUserJson.optString("museoPreferito"));
            }

            userRepository.save(existingUser);
            existingUser.setPassword(null);

            LOGGER.info("Profilo aggiornato per utente " + userId);

            // Invia risposta di successo
            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "updateProfile");
            response.put("message", "Profilo aggiornato con successo!");
            response.put("user", new JSONObject(existingUser));

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore aggiornamento profilo: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno durante l'aggiornamento: " + e.getMessage());
        }
    }

    private void handleGetPromozioni(JSONObject message) {
       System.out.println("Handling getPromozioni");
        String requestId = message.getString("requestId");
        try {
            String promozioni = SimulazioneService.getPromozioni();

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "getPromozioni");
            response.put("promozioni", new JSONArray(promozioni));

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore recupero promozioni: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno: " + e.getMessage());
        }
    }

    private void handleApplicaPromozione(JSONObject message) {
        String requestId = message.getString("requestId");
        try {
            JSONObject promozioneJson = message.getJSONObject("promozione");
            int puntiUtente = message.getInt("puntiUtente");

            // Ricostruisci l'oggetto SimulazionePromozione
            SimulazionePromozione promozione = new SimulazionePromozione(
                    promozioneJson.getString("titolo"),
                    promozioneJson.getInt("puntiNecessari"),
                    promozioneJson.getDouble("sconto")
            );

            String result = SimulazioneService.applicaPromozione(promozione, puntiUtente);

            JSONObject response = new JSONObject();
            response.put("requestId", requestId);
            response.put("status", "success");
            response.put("operation", "applicaPromozione");
            response.put("message", result);

            sendResponse(requestId, response.toString());

        } catch (Exception e) {
            LOGGER.severe("Errore applicazione promozione: " + e.getMessage());
            sendErrorResponse(requestId, "Errore interno: " + e.getMessage());
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
        LOGGER.info("UserDataModule Consumer fermato");
    }
}