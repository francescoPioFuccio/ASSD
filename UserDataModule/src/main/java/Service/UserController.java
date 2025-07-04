package Service;

import Entity.User;
import Repository.UserRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    private UserRepository userRepository;

    @GET
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GET
    @Path("/{id}")
    public User getUserById(@PathParam("id") Long id) {
        return userRepository.findById(id);
    }

    @POST
    public Response createUser(User user) {
        userRepository.save(user);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/by-email")
    public User getUserByEmail(@QueryParam("email") String email) {
        return userRepository.findByEmail(email);
    }
}
