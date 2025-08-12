package Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class LLMSimulationService {

    private static final Random random = new Random();
    private static final Map<String, JSONArray> conversazioni = new HashMap<>();

    /**
     * Simula una chiamata al servizio LLM per processare una domanda
     */
    public static String processaDomanda(String domanda, String userId) {
        System.out.println("📡 Simulazione chiamata LLM per domanda: " + domanda);

        // Simulazione di delay della rete (100-500ms)
        try {
            Thread.sleep(random.nextInt(400) + 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Risposte simulate basate sul contenuto della domanda
        String risposta = generaRispostaContestuale(domanda);

        System.out.println("🤖 Risposta LLM generata: " + risposta);
        return risposta;
    }

    /**
     * Simula l'analisi di un'immagine da parte del servizio LLM
     */


    /**
     * Simula il recupero di informazioni su un'opera d'arte
     */
    public static JSONObject getInfoOpera(String operaId, String userId) {
        System.out.println("🎨 Simulazione recupero info opera ID: " + operaId);

        JSONObject info = new JSONObject();

        // Simulazione dati opera
        String[] titoli = {
                "La Gioconda", "La Notte Stellata", "Il Bacio", "La Nascita di Venere",
                "Guernica", "L'Urlo", "La Persistenza della Memoria", "Le Ninfee"
        };

        String[] artisti = {
                "Leonardo da Vinci", "Vincent van Gogh", "Gustav Klimt", "Sandro Botticelli",
                "Pablo Picasso", "Edvard Munch", "Salvador Dalí", "Claude Monet"
        };

        String[] periodi = {
                "Rinascimento", "Post-Impressionismo", "Art Nouveau", "Rinascimento",
                "Cubismo", "Espressionismo", "Surrealismo", "Impressionismo"
        };

        int index = Math.abs(operaId.hashCode()) % titoli.length;

        info.put("id", operaId);
        info.put("titolo", titoli[index]);
        info.put("artista", artisti[index]);
        info.put("periodo", periodi[index]);
        info.put("anno", 1400 + random.nextInt(600));
        info.put("museo", "Museo " + (random.nextBoolean() ? "Nazionale" : "Civico"));
        info.put("descrizione", generaDescrizioneOpera(titoli[index], artisti[index]));

        // Informazioni aggiuntive
        JSONArray tecniche = new JSONArray();
        tecniche.put(random.nextBoolean() ? "Olio su tela" : "Tempera su tavola");
        info.put("tecniche", tecniche);

        JSONObject dimensioni = new JSONObject();
        dimensioni.put("altezza", 50 + random.nextInt(200));
        dimensioni.put("larghezza", 40 + random.nextInt(150));
        dimensioni.put("unita", "cm");
        info.put("dimensioni", dimensioni);

        info.put("disponibile", random.nextBoolean());
        info.put("prezzo_biglietto", random.nextInt(20) + 5);

        return info;
    }

    /**
     * Simula una conversazione chat con il LLM
     */
    public static JSONObject processaChat(String messaggio, String userId, String conversationId) {
        System.out.println("💬 Simulazione chat - Messaggio: " + messaggio);

        // Se non esiste conversationId, creane uno nuovo
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        // Recupera o crea la cronologia della conversazione
        JSONArray cronologia = conversazioni.getOrDefault(conversationId, new JSONArray());

        // Aggiungi il messaggio dell'utente alla cronologia
        JSONObject messaggioUtente = new JSONObject();
        messaggioUtente.put("sender", "user");
        messaggioUtente.put("message", messaggio);
        messaggioUtente.put("timestamp", System.currentTimeMillis());
        cronologia.put(messaggioUtente);

        // Genera risposta contestuale
        String risposta = generaRispostaChatContestuale(messaggio, cronologia);

        // Aggiungi la risposta del bot alla cronologia
        JSONObject messaggioBot = new JSONObject();
        messaggioBot.put("sender", "assistant");
        messaggioBot.put("message", risposta);
        messaggioBot.put("timestamp", System.currentTimeMillis());
        cronologia.put(messaggioBot);

        // Salva la cronologia aggiornata
        conversazioni.put(conversationId, cronologia);

        // Crea la risposta
        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("conversationId", conversationId);
        response.put("message", risposta);
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    // === METODI AUSILIARI ===

    private static String generaRispostaContestuale(String domanda) {
        String domandaLower = domanda.toLowerCase();


        if (domandaLower.contains("gioconda")) {
            return "La Gioconda, anche conosciuta come Monna Lisa, è uno dei dipinti più famosi al mondo, realizzato da Leonardo da Vinci. " +
                    "È nota per il suo enigmatico sorriso e per la sua straordinaria tecnica pittorica. Il dipinto si trova attualmente al " +
                    "Louvre di Parigi, dove attira milioni di visitatori ogni anno.";
        }

        if (domandaLower.contains("chi") || domandaLower.contains("autore")) {
            String[] artisti = {"Leonardo da Vinci", "Michelangelo", "Van Gogh", "Picasso"};
            return "L'autore di quest'opera è " + artisti[random.nextInt(artisti.length)] +
                    ". È considerato uno dei maestri del suo tempo.";
        }

        if (domandaLower.contains("quando") || domandaLower.contains("anno")) {
            int anno = 1400 + random.nextInt(600);
            return "Quest'opera è stata realizzata intorno al " + anno +
                    ", durante il periodo " + (anno < 1600 ? "rinascimentale" : "moderno") + ".";
        }

        if (domandaLower.contains("dove") || domandaLower.contains("museo")) {
            String[] musei = {"Louvre", "Uffizi", "Metropolitan", "Prado"};
            return "Puoi ammirare quest'opera al " + musei[random.nextInt(musei.length)] +
                    ". Ti consiglio di prenotare in anticipo!";
        }

        if (domandaLower.contains("tecnica") || domandaLower.contains("come")) {
            return "Questa tecnica prevede l'uso di " +
                    (random.nextBoolean() ? "olio su tela" : "tempera su tavola") +
                    ". L'artista ha utilizzato sfumature molto particolari.";
        }

        // Risposta generica
        return "Interessante domanda! Basandomi sulla mia conoscenza artistica, " +
                "posso dirti che quest'opera rappresenta un importante esempio del suo periodo storico. " +
                "Hai altre curiosità specifiche?";
    }

    private static String generaMotivoApprovazione() {
        String[] motivi = {
                "L'immagine presenta una buona qualità e inquadratura",
                "La composizione è ben bilanciata e l'illuminazione appropriata",
                "L'opera è chiaramente visibile e riconoscibile",
                "I colori sono fedeli e la messa a fuoco è ottimale"
        };
        return motivi[random.nextInt(motivi.length)];
    }

    private static String generaMotivoRifiuto() {
        String[] motivi = {
                "L'immagine risulta sfocata o di bassa qualità",
                "L'inquadratura non cattura completamente l'opera",
                "L'illuminazione è insufficiente o crea riflessi",
                "L'opera non è chiaramente riconoscibile"
        };
        return motivi[random.nextInt(motivi.length)];
    }

    private static JSONArray generaSuggerimentiPositivi() {
        JSONArray suggerimenti = new JSONArray();
        suggerimenti.put("Ottima foto! Potresti condividerla nella galleria community");
        suggerimenti.put("Considera di aggiungere una descrizione personale");
        suggerimenti.put("Questa immagine potrebbe essere utilizzata per una guida virtuale");
        return suggerimenti;
    }

    private static JSONArray generaSuggerimentiMiglioramento() {
        JSONArray suggerimenti = new JSONArray();
        suggerimenti.put("Prova a scattare con una migliore illuminazione");
        suggerimenti.put("Assicurati che l'opera sia completamente inquadrata");
        suggerimenti.put("Evita riflessi e ombre che possono alterare i colori");
        suggerimenti.put("Mantieni la fotocamera ferma per evitare mosso");
        return suggerimenti;
    }

    private static String simulaRisoluzione() {
        String[] risoluzioni = {"1920x1080", "1280x720", "3840x2160", "2560x1440"};
        return risoluzioni[random.nextInt(risoluzioni.length)];
    }

    private static String estraiFormato(String nomeFile) {
        if (nomeFile.contains(".")) {
            return nomeFile.substring(nomeFile.lastIndexOf(".") + 1).toUpperCase();
        }
        return "JPG";
    }

    private static String simulaDimensioneFile() {
        double size = 0.5 + (random.nextDouble() * 4.5); // 0.5-5 MB
        return String.format("%.1f MB", size);
    }

    private static String generaDescrizioneOpera(String titolo, String artista) {
        return String.format("'%s' di %s è un capolavoro che rappresenta l'eccellenza artistica " +
                "del suo periodo. L'opera si distingue per la sua tecnica raffinata e " +
                "il significato simbolico profondo.", titolo, artista);
    }

    private static String generaRispostaChatContestuale(String messaggio, JSONArray cronologia) {
        String messaggioLower = messaggio.toLowerCase();

        // Saluti
        if (messaggioLower.contains("gioconda")) {
            return "La Gioconda, anche conosciuta come Monna Lisa, è uno dei dipinti più famosi al mondo, realizzato da Leonardo da Vinci. " +
                    "È nota per il suo enigmatico sorriso e per la sua straordinaria tecnica pittorica. Il dipinto si trova attualmente al " +
                    "Louvre di Parigi, dove attira milioni di visitatori ogni anno.";
        }

        if (messaggioLower.contains("ciao") || messaggioLower.contains("salve")) {
            return "Ciao! Sono il tuo assistente per l'arte e i musei. Come posso aiutarti oggi?";
        }

        // Ringraziamenti
        if (messaggioLower.contains("grazie")) {
            return "Prego! Sono qui per aiutarti. Hai altre domande sull'arte o sui musei?";
        }

        // Domande su opere specifiche
        if (messaggioLower.contains("opera") || messaggioLower.contains("quadro")) {
            return "Dimmi di più sull'opera che ti interessa! Puoi inviarmi una foto o descrivermela, " +
                    "così potrò fornirti informazioni dettagliate su autore, periodo e curiosità.";
        }

        // Informazioni sui musei
        if (messaggioLower.contains("museo")) {
            return "I musei sono luoghi fantastici per scoprire l'arte! Quale museo ti interessa? " +
                    "Posso consigliarti orari, prezzi e le opere più importanti da vedere.";
        }

        // Risposta contestuale basata sulla cronologia
        if (cronologia.length() > 2) {
            return "Continuando il nostro discorso, posso aggiungere che ogni opera ha la sua storia unica. " +
                    "C'è qualcosa di specifico che vorresti approfondire?";
        }


        // Risposta generica ma friendly
        return "È un argomento molto interessante! L'arte ha sempre qualcosa da raccontarci. " +
                "Potresti essere più specifico sulla tua domanda? Così posso aiutarti meglio.";
    }

    // Database simulato delle opere d'arte con caratteristiche per il riconoscimento
    private static final Map<String, JSONObject> DATABASE_OPERE = initializeDatabaseOpere();

    /**
     * Analizza un'immagine inviata dall'utente per verificare se corrisponde a un'opera d'arte
     */
    public static JSONObject analizzaImmagine (String base64Image, String fileName,String userId, String descrizioneQuest) {
        System.out.println("🖼️ Simulazione analisi immagine per utente: " + userId);
        System.out.println("📝 Descrizione quest: " + descrizioneQuest);

        // Simulazione delay di analisi AI
        try {
            Thread.sleep(random.nextInt(2000) + 1000); // 1-3 secondi
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        JSONObject risultato = new JSONObject();

        // Simulazione analisi basata sulla descrizione della quest
        boolean questApprovata = simulaAnalisiOpera(descrizioneQuest, fileName);

        risultato.put("success", true);
        risultato.put("userId", userId);
        risultato.put("fileName", fileName);
        risultato.put("questApprovata", questApprovata);

        if (questApprovata) {
            risultato.put("message", "🎉 Quest completata! L'opera è stata riconosciuta correttamente.");
            risultato.put("status", "APPROVATA");
            risultato.put("punteggioBonus", random.nextInt(50) + 25); // Bonus 25-75 punti

            // Dettagli dell'opera riconosciuta
            JSONObject operaRiconosciuta = trovaOperaPerDescrizione(descrizioneQuest);
            if (operaRiconosciuta != null) {
                risultato.put("operaDettagli", operaRiconosciuta);
            }

        } else {
            risultato.put("message", "❌ Quest non approvata. L'immagine non corrisponde all'opera cercata.");
            risultato.put("status", "NON_APPROVATA");
            risultato.put("suggerimento", generaSuggerimento(descrizioneQuest));
        }

        // Informazioni tecniche dell'analisi (simulate)
        JSONObject analisiTecnica = new JSONObject();
        analisiTecnica.put("confidenzaRiconoscimento", questApprovata ?
                (85 + random.nextInt(15)) : (20 + random.nextInt(40)));
        analisiTecnica.put("caratteristicheRilevate", generaCaratteristicheRilevate(questApprovata));
        analisiTecnica.put("tempoAnalisi", 1500 + random.nextInt(1000));

        risultato.put("analisiTecnica", analisiTecnica);
        risultato.put("timestamp", System.currentTimeMillis());

        System.out.println("🎯 Risultato analisi: " + (questApprovata ? "APPROVATA" : "NON APPROVATA"));

        return risultato;
    }

    /**
     * Simula l'analisi di un'opera basata sulla descrizione della quest
     */
    private static boolean simulaAnalisiOpera(String descrizioneQuest, String fileName) {
        if (descrizioneQuest == null || descrizioneQuest.trim().isEmpty()) {
            return random.nextBoolean(); // 50% possibilità se non c'è descrizione
        }

        String desc = descrizioneQuest.toLowerCase();

        // Opere molto famose - alta probabilità di successo
        if (desc.contains("gioconda") || desc.contains("mona lisa")) {
            return random.nextInt(100) < 85; // 85% successo
        }

        if (desc.contains("ultima cena") || desc.contains("leonardo")) {
            return random.nextInt(100) < 80; // 80% successo
        }

        if (desc.contains("venere") || desc.contains("botticelli")) {
            return random.nextInt(100) < 75; // 75% successo
        }

        if (desc.contains("notte stellata") || desc.contains("van gogh")) {
            return random.nextInt(100) < 70; // 70% successo
        }

        // Opere scientifiche/storiche
        if (desc.contains("t-rex") || desc.contains("dinosauro") || desc.contains("fossile")) {
            return random.nextInt(100) < 75; // 75% successo
        }

        if (desc.contains("rosetta") || desc.contains("geroglifico")) {
            return random.nextInt(100) < 70; // 70% successo
        }

        if (desc.contains("armatura") || desc.contains("cavaliere")) {
            return random.nextInt(100) < 65; // 65% successo
        }

        // Quest generiche - probabilità media
        if (desc.contains("mistero") || desc.contains("nascosta") || desc.contains("tesoro")) {
            return random.nextInt(100) < 60; // 60% successo
        }

        // Default - probabilità moderata
        return random.nextInt(100) < 55; // 55% successo
    }

    /**
     * Trova i dettagli di un'opera basata sulla descrizione della quest
     */
    private static JSONObject trovaOperaPerDescrizione(String descrizioneQuest) {
        if (descrizioneQuest == null) return null;

        String desc = descrizioneQuest.toLowerCase();

        // Cerca nell'database delle opere
        for (JSONObject opera : DATABASE_OPERE.values()) {
            String nomeOpera = opera.optString("nome", "").toLowerCase();
            String autore = opera.optString("autore", "").toLowerCase();

            if (desc.contains(nomeOpera) || desc.contains(autore)) {
                return opera;
            }
        }

        // Se non trovata, genera opera generica
        JSONObject operaGenerica = new JSONObject();
        operaGenerica.put("nome", "Opera d'Arte");
        operaGenerica.put("autore", "Artista Riconosciuto");
        operaGenerica.put("periodo", "Periodo Artistico");
        operaGenerica.put("tecnica", "Tecnica Artistica");
        operaGenerica.put("dimensioni", "Dimensioni Standard");

        return operaGenerica;
    }

    /**
     * Genera un suggerimento per aiutare l'utente quando la quest non è approvata
     */
    private static String generaSuggerimento(String descrizioneQuest) {
        if (descrizioneQuest == null) {
            return "Assicurati di fotografare l'opera corretta seguendo le indicazioni della quest.";
        }

        String[] suggerimenti = {
                "Prova a fotografare l'opera da una angolazione diversa, assicurandoti che sia ben illuminata.",
                "Avvicinati di più all'opera per catturare maggiori dettagli.",
                "Controlla di star fotografando l'opera giusta seguendo gli indizi forniti.",
                "Assicurati che l'immagine sia nitida e che l'opera sia completamente visibile.",
                "Cerca la targa informativa vicino all'opera per confermare che sia quella corretta."
        };

        return suggerimenti[random.nextInt(suggerimenti.length)];
    }

    /**
     * Genera caratteristiche rilevate dall'analisi (simulate)
     */
    private static JSONArray generaCaratteristicheRilevate(boolean successo) {
        JSONArray caratteristiche = new JSONArray();

        if (successo) {
            String[] caratteristichePositive = {
                    "Opera d'arte riconosciuta",
                    "Corrispondenza colori corretta",
                    "Composizione artistica identificata",
                    "Stile artistico confermato",
                    "Dettagli caratteristici presenti",
                    "Proporzioni corrette"
            };

            // Aggiungi 2-4 caratteristiche positive
            Set<String> selezionate = new HashSet<>();
            int numCaratteristiche = 2 + random.nextInt(3);

            while (selezionate.size() < numCaratteristiche) {
                selezionate.add(caratteristichePositive[random.nextInt(caratteristichePositive.length)]);
            }

            for (String car : selezionate) {
                caratteristiche.put(car);
            }

        } else {
            String[] caratteristicheNegative = {
                    "Opera non corrispondente",
                    "Immagine non chiara",
                    "Elementi artistici non riconosciuti",
                    "Stile non conforme",
                    "Dettagli insufficienti",
                    "Angolazione non ottimale"
            };

            caratteristiche.put(caratteristicheNegative[random.nextInt(caratteristicheNegative.length)]);
            caratteristiche.put(caratteristicheNegative[random.nextInt(caratteristicheNegative.length)]);
        }

        return caratteristiche;
    }

    /**
     * Inizializza il database delle opere d'arte per il riconoscimento
     */
    private static Map<String, JSONObject> initializeDatabaseOpere() {
        Map<String, JSONObject> database = new HashMap<>();

        // Opere famose
        database.put("gioconda", creaOpera(
                "Gioconda", "Leonardo da Vinci", "1503-1519",
                "Olio su tavola di pioppo", "77 x 53 cm", "Rinascimento"
        ));

        database.put("ultima_cena", creaOpera(
                "L'Ultima Cena", "Leonardo da Vinci", "1495-1498",
                "Tempera grassa e olio su intonaco", "460 x 880 cm", "Rinascimento"
        ));

        database.put("nascita_venere", creaOpera(
                "La Nascita di Venere", "Sandro Botticelli", "1484-1486",
                "Tempera su tela", "172,5 x 278,9 cm", "Rinascimento"
        ));

        database.put("notte_stellata", creaOpera(
                "La Notte Stellata", "Vincent van Gogh", "1889",
                "Olio su tela", "73,7 x 92,1 cm", "Post-Impressionismo"
        ));

        // Reperti scientifici/storici
        database.put("stele_rosetta", creaOpera(
                "Stele di Rosetta", "Antico Egitto", "196 a.C.",
                "Granodiorite", "114 x 72 x 28 cm", "Archeologico"
        ));

        database.put("t_rex", creaOpera(
                "Scheletro di Tyrannosaurus Rex", "Paleontologia", "68-66 milioni di anni fa",
                "Fossile", "12,3 x 4 metri", "Paleontologico"
        ));

        return database;
    }

    private static JSONObject creaOpera(String nome, String autore, String periodo,
                                        String tecnica, String dimensioni, String categoria) {
        JSONObject opera = new JSONObject();
        opera.put("nome", nome);
        opera.put("autore", autore);
        opera.put("periodo", periodo);
        opera.put("tecnica", tecnica);
        opera.put("dimensioni", dimensioni);
        opera.put("categoria", categoria);
        opera.put("dataInserimento", System.currentTimeMillis());

        return opera;
    }

    /**
     * Metodo per testare il servizio
     */
    public static JSONObject testAnalisi() {
        return analizzaImmagine(
                "base64_test_image",
                "test.jpg",
                "user123",
                "Cerca la Gioconda di Leonardo da Vinci"
        );
    }

}