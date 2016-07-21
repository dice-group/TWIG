package org.aksw.twig.files;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;


/**
 * An instance of this class handles creation of new files.
 * It also has some static functions that are generally useful for file handling.
 */
public class FileHandler {

    private static final Logger LOGGER = LogManager.getLogger(FileHandler.class);

    private int id = 0;

    private int maxAttempts = 100;

    private File outputDirectory;

    private String fileName;

    private String fileType;

    /**
     * Creates a new file handler.
     * @param outputDirectory Directory to output the files into.
     * @param fileName Basic file name.
     * @param fileType File type (including '.').
     */
    public FileHandler(File outputDirectory, String fileName, String fileType) {
        this.outputDirectory = outputDirectory;
        this.fileName = fileName;
        this.fileType = fileType;
    }

    /**
     * Creates a new file handler.
     * @param outputDirectory Directory to output the files into.
     * @param fileName Basic file name.
     * @param fileType File type (including '.').
     * @param maxAttempts How many times should the file handler attempt to create a new file?
     */
    public FileHandler(File outputDirectory, String fileName, String fileType, int maxAttempts) {
        this(outputDirectory, fileName, fileType);
        this.maxAttempts = maxAttempts;
    }

    /**
     * Creates a new file to write into. File will be name like: {@code NAME_NUMBER.TYPE}.
     * If creation of a new file fails, it will try {@link #maxAttempts} times to create a new one.
     * @return New file.
     * @throws IOException Thrown if no new file could be created.
     */
    public File nextFile() throws IOException {

        int attempt = 0;
        File writeFile = incrementFile();

        while (writeFile.exists() && attempt < maxAttempts) {
            writeFile = incrementFile();
            attempt++;
        }

        if (attempt >= maxAttempts) {
            throw new FileNotFoundException("Couldn't resolve new file.");
        }

        while (!writeFile.exists() && attempt < maxAttempts) {
            try {
                writeFile.createNewFile();
            } catch (IOException e) {
                attempt++;
                continue;
            }
        }

        if (attempt >= maxAttempts) {
            throw new FileNotFoundException("Couldn't resolve new file.");
        }

        return writeFile;
    }

    /**
     * Creates a new file without any checks.
     * @return New file.
     */
    private File incrementFile() {
        return new File(outputDirectory, this.fileName.concat("_").concat(Integer.toString(id++)).concat(fileType));
    }

    /**
     * Gets all files from a directory.
     * @param f Directory to search.
     * @param allowRecursion {@code true} if you want to look for files in sub-folders, too.
     * @return Stream of all files.
     */
    public static Stream<File> getFiles(File f, boolean allowRecursion) {
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
     * Reads an output directory and a set of read files from a string array.
     * Following argument must be stated first:
     * <ul>
     *     <li>{@code --out=DIRECTORY} defines output directory.</li>
     * </ul>
     * After that following arguments in arbitrary order can be passed:
     * <ul>
     *     <li>{@code --in=DIRECTORY} will add all files in given directory to the set of read files.</li>
     *     <li>{@code --rec=DIRECTORY} will add all files in given directory including all sub-directories to the set of read files.</li>
     *     <li>{@code PATH} will add given file to the set of read files.</li>
     * </ul>
     * @param args One or more arguments as specified above.
     * @return Pair of output directory (left value) and set of read files (right value).
     */
    public static Pair<File, Set<File>> readArgs(String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Too few arguments given.");
        }

        Matcher outDirectoryMatcher = ARG_PREF_OUT.matcher(args[0]);
        File outputDirectory = outDirectoryMatcher.find() ? new File(outDirectoryMatcher.group(1)) : null;

        if (outputDirectory == null || !outputDirectory.isDirectory()) {
            throw new IllegalArgumentException("No --out argument given.");
        }

        // Get all files to parse
        Set<File> filesToParse = new HashSet<>();
        for (int i = 1; i < args.length; i++) {
            Matcher matcher = ARG_PREF_IN.matcher(args[i]);
            if (matcher.find()) {
                filesToParse.addAll(FileHandler.getFiles(new File(matcher.group(1)), true).collect(Collectors.toList()));
                continue;
            }

            matcher = ARG_PREF_REC.matcher(args[i]);
            if (matcher.find()) {
                filesToParse.addAll(FileHandler.getFiles(new File(matcher.group(1)), false).collect(Collectors.toList()));
                continue;
            }

            filesToParse.add(new File(args[i]));
        }

        return new ImmutablePair<>(outputDirectory, filesToParse);
    }

    /**
     * Creates a {@link BufferedReader} that decodes a file by recognizing its file ending. Possible values:
     * <ul>
     *     <li>.gz for GZip-compression</li>
     *     <li>.zip for Zip-compression</li>
     * </ul>
     * @param file File to read.
     * @return Reader that decodes the file.
     * @throws IOException Thrown during reader creation.
     */
    public static BufferedReader getDecodingReader(File file) throws IOException {
        InputStream fileStream = new FileInputStream(file);

        String[] split = file.getName().split(".");
        switch (split[split.length - 1]) {
            case "gz": fileStream = new GZIPInputStream(fileStream); break;
            case "zip": fileStream = new ZipInputStream(fileStream); break;
        }

        Reader decoder = new InputStreamReader(fileStream);
        return new BufferedReader(decoder);
    }
}
