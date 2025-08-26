package Kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

@Singleton
public class KafkaProducerService {

    @Inject
    private KafkaConfig kafkaConfig;

    private Producer<String, String> producer;

    @PostConstruct
    public void init() {
        System.out.println("🚀 Inizializzazione Kafka Producer...");
        try {
            producer = new KafkaProducer<>(kafkaConfig.getProducerProps());
            System.out.println("✅ Kafka Producer inizializzato con successo");
        } catch (Exception e) {
            System.err.println("❌ Errore inizializzazione Kafka Producer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Invia la risposta con i musei raccomandati all'app Android
     * Usa direttamente JSONObject dal tuo MuseoSimulationService
     */
    public void sendNearbyMuseums(String userId, JSONObject museumResponse) {
        try {
            String json = museumResponse.toString();

            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.NEARBY_MUSEUMS_TOPIC,
                    userId,
                    json
            );

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.printf("✅ Musei inviati a %s: partition=%d, offset=%d%n",
                            userId, metadata.partition(), metadata.offset());
                } else {
                    System.err.println("❌ Errore invio musei: " + exception.getMessage());
                    exception.printStackTrace();
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Errore invio musei: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Invia notifica di arrivo al museo
     */
    public void sendMuseumArrival(String userId, String museoId, String museoName) {
        try {
            JSONObject arrivalData = new JSONObject();
            arrivalData.put("userId", userId);
            arrivalData.put("museoId", museoId);
            arrivalData.put("museoName", museoName);
            arrivalData.put("timestamp", System.currentTimeMillis());

            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaConfig.MUSEUM_ARRIVAL_TOPIC,
                    userId,
                    arrivalData.toString()
            );

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.printf("✅ Arrivo museo inviato per %s: %s%n", userId, museoName);
                } else {
                    System.err.println("❌ Errore invio arrivo museo: " + exception.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("❌ Errore invio arrivo museo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void cleanup() {
        if (producer != null) {
            System.out.println("🔄 Chiusura Kafka Producer...");
            producer.close();
        }
    }
}