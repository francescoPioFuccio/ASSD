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

import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;

// @Consumes è stato rimosso dalla dichiarazione della classe
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    private UserRepository userRepository;

    private final Gson gson = new Gson();

    // === NUOVO ENDPOINT DI TEST ===
    @GET
    @Path("/test")
    public Response testEndpoint() {
        System.out.println("🔥 TEST ENDPOINT CHIAMATO - Server funzionante!");
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Server UserController funzionante!\", \"timestamp\": " + System.currentTimeMillis() + "}")
                .build();
    }

    // === NUOVO ENDPOINT DI TEST PER PATH SPECIFICO ===
    @PUT
    @Path("/{id}/test-punti")
    public Response testPuntiPath(@PathParam("id") String id) {
        System.out.println("🔥 TEST PUNTI PATH CHIAMATO con ID: " + id);
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"Path punti funzionante per ID: " + id + "\"}")
                .build();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON) // Aggiunto qui
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
    @Consumes(MediaType.APPLICATION_JSON) // Aggiunto qui
    public Response register(User user) {
        System.out.println("=== DEBUG REGISTER ===");
        System.out.println("Email: " + user.getEmail());
        System.out.println("Nome: " + user.getNome());
        System.out.println("Cognome: " + user.getCognome());
        System.out.println("Password (plain): " + user.getPassword());
        System.out.println("Preferenze ricevute: " + user.getMuseoPreferito());
        System.out.println("======================");

        if (user.getEmail() == null || user.getEmail().isEmpty() ||
                user.getPassword() == null || user.getPassword().isEmpty() ||
                user.getNome() == null || user.getNome().isEmpty() ||
                user.getCognome() == null || user.getCognome().isEmpty() ||
                user.getMuseoPreferito() == null || user.getMuseoPreferito().isEmpty()) {
            System.out.println("❌ Errore validazione: dati mancanti.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"message\": \"Tutti i campi (email, password, nome, cognome, almeno una preferenza museo) sono obbligatori per la registrazione.\"}")
                    .build();
        }

        if (userRepository.findByEmail(user.getEmail()) != null) {
            System.out.println("❌ Email già registrata: " + user.getEmail());
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"message\": \"Email già registrata. Prova un'altra email.\"}")
                    .build();
        }

        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);

        try {
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

    @GET
    public List<User> getAllUsers() {
        System.out.println("🔍 GET /users chiamato - Lista tutti gli utenti");
        List<User> users = userRepository.findAll();
        System.out.println("🔍 Trovati " + (users != null ? users.size() : 0) + " utenti");
        return users;
    }

    @GET
    @Path("/{id}")
    public User getUserById(@PathParam("id") Long id) {
        System.out.println("🔍 GET /users/" + id + " chiamato");
        User user = userRepository.findById(id);
        System.out.println("🔍 Utente trovato: " + (user != null ? user.getEmail() : "null"));
        return user;
    }

    @GET
    @Path("/by-email")
    public User getUserByEmail(@QueryParam("email") String email) {
        return userRepository.findByEmail(email);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON) // Aggiunto qui
    public Response updateProfile(@PathParam("id") Long id, User updatedUser) {
        System.out.println("=== DEBUG UPDATE PROFILE ===");
        System.out.println("ID utente da modificare: " + id);
        System.out.println("Email: " + updatedUser.getEmail());
        System.out.println("Nome: " + updatedUser.getNome());
        System.out.println("Cognome: " + updatedUser.getCognome());
        System.out.println("Preferenze: " + updatedUser.getMuseoPreferito());
        System.out.println("==============================");

        try {
            User existingUser = userRepository.findById(id);
            if (existingUser == null) {
                System.out.println("❌ Utente non trovato con ID: " + id);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Utente non trovato.\"}")
                        .build();
            }

            if (updatedUser.getEmail() == null || updatedUser.getEmail().isEmpty() ||
                    updatedUser.getNome() == null || updatedUser.getNome().isEmpty() ||
                    updatedUser.getCognome() == null || updatedUser.getCognome().isEmpty()) {
                System.out.println("❌ Errore validazione: dati mancanti.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"Email, nome e cognome sono obbligatori.\"}")
                        .build();
            }

            if (!updatedUser.getEmail().equals(existingUser.getEmail())) {
                User userWithSameEmail = userRepository.findByEmail(updatedUser.getEmail());
                if (userWithSameEmail != null && !userWithSameEmail.getId().equals(id)) {
                    System.out.println("❌ Email già utilizzata da un altro utente: " + updatedUser.getEmail());
                    return Response.status(Response.Status.CONFLICT)
                            .entity("{\"message\": \"Email già utilizzata da un altro utente.\"}")
                            .build();
                }
            }

            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setNome(updatedUser.getNome());
            existingUser.setCognome(updatedUser.getCognome());
            if (updatedUser.getMuseoPreferito() != null) {
                existingUser.setMuseoPreferito(updatedUser.getMuseoPreferito());
            }

            userRepository.save(existingUser);
            System.out.println("✅ Profilo aggiornato con successo!");

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
    @Consumes(MediaType.APPLICATION_JSON) // Aggiunto qui
    public Response applicaPromozione(SimulazionePromozione promozione, @QueryParam("puntiUtente") int puntiUtente) {
        String result = SimulazioneService.applicaPromozione(promozione, puntiUtente);
        return Response.status(Response.Status.OK)
                .entity("{\"message\": \"" + result + "\"}")
                .build();
    }

    // === ENDPOINT AGGIUNGI PUNTI CON DEBUG MASSIMO ===
    @PUT
    @Path("/{id}/aggiungi-punti")
    public Response aggiungiPunti(@PathParam("id") Long id, @QueryParam("punti") int punti) {
        System.out.println("🔥🔥🔥 ===== INIZIO DEBUG AGGIUNGI PUNTI ===== 🔥🔥🔥");
        System.out.println("🔥 ENDPOINT AGGIUNGI-PUNTI CHIAMATO!");
        System.out.println("🔥 Path parameter ID ricevuto: " + id);
        System.out.println("🔥 Query parameter PUNTI ricevuto: " + punti);
        System.out.println("🔥 Timestamp chiamata: " + System.currentTimeMillis());
        System.out.println("🔥 Thread corrente: " + Thread.currentThread().getName());

        try {
            System.out.println("🔥 Step 1: Ricerca utente nel database...");
            User user = userRepository.findById(id);

            if (user == null) {
                System.out.println("❌🔥 ERRORE: Utente non trovato con ID: " + id);
                System.out.println("🔥 Verifica se l'ID " + id + " esiste nel database!");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Utente non trovato con ID: " + id + "\"}")
                        .build();
            }

            System.out.println("✅🔥 Step 2: Utente trovato!");
            System.out.println("🔥 Email utente: " + user.getEmail());
            System.out.println("🔥 Nome utente: " + user.getNome() + " " + user.getCognome());
            System.out.println("🔥 Punti bonus attuali PRIMA dell'aggiornamento: " + user.getPuntiBonus());

            System.out.println("🔥 Step 3: Aggiunta punti...");
            user.addPuntiBonus(punti);

            System.out.println("🔥 Punti bonus DOPO l'addizione (in memoria): " + user.getPuntiBonus());

            System.out.println("🔥 Step 4: Salvataggio nel database...");
            userRepository.save(user);

            System.out.println("✅🔥 Step 5: Salvataggio completato con successo!");

            // Verifica che il salvataggio sia andato a buon fine ricaricando l'utente
            System.out.println("🔥 Step 6: Verifica finale - ricaricamento utente dal DB...");
            User userVerifica = userRepository.findById(id);
            System.out.println("🔥 Punti bonus dopo salvataggio nel DB: " +
                    (userVerifica != null ? userVerifica.getPuntiBonus() : "UTENTE NULL!"));

            String responseMessage = "{\"message\": \"Punti bonus aggiunti con successo.\", " +
                    "\"nuoviPunti\": " + user.getPuntiBonus() + ", " +
                    "\"puntiAggiunti\": " + punti + ", " +
                    "\"userId\": " + id + ", " +
                    "\"timestamp\": " + System.currentTimeMillis() + "}";

            System.out.println("🔥 Response JSON: " + responseMessage);
            System.out.println("✅🔥 ===== FINE DEBUG AGGIUNGI PUNTI - SUCCESSO ===== 🔥🔥🔥");

            return Response.status(Response.Status.OK)
                    .entity(responseMessage)
                    .build();

        } catch (Exception e) {
            System.out.println("❌🔥 ===== ERRORE FATALE IN AGGIUNGI PUNTI ===== 🔥🔥🔥");
            System.out.println("❌🔥 Classe eccezione: " + e.getClass().getName());
            System.out.println("❌🔥 Messaggio errore: " + e.getMessage());
            System.out.println("❌🔥 Stack trace:");
            e.printStackTrace();
            System.out.println("❌🔥 ===== FINE ERRORE FATALE ===== 🔥🔥🔥");

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno durante l'aggiunta dei punti: " + e.getMessage() + "\", \"error\": true}")
                    .build();
        }
    }

    // NESSUN @Consumes qui perché non c'è un corpo (body) nella richiesta
    @PUT
    @Path("/{id}/rimuovi-punti")
    public Response rimuoviPunti(@PathParam("id") Long id, @QueryParam("punti") int punti) {
        System.out.println("✅ Richiesta ricevuta per rimuovere " + punti + " punti all'utente " + id);
        try {
            User user = userRepository.findById(id);
            if (user == null) {
                System.out.println("❌ Utente non trovato con ID: " + id + " per rimuovere punti.");
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"message\": \"Utente non trovato.\"}")
                        .build();
            }

            if (user.getPuntiBonus() < punti) {
                System.out.println("❌ Errore: punti insufficienti per la rimozione.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"message\": \"Punti bonus insufficienti.\"}")
                        .build();
            }
            user.removePuntiBonus(punti);

            // Invece di un metodo custom "removeUpdatePuntiBonus", usiamo il save standard
            userRepository.save(user);

            System.out.println("✅ Punti rimossi con successo per l'utente " + id);
            return Response.status(Response.Status.OK)
                    .entity("{\"message\": \"Punti bonus rimossi con successo.\", \"puntiResidui\": " + user.getPuntiBonus() + "}")
                    .build();
        } catch (Exception e) {
            System.out.println("❌ Errore durante la rimozione dei punti: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"message\": \"Errore interno durante la rimozione dei punti: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}