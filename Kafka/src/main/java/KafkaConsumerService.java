
import org.apache.kafka.clients.consumer.*;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerService {

    private final Consumer<String, String> consumer;

    public KafkaConsumerService(String groupId, String autoOffsetReset) {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("kafka.properties")) {
            props.load(input);
        } catch (Exception e) {
            throw new RuntimeException("Impossibile caricare kafka.properties", e);
        }
        // Sovrascrivo groupId e offset dinamicamente
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        consumer = new KafkaConsumer<>(props);
    }

    public void subscribeAndPoll(String topic) {
        consumer.subscribe(Collections.singletonList(topic));
        System.out.println("Subscribed to topic: " + topic);

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                System.out.println("Ricevuto: key=" + record.key() + ", value=" + record.value());
            }
        }
    }

    public void close() {
        consumer.close();
    }
}
