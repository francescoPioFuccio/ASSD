package Service;

import Entity.Museum;
import Logica.MuseumService;
import com.google.gson.Gson;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/location")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LocationController {

    private final MuseumService museumService = new MuseumService();
    private final Gson gson = new Gson();

    @POST
    @Path("/search")
    public Response searchLocations(String requestBody) {
        try {
            JSONObject request = new JSONObject(requestBody);

            if (!request.has("longitude") || !request.has("latitude") || !request.has("timeLimit")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"I campi 'longitude', 'latitude' e 'timeLimit' sono obbligatori.\"}")
                        .build();
            }

            double longitude = request.getDouble("longitude");
            double latitude = request.getDouble("latitude");
            int timeLimit = request.getInt("timeLimit");

            List<Museum> results = museumService.findReachableMuseums(latitude, longitude, timeLimit);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("museums", results);
            responseMap.put("count", results.size());
            responseMap.put("status", "success");

            return Response.status(Response.Status.OK)
                    .entity(gson.toJson(responseMap))
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno del server: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
