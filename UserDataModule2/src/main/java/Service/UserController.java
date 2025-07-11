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

    @POST
    @Path("/login")
    public Response login(User user) {
        // Chiamata placeholder per login
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/register")
    public Response register(User user) {
        // Chiamata placeholder per register
        return Response.status(Response.Status.CREATED).build();
    }
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
