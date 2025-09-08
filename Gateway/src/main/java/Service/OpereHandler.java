package Service;

import Util.KafkaMessageService;
import Util.GrpcClientManager;

import io.grpc.StatusRuntimeException;
import it.unisannio.opere.grpc.GetInfoOperaRequest;
import it.unisannio.opere.grpc.GetInfoOperaResponse;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler per le operazioni relative alle opere d'arte
 */
public class OpereHandler {

    private static final Logger LOGGER = Logger.getLogger(OpereHandler.class.getName());

    private final KafkaMessageService kafkaMessageService;
    private final GrpcClientManager grpcClientManager;

    public OpereHandler(KafkaMessageService kafkaMessageService, GrpcClientManager grpcClientManager) {
        this.kafkaMessageService = kafkaMessageService;
        this.grpcClientManager = grpcClientManager;
    }

    public Response faiDomanda(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String domanda = request.getString("domanda");
            String userId = request.optString("userId", null);

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "processaDomanda");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("domanda", domanda);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Messaggio Kafka inviato per domanda LLM - RequestID: " + requestId);

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_OPERE_MODULE, "processaDomanda", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response chat(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);
            String messaggio = request.getString("messaggio");
            String userId = request.optString("userId", null);
            String conversationId = request.optString("conversationId", null);

            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "processaChat");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("messaggio", messaggio);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("conversationId", conversationId);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Messaggio Kafka inviato per chat - RequestID: " + requestId);
            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_OPERE_MODULE, "processaChat", kafkaMessage);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore chat: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response analizzaFoto(MultipartFormDataInput input) {
        try {
            Map<String, List<InputPart>> formData = input.getFormDataMap();
            String userId = formData.get("userId").get(0).getBodyAsString();
            String descrizione = formData.containsKey("descrizione") ?
                    formData.get("descrizione").get(0).getBodyAsString() : "";
            InputPart filePart = formData.get("file").get(0);
            InputStream fileInputStream = filePart.getBody(InputStream.class, null);

            String fileName = "unknown.tmp";
            String contentDisposition = filePart.getHeaders().getFirst("Content-Disposition");
            if (contentDisposition != null && contentDisposition.contains("filename=")) {
                fileName = contentDisposition.replaceFirst(".*filename=\"([^\"]+)\".*", "$1");
            }

            byte[] fileBytes = fileInputStream.readAllBytes();

            if (fileBytes.length > 10 * 1024 * 1024) {
                return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                        .entity("{\"message\": \"File troppo grande.\"}")
                        .build();
            }
            if (!isValidImageFile(fileName, fileBytes)) {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                        .entity("{\"message\": \"Formato non supportato.\"}")
                        .build();
            }

            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
            String requestId = UUID.randomUUID().toString();

            JSONObject kafkaMessage = new JSONObject();
            kafkaMessage.put("operation", "analizzaImmagine");
            kafkaMessage.put("requestId", requestId);
            kafkaMessage.put("base64Image", base64Image);
            kafkaMessage.put("fileName", fileName);
            kafkaMessage.put("userId", userId);
            kafkaMessage.put("descrizione", descrizione);
            kafkaMessage.put("timestamp", System.currentTimeMillis());

            LOGGER.info("Messaggio Kafka inviato per analisi foto - RequestID: " + requestId);

            return kafkaMessageService.sendKafkaRequestAndWait(KafkaMessageService.TOPIC_OPERE_MODULE, "analizzaImmagine", kafkaMessage);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Errore analisi foto", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    public Response getInfoOpera(String operaId, String userId) {
        try {
            LOGGER.info("Calling gRPC GetInfoOpera for operaId: " + operaId);

            // Costruisci la richiesta gRPC
            GetInfoOperaRequest request = GetInfoOperaRequest.newBuilder()
                    .setOperaId(operaId)
                    .setUserId(userId != null ? userId : "")
                    .build();

            // Esegui la chiamata sincrona (bloccante)
            GetInfoOperaResponse response = grpcClientManager.getOpereStub().getInfoOpera(request);

            // Restituisci la risposta JSON al client
            return Response.status(Response.Status.OK)
                    .entity(response.getJsonResponse())
                    .build();

        } catch (StatusRuntimeException e) {
            LOGGER.log(Level.SEVERE, "gRPC call failed: " + e.getStatus(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to contact OperaService: " + e.getStatus().getDescription() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An unexpected error occurred in getInfoOpera", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"An internal error occurred in the Gateway.\"}")
                    .build();
        }
    }

    public Response healthCheck() {
        JSONObject health = new JSONObject();
        health.put("status", "OK");
        health.put("service", "OperaService");
        health.put("timestamp", System.currentTimeMillis());
        return Response.status(Response.Status.OK).entity(health.toString()).build();
    }

    // Metodi helper per validazione file
    private boolean isValidImageFile(String fileName, byte[] fileBytes) {
        if (fileName == null || fileName.trim().isEmpty()) return false;
        String extension = getFileExtension(fileName).toLowerCase();
        boolean validExtension = extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");
        return validExtension && hasValidImageSignature(fileBytes, extension);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    private boolean hasValidImageSignature(byte[] fileBytes, String extension) {
        if (fileBytes.length < 4) return false;
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && fileBytes[2] == (byte) 0xFF;
        }
        if ("png".equals(extension)) {
            return fileBytes[0] == (byte) 0x89 && fileBytes[1] == (byte) 0x50 &&
                    fileBytes[2] == (byte) 0x4E && fileBytes[3] == (byte) 0x47;
        }
        return true;
    }
}