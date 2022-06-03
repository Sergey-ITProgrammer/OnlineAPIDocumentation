package api.OnlineAPIDocumentation;

import api.OnlineAPIDocumentation.converter.Format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private final String pathToDir;
    private final Format format;

    public Parser(String pathToFile, Format format) {
        this.pathToDir = pathToFile;
        this.format = format;
    }

    private final Map<String, String> mapOfNameFileAndTextFromFile = new HashMap<>();

    public String parse() throws IOException {
        File directory = new File(pathToDir);

        searchFiles(directory);

        Map<String, List<Map<String, String>>> mapOfNameFileAndListOfMapsOfComponents = getMapOfNameFileAndMapOfComponents();

        System.out.println(mapOfNameFileAndListOfMapsOfComponents);

//        List<Map<String, String>> listOfComponents = null;
//
//        ConverterFactory converterFactory = new ConverterFactory();

        return null;
    }

    private Map<String, List<Map<String, String>>> getMapOfNameFileAndMapOfComponents() {
        int groupOfMethod = 1;
        int groupOfField = 3;
        int groupOfFieldWithValue = 5;

        Map<String, List<Map<String, String>>> mapOfNameFileAndListOfMapsOfComponents = new HashMap<>();

        for (String nameFile : mapOfNameFileAndTextFromFile.keySet()) {
            String textFromFile = mapOfNameFileAndTextFromFile.get(nameFile);

            Matcher matcherOfJsonViewAnnotation = Pattern.compile("@JsonView\\(.+Online.+\\)").matcher(textFromFile);
            Matcher matcherOfMethodOrField = Pattern.compile(
                    "((public|private|protected)\\s+.+\\(.*\\))|((public|private|protected)[\\w\\s<>]+;|((public|private|protected)[\\w\\s]+\\n?\\s*=.+;))"
                    ).matcher(textFromFile);

            List<Map<String, String>> listOfMapOfComponents = new ArrayList<>();

            matcherOfJsonViewAnnotation.results().forEach(jsonViewAnnotation -> {

                Map<String, String> mapOfComponents = new HashMap<>();

                if (matcherOfMethodOrField.find(jsonViewAnnotation.start())) {

                    String method = matcherOfMethodOrField.group(groupOfMethod);
                    String field = matcherOfMethodOrField.group(groupOfField);
                    String fieldWithValue = matcherOfMethodOrField.group(groupOfFieldWithValue);

                    if (method != null) {
                        mapOfComponents.put("isMethod", "true");

                        StringBuilder sb = new StringBuilder(method).reverse();

                        mapOfComponents.put("name", new StringBuilder(sb.substring(0, sb.indexOf(" "))).reverse().toString());

                        sb = new StringBuilder(sb.delete(0, sb.indexOf(" ")).toString().trim());

                        mapOfComponents.put("type", new StringBuilder(sb.substring(0, sb.indexOf(" "))).reverse().toString());
                    }

                    if (fieldWithValue != null) {
                        mapOfComponents.put("isMethod", "false");

                        StringBuilder sb = new StringBuilder(fieldWithValue);
                        sb = new StringBuilder(sb.delete(sb.indexOf("="), sb.length()).toString().trim()).reverse();

                        mapOfComponents.put("name", new StringBuilder(sb.substring(0, sb.indexOf(" "))).reverse().toString());

                        sb = new StringBuilder(sb.delete(0, sb.indexOf(" ")).toString().trim());

                        mapOfComponents.put("type", new StringBuilder(sb.substring(0, sb.indexOf(" "))).reverse().toString());
                    }
                    else if (field != null) {
                        mapOfComponents.put("isMethod", "false");

                        StringBuilder sb = new StringBuilder(field).reverse();

                        mapOfComponents.put("name", new StringBuilder(sb.substring(1, sb.indexOf(" "))).reverse().toString());

                        sb = new StringBuilder(sb.delete(0, sb.indexOf(" ")).toString().trim());

                        mapOfComponents.put("type", new StringBuilder(sb.substring(0, sb.indexOf(" "))).reverse().toString());
                    }
                }

                listOfMapOfComponents.add(mapOfComponents);
            });

            mapOfNameFileAndListOfMapsOfComponents.put(nameFile, listOfMapOfComponents);
        }

        return mapOfNameFileAndListOfMapsOfComponents;
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
