package api.OnlineAPIDocumentation;

import api.OnlineAPIDocumentation.converter.ConverterFactory;
import api.OnlineAPIDocumentation.converter.Format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private final String pathToFile;
    private final Format format;

    public Parser(String pathToFile, Format format) {
        this.pathToFile = pathToFile;
        this.format = format;
    }

    public String parse() throws IOException {
        String textFromFile = getTextFromFile();

        List<Map<String, String>> listOfComponents = getListOfComponents(textFromFile);

        ConverterFactory converterFactory = new ConverterFactory();

        return converterFactory.convert(listOfComponents, format);
    }

    private List<Map<String, String>> getListOfComponents(String textFromFile) {
        List<Map<String, String>> listOfComponents = new ArrayList<>();

        Matcher matcherOfJsonViewAnnotation = Pattern.compile("@JsonView\\(.+Online.+\\)").matcher(textFromFile);

        Matcher matcherOfFieldAccess = Pattern.compile("public|private|protected").matcher(textFromFile);

        matcherOfJsonViewAnnotation.results().forEach(jsonViewAnnotation -> {
            Map<String, String> mapOfComponent = new HashMap<>();

            if (matcherOfFieldAccess.find(jsonViewAnnotation.start())) {
                String nameAndType = textFromFile.substring(matcherOfFieldAccess.end(), textFromFile.indexOf("\n", matcherOfFieldAccess.end())).trim();

                String type = nameAndType.substring(0, nameAndType.indexOf(" "));
                String name = nameAndType.substring(nameAndType.indexOf(" "), nameAndType.indexOf(";")).trim();

                mapOfComponent.put("type", type);
                mapOfComponent.put("name", name);

                listOfComponents.add(mapOfComponent);
            }
        });

        return listOfComponents;
    }

    private String getTextFromFile() throws IOException {
        return Files.readString(Paths.get(pathToFile));
    }
}
