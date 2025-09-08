package Entity; // Assicurati che il package sia corretto

import Entity.JsonStringListConverter;
import jakarta.persistence.*;

import java.util.List;
// Assicurati che il path sia corretto per il tuo convertitore

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String password;
    private String nome;
    private String cognome;
    private int puntiBonus = 0;


    // Campo per le preferenze con il convertitore
    @Column(columnDefinition = "TEXT") // Assicurati che la colonna nel DB supporti stringhe lunghe
    @Convert(converter = JsonStringListConverter.class) // Applica il convertitore
    private List<String> museoPreferito;

    public User() {
        // Costruttore vuoto richiesto da JPA
    }

    public User(String email, String password, String nome, String cognome, List<String> museoPreferito) {
        this.email = email;
        this.password = password;
        this.nome = nome;
        this.cognome = cognome;
        this.museoPreferito = museoPreferito;
        this.puntiBonus = 0;
    }

    // --- GETTER E SETTER PER TUTTI I CAMPI ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public List<String> getMuseoPreferito() {
        return museoPreferito;
    }

    public void setMuseoPreferito(List<String> museoPreferito) {
        this.museoPreferito = museoPreferito;
    }

    public void addPuntiBonus(int punti) {
        this.puntiBonus += punti;
    }
    public int getPuntiBonus() {
        return puntiBonus;
    }
    public void setPuntiBonus(int puntiBonus) {
        this.puntiBonus = puntiBonus;
    }
    public void resetPuntiBonus() {
        this.puntiBonus = 0;
    }
    public void removePuntiBonus(int punti) {
        this.puntiBonus -= punti;
    }
}