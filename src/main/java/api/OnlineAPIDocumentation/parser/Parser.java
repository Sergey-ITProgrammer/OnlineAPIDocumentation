package api.OnlineAPIDocumentation.parser;

import api.OnlineAPIDocumentation.converter.ConverterFactory;
import api.OnlineAPIDocumentation.converter.Format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private final String pathToDir;
    private final boolean withPackageName;
    private final Format format;

    public Parser(String pathToFile, boolean withPackageName, Format format) {
        this.pathToDir = pathToFile;
        this.withPackageName = withPackageName;
        this.format = format;
    }

    public String parse() throws IOException {
        Map<String, String> mapOfFileNameAndTextFromFile = new HashMap<>();

        File directory = new File(pathToDir);
        searchFiles(directory, mapOfFileNameAndTextFromFile);

        Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents
                = getMapOfFileNameAndListOfMapsOfFieldOrMethodComponents(mapOfFileNameAndTextFromFile);

        ConverterFactory converter = new ConverterFactory();

        return converter.convert(mapOfFileNameAndListOfMapsOfFieldOrMethodComponents, format);
    }

    private Map<String, List<Map<String, String>>> getMapOfFileNameAndListOfMapsOfFieldOrMethodComponents(Map<String, String> mapOfFileNameAndTextFromFile) {
        return processMapOfFileNameAndTextFromFile(mapOfFileNameAndTextFromFile);
    }

    private Map<String, List<Map<String, String>>> processMapOfFileNameAndTextFromFile(Map<String, String> mapOfFileNameAndTextFromFile) {
        Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents = new HashMap<>();

        for (String fileName : mapOfFileNameAndTextFromFile.keySet()) {
            String textFromFile = mapOfFileNameAndTextFromFile.get(fileName);

            Matcher matcherOfJsonViewAnnotation = Pattern.compile("@JsonView\\(.+Online.+\\)").matcher(textFromFile);
            Matcher matcherOfMethodOrField = Pattern.compile(
                    "(?<method>(public|private|protected)[\\w\\s\\n<>?,]+\\s+(?<methodName>\\w+\\s*\\([\\w\\s\\n<>?,]*\\)))" +
                            "|((?<field1>(public|private|protected)[\\w\\s\\n<>?,]+);" +
                            "|(?<fieldWithValue>(?<field2>(public|private|protected)[\\w\\s\\n<>?,]+)=\\s*.+\\n?\\s*.+;))"
                    ).matcher(textFromFile);

            List<Map<String, String>> listOfMapsOfFieldOrMethodComponents = new ArrayList<>();

            matcherOfJsonViewAnnotation.results().forEach(jsonViewAnnotation -> {

                Map<String, String> mapOfFieldOrMethodComponents = new HashMap<>();

                if (matcherOfMethodOrField.find(jsonViewAnnotation.start())) {

                    String method = matcherOfMethodOrField.group("method");
                    String methodName = matcherOfMethodOrField.group("methodName");

                    String field = matcherOfMethodOrField.group("field1");
                    String fieldWithValue = matcherOfMethodOrField.group("field2");

                    if (method != null) {
                        putComponentsOfMethodIntoMap(mapOfFieldOrMethodComponents, method, methodName, textFromFile);
                    }
                    if (fieldWithValue != null) {
                        putComponentsOfFieldIntoMap(mapOfFieldOrMethodComponents, fieldWithValue, textFromFile);
                    }
                    else if (field != null) {
                        putComponentsOfFieldIntoMap(mapOfFieldOrMethodComponents, field, textFromFile);
                    }
                }

                listOfMapsOfFieldOrMethodComponents.add(mapOfFieldOrMethodComponents);
            });

            mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.put(fileName, listOfMapsOfFieldOrMethodComponents);
        }

        addFieldOrMethodComponentsIfExtends(mapOfFileNameAndTextFromFile, mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);

        return mapOfFileNameAndListOfMapsOfFieldOrMethodComponents;
    }

    private void addFieldOrMethodComponentsIfExtends(Map<String, String> mapOfFileNameAndTextFromFile,
                                                     Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents) {
        for (String fileName : mapOfFileNameAndTextFromFile.keySet()) {
            List<Map<String, String>> fieldOrMethodFromParentClasses = new ArrayList<>();

            getFieldOrMethodComponentsFromParentClasses(fileName, fieldOrMethodFromParentClasses,
                    mapOfFileNameAndTextFromFile, mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);

            mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.get(fileName).addAll(fieldOrMethodFromParentClasses);
        }
    }

    private void getFieldOrMethodComponentsFromParentClasses(String fileName,
                                                             List<Map<String, String>> fieldOrMethodFromParentClasses,
                                                             Map<String, String> mapOfFileNameAndTextFromFile,
                                                             Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents) {
        String parentClasses = getParentClasses(fileName, mapOfFileNameAndTextFromFile);

        if (parentClasses != null) {
            String[] arrOfParentClass = parentClasses.split(",");

            for (String parentClass : arrOfParentClass) {
                for (String fileName1 : mapOfFileNameAndTextFromFile.keySet()) {
                    if (fileName1.contains(parentClass.trim())) {
                        fieldOrMethodFromParentClasses.addAll(mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.get(fileName1));
                    }

                    String parentClass1 = getParentClasses(fileName1, mapOfFileNameAndTextFromFile);

                    if (parentClass1 != null) {
                        for (String fileName2 : mapOfFileNameAndTextFromFile.keySet()) {
                            if (fileName2.contains(parentClass1)) {
                                getFieldOrMethodComponentsFromParentClasses(fileName2, fieldOrMethodFromParentClasses,
                                        mapOfFileNameAndTextFromFile, mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getParentClasses(String fileName, Map<String, String> mapOfFileNameAndTextFromFile) {
        String textFromFile = mapOfFileNameAndTextFromFile.get(fileName);

        Matcher matcherOfParentClass = Pattern.compile("class[\\s\\n]+\\w+[\\s\\n]+extends[\\s\\n]+(?<parentClass>[\\w\\s\\n,]+)").matcher(textFromFile);
        Matcher matcherOfParentClassWithImplements = Pattern.compile("class[\\s\\n]+\\w+[\\s\\n]+extends[\\s\\n]+(?<parentClass>[\\w\\s\\n,]+)implements")
                .matcher(textFromFile);

        String parentClasses = null;

        if (matcherOfParentClassWithImplements.find()) {
            parentClasses = matcherOfParentClassWithImplements.group("parentClass");
        }
        else if (matcherOfParentClass.find()) {
            parentClasses = matcherOfParentClass.group("parentClass");
        }

        return parentClasses;
    }

    private void putComponentsOfFieldIntoMap(Map<String, String> mapOfFieldOrMethodComponents, String field, String textFromFile) {
        mapOfFieldOrMethodComponents.put("isMethod", "false");

        StringBuilder sb = new StringBuilder(field.trim()).reverse();

        String name = new StringBuilder(sb.substring(0, sb.indexOf(" "))).reverse().toString();

        mapOfFieldOrMethodComponents.put("name", name);

        sb = new StringBuilder(sb.delete(0, name.length() + 1).toString().trim());

        Matcher matcherOfTypeWithGeneric = Pattern.compile("\\s\\w+\\s*<[\\w\\s\\n<>?,]*>").matcher(sb.reverse().toString());

        if (matcherOfTypeWithGeneric.find()) {
            mapOfFieldOrMethodComponents.put("type", getType(matcherOfTypeWithGeneric.group().trim(), textFromFile));
        } else {
            mapOfFieldOrMethodComponents.put("type", getType(new StringBuilder(sb.reverse().substring(0, sb.indexOf(" "))).reverse().toString(), textFromFile));
        }
    }

    private void putComponentsOfMethodIntoMap(Map<String, String> mapOfFieldOrMethodComponents, String method, String methodName, String textFromFile) {
        mapOfFieldOrMethodComponents.put("isMethod", "true");

        mapOfFieldOrMethodComponents.put("name", getNameOfMethod(methodName));

        StringBuilder sb = new StringBuilder(method).reverse();

        sb = new StringBuilder(sb.delete(0, methodName.length()).toString().trim());

        Matcher matcherOfTypeWithGeneric = Pattern.compile("\\s\\w+\\s*<[\\w\\s\\n<>?,]*>").matcher(sb.reverse().toString());

        if (matcherOfTypeWithGeneric.find()) {
            mapOfFieldOrMethodComponents.put("type", getType(matcherOfTypeWithGeneric.group().trim(), textFromFile));
        } else {
            mapOfFieldOrMethodComponents.put("type", getType(new StringBuilder(sb.reverse().substring(0, sb.indexOf(" "))).reverse().toString(), textFromFile));
        }
    }

    private String getNameOfMethod(String nameMethod) {
        Matcher matcherOfNameMethod = Pattern.compile("get(?<name>\\w+)\\s*\\([\\w\\s\\n<>?,]*\\)").matcher(nameMethod);

        if (matcherOfNameMethod.find()) {
            return matcherOfNameMethod.group("name");
        } else {
            return nameMethod;
        }
    }

    private String getType(String type, String textFromFile) {
        if (withPackageName) {
            return getTypeWithPackageName(type, textFromFile);
        } else {
            return type;
        }
    }

    private String getTypeWithPackageName(String value, String textFromFile) {
        Matcher matcherOfTypeWithGeneric = Pattern.compile("(?<typeWithoutGeneric>\\w+)[\\s\\n]*<[\\w\\s\\n<>?,]+>").matcher(value);

        String type = value;
        if (matcherOfTypeWithGeneric.find()) {
            type = matcherOfTypeWithGeneric.group("typeWithoutGeneric").trim();
        }

        Matcher matcherOfPackageName = Pattern.compile("import(?<packageName>[\\s\\n]+[\\w\\s\\n.]+[\\s\\n.]+" + type + "[\\s\\n]*);").matcher(textFromFile);

        if (matcherOfPackageName.find()) {
            return matcherOfPackageName.group("packageName").trim();
        } else {
            return type;
        }
    }

    private void searchFiles(File dir, Map<String, String> mapOfFileNameAndTextFromFile) throws IOException {
        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isDirectory()) {
                    searchFiles(file, mapOfFileNameAndTextFromFile);
                } else {
                    String filePath = file.getPath();

                    Matcher matcherOfSrcDir = Pattern.compile("/src/main/java").matcher(filePath);
                    if (matcherOfSrcDir.find()) {
                        filePath = filePath.substring(matcherOfSrcDir.end());
                    }

                    mapOfFileNameAndTextFromFile.put(filePath, Files.readString(file.toPath()));
                }
            }
        }
    }
}
