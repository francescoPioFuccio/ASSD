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

        JSONObject risultato = new JSONObject();
        JSONArray musei = new JSONArray();

        // Genera 3 musei basandosi sui parametri
        for (int i = 0; i < 3; i++) {
            JSONObject museo = generaMuseoVicino(latitudine, longitudine, preferenze, raggio, i + 1);
            musei.put(museo);
        }

        risultato.put("success", true);
        risultato.put("userId", userId);
        risultato.put("musei", musei);
        risultato.put("totalFound", 3);
        risultato.put("searchRadius", raggio);
        risultato.put("searchLocation", new JSONObject()
                .put("latitudine", latitudine)
                .put("longitudine", longitudine));
        risultato.put("timestamp", System.currentTimeMillis());

        System.out.println("🏛️ Musei raccomandati generati con successo");
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

            // Aggiungi dettagli extra
            aggiungiDettagliCompleti(dettaglio, museoId);

        } else {
            // Genera dati per museo non in database (simulazione)
            dettaglio.put("found", true);
            dettaglio.put("id", museoId);
            dettaglio = generaDettaglioMuseoCompleto(museoId);
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

        // Museo 1
        JSONObject museo1 = new JSONObject();
        museo1.put("id", "MUS_001");
        museo1.put("nome", "Museo Nazionale Romano");
        museo1.put("tipologia", "Archeologia");
        museo1.put("descrizione", "La più importante collezione di arte antica romana al mondo.");
        museo1.put("coordinate", new JSONObject().put("latitudine", 41.9028).put("longitudine", 12.4964));
        database.put("MUS_001", museo1);

        // Museo 2
        JSONObject museo2 = new JSONObject();
        museo2.put("id", "MUS_002");
        museo2.put("nome", "Palazzo Altemps");
        museo2.put("tipologia", "Arte Classica");
        museo2.put("descrizione", "Splendida collezione di sculture antiche in palazzo rinascimentale.");
        museo2.put("coordinate", new JSONObject().put("latitudine", 41.9008).put("longitudine", 12.4734));
        database.put("MUS_002", museo2);

        // Museo 3
        JSONObject museo3 = new JSONObject();
        museo3.put("id", "MUS_003");
        museo3.put("nome", "MAXXI - Museo Nazionale");
        museo3.put("tipologia", "Arte Contemporanea");
        museo3.put("descrizione", "Primo museo nazionale dedicato alla creatività contemporanea.");
        museo3.put("coordinate", new JSONObject().put("latitudine", 41.9331).put("longitudine", 12.4728));
        database.put("MUS_003", museo3);

        return database;
    }
}