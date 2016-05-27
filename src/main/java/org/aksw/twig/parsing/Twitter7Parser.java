package org.aksw.twig.parsing;

import org.apache.jena.rdf.model.Model;

import java.io.File;
import java.io.IOException;

public final class Twitter7Parser {

    private static final long MODEL_MAX_SIZE = 10000;

    public static void parseFile(String path) throws IOException {
        parseFile(new File(path));
    }

    public static void parseFile(File file) throws IOException {
        Twitter7ResultCollector collector = new Twitter7ResultCollector(MODEL_MAX_SIZE);
        Twitter7Reader<Model> reader = new Twitter7Reader<>(file, () -> collector, Twitter7BlockParser::new);
    }
}
