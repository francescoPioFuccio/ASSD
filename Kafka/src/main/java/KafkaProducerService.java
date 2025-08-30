
import org.apache.kafka.clients.producer.*;
import java.io.InputStream;
import java.util.Properties;

public class KafkaProducerService {

    private final Producer<String, String> producer;

    public KafkaProducerService() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("kafka.properties")) {
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Impossibile caricare kafka.properties", e);
        }
        producer = new KafkaProducer<>(props);
    }

    public void sendMessage(String topic, String key, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
            } else {
                System.out.println("Messaggio inviato a topic " + metadata.topic() +
                        ", partizione " + metadata.partition() +
                        ", offset " + metadata.offset());
            }
        });
    }

    public void close() {
        producer.close();
    }
}
