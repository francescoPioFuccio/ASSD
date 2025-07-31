package Service;

import Entity.User;
import Repository.UserRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

// Import per la libreria di hashing BCrypt
import org.mindrot.jbcrypt.BCrypt;
// ... (altri import esistenti) ...

// Import per la libreria di hashing BCrypt
import org.mindrot.jbcrypt.BCrypt; // <-- Assicurati che questa riga sia presente

// Import per Gson
import com.google.gson.Gson; // <-- Assicurati che questa riga sia presente
// Import per Gson (necessario per serializzare l'oggetto User nel JSON di risposta)
import com.google.gson.Gson; // Aggiungi questo import

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    private UserRepository userRepository;

    private final Gson gson = new Gson(); // Istanzia Gson per serializzare l'oggetto User

    @POST
    @Path("/login")
    public Response login(User user) {
        // 1. Validazione input di base
        if (user.getEmail() == null || user.getEmail().isEmpty() ||
                user.getPassword() == null || user.getPassword().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST) // 400 Bad Request
                    .entity("{\"message\": \"Email e password sono richiesti.\"}")
                    .build();
        }

        // 2. Cerca l'utente per email nel database
        User foundUser = userRepository.findByEmail(user.getEmail());

        // 3. Verifica se l'utente esiste
        if (foundUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED) // 401 Unauthorized (o 403 Forbidden)
                    .entity("{\"message\": \"Credenziali non valide.\"}")
                    .build();
        }

        // 4. Verifica la password hashata con quella fornita
        if (BCrypt.checkpw(user.getPassword(), foundUser.getPassword())) {
            // Login riuscito!
            // NON restituire la password hashata o altre informazioni sensibili nel JSON di risposta
            foundUser.setPassword(null); // Rimuovi la password prima di inviare l'oggetto utente
            return Response.status(Response.Status.OK) // 200 OK
                    .entity("{\"message\": \"Login riuscito!\", \"user\": " +
                            gson.toJson(foundUser) + "}") // Restituisci l'utente (senza password) e un messaggio
                    .build();
        } else {
            // Password errata
            return Response.status(Response.Status.UNAUTHORIZED) // 401 Unauthorized
                    .entity("{\"message\": \"Credenziali non valide.\"}")
                    .build();
        }
    }

    @POST
    @Path("/register")
    public Response register(User user) {
        // 1. Validazione input: tutti i campi sono obbligatori
        if (user.getEmail() == null || user.getEmail().isEmpty() ||
                user.getPassword() == null || user.getPassword().isEmpty() ||
                user.getNome() == null || user.getNome().isEmpty() ||
                user.getCognome() == null || user.getCognome().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST) // 400 Bad Request
                    .entity("{\"message\": \"Tutti i campi sono obbligatori per la registrazione.\"}")
                    .build();
        }

        // 2. Controlla se l'email è già registrata
        if (userRepository.findByEmail(user.getEmail()) != null) {
            return Response.status(Response.Status.CONFLICT) // 409 Conflict
                    .entity("{\"message\": \"Email già registrata. Prova un'altra email.\"}")
                    .build();
        }

        // 3. Hash della password prima di salvarla nel database
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        try {
            // 4. Salva il nuovo utente nel database
            userRepository.save(user);
            return Response.status(Response.Status.CREATED) // 201 Created
                    .entity("{\"message\": \"Registrazione avvenuta con successo!\"}")
                    .build();
        } catch (Exception e) {
            // 5. Gestione di eventuali altri errori di salvataggio (es. problemi DB)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR) // 500 Internal Server Error
                    .entity("{\"message\": \"Errore interno durante la registrazione: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // --- Metodi esistenti (Lasciali come sono, o modificali se vuoi renderli più robusti) ---
    @GET
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GET
    @Path("/{id}")
    public User getUserById(@PathParam("id") Long id) {
        return userRepository.findById(id);
    }

    @POST // Questo endpoint è per creare un utente generico, non per la registrazione pubblica
    // Potrebbe essere utile se hai un pannello admin che crea utenti
    public Response createUser(User user) {
        // Anche qui dovresti hashare la password se usi questo endpoint per creare utenti
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