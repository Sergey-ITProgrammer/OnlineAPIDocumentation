package api.OnlineAPIDocumentation;

import api.OnlineAPIDocumentation.converter.Format;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
//        String pathToFile = "";
//        String format = "json";
//
//        Format formatEnum;
//        switch (format) {
//            default:
//                formatEnum = Format.json;
//        }

        try {
            Parser parser = new Parser("/home/sergey/Desktop/ex.java", Format.json);

            System.out.println(parser.parse());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
