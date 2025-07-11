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
        em.persist(user);
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
        em.remove(em.merge(user));
    }
}
