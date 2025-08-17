package Util;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class QuestSimulationService {

    private static final Random random = new Random();
    private static final Map<String, JSONObject> questUtenti = new HashMap<>();
    private static final Map<String, JSONObject> storicoUtenti = new HashMap<>();

    // Database simulato delle quest per diversi musei
    private static final Map<String, List<JSONObject>> DATABASE_QUEST = initializeDatabaseQuest();

    /**
     * Ottiene le quest disponibili per un museo basate su preferenze utente
     */
    public static JSONObject getQuestDisponibili(String userId, String museoId,
                                                 String preferenze, String difficolta) {
        System.out.println("🏛️ Simulazione recupero quest per museo: " + museoId);

        JSONObject risultato = new JSONObject();

        // Ottieni quest per il museo
        List<JSONObject> questMuseo = DATABASE_QUEST.get(museoId);

        if (questMuseo == null || questMuseo.isEmpty()) {
            // Se non ci sono quest specifiche per il museo, genera quest generiche
            System.out.println("questMuseo non trovato, generazione quest generiche...");
            questMuseo = generaQuestGeneriche(museoId);
        }

        // Filtra per preferenze se specificate
        List<JSONObject> questFiltrate = filtraQuestPerPreferenze(questMuseo, preferenze);

        // Filtra per difficoltà
        questFiltrate = filtraQuestPerDifficolta(questFiltrate, difficolta);

        // Limita a massimo 5 quest per non sovraccaricare l'utente
        if (questFiltrate.size() > 5) {
            Collections.shuffle(questFiltrate);
            questFiltrate = questFiltrate.subList(0, 5);
        }

        // Aggiungi informazioni di stato per ogni quest
        for (JSONObject quest : questFiltrate) {
            aggiungiStatoQuest(quest, userId, museoId);
        }

        JSONArray questArray = new JSONArray();
        for (JSONObject quest : questFiltrate) {
            questArray.put(quest);
        }

        risultato.put("found", true);
        risultato.put("success", true);
        risultato.put("userId", userId);
        risultato.put("museoId", museoId);
        risultato.put("quest", questArray);
        risultato.put("totalFound", questArray.length());
        risultato.put("filtri", new JSONObject()
                .put("preferenze", preferenze)
                .put("difficolta", difficolta));
        risultato.put("timestamp", System.currentTimeMillis());

        System.out.println("🎯 Quest generate con successo (" + questArray.length() + ")");
        return risultato;
    }

    /**
     * Ottiene i dettagli completi di una quest specifica
     */
    public static JSONObject getDettaglioQuest(String questId, String userId) {
        System.out.println("📋 Simulazione recupero dettagli quest: " + questId);

        JSONObject dettaglio = new JSONObject();

        // Cerca la quest in tutti i musei
        JSONObject questTrovata = null;
        for (List<JSONObject> questMuseo : DATABASE_QUEST.values()) {
            for (JSONObject quest : questMuseo) {
                if (questId.equals(quest.getString("idQuest"))) {
                    questTrovata = new JSONObject(quest.toString());
                    break;
                }
            }
            if (questTrovata != null) break;
        }

        if (questTrovata == null) {
            // Genera quest al volo se non trovata
            questTrovata = generaQuestDettagliata(questId);
        }

        dettaglio.put("found", true);
        dettaglio.put("idQuest", questId);
        dettaglio.put("userId", userId);

        // Copia tutti i dati della quest
        for (String key : questTrovata.keySet()) {
            dettaglio.put(key, questTrovata.get(key));
        }

        // Aggiungi dettagli extra per la visualizzazione completa
        aggiungiDettagliCompleti(dettaglio, userId);

        dettaglio.put("timestamp", System.currentTimeMillis());
        return dettaglio;
    }

    /**
     * Simula l'avvio di una quest
     */
    public static JSONObject iniziaQuest(String userId, String questId, String museoId) {
        System.out.println("🚀 Simulazione avvio quest: " + questId + " per utente: " + userId + " nel museo: " + museoId);

        JSONObject risultato = new JSONObject();

        // Verifica se la quest esiste nel museo specificato
        boolean questEsiste = verificaEsistenzaQuest(questId, museoId);

        if (!questEsiste) {
            risultato.put("success", false);
            risultato.put("message", "Quest non trovata in questo museo");
            return risultato;
        }

        // Verifica se l'utente ha già una quest attiva
        JSONObject questAttive = questUtenti.get(userId);
        if (questAttive != null && questAttive.has("questAttiva")) {
            risultato.put("success", false);
            risultato.put("message", "Hai già una quest attiva. Completa quella prima di iniziarne una nuova.");
            return risultato;
        }

        // Salva la quest come attiva per l'utente
        JSONObject questData = new JSONObject();
        questData.put("questAttiva", questId);
        questData.put("museoId", museoId); // Aggiungi il museo ID
        questData.put("dataInizio", System.currentTimeMillis());
        questData.put("stato", "in_corso");
        questData.put("progressoOpere", new JSONArray()); // Opere già trovate

        questUtenti.put(userId, questData);

        risultato.put("success", true);
        risultato.put("message", "Quest avviata con successo!");
        risultato.put("questId", questId);
        risultato.put("userId", userId);
        risultato.put("museoId", museoId);
        risultato.put("dataInizio", questData.getLong("dataInizio"));
        risultato.put("stato", "in_corso");
        risultato.put("timestamp", System.currentTimeMillis());

        return risultato;
    }

    /**
     * Simula il completamento di una quest
     */
    public static JSONObject completaQuest(String userId, String questId, int tempoCompletamento) {
        System.out.println("🏆 Simulazione completamento quest: " + questId);

        JSONObject risultato = new JSONObject();

        // Ottieni dati quest utente
        JSONObject questData = questUtenti.get(userId);
        if (questData == null || !questId.equals(questData.optString("questAttiva"))) {
            risultato.put("success", false);
            risultato.put("message", "Quest non attiva per questo utente");
            return risultato;
        }

        String museoId = questData.optString("museoId", "");

        // Calcola punteggio basato su tempo e difficoltà
        int punteggioBase = calcolaPunteggio(questId, tempoCompletamento);
        int bonus = random.nextInt(50); // Bonus casuale 0-50 punti
        int punteggioTotale = punteggioBase + bonus;

        // Salva nel storico con informazione del museo
        salvaQuestNelloStorico(userId, questId, museoId, punteggioTotale, tempoCompletamento);

        // Rimuovi quest attiva
        questUtenti.remove(userId);

        risultato.put("success", true);
        risultato.put("message", "Congratulazioni! Quest completata con successo!");
        risultato.put("questId", questId);
        risultato.put("userId", userId);
        risultato.put("museoId", museoId);
        risultato.put("punteggioOttenuto", punteggioTotale);
        risultato.put("tempoCompletamento", tempoCompletamento);
        risultato.put("bonus", bonus);
        risultato.put("dataCompletamento", System.currentTimeMillis());
        risultato.put("ricompense", generaRicompense(punteggioTotale));
        risultato.put("timestamp", System.currentTimeMillis());

        return risultato;
    }

    /**
     * Ottiene lo storico delle quest dell'utente
     */
    public static JSONObject getStoricoQuest(String userId) {
        System.out.println("📊 Recupero storico quest per utente: " + userId);

        JSONObject storico = storicoUtenti.getOrDefault(userId, new JSONObject());

        if (!storico.has("questCompletate")) {
            storico.put("questCompletate", new JSONArray());
            storico.put("statistiche", new JSONObject()
                    .put("questTotali", 0)
                    .put("punteggioTotale", 0)
                    .put("tempoMedioCompletamento", 0)
                    .put("museiVisitati", 0));
        }

        try {
            // Arricchisci ogni quest con informazioni dettagliate
            JSONArray questCompletate = storico.getJSONArray("questCompletate");
            JSONArray questArricchite = new JSONArray();

            for (int i = 0; i < questCompletate.length(); i++) {
                JSONObject quest = questCompletate.getJSONObject(i);
                JSONObject questArricchita = new JSONObject(quest.toString());

                // Aggiungi informazioni del museo
                String museoId = quest.optString("museoId", "");
                questArricchita.put("nomeMuseo", getNomeMuseo(museoId));
                questArricchita.put("cittaMuseo", getCittaMuseo(museoId));

                // Aggiungi informazioni della quest
                String questId = quest.optString("questId", "");
                questArricchita.put("titoloQuest", getTitoloQuest(questId));
                questArricchita.put("difficolta", getDifficoltaQuest(questId));
                questArricchita.put("categoriaQuest", getCategoriaQuest(questId));

                questArricchite.put(questArricchita);
            }

            storico.put("questCompletate", questArricchite);

            // Calcola statistiche aggiornate
            JSONObject stats = storico.getJSONObject("statistiche");
            stats.put("museiVisitati", calcolaMuseiUnici(questArricchite));

        } catch (Exception e) {
            System.out.println("❌ Errore nell'arricchimento dati: " + e.getMessage());
        }

        storico.put("userId", userId);
        storico.put("timestamp", System.currentTimeMillis());

        return storico;
    }

    // Metodi di supporto aggiuntivi:
    private static String getTitoloQuest(String questId) {
        if (questId.contains("GIOCONDA")) return "Trova la Gioconda";
        if (questId.contains("ULTIMA_CENA")) return "L'Ultima Cena";
        if (questId.contains("VENERE")) return "La Nascita di Venere";
        if (questId.contains("NOTTE_STELLATA")) return "La Notte Stellata";
        if (questId.contains("T_REX")) return "Il Fossile del T-Rex";
        if (questId.contains("TAVOLA_PERIODICA")) return "La Tavola Periodica Originale";
        if (questId.contains("TELESCOPIO")) return "Il Telescopio di Galileo";
        if (questId.contains("ROSETTA")) return "La Stele di Rosetta";
        if (questId.contains("ARMATURA")) return "L'Armatura del Cavaliere";
        if (questId.contains("MANOSCRITTO")) return "Il Manoscritto Illuminato";
        if (questId.contains("COMPUTER")) return "Il Primo Computer";
        if (questId.contains("FORD")) return "L'Automobile di Ford";
        if (questId.contains("MONARCA")) return "La Farfalla Monarca";
        if (questId.contains("QUARZO")) return "Il Cristallo di Quarzo Gigante";
        return "Quest Misteriosa";
    }

    private static String getDifficoltaQuest(String questId) {
        if (questId.contains("001")) return "facile";
        if (questId.contains("002")) return "media";
        if (questId.contains("003")) return "difficile";
        return "media";
    }

    private static String getCategoriaQuest(String questId) {
        if (questId.contains("ARTE")) return "arte";
        if (questId.contains("SCIENZA")) return "scienza";
        if (questId.contains("STORIA")) return "storia";
        if (questId.contains("TECNOLOGIA")) return "tecnologia";
        if (questId.contains("NATURA")) return "natura";
        return "generale";
    }

    private static int calcolaMuseiUnici(JSONArray questCompletate) {
        try {
            Set<String> museiUnici = new HashSet<>();
            for (int i = 0; i < questCompletate.length(); i++) {
                JSONObject quest = questCompletate.getJSONObject(i);
                String museoId = quest.optString("museoId", "");
                if (!museoId.isEmpty()) {
                    museiUnici.add(museoId);
                }
            }
            return museiUnici.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Restituisce il numero totale di quest disponibili
     */
    public static int getTotaleQuestDisponibili() {
        int totale = 0;
        for (List<JSONObject> questMuseo : DATABASE_QUEST.values()) {
            totale += questMuseo.size();
        }
        return totale;
    }

    // === METODI AUSILIARI ===

    /**
     * Inizializza il database delle quest per diversi musei
     */
    private static Map<String, List<JSONObject>> initializeDatabaseQuest() {
        Map<String, List<JSONObject>> database = new HashMap<>();

        // Quest per il Museo d'Arte (MUS_ARTE)
        List<JSONObject> questArte = new ArrayList<>();

        questArte.add(creaQuest("MUS_ARTE_QUEST_001", "Trova la Gioconda",
                "Cerca il ritratto più famoso al mondo di Leonardo da Vinci",
                "Gioconda", "Leonardo da Vinci", "rinascimento", "facile", "MUS_ARTE"));

        questArte.add(creaQuest("MUS_ARTE_QUEST_002", "L'Ultima Cena",
                "Individua il celebre dipinto dell'ultima cena di Cristo",
                "L'Ultima Cena", "Leonardo da Vinci", "rinascimento", "media", "MUS_ARTE"));

        questArte.add(creaQuest("MUS_ARTE_QUEST_003", "La Nascita di Venere",
                "Trova la dea dell'amore che emerge dalle acque marine",
                "La Nascita di Venere", "Sandro Botticelli", "rinascimento", "media", "MUS_ARTE"));

        questArte.add(creaQuest("MUS_ARTE_QUEST_004", "La Notte Stellata",
                "Cerca il cielo vorticoso dipinto dal maestro olandese",
                "La Notte Stellata", "Vincent van Gogh", "impressionismo", "difficile", "MUS_ARTE"));

        database.put("MUS_ARTE", questArte);

        // Quest per il Museo delle Scienze (MUS_SCIENZA)
        List<JSONObject> questScienza = new ArrayList<>();

        questScienza.add(creaQuest("MUS_SCIENZA_QUEST_001", "Il Fossile del T-Rex",
                "Trova il più grande predatore preistorico mai esistito",
                "Scheletro di Tyrannosaurus Rex", "Paleontologia", "paleontologia", "facile", "MUS_SCIENZA"));

        questScienza.add(creaQuest("MUS_SCIENZA_QUEST_002", "La Tavola Periodica Originale",
                "Cerca la prima versione della classificazione degli elementi",
                "Tavola Periodica di Mendeleev", "Dmitri Mendeleev", "chimica", "media", "MUS_SCIENZA"));

        questScienza.add(creaQuest("MUS_SCIENZA_QUEST_003", "Il Telescopio di Galileo",
                "Individua lo strumento che rivoluzionò l'astronomia",
                "Telescopio Galileiano", "Galileo Galilei", "astronomia", "difficile", "MUS_SCIENZA"));

        database.put("MUS_SCIENZA", questScienza);

        // Quest per il Museo di Storia (MUS_STORIA)
        List<JSONObject> questStoria = new ArrayList<>();

        questStoria.add(creaQuest("MUS_STORIA_QUEST_001", "La Stele di Rosetta",
                "Trova la chiave per decifrare i geroglifici egizi",
                "Stele di Rosetta", "Antico Egitto", "archeologia", "media", "MUS_STORIA"));

        questStoria.add(creaQuest("MUS_STORIA_QUEST_002", "L'Armatura del Cavaliere",
                "Cerca l'armatura completa di un cavaliere medievale",
                "Armatura Medievale", "Periodo Medievale", "storia medievale", "facile", "MUS_STORIA"));

        questStoria.add(creaQuest("MUS_STORIA_QUEST_003", "Il Manoscritto Illuminato",
                "Individua il prezioso libro decorato a mano dai monaci",
                "Libro delle Ore", "Monasteri Medievali", "arte medievale", "difficile", "MUS_STORIA"));

        database.put("MUS_STORIA", questStoria);

        // Quest per il Museo della Tecnologia (MUS_TECNOLOGIA)
        List<JSONObject> questTecnologia = new ArrayList<>();

        questTecnologia.add(creaQuest("MUS_TECNOLOGIA_QUEST_001", "Il Primo Computer",
                "Trova la macchina che diede inizio all'era digitale",
                "ENIAC", "Ingegneria Informatica", "informatica", "media", "MUS_TECNOLOGIA"));

        questTecnologia.add(creaQuest("MUS_TECNOLOGIA_QUEST_002", "L'Automobile di Ford",
                "Cerca l'auto che rivoluzionò la produzione industriale",
                "Ford Modello T", "Henry Ford", "industria", "facile", "MUS_TECNOLOGIA"));

        database.put("MUS_TECNOLOGIA", questTecnologia);

        // Quest per il Museo di Scienze Naturali (MUS_NATURA)
        List<JSONObject> questNatura = new ArrayList<>();

        questNatura.add(creaQuest("MUS_NATURA_QUEST_001", "La Farfalla Monarca",
                "Trova l'esemplare del lepidottero migratore più famoso",
                "Farfalla Monarca", "Natura", "entomologia", "facile", "MUS_NATURA"));

        questNatura.add(creaQuest("MUS_NATURA_QUEST_002", "Il Cristallo di Quarzo Gigante",
                "Cerca il più grande cristallo della collezione mineralogica",
                "Quarzo Rosa Gigante", "Geologia", "mineralogia", "media", "MUS_NATURA"));

        database.put("MUS_NATURA", questNatura);

        return database;
    }

    /**
     * Crea un oggetto quest con tutti i parametri necessari
     */
    private static JSONObject creaQuest(String id, String titoloQuest, String descrizioneQuest,
                                        String nomeOpera, String autore, String categoria, String difficolta, String museoId) {
        JSONObject quest = new JSONObject();
        quest.put("idQuest", id);
        quest.put("titoloQuest", titoloQuest);
        quest.put("descrizioneQuest", descrizioneQuest);
        quest.put("nomeOpera", nomeOpera);
        quest.put("autoreOpera", autore);
        quest.put("categoria", categoria);
        quest.put("difficolta", difficolta);
        quest.put("museoId", museoId); // Aggiungi il museo ID

        // Aggiungi informazioni extra
        quest.put("puntiRicompensa", calcolaPuntiRicompensa(difficolta));
        quest.put("tempoStimato", calcolaTempoStimato(difficolta));
        quest.put("indizi", generaIndizi(nomeOpera, categoria));

        return quest;
    }

    private static List<JSONObject> generaQuestGeneriche(String museoId) {
        System.out.println("🎲 Generazione quest generiche per museo: " + museoId);

        List<JSONObject> questGeneriche = new ArrayList<>();

        String[] titoli = {
                "Il Mistero dell'Opera Nascosta",
                "Caccia al Tesoro Artistico",
                "Trova l'Opera Perduta",
                "L'Enigma dell'Artista",
                "Scopri il Capolavoro"
        };

        String[] opere = {
                "Opera Misteriosa", "Capolavoro Nascosto", "Tesoro del Museo",
                "Pezzo da Collezione", "Opera Rara"
        };

        for (int i = 0; i < 3; i++) {
            String questId = museoId + "_QUEST_GEN_" + String.format("%03d", i + 1);
            System.out.println("🔍 Creazione quest generica: " + questId);
            String titolo = titoli[i % titoli.length];
            String opera = opere[i % opere.length];
            String difficolta = (i == 0) ? "facile" : (i == 1) ? "media" : "difficile";

            questGeneriche.add(creaQuest(
                    questId,
                    titolo,
                    "Una quest generica per esplorare il museo e scoprire opere interessanti",
                    opera,
                    "Artista Sconosciuto",
                    "generale",
                    difficolta,
                    museoId // Passa il museo ID
            ));
        }

        return questGeneriche;
    }

    private static List<JSONObject> filtraQuestPerPreferenze(List<JSONObject> quest, String preferenze) {
        if (preferenze == null || preferenze.trim().isEmpty()) {
            return quest;
        }

        String prefLower = preferenze.toLowerCase();
        List<JSONObject> filtrate = new ArrayList<>();

        for (JSONObject q : quest) {
            String categoria = q.optString("categoria", "").toLowerCase();
            if (prefLower.contains(categoria) || categoria.contains("generale")) {
                filtrate.add(q);
            }
        }

        return filtrate.isEmpty() ? quest : filtrate;
    }

    private static List<JSONObject> filtraQuestPerDifficolta(List<JSONObject> quest, String difficolta) {
        if (difficolta == null || "tutte".equalsIgnoreCase(difficolta)) {
            return quest;
        }

        List<JSONObject> filtrate = new ArrayList<>();
        for (JSONObject q : quest) {
            String questDifficolta = q.optString("difficolta", "media");
            if (questDifficolta.equalsIgnoreCase(difficolta)) {
                filtrate.add(q);
            }
        }

        return filtrate.isEmpty() ? quest : filtrate;
    }

    private static void aggiungiStatoQuest(JSONObject quest, String userId, String museoId) {
        // Verifica se l'utente ha già completato questa quest
        // Ora gli ID sono univoci per museo, quindi basta controllare questId
        JSONObject storico = storicoUtenti.get(userId);
        boolean completata = false;

        if (storico != null && storico.has("questCompletate")) {
            JSONArray questCompletate = storico.getJSONArray("questCompletate");
            for (int i = 0; i < questCompletate.length(); i++) {
                JSONObject questCompleta = questCompletate.getJSONObject(i);
                // Con ID univoci, basta controllare solo questId
                if (quest.getString("idQuest").equals(questCompleta.getString("questId"))) {
                    completata = true;
                    break;
                }
            }
        }

        // Verifica se è la quest attualmente attiva
        JSONObject questAttive = questUtenti.get(userId);
        boolean attiva = questAttive != null &&
                quest.getString("idQuest").equals(questAttive.optString("questAttiva"));

        quest.put("completata", completata);
        quest.put("attiva", attiva);
        quest.put("disponibile", !completata && !attiva);
    }

    private static void aggiungiDettagliCompleti(JSONObject quest, String userId) {
        String questId = quest.getString("idQuest");

        // Informazioni dettagliate per la quest
        JSONObject dettagliOpera = new JSONObject();
        dettagliOpera.put("dimensioni", generaDimensioniOpera());
        dettagliOpera.put("anno", generaAnnoOpera(quest.optString("categoria")));
        dettagliOpera.put("tecnica", generaTecnicaOpera(quest.optString("categoria")));
        dettagliOpera.put("provenienza", generaProvenienzaOpera());
        quest.put("dettagliOpera", dettagliOpera);

        // Obiettivi della quest
        JSONArray obiettivi = new JSONArray();
        obiettivi.put("Individua l'opera d'arte specificata");
        obiettivi.put("Leggi le informazioni sulla targa descrittiva");
        obiettivi.put("Scatta una foto dell'opera (opzionale)");
        if ("difficile".equals(quest.optString("difficolta"))) {
            obiettivi.put("Rispondi a una domanda sull'opera");
        }
        quest.put("obiettivi", obiettivi);

        // Suggerimenti per trovare l'opera
        quest.put("suggerimenti", generaSuggerimenti(quest.optString("categoria")));

        // Informazioni di completamento
        JSONObject infoCompletamento = new JSONObject();
        infoCompletamento.put("verificaRichiesta", true);
        infoCompletamento.put("metodiVerifica", new JSONArray()
                .put("Scansione QR code vicino all'opera")
                .put("Conferma posizione GPS")
                .put("Riconoscimento fotografico"));
        quest.put("completamento", infoCompletamento);
    }

    private static boolean verificaEsistenzaQuest(String questId, String museoId) {
        List<JSONObject> questMuseo = DATABASE_QUEST.get(museoId);
        if (questMuseo != null) {
            for (JSONObject quest : questMuseo) {
                if (questId.equals(quest.getString("idQuest"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int calcolaPunteggio(String questId, int tempoCompletamento) {
        // Ottieni difficoltà della quest
        String difficolta = "media"; // default

        for (List<JSONObject> questMuseo : DATABASE_QUEST.values()) {
            for (JSONObject quest : questMuseo) {
                if (questId.equals(quest.getString("idQuest"))) {
                    difficolta = quest.optString("difficolta", "media");
                    break;
                }
            }
        }

        // Punteggio base per difficoltà
        int punteggioBase = switch (difficolta) {
            case "facile" -> 100;
            case "media" -> 200;
            case "difficile" -> 300;
            default -> 150;
        };

        // Bonus tempo: più veloce = più punti
        int bonusTempo = Math.max(0, 50 - tempoCompletamento);

        return punteggioBase + bonusTempo;
    }

    private static void salvaQuestNelloStorico(String userId, String questId, String museoId, int punteggio, int tempo) {
        JSONObject storico = storicoUtenti.getOrDefault(userId, new JSONObject());

        if (!storico.has("questCompletate")) {
            storico.put("questCompletate", new JSONArray());
            storico.put("statistiche", new JSONObject()
                    .put("questTotali", 0)
                    .put("punteggioTotale", 0)
                    .put("tempoTotaleMinuti", 0));
        }

        // Aggiungi quest completata con informazione del museo
        JSONObject questCompletata = new JSONObject();
        questCompletata.put("questId", questId);
        questCompletata.put("museoId", museoId); // Importante: salva anche il museo
        questCompletata.put("dataCompletamento", System.currentTimeMillis());
        questCompletata.put("punteggio", punteggio);
        questCompletata.put("tempoCompletamento", tempo);

        storico.getJSONArray("questCompletate").put(questCompletata);

        // Aggiorna statistiche
        JSONObject stats = storico.getJSONObject("statistiche");
        stats.put("questTotali", stats.getInt("questTotali") + 1);
        stats.put("punteggioTotale", stats.getInt("punteggioTotale") + punteggio);
        stats.put("tempoTotaleMinuti", stats.getInt("tempoTotaleMinuti") + tempo);

        storicoUtenti.put(userId, storico);
    }

    private static JSONObject generaRicompense(int punteggio) {
        JSONObject ricompense = new JSONObject();

        // Badge basati sul punteggio
        JSONArray badge = new JSONArray();
        if (punteggio >= 250) {
            badge.put("Esploratore Esperto");
        }
        if (punteggio >= 300) {
            badge.put("Cacciatore di Tesori");
        }

        ricompense.put("badge", badge);
        ricompense.put("esperienza", punteggio / 10);

        // Ricompense speciali casuali
        if (random.nextInt(100) < 20) { // 20% possibilità
            ricompense.put("ricompensaSpeciale", "Accesso VIP alla prossima mostra temporanea");
        }

        return ricompense;
    }

    private static JSONObject generaQuestDettagliata(String questId) {
        JSONObject quest = new JSONObject();
        quest.put("idQuest", questId);
        quest.put("titoloQuest", "Quest Generata: " + questId);
        quest.put("descrizioneQuest", "Una quest generata automaticamente per l'esplorazione del museo");
        quest.put("nomeOpera", "Opera Misteriosa");
        quest.put("autoreOpera", "Artista da Scoprire");
        quest.put("categoria", "generale");
        quest.put("difficolta", "media");
        quest.put("puntiRicompensa", 80);
        quest.put("tempoStimato", 20);
        quest.put("indizi", new JSONArray().put("Cerca nell'ala principale del museo"));

        return quest;
    }

    // === METODI DI SUPPORTO PER LA GENERAZIONE DATI ===

    private static int calcolaPuntiRicompensa(String difficolta) {
        return switch (difficolta) {
            case "facile" -> 20;
            case "media" -> 40;
            case "difficile" -> 80;
            default -> 30;
        };
    }

    private static int calcolaTempoStimato(String difficolta) {
        return switch (difficolta) {
            case "facile" -> 10; // 10 minuti
            case "media" -> 20;  // 20 minuti
            case "difficile" -> 35; // 35 minuti
            default -> 20;
        };
    }

    private static JSONArray generaIndizi(String nomeOpera, String categoria) {
        JSONArray indizi = new JSONArray();

        // Indizi generici
        indizi.put("Cerca nell'area dedicata a: " + categoria);
        indizi.put("L'opera è esposta in una cornice/teca ben visibile");

        // Indizi specifici per nome opera
        if (nomeOpera.toLowerCase().contains("gioconda")) {
            indizi.put("Cerca il sorriso più enigmatico della storia dell'arte");
            indizi.put("Si trova nella sezione Rinascimento Italiano");
        } else if (nomeOpera.toLowerCase().contains("venere")) {
            indizi.put("Cerca una figura femminile che emerge dal mare");
        } else if (nomeOpera.toLowerCase().contains("ultima cena")) {
            indizi.put("Una tavola con 13 persone sedute");
        }

        return indizi;
    }

    private static String generaDimensioniOpera() {
        String[] dimensioni = {
                "77 x 53 cm", "120 x 80 cm", "200 x 150 cm",
                "50 x 40 cm", "180 x 120 cm", "90 x 70 cm"
        };
        return dimensioni[random.nextInt(dimensioni.length)];
    }

    private static String generaAnnoOpera(String categoria) {
        return switch (categoria) {
            case "rinascimento" -> String.valueOf(1450 + random.nextInt(100));
            case "impressionismo" -> String.valueOf(1860 + random.nextInt(40));
            case "paleontologia" -> "65 milioni di anni fa";
            case "archeologia" -> String.valueOf(500 + random.nextInt(1500)) + " a.C.";
            default -> String.valueOf(1800 + random.nextInt(200));
        };
    }

    private static String generaTecnicaOpera(String categoria) {
        Map<String, String[]> tecniche = new HashMap<>();
        tecniche.put("arte", new String[]{"Olio su tela", "Tempera su tavola", "Affresco", "Acquerello"});
        tecniche.put("scienza", new String[]{"Fossile", "Modello in scala", "Reperto originale", "Ricostruzione"});
        tecniche.put("storia", new String[]{"Manufatto originale", "Replica fedele", "Restauro moderno"});

        String[] opzioni = tecniche.getOrDefault(categoria, new String[]{"Tecnica mista", "Materiale originale"});
        return opzioni[random.nextInt(opzioni.length)];
    }

    private static String generaProvenienzaOpera() {
        String[] provenienze = {
                "Collezione privata donata al museo",
                "Acquisizione del museo nel 1985",
                "Prestito da museo internazionale",
                "Ritrovamento archeologico locale",
                "Donazione della famiglia dell'artista"
        };
        return provenienze[random.nextInt(provenienze.length)];
    }

    private static JSONArray generaSuggerimenti(String categoria) {
        JSONArray suggerimenti = new JSONArray();

        switch (categoria) {
            case "arte", "rinascimento", "impressionismo" -> {
                suggerimenti.put("Osserva attentamente i colori e le pennellate");
                suggerimenti.put("Leggi la targa informativa accanto all'opera");
                suggerimenti.put("Nota lo stile artistico caratteristico del periodo");
            }
            case "scienza", "paleontologia" -> {
                suggerimenti.put("Cerca nelle sale dedicate alle scienze naturali");
                suggerimenti.put("Osserva le dimensioni e la struttura del reperto");
                suggerimenti.put("Leggi le informazioni scientifiche fornite");
            }
            case "storia", "archeologia" -> {
                suggerimenti.put("Visita la sezione storica del museo");
                suggerimenti.put("Nota il contesto storico dell'oggetto");
                suggerimenti.put("Osserva i dettagli decorativi e simbolici");
            }
            default -> {
                suggerimenti.put("Esplora le diverse sezioni del museo");
                suggerimenti.put("Chiedi informazioni al personale se necessario");
                suggerimenti.put("Usa la mappa del museo per orientarti");
            }
        }

        return suggerimenti;
    }
    public static JSONObject getMuseiVisitati(String userId) {
        System.out.println("📊 Recupero musei visitati per utente: " + userId);

        JSONObject storico = storicoUtenti.getOrDefault(userId, new JSONObject());
        JSONObject risultato = new JSONObject();

        try {
            JSONArray questCompletate = storico.optJSONArray("questCompletate");
            if (questCompletate == null) {
                questCompletate = new JSONArray();
            }

            // Mappa per raccogliere musei unici
            Map<String, JSONObject> museiVisitatiMap = new HashMap<>();

            for (int i = 0; i < questCompletate.length(); i++) {
                JSONObject quest = questCompletate.getJSONObject(i);
                String museoId = quest.optString("museoId", "");

                if (!museoId.isEmpty()) {
                    if (museiVisitatiMap.containsKey(museoId)) {
                        // Incrementa il contatore delle quest per questo museo
                        JSONObject museoInfo = museiVisitatiMap.get(museoId);
                        int questCount = museoInfo.optInt("questCompletate", 0);
                        museoInfo.put("questCompletate", questCount + 1);

                        // Aggiorna la data di ultima visita se più recente
                        long dataEsistente = museoInfo.optLong("ultimaVisita", 0);
                        long nuovaData = quest.optLong("dataCompletamento", 0);
                        if (nuovaData > dataEsistente) {
                            museoInfo.put("ultimaVisita", nuovaData);
                        }
                    } else {
                        // Primo incontro con questo museo
                        JSONObject museoInfo = new JSONObject();
                        museoInfo.put("museoId", museoId);
                        museoInfo.put("nomeMuseo", getNomeMuseo(museoId));
                        museoInfo.put("citta", getCittaMuseo(museoId));
                        museoInfo.put("questCompletate", 1);
                        museoInfo.put("ultimaVisita", quest.optLong("dataCompletamento", 0));
                        museoInfo.put("primaVisita", quest.optLong("dataCompletamento", 0));

                        museiVisitatiMap.put(museoId, museoInfo);
                    }
                }
            }

            // Converti la mappa in JSONArray
            JSONArray museiArray = new JSONArray();
            for (JSONObject museoInfo : museiVisitatiMap.values()) {
                museiArray.put(museoInfo);
            }

            risultato.put("success", true);
            risultato.put("userId", userId);
            risultato.put("museiVisitati", museiArray);
            risultato.put("totaleMusei", museiArray.length());
            risultato.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            System.out.println("❌ Errore recupero musei visitati: " + e.getMessage());
            risultato.put("success", false);
            risultato.put("message", "Errore nel recupero dei musei visitati");
        }

        return risultato;
    }

    // Metodi di supporto da aggiungere:
    private static String getNomeMuseo(String museoId) {
        switch (museoId) {
            case "MUS_ARTE": return "Museo d'Arte";
            case "MUS_SCIENZA": return "Museo delle Scienze";
            case "MUS_STORIA": return "Museo di Storia";
            case "MUS_TECNOLOGIA": return "Museo della Tecnologia";
            case "MUS_NATURA": return "Museo di Scienze Naturali";
            default: return "Museo Sconosciuto";
        }
    }

    private static String getCittaMuseo(String museoId) {
        switch (museoId) {
            case "MUS_ARTE": return "Firenze";
            case "MUS_SCIENZA": return "Milano";
            case "MUS_STORIA": return "Roma";
            case "MUS_TECNOLOGIA": return "Torino";
            case "MUS_NATURA": return "Napoli";
            default: return "Città non specificata";
        }
    }


}