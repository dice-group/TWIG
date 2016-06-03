package org.aksw.twig.parsing;

import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final Pattern ARG_PREF_IN = Pattern.compile("--in=(.*)");
    private static final Pattern ARG_PREF_REC = Pattern.compile("--rec=(.*)");
    private static final Pattern ARG_PREF_OUT = Pattern.compile("--out=(.*)");

    /**
     * Parses one or more files according to twitter7 format.
     * Following argument must be stated first:
     * <ul>
     *     <li>{@code --out=DIRECTORY} will write parse results in stated output directory.</li>
     * </ul>
     * After that following arguments in arbitrary order can be passed:
     * <ul>
     *     <li>{@code --in=DIRECTORY} parses all files in given directory</li>
     *     <li>{@code --rec=DIRECTORY} parses all files in given directory including all sub-directories</li>
     *     <li>{@code PATH} parses given file</li>
     * </ul>
     * You should not parse files with the same name from different directories as that could mess up the output.
     * @param args One or more arguments as specified above.
     * @see Twitter7Reader
     */
    public static void main(String[] args) {

        if (args.length < 2) {
            LOGGER.error("Insufficient arguments given.");
            return;
        }

        Matcher outDirectoryMatcher = ARG_PREF_OUT.matcher(args[0]);
        File outputDirectory = outDirectoryMatcher.find() ? new File(outDirectoryMatcher.group(1)) : null;

        if (outputDirectory == null || !outputDirectory.isDirectory()) {
            LOGGER.error("No --out argument given.");
        }

        // Get all files to parse
        Set<File> filesToParse = new HashSet<>();
        for (int i = 1; i < args.length; i++) {
            Matcher matcher = ARG_PREF_IN.matcher(args[i]);
            if (matcher.find()) {
                filesToParse.addAll(getFiles(new File(matcher.group(1)), true).collect(Collectors.toList()));
                continue;
            }

            matcher = ARG_PREF_REC.matcher(args[i]);
            if (matcher.find()) {
                filesToParse.addAll(getFiles(new File(matcher.group(1)), false).collect(Collectors.toList()));
                continue;
            }

            filesToParse.add(new File(args[i]));
        }

        // Start parsing
        final ExecutorService service = Executors.newCachedThreadPool();
        filesToParse.stream().forEach(file -> service.execute(new Twitter7Parser(file, outputDirectory)));
        service.shutdown();
    }
}
