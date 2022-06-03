package api.OnlineAPIDocumentation.converter;

import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class JSONConverter implements Converter {

    @Override
    public String convert(List<Map<String, String>> listOfComponents) {
        JSONObject jsonObject = new JSONObject();

//        for (int i = 0; i < listOfComponents.size(); i++) {
//            JSONObject component = new JSONObject();
//
//            component.putAll(listOfComponents.get(i));
//
//            jsonObject.put(i + 1, component);
//        }

        return jsonObject.toJSONString();
    }
}
