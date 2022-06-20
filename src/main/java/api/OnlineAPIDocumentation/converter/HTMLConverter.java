package api.OnlineAPIDocumentation.converter;

import api.OnlineAPIDocumentation.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class HTMLConverter implements Converter {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public String convert(Map<String, List<Map<String, String>>> mapOfFileNameAndListOfMapsOfFieldOrMethodComponents) {
        Context context = new Context();
        context.setVariable("mapOfFileNameAndListOfMapsOfFieldOrMethodComponents", mapOfFileNameAndListOfMapsOfFieldOrMethodComponents);

        FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        File file = new File("OnlineAPIDocumentation.html");

        try (Writer writer = new FileWriter(file)) {
            File defaultHTMLTemplate = new File("src/main/resources/defaultHTMLTemplate.html");
            File defaultHTMLTemplateIfJar = new File("classes/defaultHTMLTemplate.html");

            if (defaultHTMLTemplate.exists()) {
                writer.write(templateEngine.process(defaultHTMLTemplate.getPath(), context));
            } else if (defaultHTMLTemplateIfJar.exists()) {
                writer.write(templateEngine.process(defaultHTMLTemplateIfJar.getPath(), context));
            }
        } catch (IOException e) {
            logger.error("Cannot write to " + file.getAbsolutePath() + " file", e);

            System.exit(1);
        } catch (TemplateEngineException e) {
            logger.error("Exception processing template", e);

            System.exit(1);
        }

        if (file.exists()) {
            return "The " + file.getAbsolutePath() + " file was created successfully";
        }

        return null;
    }
}
