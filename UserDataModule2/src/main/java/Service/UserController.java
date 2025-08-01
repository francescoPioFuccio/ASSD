package Service;

import Entity.User;
import Repository.UserRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;
import com.google.gson.Gson;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    private UserRepository userRepository;

    private final Gson gson = new Gson();

    @POST
    @Path("/login")
    public Response login(User user) {
        User foundUser = userRepository.findByEmail(user.getEmail());
        if (foundUser == null || !BCrypt.checkpw(user.getPassword(), foundUser.getPassword())) {
            System.out.println("❌ Tentativo di login fallito per email: " + user.getEmail());
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"message\": \"Credenziali non valide.\"}")
                    .build();
        }
        foundUser.setPassword(null);

        System.out.println("✅ Login riuscito per: " + foundUser.getEmail());
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Login riuscito!\", \"user\": " +
                        gson.toJson(foundUser) + "}")
                .build();
    }

    @POST
    @Path("/register")
    public Response register(User user) {
        // 🔎 Log input ricevuto dal client
        System.out.println("=== DEBUG REGISTER ===");
        System.out.println("Email: " + user.getEmail());
        System.out.println("Nome: " + user.getNome());
        System.out.println("Cognome: " + user.getCognome());
        System.out.println("Password (plain): " + user.getPassword());
        System.out.println("Preferenze ricevute: " + user.getMuseoPreferito());
        System.out.println("======================");

        // 1. Validazione input
        if (user.getEmail() == null || user.getEmail().isEmpty() ||
                user.getPassword() == null || user.getPassword().isEmpty() ||
                user.getNome() == null || user.getNome().isEmpty() ||
                user.getCognome() == null || user.getCognome().isEmpty() ||
                user.getMuseoPreferito() == null || user.getMuseoPreferito().isEmpty()
        ) {
            System.out.println("❌ Errore validazione: dati mancanti.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Tutti i campi (email, password, nome, cognome, almeno una preferenza museo) sono obbligatori per la registrazione.\"}")
                    .build();
        }

        // 2. Controlla se l'email è già registrata
        if (userRepository.findByEmail(user.getEmail()) != null) {
            System.out.println("❌ Email già registrata: " + user.getEmail());
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"message\": \"Email già registrata. Prova un'altra email.\"}")
                    .build();
        }

        // 3. Hash della password
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        try {
            // 4. Salva il nuovo utente nel database
            userRepository.save(user);
            System.out.println("✅ Utente salvato con successo!");
            System.out.println("Preferenze salvate nel DB: " + user.getMuseoPreferito());

            return Response.status(Response.Status.CREATED)
                    .entity("{\"message\": \"Registrazione avvenuta con successo!\"}")
                    .build();
        } catch (Exception e) {
            System.out.println("❌ Errore durante la registrazione: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno durante la registrazione: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // --- Metodi esistenti ---
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
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/by-email")
    public User getUserByEmail(@QueryParam("email") String email) {
        return userRepository.findByEmail(email);
    }
}
