package Entity;

public class SimulazionePromozione {
    private String titolo;
    private int puntiNecessari;
    private double sconto;

    // Costruttore
    public SimulazionePromozione(String titolo, int puntiNecessari, double sconto) {
        this.titolo = titolo;
        this.puntiNecessari = puntiNecessari;
        this.sconto = sconto;
    }

    // Getters e Setters
    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public int getPuntiNecessari() {
        return puntiNecessari;
    }

    public void setPuntiNecessari(int puntiNecessari) {
        this.puntiNecessari = puntiNecessari;
    }

    public double getSconto() {
        return sconto;
    }

    public void setSconto(double sconto) {
        this.sconto = sconto;
    }

    // Metodo per simulare l'applicazione della promozione
    public String applicaPromozione(int puntiUtente) {
        if (puntiUtente >= puntiNecessari) {
            return "Promozione applicata! Hai uno sconto del " + sconto + "%";
        } else {
            return "Non hai abbastanza punti per questa promozione.";
        }
    }

    // Metodo per visualizzare la promozione
    public String visualizzaPromozione() {
        return "Promozione: " + titolo + "\nPunti necessari: " + puntiNecessari + "\nSconto: " + sconto + "%";
    }

}
