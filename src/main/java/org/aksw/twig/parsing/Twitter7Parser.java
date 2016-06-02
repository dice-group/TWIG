package org.aksw.twig.parsing;

import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Class that handles parsing of Twitter7 data files.
 * @author Felix Linker
 */
public final class Twitter7Parser implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7Parser.class);

    private static final long MODEL_MAX_SIZE = 10000;

    private File fileToParse;

    /**
     * Creates a new instance setting class variables.
     * @param fileToParse File to be parsed by the parser.
     */
    public Twitter7Parser(File fileToParse) {
        this.fileToParse = fileToParse;
    }

    @Override
    public void run() {
        Twitter7ResultCollector collector = new Twitter7ResultCollector(MODEL_MAX_SIZE);
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

    private static final Pattern ARG_PREF_IN = Pattern.compile("--in=(.*)");
    private static final Pattern ARG_PREF_REC= Pattern.compile("--rec=(.*)");

    /**
     * Gets all files from a directory.
     * @param f Directory to search.
     * @param allowRecursion {@code true} if you want to look for files in sub-folders, too.
     * @return Stream of all files.
     */
    private static Stream<File> getFiles(File f, boolean allowRecursion) {
        Stream<File> fileStream = Stream.empty();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(f.toPath())) {
            for (Path iteratePath: stream) {
                File file = iteratePath.toFile();

                if (file.isDirectory()) {
                    if (allowRecursion) {
                        fileStream = Stream.concat(fileStream, getFiles(file, true));
                    }
                } else {
                    fileStream = Stream.concat(fileStream, Stream.of(file));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't read path {}", f.getPath());
        }

        return fileStream;
    }

    /**
     * Parses one or more files according to twitter7 format. Following arguments in arbitrary order can be passed:
     * <ul>
     *     <li>{@code --in=FOLDER} parses all files in given folder</li>
     *     <li>{@code --rec=FOLDER} parses all files in given folder including all sub-folders</li>
     *     <li>{@code PATH} parses given file</li>
     * </ul>
     * @param args One or more arguments as specified above.
     * @see Twitter7Reader
     */
    public static void main(String[] args) {

        if (args.length == 0) {
            LOGGER.info("No arguments given.");
            return;
        }

        ExecutorService service = Executors.newCachedThreadPool();
        Arrays.stream(args)
                .flatMap(arg -> {

                    Matcher matcher = ARG_PREF_IN.matcher(arg);
                    if (matcher.find()) {
                        return getFiles(new File(matcher.group()), true);
                    }

                    matcher = ARG_PREF_REC.matcher(arg);
                    if (matcher.find()) {
                        return getFiles(new File(matcher.group()), false);
                    }

                    return Stream.of(new File(arg));
                })
                .forEach(file -> service.execute(new Twitter7Parser(file)));
    }
}
