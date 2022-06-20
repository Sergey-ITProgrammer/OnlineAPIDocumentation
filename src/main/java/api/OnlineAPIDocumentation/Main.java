package api.OnlineAPIDocumentation;

import api.OnlineAPIDocumentation.converter.Format;
import api.OnlineAPIDocumentation.parser.Parser;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String PATH_TO_DIR_OPTION_DESC = "path to directory";
    private static final String WITH_PACKAGE_NAME_OPTION_DESC = "get type with package name or not";
    private static final String FORMAT_OPTION_DESC = "format";
    private static final Option pathToDirOption = new Option("p", "pathToDir", true, PATH_TO_DIR_OPTION_DESC);
    private static final Option withPackageNameOption = new Option("w", "withPackageName", true, WITH_PACKAGE_NAME_OPTION_DESC);
    private static final Option formatOption = new Option("f", "format", true, FORMAT_OPTION_DESC);

    private static String pathToDir = "";
    private static String withPackageName = withPackageNameOption.getValue("false");
    private static String format = formatOption.getValue("json");

    public static void main(String[] args) {
        Options options = getOptions();

        CommandLine commandLine = getCommandLine(options, args);

        if (!commandLine.hasOption("p")) {
            for (Option option : options.getOptions()) {
                System.out.println(option);
            }

            System.exit(2);
        }

        getOptionValuesFromCommandLine(commandLine);

        try {
            Parser parser = new Parser(pathToDir, getWithPackageName(), getFormat());

            System.out.println(parser.parse());
        } catch (IOException e) {
            logger.error("The file on the " + pathToDir + " path is incorrect", e);

            System.exit(1);
        }
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(pathToDirOption);
        options.addOption(withPackageNameOption);
        options.addOption(formatOption);

        return options;
    }

    private static CommandLine getCommandLine(Options options, String[] args) {
        CommandLineParser commandLineParser = new DefaultParser();

        CommandLine commandLine = null;
        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (ParseException e) {
            logger.error("Cannot parse this args", e);

            System.exit(1);
        }

        return commandLine;
    }

    private static void getOptionValuesFromCommandLine(CommandLine commandLine) {
        if (commandLine.hasOption("p")) {
            pathToDir = commandLine.getOptionValue(pathToDirOption);
        }
        if (commandLine.hasOption("w")) {
            withPackageName = commandLine.getOptionValue(withPackageNameOption);
        }
        if (commandLine.hasOption("f")) {
            format = commandLine.getOptionValue(formatOption);
        }
    }

    private static Format getFormat() {
        Format formatEnum;

        switch (format) {
            case "html":
                formatEnum = Format.HTML;
                break;
            default:
                formatEnum = Format.JSON;
        }

        return formatEnum;
    }

    private static Boolean getWithPackageName() {
        return withPackageName.equalsIgnoreCase("true");
    }
}
