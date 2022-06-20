package api.OnlineAPIDocumentation.converter;

import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class JSONConverter implements Converter {

    @Override
    public String convert(Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents) {
        JSONObject jsonObject = new JSONObject();

        for (String nameFile : mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.keySet()) {
            JSONObject listOfMapsOfFieldOrMethodComponentsJsonObject = new JSONObject();

            for (int i = 0; i < mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.get(nameFile).size(); i++) {
                listOfMapsOfFieldOrMethodComponentsJsonObject.put(i + 1, mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.get(nameFile).get(i));
            }

            jsonObject.put(nameFile, listOfMapsOfFieldOrMethodComponentsJsonObject);
        }

        return jsonObject.toJSONString();
    }
}
