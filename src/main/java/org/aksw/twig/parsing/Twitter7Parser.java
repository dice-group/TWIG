package org.aksw.twig.parsing;

import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Class that handles parsing of Twitter7 data files.
 * @author Felix Linker
 */
public final class Twitter7Parser {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7Parser.class);

    private static final long MODEL_MAX_SIZE = 10000;

    /**
     * Parses a file that contains twitter7 data.
     * @param path Path to file to parse.
     * @throws IOException Thrown if file is not readable. Will be thrown only upon read stream creation.
     */
    public static void parseFile(String path) throws IOException {
        parseFile(new File(path));
    }

    /**
     * Parses a file that contains twitter7 data.
     * @param file File to parse.
     * @throws IOException Thrown if file is not readable. Will be thrown only upon read stream creation.
     */
    public static void parseFile(File file) throws IOException {

        LOGGER.info("Parsing file {} for twitter7 data.", file.getPath());

        Twitter7ResultCollector collector = new Twitter7ResultCollector(MODEL_MAX_SIZE);
        Twitter7Reader<TwitterModelWrapper> reader = new Twitter7Reader<>(file, () -> collector, Twitter7BlockParser::new);

        reader.run();
        while (!reader.isFinished());
        collector.writeModel();
    }
}
