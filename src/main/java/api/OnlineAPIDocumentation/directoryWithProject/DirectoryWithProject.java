package api.OnlineAPIDocumentation.directoryWithProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DirectoryWithProject {
    private final static String srcDirPath = "/src/main/java";

    private final String pathToDir;

    public DirectoryWithProject(String pathToDir) {
        this.pathToDir = pathToDir;
    }

    public Map<String, String> getMapOfFileNameAndTextFromFile() throws IOException {
        File directory = new File(pathToDir);

        Map<String, String> mapOfFileNameAndTextFromFile = new HashMap<>();

        searchFiles(directory, mapOfFileNameAndTextFromFile);

        return mapOfFileNameAndTextFromFile;
    }

    private void searchFiles(File dir, Map<String, String> mapOfFileNameAndTextFromFile) throws IOException {
        BasicFileAttributes fileAttributes = Files.readAttributes(dir.toPath(), BasicFileAttributes.class);

        if (fileAttributes.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isDirectory()) {
                    searchFiles(file, mapOfFileNameAndTextFromFile);
                } else {
                    mapOfFileNameAndTextFromFile.put(getFilePath(file), Files.readString(file.toPath()));
                }
            }
        }
        else if (fileAttributes.isRegularFile()) {
            mapOfFileNameAndTextFromFile.put(getFilePath(dir), Files.readString(dir.toPath()));
        }
    }

    private String getFilePath(File file) {
        String filePath = file.getPath();

        Matcher matcherOfSrcDir = Pattern.compile(srcDirPath).matcher(filePath);
        if (matcherOfSrcDir.find()) {
            filePath = filePath.substring(matcherOfSrcDir.end());
        }

        return filePath;
    }
}
