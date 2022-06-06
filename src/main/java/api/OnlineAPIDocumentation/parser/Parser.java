package api.OnlineAPIDocumentation.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private final String pathToDir;

    public Parser(String pathToFile) {
        this.pathToDir = pathToFile;
    }

    private final Map<String, String> mapOfNameFileAndTextFromFile = new HashMap<>();

    public String parse() throws IOException {
        File directory = new File(pathToDir);

        searchFiles(directory);

        Map<String, List<Map<String, String>>> mapOfNameFileAndListOfMapsOfFieldOrMethodComponents
                = getMapOfNameFileAndListOfMapsOfFieldOrMethodComponents();

        return new JSONConverter().convert(mapOfNameFileAndListOfMapsOfFieldOrMethodComponents);
    }

    private Map<String, List<Map<String, String>>> getMapOfNameFileAndListOfMapsOfFieldOrMethodComponents() {
        Map<String, List<Map<String, String>>> mapOfNameFileAndListOfMapsOfFieldOrMethodComponents = new HashMap<>();

        for (String nameFile : mapOfNameFileAndTextFromFile.keySet()) {
            String textFromFile = mapOfNameFileAndTextFromFile.get(nameFile);

            Matcher matcherOfJsonViewAnnotation = Pattern.compile("@JsonView\\(.+Online.+\\)").matcher(textFromFile);
            Matcher matcherOfMethodOrField = Pattern.compile(
                    "(?<method>(public|private|protected)[\\w\\s\\n<>?,]+\\s+(?<nameMethod>\\w+\\s*\\([\\w\\s\\n<>?,]*\\)))" +
                            "|((?<field1>(public|private|protected)[\\w\\s\\n<>?,]+);" +
                            "|(?<fieldWithValue>(?<field2>(public|private|protected)[\\w\\s\\n<>?,]+)=\\s*.+\\n?\\s*.+;))"
                    ).matcher(textFromFile);

            List<Map<String, String>> listOfMapsOfFieldOrMethodComponents = new ArrayList<>();

            matcherOfJsonViewAnnotation.results().forEach(jsonViewAnnotation -> {

                Map<String, String> mapOfComponents = new HashMap<>();

                if (matcherOfMethodOrField.find(jsonViewAnnotation.start())) {

                    String method = matcherOfMethodOrField.group("method");
                    String nameMethod = matcherOfMethodOrField.group("nameMethod");

                    String field = matcherOfMethodOrField.group("field1");
                    String fieldWithValue = matcherOfMethodOrField.group("field2");

                    if (method != null) {
                        putComponentsOfMethodIntoMap(mapOfComponents, method, nameMethod);
                    }
                    if (fieldWithValue != null) {
                        putComponentsOfFieldIntoMap(mapOfComponents, fieldWithValue);
                    }
                    else if (field != null) {
                        putComponentsOfFieldIntoMap(mapOfComponents, field);
                    }
                }

                listOfMapsOfFieldOrMethodComponents.add(mapOfComponents);
            });

            mapOfNameFileAndListOfMapsOfFieldOrMethodComponents.put(nameFile, listOfMapsOfFieldOrMethodComponents);
        }

        return mapOfNameFileAndListOfMapsOfFieldOrMethodComponents;
    }

    private void putComponentsOfFieldIntoMap(Map<String, String> mapOfComponents, String field) {
        mapOfComponents.put("isMethod", "false");

        StringBuilder sb = new StringBuilder(field.trim()).reverse();

        String name = new StringBuilder(sb.substring(0, sb.indexOf(" "))).reverse().toString();

        mapOfComponents.put("name", name);

        sb = new StringBuilder(sb.delete(0, name.length() + 1).toString().trim());

        Matcher matcherOfValueWithGeneric = Pattern.compile("\\s\\w+<[\\w\\s\\n<>?,]*>").matcher(sb.reverse().toString());

        if (matcherOfValueWithGeneric.find()) {
            mapOfComponents.put("type", matcherOfValueWithGeneric.group().trim());
        } else {
            mapOfComponents.put("type", new StringBuilder(sb.reverse().substring(0, sb.indexOf(" "))).reverse().toString());
        }
    }

    private void putComponentsOfMethodIntoMap(Map<String, String> mapOfComponents, String method, String nameMethod) {
        mapOfComponents.put("isMethod", "true");

        mapOfComponents.put("name", nameMethod);

        StringBuilder sb = new StringBuilder(method).reverse();

        sb = new StringBuilder(sb.delete(0, nameMethod.length()).toString().trim());

        Matcher matcherOfValueWithGeneric = Pattern.compile("\\s\\w+<[\\w\\s\\n<>?,]*>").matcher(sb.reverse().toString());

        if (matcherOfValueWithGeneric.find()) {
            mapOfComponents.put("type", matcherOfValueWithGeneric.group().trim());
        } else {
            mapOfComponents.put("type", new StringBuilder(sb.reverse().substring(0, sb.indexOf(" "))).reverse().toString());
        }
    }

    private void searchFiles(File dir) throws IOException {
        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isDirectory()) {
                    searchFiles(file);
                } else {
                    String filePath = file.getPath();

                    Matcher matcherOfSrcDir = Pattern.compile("/src/main/java").matcher(filePath);
                    if (matcherOfSrcDir.find()) {
                        filePath = filePath.substring(matcherOfSrcDir.end());
                    }

                    mapOfNameFileAndTextFromFile.put(filePath, Files.readString(file.toPath()));
                }
            }
        }
    }
}
