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
        Map<String, List<Map<String, String>>> mapOfNameFileAndListOfMapsOfComponents = new HashMap<>();

        for (String nameFile : mapOfNameFileAndTextFromFile.keySet()) {
            String textFromFile = mapOfNameFileAndTextFromFile.get(nameFile);

            Matcher matcherOfJsonViewAnnotation = Pattern.compile("@JsonView\\(.+Online.+\\)").matcher(textFromFile);
            Matcher matcherOfMethodOrField = Pattern.compile(
                    "(?<method>(public|private|protected)[\\w\\s\\n<>?,]+\\s+(?<nameMethod>\\w+\\s*\\([\\w\\s\\n<>?,]*\\)))|((?<field1>(public|private|protected)[\\w\\s\\n<>?,]+;)|(?<fieldWithValue>(?<field2>(public|private|protected)[\\w\\s\\n<>?,]+)=\\s*.+\\n?\\s*.+;))"
                    ).matcher(textFromFile);

            Pattern patternOfValueWithGeneric = Pattern.compile("\\s\\w+<[\\w\\s\\n<>?,]*>");

            List<Map<String, String>> listOfMapOfComponents = new ArrayList<>();

            matcherOfJsonViewAnnotation.results().forEach(jsonViewAnnotation -> {

                Map<String, String> mapOfComponents = new HashMap<>();

                if (matcherOfMethodOrField.find(jsonViewAnnotation.start())) {

                    String method = matcherOfMethodOrField.group("method");
                    String nameMethod = matcherOfMethodOrField.group("nameMethod");

                    String field = matcherOfMethodOrField.group("field1");
                    String fieldWithValue = matcherOfMethodOrField.group("field2");

                    if (method != null) {
                        mapOfComponents.put("isMethod", "true");

                        mapOfComponents.put("name", nameMethod);

                        StringBuilder sb = new StringBuilder(method).reverse();

                        sb = new StringBuilder(sb.delete(0, nameMethod.length()).toString().trim());

                        Matcher matcherOfValueWithGeneric = patternOfValueWithGeneric.matcher(sb.reverse().toString());

                        if (matcherOfValueWithGeneric.find()) {
                            mapOfComponents.put("type", matcherOfValueWithGeneric.group().trim());
                        } else {
                            mapOfComponents.put("type", new StringBuilder(sb.reverse().substring(0, sb.indexOf(" "))).reverse().toString());
                        }
                    }

                    if (fieldWithValue != null) {
                        mapOfComponents.put("isMethod", "false");

                        StringBuilder sb = new StringBuilder(fieldWithValue.trim()).reverse();

                        String name = new StringBuilder(sb.substring(0, sb.indexOf(" "))).reverse().toString();

                        mapOfComponents.put("name", name);

                        sb = new StringBuilder(sb.delete(0, name.length() + 1).toString().trim());

                        Matcher matcherOfValueWithGeneric = patternOfValueWithGeneric.matcher(sb.reverse().toString());

                        if (matcherOfValueWithGeneric.find()) {
                            mapOfComponents.put("type", matcherOfValueWithGeneric.group().trim());
                        } else {
                            mapOfComponents.put("type", new StringBuilder(sb.reverse().substring(0, sb.indexOf(" "))).reverse().toString());
                        }
                    }
                    else if (field != null) {
                        mapOfComponents.put("isMethod", "false");

                        StringBuilder sb = new StringBuilder(field).reverse();

                        String name = new StringBuilder(sb.substring(1, sb.indexOf(" "))).reverse().toString();

                        mapOfComponents.put("name", name);

                        sb = new StringBuilder(sb.delete(0, name.length() + 1).toString().trim());

                        Matcher matcherOfValueWithGeneric = patternOfValueWithGeneric.matcher(sb.reverse().toString());

                        if (matcherOfValueWithGeneric.find()) {
                            mapOfComponents.put("type", matcherOfValueWithGeneric.group().trim());
                        } else {
                            mapOfComponents.put("type", new StringBuilder(sb.reverse().substring(0, sb.indexOf(" "))).reverse().toString());
                        }
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
