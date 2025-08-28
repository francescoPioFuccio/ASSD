package Service;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestController {

    @GET
    @Path("/hello")
    public Response hello() {
        return Response.ok("{\"message\": \"Hello World\"}").build();
    }
}