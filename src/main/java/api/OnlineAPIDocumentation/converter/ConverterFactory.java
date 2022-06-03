package api.OnlineAPIDocumentation.converter;

import java.util.List;
import java.util.Map;

public class ConverterFactory {
    public String convert(List<Map<String, String>> splitCommits, Format format) {
        String result = "";

        switch (format) {
            case json:
                result = new JSONConverter().convert(splitCommits);
                break;
        }

        return result;
    }
}
