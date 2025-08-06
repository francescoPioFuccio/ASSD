package Util;

import org.json.JSONArray;
import org.json.JSONObject;
import Entity.User;
import Entity.SimulazionePromozione;

public class SimulazioneService {

    // Metodo per simulare la risposta della promozione
    public static String getPromozioni() {
        // Creazione di un JSONArray con più promozioni
        JSONArray promotionsArray = new JSONArray();

        // Aggiunta di alcune promozioni di esempio
        SimulazionePromozione promozione1 = new SimulazionePromozione("Voucher Museo", 100, 0.2);
        SimulazionePromozione promozione2 = new SimulazionePromozione("Sconto Mostra", 0, 0.05);

        // Aggiunta delle promozioni al JSONArray
        promotionsArray.put(new JSONObject(promozione1));
        promotionsArray.put(new JSONObject(promozione2));

        // Restituisce un array JSON di promozioni
        return promotionsArray.toString();
    }

    // Metodo per simulare la risposta dell'applicazione di una promozione
    public static String applicaPromozione(SimulazionePromozione promozione, int puntiUtente) {
        if (puntiUtente >= promozione.getPuntiNecessari()) {
            return "Promozione applicata! Hai uno sconto del " + promozione.getSconto() + "%";
        } else {
            return "Non hai abbastanza punti per questa promozione.";
        }
    }

    // Simulazione della modifica delle preferenze utente
    public static String modificaPreferenze(User user) {
        JSONObject response = new JSONObject();
        response.put("message", "Preferenze aggiornate con successo.");
        response.put("user", new JSONObject(user));

        return response.toString();
    }

    // Simulazione della storia della roba vista dall'utente
    public static String getStoricoRobaVista(Long userId) {
        JSONArray storico = new JSONArray();

        // Creazione di un esempio di "roba vista"
        JSONObject item1 = new JSONObject();
        item1.put("museo", "Museo di Arte Moderna");
        item1.put("opera", "La Nascita di Venere");

        JSONObject item2 = new JSONObject();
        item2.put("museo", "Museo Egizio");
        item2.put("opera", "Mummia del faraone");

        storico.put(item1);
        storico.put(item2);

        return storico.toString();
    }




}
