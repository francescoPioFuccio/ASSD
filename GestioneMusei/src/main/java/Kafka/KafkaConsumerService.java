package Kafka;

import Util.MuseoSimulationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@Startup
public class KafkaConsumerService {

    @Inject
    private KafkaConfig kafkaConfig;

    @Inject
    private KafkaProducerService producerService;

    // Usa ManagedExecutorService di WildFly invece di Executors
    @Resource
    private ManagedExecutorService executorService;

    private Consumer<String, String> consumer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        System.out.println("👂 Inizializzazione Kafka Consumer...");
        try {
            consumer = new KafkaConsumer<>(kafkaConfig.getConsumerProps());

            // Subscribe ai topics
            consumer.subscribe(Arrays.asList(
                    KafkaConfig.USER_LOCATION_TOPIC,
                    KafkaConfig.MUSEUM_ARRIVAL_TOPIC
            ));

            // Avvia consumer in thread gestito da WildFly
            running.set(true);
            executorService.submit(this::consumeMessages);

            System.out.println("✅ Kafka Consumer avviato e in ascolto...");
        } catch (Exception e) {
            System.err.println("❌ Errore inizializzazione Kafka Consumer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void consumeMessages() {
        System.out.println("🔄 Consumer thread avviato, in ascolto per messaggi...");
        int retryCount = 0;
        final int MAX_RETRIES = 5;

        while (running.get()) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                // Reset retry counter se polling ha successo
                retryCount = 0;

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        handleMessage(record);
                    } catch (Exception e) {
                        System.err.println("❌ Errore processamento singolo messaggio: " + e.getMessage());
                        e.printStackTrace();
                        // Non fermare tutto per un singolo messaggio errato
                    }
                }

            } catch (Exception e) {
                retryCount++;
                System.err.println("❌ Errore nel polling dei messaggi (tentativo " + retryCount + "/" + MAX_RETRIES + "): " + e.getMessage());

                if (retryCount >= MAX_RETRIES) {
                    System.err.println("💥 Troppi errori consecutivi, fermo il consumer");
                    running.set(false);
                    break;
                }

                // Backoff esponenziale
                try {
                    long sleepTime = Math.min(30000, 1000 * (long) Math.pow(2, retryCount));
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.out.println("🛑 Consumer thread terminato");
    }

    private void handleMessage(ConsumerRecord<String, String> record) {
        try {
            String topic = record.topic();
            String userId = record.key();
            String message = record.value();

            System.out.printf("📨 Messaggio ricevuto - Topic: %s, UserId: %s%n", topic, userId);

            switch (topic) {
                case KafkaConfig.USER_LOCATION_TOPIC:
                    handleUserLocationMessage(userId, message);
                    break;
                case KafkaConfig.MUSEUM_ARRIVAL_TOPIC:
                    handleMuseumArrivalMessage(userId, message);
                    break;
                default:
                    System.out.println("❓ Topic non riconosciuto: " + topic);
            }
        } catch (Exception e) {
            System.err.println("❌ Errore gestione messaggio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUserLocationMessage(String userId, String message) {
        try {
            JSONObject locationData = new JSONObject(message);

            double latitude = locationData.getDouble("latitude");
            double longitude = locationData.getDouble("longitude");

            System.out.printf("📍 Posizione ricevuta da %s: %.6f, %.6f%n", userId, latitude, longitude);

            // USA DIRETTAMENTE IL TUO MuseoSimulationService ESISTENTE
            JSONObject risultatoMusei = MuseoSimulationService.trovaMuseiRaccomandati(
                    userId,
                    latitude,
                    longitude,
                    "", // preferenze vuote per ora
                    10  // raggio 10km
            );

            // Invia la risposta JSON direttamente all'app Android via Kafka
            producerService.sendNearbyMuseums(userId, risultatoMusei);

            System.out.printf("🏛 Inviati %d musei a %s via Kafka%n",
                    risultatoMusei.getJSONArray("musei").length(), userId);

        } catch (Exception e) {
            System.err.println("❌ Errore gestione posizione utente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleMuseumArrivalMessage(String userId, String message) {
        try {
            JSONObject arrivalData = new JSONObject(message);

            String museoId = arrivalData.getString("museoId");
            String museoName = arrivalData.optString("museoName", "Museo Sconosciuto");
            long timestamp = arrivalData.optLong("timestamp", System.currentTimeMillis());

            System.out.printf("🎯 Arrivo registrato - Utente: %s, Museo: %s (%s)%n",
                    userId, museoName, museoId);

            // Qui puoi aggiungere logica per:
            // - Salvare l'arrivo nel database
            // - Inviare notifiche
            // - Fare analytics
            // - Attivare servizi location-based

            // Per ora logghiamo soltanto
            System.out.println("✅ Arrivo al museo registrato con successo");

        } catch (Exception e) {
            System.err.println("❌ Errore gestione arrivo museo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("🔄 Pulizia Kafka Consumer...");
        running.set(false);
        if (consumer != null) {
            consumer.close();
        }
        System.out.println("✅ Kafka Consumer fermato");
    }
}