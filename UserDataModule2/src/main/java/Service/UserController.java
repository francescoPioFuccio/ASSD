package Service;

import Entity.SimulazionePromozione;
import Entity.User;
import Repository.*;
import Util.*;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    @Path("/{id: \\d+}")
    public User getUserById(@PathParam("id") Long id) {
        return userRepository.findById(id);
    }


    @GET
    @Path("/by-email")
    public User getUserByEmail(@QueryParam("email") String email) {
        return userRepository.findByEmail(email);
    }

    // PUT per modificare il profilo
    // ------------------------------------------------------FORSE DA CAMBIARE IL PEROCORSO UPDATE/ID
    @PUT
    @Path("/{id: \\d+}")
    public Response updateProfile(@PathParam("id") Long id, User updatedUser) {
        System.out.println("=== DEBUG UPDATE PROFILE ===");
        System.out.println("ID utente da modificare: " + id);
        System.out.println("Email: " + updatedUser.getEmail());
        System.out.println("Nome: " + updatedUser.getNome());
        System.out.println("Cognome: " + updatedUser.getCognome());
        System.out.println("Preferenze: " + updatedUser.getMuseoPreferito());
        System.out.println("==============================");

        try {
            // 1. Trova l'utente esistente
            User existingUser = userRepository.findById(id);
            if (existingUser == null) {
                System.out.println("❌ Utente non trovato con ID: " + id);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Utente non trovato.\"}")
                        .build();
            }

            // 2. Validazione input
            if (updatedUser.getEmail() == null || updatedUser.getEmail().isEmpty() ||
                    updatedUser.getNome() == null || updatedUser.getNome().isEmpty() ||
                    updatedUser.getCognome() == null || updatedUser.getCognome().isEmpty()) {
                System.out.println("❌ Errore validazione: dati mancanti.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"Email, nome e cognome sono obbligatori.\"}")
                        .build();
            }

            // 3. Controlla se la nuova email è già utilizzata da un altro utente
            if (!updatedUser.getEmail().equals(existingUser.getEmail())) {
                User userWithSameEmail = userRepository.findByEmail(updatedUser.getEmail());
                if (userWithSameEmail != null && !userWithSameEmail.getId().equals(id)) {
                    System.out.println("❌ Email già utilizzata da un altro utente: " + updatedUser.getEmail());
                    return Response.status(Response.Status.CONFLICT)
                            .entity("{\"message\": \"Email già utilizzata da un altro utente.\"}")
                            .build();
                }
            }

            // 4. Aggiorna i campi (mantenendo la password esistente)
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setNome(updatedUser.getNome());
            existingUser.setCognome(updatedUser.getCognome());
            if (updatedUser.getMuseoPreferito() != null) {
                existingUser.setMuseoPreferito(updatedUser.getMuseoPreferito());
            }

            // 5. Salva le modifiche
            userRepository.save(existingUser);
            System.out.println("✅ Profilo aggiornato con successo!");

            // 6. Restituisci l'utente aggiornato (senza password)
            existingUser.setPassword(null);
            return Response.status(Response.Status.OK)
                    .entity("{\"message\": \"Profilo aggiornato con successo!\", \"user\": " +
                            gson.toJson(existingUser) + "}")
                    .build();

        } catch (Exception e) {
            System.out.println("❌ Errore durante l'aggiornamento: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno durante l'aggiornamento: " + e.getMessage() + "\"}")
                    .build();
        }
    }
    //
    @GET
    @Path("/promozioni")
    public Response getPromozioni() {
        String simulatedResponse = SimulazioneService.getPromozioni();
        return Response.status(Response.Status.OK)
                .entity(simulatedResponse)
                .build();
    }

    @POST
    @Path("/applica-promozione")
    public Response applicaPromozione(SimulazionePromozione promozione, @QueryParam("puntiUtente") int puntiUtente) {
        String result = SimulazioneService.applicaPromozione(promozione, puntiUtente);
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"" + result + "\"}")
                .build();
    }
}
