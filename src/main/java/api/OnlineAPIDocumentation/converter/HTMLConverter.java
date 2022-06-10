package api.OnlineAPIDocumentation.converter;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class HTMLConverter implements Converter {
    @Override
    public String convert(Map<String, List<Map<String, String>>> mapOfNameFileAndListOfMapsOfFieldOrMethodComponents) {
        Context context = new Context();
        context.setVariable("mapOfNameFileAndListOfMapsOfFieldOrMethodComponents", mapOfNameFileAndListOfMapsOfFieldOrMethodComponents);

        FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        File file = new File("OnlineAPIDocumentation.html");

        try (Writer writer = new FileWriter(file)) {
            writer.write(templateEngine.process("src/main/resources/defaultHTMLTemplate.html", context));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (file.exists()) {
            return "The " + file.getName() + " file was created successfully";
        }

        return null;
    }
}
