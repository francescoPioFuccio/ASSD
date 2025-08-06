package Util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Converter
public class JsonStringListConverter implements AttributeConverter<List<String>, String> {

    private final Gson gson = new Gson();
    private final Type type = new TypeToken<List<String>>() {}.getType();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) {
            return null;
        }
        String json = gson.toJson(attribute);
        System.out.println("💾 [JsonStringListConverter] Converting to DB JSON: " + json);
        return json;
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Caso corretto: JSON array
            List<String> result = gson.fromJson(dbData, type);
            System.out.println("✅ [JsonStringListConverter] Parsed JSON array: " + result);
            return result;
        } catch (Exception e) {
            // Caso legacy: semplice stringa (es. "Arte, Scienza, Storia")
            System.out.println("⚠️ [JsonStringListConverter] Fallback attivato: dbData non è un array JSON -> " + dbData);
            List<String> fallback = new ArrayList<>(Arrays.asList(
                    dbData.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "")
                            .split(",")
            ));
            // Rimuove eventuali spazi bianchi
            fallback.replaceAll(String::trim);
            System.out.println("👉 [JsonStringListConverter] Fallback convertito in lista: " + fallback);
            return fallback;
        }
    }
}
