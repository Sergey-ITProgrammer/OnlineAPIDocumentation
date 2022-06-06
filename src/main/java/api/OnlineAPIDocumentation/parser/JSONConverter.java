package api.OnlineAPIDocumentation.parser;

import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class JSONConverter implements Converter {

    @Override
    public String convert(Map<String, List<Map<String, String>>> mapOfNameFileAndListOfMapsOfFieldOrMethodComponents) {
        JSONObject jsonObject = new JSONObject();

        for (String nameFile : mapOfNameFileAndListOfMapsOfFieldOrMethodComponents.keySet()) {
            JSONObject listOfMapsOfFieldOrMethodComponentsJsonObject = new JSONObject();

            for (int i = 0; i < mapOfNameFileAndListOfMapsOfFieldOrMethodComponents.get(nameFile).size(); i++) {
                listOfMapsOfFieldOrMethodComponentsJsonObject.put(i + 1, mapOfNameFileAndListOfMapsOfFieldOrMethodComponents.get(nameFile).get(i));
            }

            jsonObject.put(nameFile, listOfMapsOfFieldOrMethodComponentsJsonObject);
        }

        return jsonObject.toJSONString();
    }
}
