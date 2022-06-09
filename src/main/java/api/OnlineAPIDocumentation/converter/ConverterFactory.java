package api.OnlineAPIDocumentation.converter;

import java.util.List;
import java.util.Map;

public class ConverterFactory {
    public String convert(Map<String, List<Map<String, String>>> mapOfNameFileAndListOfMapsOfFieldOrMethodComponents, Format format) {
        String result = "";

        switch (format) {
            case JSON:
                result = new JSONConverter().convert(mapOfNameFileAndListOfMapsOfFieldOrMethodComponents);
                break;
            case HTML:
                result = new HTMLConverter().convert(mapOfNameFileAndListOfMapsOfFieldOrMethodComponents);
                break;
        }

        return result;
    }
}
