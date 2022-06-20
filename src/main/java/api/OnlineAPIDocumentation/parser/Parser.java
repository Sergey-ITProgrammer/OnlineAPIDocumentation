package api.OnlineAPIDocumentation.parser;

import api.OnlineAPIDocumentation.Main;
import api.OnlineAPIDocumentation.converter.ConverterFactory;
import api.OnlineAPIDocumentation.converter.Format;
import api.OnlineAPIDocumentation.directoryWithProject.DirectoryWithProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final String pathToDir;
    private final boolean withPackageName;
    private final Format format;

    public Parser(String pathToFile, boolean withPackageName, Format format) {
        this.pathToDir = pathToFile;
        this.withPackageName = withPackageName;
        this.format = format;
    }

    public String parse() throws IOException {
        Map<String, String> mapOfFileNameAndTextFromFile = new DirectoryWithProject(pathToDir).getMapOfFileNameAndTextFromFile();

        Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents
                = getMapOfFileNameAndListOfMapsOfFieldOrMethodComponents(mapOfFileNameAndTextFromFile);

        ConverterFactory converter = new ConverterFactory();

        logger.info("The converting was completed successfully");

        return converter.convert(mapOfFileNameAndListOfMapsOfFieldOrMethodComponents, format);
    }

    private Map<String, List<Map<String, String>>> getMapOfFileNameAndListOfMapsOfFieldOrMethodComponents(Map<String, String> mapOfFileNameAndTextFromFile) {
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

            matcherOfJsonViewAnnotation.results().forEach(jsonViewAnnotationMatchResult -> {

                Map<String, String> mapOfFieldOrMethodComponents = new HashMap<>();

                if (matcherOfMethodOrField.find(jsonViewAnnotationMatchResult.start())) {
                    String description = getFieldOrMethodOrClassDescription(textFromFile, jsonViewAnnotationMatchResult.start());

                    String method = matcherOfMethodOrField.group("method");
                    String methodName = matcherOfMethodOrField.group("methodName");

                    String field = matcherOfMethodOrField.group("field1");
                    String fieldWithValue = matcherOfMethodOrField.group("field2");

                    if (method != null) {
                        putMethodComponentsIntoMap(mapOfFieldOrMethodComponents, method, methodName, textFromFile, description);

                        putDescriptionIfMethodIsGetter(listOfMapsOfFieldOrMethodComponents, mapOfFieldOrMethodComponents);
                    }
                    else if (fieldWithValue != null) {
                        putFieldComponentsIntoMap(mapOfFieldOrMethodComponents, fieldWithValue, textFromFile, description);
                    }
                    else if (field != null) {
                        putFieldComponentsIntoMap(mapOfFieldOrMethodComponents, field, textFromFile, description);
                    }
                }

                listOfMapsOfFieldOrMethodComponents.add(mapOfFieldOrMethodComponents);
            });

            mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.put(fileName, listOfMapsOfFieldOrMethodComponents);
        }

        addFieldOrMethodComponentsIfClassExtends(mapOfFileNameAndTextFromFile, mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);

        removeIfNoFieldsOrMethodsWithJsonViewAnnotation(mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);

        putClassDescription(mapOfFileNameAndTextFromFile, mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);

        return mapOfFileNameAndListOfMapsOfFieldOrMethodComponents;
    }

    private void putFieldComponentsIntoMap(Map<String, String> mapOfFieldOrMethodComponents, String field, String textFromFile, String description) {
        mapOfFieldOrMethodComponents.put("isMethod", "false");

        StringBuilder fieldStringBuilder = new StringBuilder(field.trim()).reverse();

        String name = new StringBuilder(fieldStringBuilder.substring(0, fieldStringBuilder.indexOf(" "))).reverse().toString();

        mapOfFieldOrMethodComponents.put("name", name);

        fieldStringBuilder = new StringBuilder(fieldStringBuilder.delete(0, name.length() + 1).toString().trim());

        mapOfFieldOrMethodComponents.put("type", getFieldOrMethodType(fieldStringBuilder, textFromFile));

        mapOfFieldOrMethodComponents.put("desc", description);
    }

    private void putMethodComponentsIntoMap(Map<String, String> mapOfFieldOrMethodComponents,
                                            String method, String methodName, String textFromFile, String description) {
        mapOfFieldOrMethodComponents.put("isMethod", "true");

        mapOfFieldOrMethodComponents.put("name", getMethodName(methodName));

        StringBuilder methodStringBuilder = new StringBuilder(method).reverse();

        methodStringBuilder = new StringBuilder(methodStringBuilder.delete(0, methodName.length()).toString().trim());

        mapOfFieldOrMethodComponents.put("type", getFieldOrMethodType(methodStringBuilder, textFromFile));

        mapOfFieldOrMethodComponents.put("desc", description);
    }

    private String getMethodName(String nameMethod) {
        Matcher matcherOfNameMethod = Pattern.compile("get(?<name>\\w+)\\s*\\([\\w\\s\\n<>?,]*\\)").matcher(nameMethod);

        if (matcherOfNameMethod.find()) {
            return Character.toLowerCase(matcherOfNameMethod.group("name").charAt(0))
                    + matcherOfNameMethod.group("name").substring(1);
        } else {
            return nameMethod;
        }
    }

    private String getFieldOrMethodType(StringBuilder fieldOrMethodStringBuilder, String textFromFile) {
        Matcher matcherOfTypeWithGeneric = Pattern.compile("\\s\\w+\\s*<[\\w\\s\\n<>?,]*>").matcher(fieldOrMethodStringBuilder.reverse().toString());

        String type;
        if (matcherOfTypeWithGeneric.find()) {
            type = matcherOfTypeWithGeneric.group().trim();
        } else {
            type = new StringBuilder(fieldOrMethodStringBuilder.reverse().substring(0, fieldOrMethodStringBuilder.indexOf(" "))).reverse().toString();
        }

        if (withPackageName) {
            return getFieldOrMethodTypeWithPackageName(type, textFromFile);
        }

        return type;
    }

    private String getFieldOrMethodTypeWithPackageName(String value, String textFromFile) {
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

    private String getFieldOrMethodOrClassDescription(String textFromFile, Integer start) {
        String description = "";

        StringBuilder descriptionStringBuilder = new StringBuilder(textFromFile.substring(0, start)).reverse();

        Matcher matcherOfBorderBetweenFields = Pattern.compile("\n\\s*\n").matcher(descriptionStringBuilder.toString());

        if (matcherOfBorderBetweenFields.find()) {
            descriptionStringBuilder = new StringBuilder(descriptionStringBuilder.substring(0, matcherOfBorderBetweenFields.end())).reverse();

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

        return description;
    }

    private void putDescriptionIfMethodIsGetter(List<Map<String, String>> listOfMapsOfFieldOrMethodComponents, Map<String, String> mapOfFieldOrMethodComponents) {
        for (Map<String, String> map : listOfMapsOfFieldOrMethodComponents) {
            if (map.get("name").equals(mapOfFieldOrMethodComponents.get("name")) && map.get("type").equals(mapOfFieldOrMethodComponents.get("type"))
                    && map.get("isMethod").equals("false") && mapOfFieldOrMethodComponents.get("isMethod").equals("true")) {
                mapOfFieldOrMethodComponents.put("desc", map.get("desc"));
            }
        }
    }

    private void addFieldOrMethodComponentsIfClassExtends(Map<String, String> mapOfFileNameAndTextFromFile,
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
        String parentClasses = getParentClasses(mapOfFileNameAndTextFromFile.get(fileName));

        if (parentClasses != null) {
            String[] arrOfParentClass = parentClasses.split(",");

            for (String parentClass : arrOfParentClass) {
                for (String fileName1 : mapOfFileNameAndTextFromFile.keySet()) {
                    if (fileName1.contains(parentClass.trim())) {
                        fieldOrMethodFromParentClasses.addAll(mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.get(fileName1));
                    }

                    String parentClass1 = getParentClasses(mapOfFileNameAndTextFromFile.get(fileName1));

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

    private String getParentClasses(String textFromFile) {
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

    private void removeIfNoFieldsOrMethodsWithJsonViewAnnotation(Map<String,
            List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents) {
        for (String fileName : mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.keySet()) {
            if (mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.get(fileName).isEmpty()) {
                mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.remove(fileName);
            }
        }
    }

    private void putClassDescription(Map<String, String> mapOfFileNameAndTextFromFile,
                                     Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents) {
        for (String fileName : mapOfFileNameAndTextFromFile.keySet()) {
            String textFromFile = mapOfFileNameAndTextFromFile.get(fileName);

            String descriptionOfClass = "";

            Matcher matcherOfClass = Pattern.compile("class[\\s\\n]+\\w+").matcher(textFromFile);

            if (matcherOfClass.find()) {
                descriptionOfClass = getFieldOrMethodOrClassDescription(textFromFile, matcherOfClass.start());
            }

            if (!descriptionOfClass.isEmpty()) {
                mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.put(
                        String.format("%s - %s", fileName, descriptionOfClass), mapOfFileNameAndListOfMapsOfFieldOrMethodComponents.remove(fileName));
            }
        }
    }
}