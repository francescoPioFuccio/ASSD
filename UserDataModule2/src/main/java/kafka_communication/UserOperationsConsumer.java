// Assicurati che il package sia corretto per il tuo progetto
package kafka_communication;

import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;

import Entity.User; // Importa la tua entità User
import Repository.UserRepository; // Importa il tuo repository

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

/**
 * Questa classe EJB Singleton combina i ruoli di Listener Kafka e Service.
 * 1. Si avvia con l'applicazione (@Startup) e rimane attiva (@Singleton).
 * 2. Ascolta gli eventi sul topic Kafka 'user-operations'.
 * 3. Contiene direttamente la logica per processare questi eventi e interagire
 *    con il database tramite il UserRepository.
 */
@Singleton
@Startup
public class UserOperationsConsumer implements Runnable {

    // Inietta direttamente il Repository per l'accesso al database
    @Inject
    private UserRepository userRepository;

    private Consumer<String, String> consumer;
    private final Gson gson = new Gson();
    private volatile boolean running = true;
    private Thread consumerThread;

    // --- PARTE 1: GESTIONE DEL CICLO DI VITA E INFRASTRUTTURA KAFKA ---

    @PostConstruct
    public void initialize() {
        System.out.println("INFO: Inizializzazione del listener di eventi utente (Kafka)...");
        Properties props = loadKafkaProperties();
        if (props == null) return;

        this.consumer = new KafkaConsumer<>(props);
        this.consumerThread = new Thread(this);
        this.consumerThread.setName("KafkaUserOperationsListenerThread");
        this.consumerThread.start();
    }

    private Properties loadKafkaProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("kafka.properties")) {
            if (input == null) {
                System.err.println("ERRORE: File kafka.properties non trovato. Il consumer non può partire.");
                return null;
            }
            props.load(input);
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-profile-updater-group");
            return props;
        } catch (Exception e) {
            throw new RuntimeException("Impossibile caricare kafka.properties", e);
        }
    }

    @Override
    public void run() {
        final String topic = "user-operations";
        try {
            consumer.subscribe(Collections.singletonList(topic));
            System.out.println("INFO: Listener sottoscritto al topic: " + topic);

            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("INFO: Evento ricevuto [topic=" + record.topic() + ", key=" + record.key() + "]");
                    processEvent(record.value());
                }
            }
        } catch (Exception e) {
            System.err.println("ERRORE: Eccezione nel thread del listener Kafka: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("INFO: Chiusura del consumer Kafka.");
            consumer.close();
        }
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("INFO: Arresto in corso del listener...");
        this.running = false;
        try {
            consumerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("WARN: Interruzione durante l'attesa della terminazione del thread.");
        }
        System.out.println("INFO: Listener arrestato correttamente.");
    }


    // --- PARTE 2: LOGICA DI BUSINESS PER PROCESSARE L'EVENTO ---

    /**
     * Interpreta il messaggio Kafka e decide quale azione intraprendere.
     */
    private void processEvent(String eventPayload) {
        try {
            JSONObject kafkaMessage = new JSONObject(eventPayload);
            String operation = kafkaMessage.optString("operation");

            if ("UPDATE_PROFILE".equals(operation)) {
                Long userId = kafkaMessage.getLong("userId");
                String userDataJson = kafkaMessage.getString("userData");
                User userToUpdate = gson.fromJson(userDataJson, User.class);

                System.out.println("INFO: Processando evento UPDATE_PROFILE per userId: " + userId);
                updateUserInDatabase(userId, userToUpdate); // Chiama il metodo di logica interno

            } else {
                System.out.println("WARN: Ricevuto evento con operazione non gestita: " + operation);
            }
        } catch (Exception e) {
            System.err.println("ERRORE: Fallimento nel processare l'evento Kafka: " + eventPayload);
            e.printStackTrace();
        }
    }

    /**
     * Contiene la logica di business effettiva per aggiornare l'utente nel database.
     * È annotato con @Transactional per garantire la consistenza dei dati.
     */
    @Transactional
    private void updateUserInDatabase(Long userId, User updatedUserData) {
        User existingUser = userRepository.findById(userId);

        if (existingUser == null) {
            System.err.println("ERRORE: Tentativo di aggiornare un utente non esistente con ID: " + userId);
            return;
        }

        // Aggiorna i campi solo se sono stati forniti nella richiesta
        if (updatedUserData.getNome() != null) {
            existingUser.setNome(updatedUserData.getNome());
        }
        if (updatedUserData.getCognome() != null) {
            existingUser.setCognome(updatedUserData.getCognome());
        }
        if (updatedUserData.getEmail() != null) {
            existingUser.setEmail(updatedUserData.getEmail());
        }
        if (updatedUserData.getPassword() != null && !updatedUserData.getPassword().isEmpty()) {
            // ATTENZIONE: In produzione, la password deve essere sempre hashata!
            existingUser.setPassword(updatedUserData.getPassword());
        }
        if (updatedUserData.getMuseoPreferito() != null) {
            existingUser.setMuseoPreferito(updatedUserData.getMuseoPreferito());
        }

        userRepository.update(existingUser); // Usa il repository per salvare le modifiche

        System.out.println("INFO: Profilo per l'utente con ID: " + userId + " aggiornato con successo nel database.");
    }
}