package com.example.app1.ui.quest;

import java.io.Serializable;
import java.util.List;

public class Quest implements Serializable {

    private String idQuest;
    private String titoloQuest;
    private String descrizioneQuest;
    private String nomeOpera;
    private String autoreOpera;
    private String categoria;
    private String difficolta;
    private int puntiRicompensa;
    private int tempoStimato;
    private List<String> indizi;
    private List<String> obiettivi;
    private List<String> suggerimenti;

    // Stati della quest
    private boolean completata;
    private boolean attiva;
    private boolean disponibile;

    // Dettagli extra
    private String dimensioni;
    private String anno;
    private String tecnica;
    private String provenienza;

    // Costruttori
    public Quest() {
    }

    public Quest(String idQuest, String titoloQuest, String descrizioneQuest,
                 String nomeOpera, String autoreOpera, String categoria, String difficolta) {
        this.idQuest = idQuest;
        this.titoloQuest = titoloQuest;
        this.descrizioneQuest = descrizioneQuest;
        this.nomeOpera = nomeOpera;
        this.autoreOpera = autoreOpera;
        this.categoria = categoria;
        this.difficolta = difficolta;
        this.disponibile = true;
    }

    // Getters e Setters
    public String getIdQuest() {
        return idQuest;
    }

    public void setIdQuest(String idQuest) {
        this.idQuest = idQuest;
    }

    public String getTitoloQuest() {
        return titoloQuest;
    }

    public void setTitoloQuest(String titoloQuest) {
        this.titoloQuest = titoloQuest;
    }

    public String getDescrizioneQuest() {
        return descrizioneQuest;
    }

    public void setDescrizioneQuest(String descrizioneQuest) {
        this.descrizioneQuest = descrizioneQuest;
    }

    public String getNomeOpera() {
        return nomeOpera;
    }

    public void setNomeOpera(String nomeOpera) {
        this.nomeOpera = nomeOpera;
    }

    public String getAutoreOpera() {
        return autoreOpera;
    }

    public void setAutoreOpera(String autoreOpera) {
        this.autoreOpera = autoreOpera;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDifficolta() {
        return difficolta;
    }

    public void setDifficolta(String difficolta) {
        this.difficolta = difficolta;
    }

    public int getPuntiRicompensa() {
        return puntiRicompensa;
    }

    public void setPuntiRicompensa(int puntiRicompensa) {
        this.puntiRicompensa = puntiRicompensa;
    }

    public int getTempoStimato() {
        return tempoStimato;
    }

    public void setTempoStimato(int tempoStimato) {
        this.tempoStimato = tempoStimato;
    }

    public List<String> getIndizi() {
        return indizi;
    }

    public void setIndizi(List<String> indizi) {
        this.indizi = indizi;
    }

    public List<String> getObiettivi() {
        return obiettivi;
    }

    public void setObiettivi(List<String> obiettivi) {
        this.obiettivi = obiettivi;
    }

    public List<String> getSuggerimenti() {
        return suggerimenti;
    }

    public void setSuggerimenti(List<String> suggerimenti) {
        this.suggerimenti = suggerimenti;
    }

    public boolean isCompletata() {
        return completata;
    }

    public void setCompletata(boolean completata) {
        this.completata = completata;
    }

    public boolean isAttiva() {
        return attiva;
    }

    public void setAttiva(boolean attiva) {
        this.attiva = attiva;
    }

    public boolean isDisponibile() {
        return disponibile;
    }

    public void setDisponibile(boolean disponibile) {
        this.disponibile = disponibile;
    }

    public String getDimensioni() {
        return dimensioni;
    }

    public void setDimensioni(String dimensioni) {
        this.dimensioni = dimensioni;
    }

    public String getAnno() {
        return anno;
    }

    public void setAnno(String anno) {
        this.anno = anno;
    }

    public String getTecnica() {
        return tecnica;
    }

    public void setTecnica(String tecnica) {
        this.tecnica = tecnica;
    }

    public String getProvenienza() {
        return provenienza;
    }

    public void setProvenienza(String provenienza) {
        this.provenienza = provenienza;
    }

    // Metodi di utilità
    public String getDifficoltaColore() {
        switch (difficolta) {
            case "facile":
                return "#4CAF50"; // Verde
            case "media":
                return "#FF9800"; // Arancione
            case "difficile":
                return "#F44336"; // Rosso
            default:
                return "#9E9E9E"; // Grigio
        }
    }

    public String getCategoriaColore() {
        switch (categoria) {
            case "arte":
            case "rinascimento":
            case "impressionismo":
                return "#9C27B0"; // Viola
            case "scienza":
            case "paleontologia":
                return "#2196F3"; // Blu
            case "storia":
            case "archeologia":
                return "#795548"; // Marrone
            case "tecnologia":
                return "#607D8B"; // Blu grigio
            case "natura":
                return "#4CAF50"; // Verde
            default:
                return "#6200EE"; // Viola scuro
        }
    }

    public int getDifficoltaIcona() {
        switch (difficolta) {
            case "facile":
                return android.R.drawable.star_on;
            case "media":
                return android.R.drawable.star_on;
            case "difficile":
                return android.R.drawable.star_on;
            default:
                return android.R.drawable.star_off;
        }
    }

    @Override
    public String toString() {
        return "Quest{" +
                "idQuest='" + idQuest + '\'' +
                ", titoloQuest='" + titoloQuest + '\'' +
                ", nomeOpera='" + nomeOpera + '\'' +
                ", categoria='" + categoria + '\'' +
                ", difficolta='" + difficolta + '\'' +
                ", disponibile=" + disponibile +
                '}';
    }
}