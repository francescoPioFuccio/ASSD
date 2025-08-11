package Util;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class MuseoSimulationService {

    private static final Random random = new Random();
    private static final Map<String, JSONObject> preferitiUtenti = new HashMap<>();

    // Database simulato di musei
    private static final Map<String, JSONObject> DATABASE_MUSEI = initializeDatabaseMusei();

    /**
     * Simula la chiamata al servizio di raccomandazione musei
     */
    public static JSONObject trovaMuseiRaccomandati(String userId, Double latitudine, Double longitudine,
                                                    String preferenze, Integer raggio) {
        System.out.println("🗺️ Simulazione ricerca musei per posizione: " + latitudine + ", " + longitudine);

        // Simulazione di delay della rete (200-800ms)
        try {
            Thread.sleep(random.nextInt(600) + 200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Normalizza preferenze verso categorie supportate
        List<String> preferenzeRichieste = normalizzaPreferenze(preferenze);

        // Seleziona dal database i musei che matchano le preferenze (oppure tutti se nessuna preferenza)
        List<JSONObject> museiSelezionati = new java.util.ArrayList<>();
        for (Map.Entry<String, JSONObject> entry : DATABASE_MUSEI.entrySet()) {
            JSONObject museoDb = entry.getValue();
            String categoria = museoDb.optString("categoria", "").toLowerCase();
            if (preferenzeRichieste.isEmpty() || preferenzeRichieste.contains(categoria)) {
                JSONObject museoClonato = new JSONObject(museoDb.toString());
                applicaPosizioneEDettagliDinamici(museoClonato, latitudine, longitudine, raggio);
                museiSelezionati.add(museoClonato);
            }
        }

        // Ordina per distanza crescente
        museiSelezionati.sort((a, b) -> Double.compare(a.optDouble("distanza", 0.0), b.optDouble("distanza", 0.0)));

        // Prepara risposta
        JSONObject risultato = new JSONObject();
        JSONArray musei = new JSONArray();
        for (JSONObject m : museiSelezionati) {
            musei.put(m);
        }

        risultato.put("success", true);
        risultato.put("userId", userId);
        risultato.put("musei", musei);
        risultato.put("totalFound", musei.length());
        risultato.put("searchRadius", raggio);
        risultato.put("searchLocation", new JSONObject()
                .put("latitudine", latitudine)
                .put("longitudine", longitudine));
        risultato.put("timestamp", System.currentTimeMillis());

        System.out.println("🏛️ Musei raccomandati generati con successo (" + musei.length() + ")");
        return risultato;
    }

    /**
     * Simula il recupero dei dettagli di un museo specifico
     */
    public static JSONObject getDettaglioMuseo(String museoId, String userId) {
        System.out.println("🏛️ Simulazione recupero dettagli museo ID: " + museoId);

        JSONObject dettaglio = new JSONObject();

        // Controlla se il museo esiste nel database simulato
        if (DATABASE_MUSEI.containsKey(museoId)) {
            JSONObject museoBase = DATABASE_MUSEI.get(museoId);

            // Copia i dati base
            dettaglio.put("found", true);
            dettaglio.put("id", museoId);
            dettaglio.put("nome", museoBase.getString("nome"));
            dettaglio.put("tipologia", museoBase.getString("tipologia"));
            dettaglio.put("descrizione", museoBase.getString("descrizione"));
            dettaglio.put("coordinate", museoBase.getJSONObject("coordinate"));
            // Compatibilità: latitudine/longitudine anche a livello top
            dettaglio.put("latitudine", museoBase.getJSONObject("coordinate").optDouble("latitudine", 0.0));
            dettaglio.put("longitudine", museoBase.getJSONObject("coordinate").optDouble("longitudine", 0.0));

            // Aggiungi dettagli extra
            aggiungiDettagliCompleti(dettaglio, museoId);

        } else {
            // Genera dati per museo non in database (simulazione)
            dettaglio.put("found", true);
            dettaglio.put("id", museoId);
            dettaglio = generaDettaglioMuseoCompleto(museoId);
            if (dettaglio.has("coordinate")) {
                dettaglio.put("latitudine", dettaglio.getJSONObject("coordinate").optDouble("latitudine", 0.0));
                dettaglio.put("longitudine", dettaglio.getJSONObject("coordinate").optDouble("longitudine", 0.0));
            }
        }

        dettaglio.put("timestamp", System.currentTimeMillis());
        return dettaglio;
    }

    /**
     * Simula l'aggiunta di un museo ai preferiti
     */
    public static JSONObject aggiungiAiPreferiti(String userId, String museoId) {
        System.out.println("❤️ Simulazione aggiunta museo ai preferiti - User: " + userId + ", Museo: " + museoId);

        JSONObject risultato = new JSONObject();

        // Simula il salvataggio
        JSONObject preferiti = preferitiUtenti.getOrDefault(userId, new JSONObject());
        JSONArray listaPreferiti = preferiti.optJSONArray("musei");
        if (listaPreferiti == null) {
            listaPreferiti = new JSONArray();
        }

        // Controlla se già presente
        boolean giaPresente = false;
        for (int i = 0; i < listaPreferiti.length(); i++) {
            if (listaPreferiti.getString(i).equals(museoId)) {
                giaPresente = true;
                break;
            }
        }

        if (!giaPresente) {
            listaPreferiti.put(museoId);
            preferiti.put("musei", listaPreferiti);
            preferitiUtenti.put(userId, preferiti);

            risultato.put("success", true);
            risultato.put("message", "Museo aggiunto ai preferiti con successo");
            risultato.put("action", "added");
        } else {
            risultato.put("success", true);
            risultato.put("message", "Museo già presente nei preferiti");
            risultato.put("action", "already_exists");
        }

        risultato.put("userId", userId);
        risultato.put("museoId", museoId);
        risultato.put("totalPreferiti", listaPreferiti.length());
        risultato.put("timestamp", System.currentTimeMillis());

        return risultato;
    }

    // === METODI AUSILIARI ===

    // Sposta il museo vicino all'utente e aggiunge campi dinamici utili alla lista
    private static void applicaPosizioneEDettagliDinamici(JSONObject museo,
                                                          Double latBase,
                                                          Double lonBase,
                                                          Integer raggio) {
        // Per 5 minuti in macchina (circa 3-5 km a velocità urbana)
        double raggioKm = 3.0 + random.nextDouble() * 2.0; // 3-5 km
        // Conversione approssimativa: 1° ≈ 111km, quindi per 3-5km usiamo 0.003-0.005°
        double deltaMax = raggioKm / 111000.0; // Circa 0.003-0.005 gradi
        
        JSONObject coordinate = new JSONObject();
        double deltaLat = (random.nextDouble() - 0.5) * deltaMax;
        double deltaLon = (random.nextDouble() - 0.5) * deltaMax;

        double lat = Math.round((latBase + deltaLat) * 1000000.0) / 1000000.0;
        double lon = Math.round((lonBase + deltaLon) * 1000000.0) / 1000000.0;

        coordinate.put("latitudine", lat);
        coordinate.put("longitudine", lon);
        museo.put("coordinate", coordinate);
        // Campi top-level per compatibilità con l'app Android
        museo.put("latitudine", lat);
        museo.put("longitudine", lon);

        // Informazioni dinamiche per la lista - distanza reale calcolata
        museo.put("distanza", Math.round(raggioKm * 10.0) / 10.0);
        museo.put("rating", Math.round((3.5 + Math.random() * 1.5) * 10.0) / 10.0);
        museo.put("aperto", random.nextBoolean());
        museo.put("ingressoGratuito", random.nextBoolean());
        if (!museo.getBoolean("ingressoGratuito")) {
            museo.put("prezzoIngresso", 5 + random.nextInt(20));
        }
    }

    // Normalizza stringa preferenze in un insieme di categorie supportate
    private static List<String> normalizzaPreferenze(String preferenze) {
        List<String> risultato = new java.util.ArrayList<>();
        if (preferenze == null || preferenze.trim().isEmpty()) {
            return risultato; // Vuoto => tutte le categorie
        }
        String prefLower = preferenze.toLowerCase();
        if (prefLower.contains("arte")) risultato.add("arte");
        if (prefLower.contains("scienza")) risultato.add("scienza");
        if (prefLower.contains("storia")) risultato.add("storia");
        if (prefLower.contains("tecnologia") || prefLower.contains("teconologia")) risultato.add("tecnologia");
        if (prefLower.contains("natura")) risultato.add("natura");
        return risultato;
    }

    private static JSONObject generaMuseoVicino(Double latBase, Double lonBase, String preferenze,
                                                Integer raggio, int index) {
        JSONObject museo = new JSONObject();

        // Genera ID univoco
        String museoId = "MUS_" + String.format("%03d", Math.abs(Double.valueOf(latBase + lonBase + index).hashCode() % 1000));

        // Lista di musei simulati
        String[] nomiMusei = {
                "Museo Nazionale di Arte Contemporanea",
                "Pinacoteca Civica",
                "Museo Archeologico Regionale",
                "Galleria d'Arte Moderna",
                "Museo di Storia Naturale",
                "Palazzo delle Esposizioni"
        };

        String[] tipologie = {
                "Arte Contemporanea",
                "Arte Classica",
                "Archeologia",
                "Arte Moderna",
                "Scienze Naturali",
                "Mostre Temporanee"
        };

        // Seleziona museo basandosi su preferenze se fornite
        int museoIndex = selezionaMuseoPerPreferenze(preferenze, index);

        museo.put("id", museoId);
        museo.put("nome", nomiMusei[museoIndex % nomiMusei.length]);
        museo.put("tipologia", tipologie[museoIndex % tipologie.length]);
        museo.put("descrizione", generaDescrizioneMuseo(tipologie[museoIndex % tipologie.length]));

        // Genera coordinate vicine alla posizione dell'utente
        JSONObject coordinate = new JSONObject();
        double deltaLat = (random.nextDouble() - 0.5) * (raggio * 0.01); // Approssimativo: 1° ≈ 111km
        double deltaLon = (random.nextDouble() - 0.5) * (raggio * 0.01);

        coordinate.put("latitudine", Math.round((latBase + deltaLat) * 1000000.0) / 1000000.0);
        coordinate.put("longitudine", Math.round((lonBase + deltaLon) * 1000000.0) / 1000000.0);
        museo.put("coordinate", coordinate);

        // Informazioni aggiuntive
        museo.put("distanza", Math.round((Math.random() * raggio * 0.8 + 0.5) * 10.0) / 10.0); // km
        museo.put("rating", Math.round((3.5 + Math.random() * 1.5) * 10.0) / 10.0); // 3.5-5.0
        museo.put("aperto", random.nextBoolean());
        museo.put("ingressoGratuito", random.nextBoolean());

        if (!museo.getBoolean("ingressoGratuito")) {
            museo.put("prezzoIngresso", 5 + random.nextInt(20)); // 5-25 euro
        }

        return museo;
    }

    private static int selezionaMuseoPerPreferenze(String preferenze, int defaultIndex) {
        if (preferenze == null || preferenze.isEmpty()) {
            return defaultIndex;
        }

        String prefLower = preferenze.toLowerCase();

        if (prefLower.contains("contemporanea") || prefLower.contains("moderna")) {
            return 0; // Arte Contemporanea
        } else if (prefLower.contains("classica") || prefLower.contains("rinascimento")) {
            return 1; // Arte Classica
        } else if (prefLower.contains("archeologia") || prefLower.contains("storia")) {
            return 2; // Archeologia
        } else if (prefLower.contains("natura") || prefLower.contains("scienza")) {
            return 4; // Scienze Naturali
        }

        return defaultIndex;
    }

    private static String generaDescrizioneMuseo(String tipologia) {
        Map<String, String[]> descrizioni = new HashMap<>();

        descrizioni.put("Arte Contemporanea", new String[]{
                "Una collezione straordinaria di opere d'arte contemporanea con artisti internazionali.",
                "Spazio espositivo dedicato all'arte del XXI secolo con installazioni innovative.",
                "Museo all'avanguardia che presenta le tendenze artistiche più attuali."
        });

        descrizioni.put("Arte Classica", new String[]{
                "Prestigiosa collezione di dipinti e sculture dal Rinascimento al XIX secolo.",
                "Tesori artistici che raccontano la storia dell'arte europea attraversi secoli.",
                "Capolavori di maestri antichi in un palazzo storico restaurato."
        });

        descrizioni.put("Archeologia", new String[]{
                "Reperti archeologici che testimoniano la ricca storia del territorio.",
                "Viaggio nel tempo attraverso manufatti e testimonianze delle civiltà passate.",
                "Importante collezione archeologica con pezzi unici e di grande valore storico."
        });

        descrizioni.put("Scienze Naturali", new String[]{
                "Esplorazione della biodiversità con collezioni naturalistiche straordinarie.",
                "Museo interattivo dedicato alla natura e alle scienze della Terra.",
                "Scopri i segreti del mondo naturale attraverso exhibit coinvolgenti."
        });

        String[] opzioni = descrizioni.get(tipologia);
        if (opzioni == null) {
            return "Museo di grande interesse culturale con collezioni di rilievo nazionale.";
        }

        return opzioni[random.nextInt(opzioni.length)];
    }

    private static JSONObject generaDettaglioMuseoCompleto(String museoId) {
        JSONObject dettaglio = new JSONObject();

        // Usa dati da DATABASE_MUSEI se disponibili, altrimenti genera
        if (DATABASE_MUSEI.containsKey(museoId)) {
            dettaglio = new JSONObject(DATABASE_MUSEI.get(museoId).toString());
        } else {
            // Genera dati base
            dettaglio.put("id", museoId);
            dettaglio.put("nome", "Museo Civico " + museoId);
            dettaglio.put("tipologia", "Arte Generale");
            dettaglio.put("descrizione", "Museo di interesse locale con collezioni variegate.");
        }

        // Aggiungi sempre dettagli completi
        aggiungiDettagliCompleti(dettaglio, museoId);

        return dettaglio;
    }

    private static void aggiungiDettagliCompleti(JSONObject museo, String museoId) {
        // Orari di apertura
        JSONObject orari = new JSONObject();
        orari.put("lunedi", "Chiuso");
        orari.put("martedi", "09:00-17:00");
        orari.put("mercoledi", "09:00-17:00");
        orari.put("giovedi", "09:00-20:00");
        orari.put("venerdi", "09:00-17:00");
        orari.put("sabato", "09:00-19:00");
        orari.put("domenica", "10:00-18:00");
        museo.put("orari", orari);

        // Contatti
        JSONObject contatti = new JSONObject();
        contatti.put("telefono", "+39 " + (random.nextInt(900000000) + 100000000));
        contatti.put("email", "info@museo" + museoId.toLowerCase() + ".it");
        contatti.put("sito", "www.museo" + museoId.toLowerCase() + ".it");
        museo.put("contatti", contatti);

        // Servizi
        JSONArray servizi = new JSONArray();
        String[] possibiliServizi = {
                "Visite guidate", "Audio guide", "Bookshop", "Caffetteria",
                "Accessibilità disabili", "Parcheggio", "WiFi gratuito"
        };

        for (int i = 0; i < 3 + random.nextInt(4); i++) {
            servizi.put(possibiliServizi[random.nextInt(possibiliServizi.length)]);
        }
        museo.put("servizi", servizi);

        // Mostre attuali
        JSONArray mostre = new JSONArray();
        if (random.nextBoolean()) {
            JSONObject mostra = new JSONObject();
            mostra.put("titolo", "Mostra Temporanea " + (2024 + random.nextInt(2)));
            mostra.put("dataInizio", "2024-" + String.format("%02d", 1 + random.nextInt(12)) + "-01");
            mostra.put("dataFine", "2024-" + String.format("%02d", 6 + random.nextInt(7)) + "-30");
            mostre.put(mostra);
        }
        museo.put("mostreAttuali", mostre);
    }

    private static Map<String, JSONObject> initializeDatabaseMusei() {
        Map<String, JSONObject> database = new HashMap<>();

        // 1) Arte
        JSONObject museoArte = new JSONObject();
        museoArte.put("id", "MUS_ARTE");
        museoArte.put("nome", "Galleria d'Arte Moderna");
        museoArte.put("tipologia", "Arte");
        museoArte.put("categoria", "arte");
        museoArte.put("descrizione", "Collezione di opere d'arte moderna e contemporanea.");
        museoArte.put("indirizzo", "Via delle Arti 1");
        museoArte.put("coordinate", new JSONObject().put("latitudine", 41.9123).put("longitudine", 12.4801));
        database.put("MUS_ARTE", museoArte);

        // 2) Scienza
        JSONObject museoScienza = new JSONObject();
        museoScienza.put("id", "MUS_SCIENZA");
        museoScienza.put("nome", "Museo delle Scienze");
        museoScienza.put("tipologia", "Scienza");
        museoScienza.put("categoria", "scienza");
        museoScienza.put("descrizione", "Esposizioni interattive su fisica, chimica e biologia.");
        museoScienza.put("indirizzo", "Piazza della Scienza 5");
        museoScienza.put("coordinate", new JSONObject().put("latitudine", 41.9055).put("longitudine", 12.4905));
        database.put("MUS_SCIENZA", museoScienza);

        // 3) Storia
        JSONObject museoStoria = new JSONObject();
        museoStoria.put("id", "MUS_STORIA");
        museoStoria.put("nome", "Museo di Storia e Archeologia");
        museoStoria.put("tipologia", "Storia");
        museoStoria.put("categoria", "storia");
        museoStoria.put("descrizione", "Percorso sulla storia locale con reperti archeologici.");
        museoStoria.put("indirizzo", "Corso Storico 10");
        museoStoria.put("coordinate", new JSONObject().put("latitudine", 41.8977).put("longitudine", 12.4755));
        database.put("MUS_STORIA", museoStoria);

        // 4) Tecnologia
        JSONObject museoTecnologia = new JSONObject();
        museoTecnologia.put("id", "MUS_TECNOLOGIA");
        museoTecnologia.put("nome", "Museo della Tecnologia");
        museoTecnologia.put("tipologia", "Tecnologia");
        museoTecnologia.put("categoria", "tecnologia");
        museoTecnologia.put("descrizione", "Mostre su innovazione, robotica e informatica.");
        museoTecnologia.put("indirizzo", "Viale Innovazione 3");
        museoTecnologia.put("coordinate", new JSONObject().put("latitudine", 41.9202).put("longitudine", 12.5002));
        database.put("MUS_TECNOLOGIA", museoTecnologia);

        // 5) Natura
        JSONObject museoNatura = new JSONObject();
        museoNatura.put("id", "MUS_NATURA");
        museoNatura.put("nome", "Museo di Scienze Naturali");
        museoNatura.put("tipologia", "Natura");
        museoNatura.put("categoria", "natura");
        museoNatura.put("descrizione", "Biodiversità, geologia e ambienti naturali.");
        museoNatura.put("indirizzo", "Largo Natura 7");
        museoNatura.put("coordinate", new JSONObject().put("latitudine", 41.9301).put("longitudine", 12.4707));
        database.put("MUS_NATURA", museoNatura);

        return database;
    }

}