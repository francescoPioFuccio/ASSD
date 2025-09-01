package Repository;

import Entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;

import jakarta.ejb.Stateless;

@Stateless  // EJB Stateless Bean (per JTA e WildFly)
public class UserRepository {

    @PersistenceContext(unitName = "UserPersistenceUnit")
    private EntityManager em;

    @Transactional
    public void save(User user) {
        // Se l'ID è nullo, è una nuova entità (persist). Altrimenti, è un aggiornamento (merge).
        // Anche se save dovrebbe essere solo per nuove entità, merge può gestire entrambi.
        if (user.getId() == null) {
            em.persist(user);
        } else {
            em.merge(user); // Usa merge per gestire sia persist che update in modo flessibile
        }
    }
    public void update(User user) {
        em.merge(user);
    }


    public User findById(Long id) {
        return em.find(User.class, id);
    }

    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    public User findByEmail(String email) {
        List<User> results = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Transactional
    public void delete(User user) {
        // Per eliminare un'entità, deve essere nello stato "managed" (gestito dal contesto di persistenza).
        // Se l'entità 'user' passata al metodo proviene da un contesto diverso o è un'entità "detached",
        // 'em.merge(user)' la ricollega al contesto corrente, rendendola "managed".
        // Solo dopo può essere eliminata con 'em.remove()'.
        em.remove(em.merge(user));
    }

    // Puoi anche aggiungere un metodo delete per ID, che è spesso più comodo dal controller
    @Transactional
    public void deleteById(Long id) {
        User user = findById(id); // Trova l'utente per ID
        if (user != null) {
            em.remove(user); // Se trovato, eliminalo
        }
    }

    @Transactional
    public void addUpdatePuntiBonus(User user) {
            em.merge(user); // Esegui il merge per aggiornare l'entità
    }

    @Transactional
    public void removeUpdatePuntiBonus(User user) {
        em.merge(user); // Esegui il merge per aggiornare l'entità
    }
}