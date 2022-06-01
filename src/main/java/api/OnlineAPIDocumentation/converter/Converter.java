package api.OnlineAPIDocumentation.converter;

import java.util.List;
import java.util.Map;

public interface Converter {
    String convert(List<Map<String, String>> listOfComponents);
}
