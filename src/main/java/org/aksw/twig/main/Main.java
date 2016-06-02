package org.aksw.twig.main;

import org.aksw.twig.parsing.Twitter7Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public final class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.error("No file given");
        }

        try {
            Twitter7Parser.parseFile(ClassLoader.getSystemResource(args[0]).getPath());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
}
