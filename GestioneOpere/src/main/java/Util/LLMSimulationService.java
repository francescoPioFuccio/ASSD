package Util;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    public static JSONObject analizzaImmagine(String imageBase64, String nomeFile, String userId, String descrizione) {
        System.out.println("🖼️ Simulazione analisi immagine: " + nomeFile);

        // Simulazione di delay per processing dell'immagine (1-3 secondi)
        try {
            Thread.sleep(random.nextInt(2000) + 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        JSONObject risultato = new JSONObject();

        // Simula l'esito dell'analisi (70% positive, 30% negative)
        boolean approved = random.nextDouble() > 0.3;

        risultato.put("success", true);
        risultato.put("approved", approved);
        risultato.put("confidence", Math.round((random.nextDouble() * 0.3 + 0.7) * 100.0) / 100.0); // 70-100%
        risultato.put("timestamp", System.currentTimeMillis());

        if (approved) {
            risultato.put("message", "✅ L'immagine è stata approvata!");
            risultato.put("reason", generaMotivoApprovazione());
            risultato.put("suggerimenti", generaSuggerimentiPositivi());
        } else {
            risultato.put("message", "❌ L'immagine non soddisfa i criteri richiesti.");
            risultato.put("reason", generaMotivoRifiuto());
            risultato.put("suggerimenti", generaSuggerimentiMiglioramento());
        }

        // Aggiungi dettagli tecnici simulati
        JSONObject dettagliTecnici = new JSONObject();
        dettagliTecnici.put("risoluzione", simulaRisoluzione());
        dettagliTecnici.put("formato", estraiFormato(nomeFile));
        dettagliTecnici.put("dimensione", simulaDimensioneFile());
        dettagliTecnici.put("qualita", random.nextInt(3) == 0 ? "alta" : "media");

        risultato.put("dettagliTecnici", dettagliTecnici);

        // Se è presente una descrizione, aggiungila all'analisi
        if (descrizione != null && !descrizione.isEmpty()) {
            risultato.put("descrizioneUtente", descrizione);
            risultato.put("matchDescrizione", random.nextBoolean());
        }

        return risultato;
    }

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
}