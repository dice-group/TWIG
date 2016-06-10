package org.aksw.twig.parsing;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class that handles parsing of Twitter7 data files.
 * @author Felix Linker
 */
public final class Twitter7Parser implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7Parser.class);

    private static final long MODEL_MAX_SIZE = 10000;

    private File fileToParse;

    private File outputDirectory;

    /**
     * Creates a new instance setting class variables.
     * @param fileToParse File to be parsed by the parser.
     * @param outputDirectory Output directory for parsing results.
     */
    public Twitter7Parser(File fileToParse, File outputDirectory) {
        this.fileToParse = fileToParse;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void run() {
        String fileName = this.fileToParse.getName();
        int typeIndex = fileName.lastIndexOf('.');
        fileName = fileName.substring(0, typeIndex);

        Twitter7ResultCollector collector = new Twitter7ResultCollector(MODEL_MAX_SIZE, this.outputDirectory, fileName);
        Twitter7Reader<TwitterModelWrapper> reader;
        try {
            reader = new Twitter7Reader<>(fileToParse, () -> collector, Twitter7BlockParser::new);
        } catch (IOException e) {
            LOGGER.error("Error: exception during parse start for file {} - {}",fileToParse.getPath() , e.getMessage());
            return;
        }

        LOGGER.info("Parsing file {} for twitter7 data.", fileToParse.getPath());
        reader.read();
        while (!reader.isFinished()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) { }
        }

        collector.writeModel();
    }

    /**
     * Parses one or more files according to twitter7 format. Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])}.
     * You should not parse files with the same name from different directories as that could mess up the output.
     * @param args One or more arguments as specified above.
     * @see Twitter7Reader
     */
    public static void main(String[] args) {

        Pair<File, Set<File>> parsedArgs = FileHandler.readArgs(args);
        File outputDirectory = parsedArgs.getLeft();
        Set<File> filesToParse = parsedArgs.getRight();

        // Start parsing
        final ExecutorService service = Executors.newCachedThreadPool();
        filesToParse.stream().forEach(file -> service.execute(new Twitter7Parser(file, outputDirectory)));
        service.shutdown();
    }
}
