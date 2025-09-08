package Service;

import Util.KafkaMessageService;

import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handler per health checks, monitoraggio e test del sistema
 */
public class HealthHandler {

    private static final Logger LOGGER = Logger.getLogger(HealthHandler.class.getName());

    private final KafkaMessageService kafkaMessageService;

    public HealthHandler(KafkaMessageService kafkaMessageService) {
        this.kafkaMessageService = kafkaMessageService;
    }

    public Response getKafkaStatus() {
        JSONObject status = new JSONObject();
        status.put("kafkaEnabled", true);
        status.put("responseListenerRunning", kafkaMessageService.isResponseListenerRunning());
        status.put("pendingRequests", kafkaMessageService.getPendingRequestsCount());
        status.put("topics", new JSONArray()
                .put(KafkaMessageService.TOPIC_USER_MODULE)
                .put(KafkaMessageService.TOPIC_QUEST_MODULE)
                .put(KafkaMessageService.TOPIC_OPERE_MODULE)
                .put(KafkaMessageService.TOPIC_MUSEI_MODULE)
                .put(KafkaMessageService.TOPIC_RESPONSE));
        status.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.OK).entity(status.toString()).build();
    }

    public Response getPendingRequests() {
        JSONObject status = new JSONObject();
        status.put("totalPending", kafkaMessageService.getPendingRequestsCount());
        status.put("requestIds", new JSONArray(kafkaMessageService.getPendingRequestIds()));
        status.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.OK).entity(status.toString()).build();
    }

    public Response testAsyncResponse(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String testMessage = request.optString("message", "Test message");

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "getUserById");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("userId", "1");
            kafkaMessage.put("testMessage", testMessage);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_USER_MODULE, "testAsyncResponse", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore test: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response sendTestMessage(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String topic = request.getString("topic");
            String message = request.getString("message");

            String testId = UUID.randomUUID().toString();
            kafkaMessageService.sendMessage(topic, testId, message);

            return Response.status(Response.Status.OK)
                    .entity("{\"message\": \"Test message sent\", \"testId\": \"" + testId + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore invio test: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}