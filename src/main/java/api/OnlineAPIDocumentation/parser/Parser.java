package api.OnlineAPIDocumentation.parser;

import api.OnlineAPIDocumentation.converter.ConverterFactory;
import api.OnlineAPIDocumentation.converter.Format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
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
                          "|(?<fieldWithValue>(?<field2>(public|private|protected)[\\w\\s\\n<>?,]+)=[\\w\\s\\n@#$%^&*`<>?,.:;{}()\"']+;))"
                    ).matcher(textFromFile);

            List<Map<String, String>> listOfMapsOfFieldOrMethodComponents = new ArrayList<>();

            matcherOfJsonViewAnnotation.results().forEach(jsonViewAnnotation -> {

                Map<String, String> mapOfFieldOrMethodComponents = new HashMap<>();

                if (matcherOfMethodOrField.find(jsonViewAnnotation.start())) {

                    String description = "";

                    StringBuilder descriptionStringBuilder = new StringBuilder(textFromFile.substring(0, jsonViewAnnotation.start())).reverse();

                    Matcher matcher = Pattern.compile("\n\\s*\n").matcher(descriptionStringBuilder.toString());

                    if (matcher.find()) {
                        descriptionStringBuilder = new StringBuilder(descriptionStringBuilder.substring(0, matcher.end())).reverse();

                        Matcher matcherOfDescription = Pattern.compile("/\\*\\*(?<desc>[\\n\\W\\w]+)\\*/")
                                .matcher(descriptionStringBuilder.toString());

                        if (matcherOfDescription.find()) {
                            descriptionStringBuilder = new StringBuilder(matcherOfDescription.group("desc").trim());

                            for (int i = 0; i < descriptionStringBuilder.length(); i++) {
                                if (descriptionStringBuilder.charAt(i) == '*') {
                                    descriptionStringBuilder.deleteCharAt(i);
                                    i--;
                                }
                            }

                            description = descriptionStringBuilder.toString();
                        }
                    }

                    String method = matcherOfMethodOrField.group("method");
                    String methodName = matcherOfMethodOrField.group("methodName");

                    String field = matcherOfMethodOrField.group("field1");
                    String fieldWithValue = matcherOfMethodOrField.group("field2");

                    if (method != null) {
                        putComponentsOfMethodIntoMap(mapOfFieldOrMethodComponents, method, methodName, textFromFile, description);
                    }
                    else if (fieldWithValue != null) {
                        putComponentsOfFieldIntoMap(mapOfFieldOrMethodComponents, fieldWithValue, textFromFile, description);
                    }
                    else if (field != null) {
                        putComponentsOfFieldIntoMap(mapOfFieldOrMethodComponents, field, textFromFile, description);
                    }
                }

                for (Map<String, String> map : listOfMapsOfFieldOrMethodComponents) {
                    if (map.get("name").equals(mapOfFieldOrMethodComponents.get("name"))
                            && map.get("isMethod").equals("false") && mapOfFieldOrMethodComponents.get("isMethod").equals("true")) {
                        mapOfFieldOrMethodComponents.put("desc", map.get("desc"));
                    }
                }

                listOfMapsOfFieldOrMethodComponents.add(mapOfFieldOrMethodComponents);
            });

            mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.put(fileName, listOfMapsOfFieldOrMethodComponents);
        }

        addFieldOrMethodComponentsIfExtends(mapOfFileNameAndTextFromFile, mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);

        removeIfEmpty(mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);

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

        Matcher matcherOfParentClass = Pattern.compile("class[\\s\\n]+\\w+[\\s\\n]+extends[\\s\\n]+(?<parentClass>[\\w\\s\\n,]+)")
                .matcher(textFromFile);
        Matcher matcherOfParentClassWithImplements = Pattern
                .compile("class[\\s\\n]+\\w+[\\s\\n]+extends[\\s\\n]+(?<parentClass>[\\w\\s\\n,]+)implements").matcher(textFromFile);

        String parentClasses = null;

        if (matcherOfParentClassWithImplements.find()) {
            parentClasses = matcherOfParentClassWithImplements.group("parentClass");
        }
        else if (matcherOfParentClass.find()) {
            parentClasses = matcherOfParentClass.group("parentClass");
        }

        return parentClasses;
    }

    private void removeIfEmpty(Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents) {
        for (String fileName : mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.keySet()) {
            if (mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.get(fileName).isEmpty()) {
                mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.remove(fileName);
            }
        }
    }

    private void putComponentsOfFieldIntoMap(Map<String, String> mapOfFieldOrMethodComponents, String field, String textFromFile, String description) {
        mapOfFieldOrMethodComponents.put("isMethod", "false");

        StringBuilder fieldStringBuilder = new StringBuilder(field.trim()).reverse();

        String name = new StringBuilder(fieldStringBuilder.substring(0, fieldStringBuilder.indexOf(" "))).reverse().toString();

        mapOfFieldOrMethodComponents.put("name", name);

        fieldStringBuilder = new StringBuilder(fieldStringBuilder.delete(0, name.length() + 1).toString().trim());

        Matcher matcherOfTypeWithGeneric = Pattern.compile("\\s\\w+\\s*<[\\w\\s\\n<>?,]*>").matcher(fieldStringBuilder.reverse().toString());

        if (matcherOfTypeWithGeneric.find()) {
            mapOfFieldOrMethodComponents.put("type", getType(matcherOfTypeWithGeneric.group().trim(), textFromFile));
        } else {
            mapOfFieldOrMethodComponents.put("type",
                    getType(new StringBuilder(fieldStringBuilder.reverse().substring(0, fieldStringBuilder.indexOf(" "))).reverse().toString(), textFromFile));
        }

        mapOfFieldOrMethodComponents.put("desc", description);
    }

    private void putComponentsOfMethodIntoMap(Map<String, String> mapOfFieldOrMethodComponents,
                                              String method, String methodName, String textFromFile, String description) {
        mapOfFieldOrMethodComponents.put("isMethod", "true");

        mapOfFieldOrMethodComponents.put("name", getNameOfMethod(methodName));

        StringBuilder methodStringBuilder = new StringBuilder(method).reverse();

        methodStringBuilder = new StringBuilder(methodStringBuilder.delete(0, methodName.length()).toString().trim());

        Matcher matcherOfTypeWithGeneric = Pattern.compile("\\s\\w+\\s*<[\\w\\s\\n<>?,]*>").matcher(methodStringBuilder.reverse().toString());

        if (matcherOfTypeWithGeneric.find()) {
            mapOfFieldOrMethodComponents.put("type", getType(matcherOfTypeWithGeneric.group().trim(), textFromFile));
        } else {
            mapOfFieldOrMethodComponents.put("type", getType(
                    new StringBuilder(methodStringBuilder.reverse().substring(0, methodStringBuilder.indexOf(" "))).reverse().toString(), textFromFile));
        }

        mapOfFieldOrMethodComponents.put("desc", description);
    }

    private String getNameOfMethod(String nameMethod) {
        Matcher matcherOfNameMethod = Pattern.compile("get(?<name>\\w+)\\s*\\([\\w\\s\\n<>?,]*\\)").matcher(nameMethod);

        if (matcherOfNameMethod.find()) {
            return Character.toLowerCase(matcherOfNameMethod.group("name").charAt(0))
                    + matcherOfNameMethod.group("name").substring(1);
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

        Matcher matcherOfPackageName = Pattern.compile("import(?<packageName>[\\s\\n]+[\\w\\s\\n.]+[\\s\\n.]+" + type + "[\\s\\n]*);")
                .matcher(textFromFile);

        if (matcherOfPackageName.find()) {
            return matcherOfPackageName.group("packageName").trim();
        } else {
            return type;
        }
    }

    private void searchFiles(File dir, Map<String, String> mapOfFileNameAndTextFromFile) throws IOException {
        BasicFileAttributes fileAttributes = Files.readAttributes(dir.toPath(), BasicFileAttributes.class);

        if (fileAttributes.isDirectory()) {
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
        } else if (fileAttributes.isRegularFile()) {
            String filePath = dir.getPath();

            Matcher matcherOfSrcDir = Pattern.compile("/src/main/java").matcher(filePath);
            if (matcherOfSrcDir.find()) {
                filePath = filePath.substring(matcherOfSrcDir.end());
            }

            mapOfFileNameAndTextFromFile.put(filePath, Files.readString(dir.toPath()));
        }
    }
}