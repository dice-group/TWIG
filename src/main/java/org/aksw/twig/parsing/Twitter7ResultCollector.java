package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * This class will handle parsed models. It will do so by collecting results of {@link com.google.common.util.concurrent.ListenableFuture} to which this collector has been added.
 * Results will be merged into one {@link TwitterModelWrapper} that is then printed into a file.
 * @author Felix Linker
 */
class Twitter7ResultCollector implements FutureCallback<TwitterModelWrapper> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7ResultCollector.class);

    private TwitterModelWrapper currentModel = new TwitterModelWrapper();

    private long modelMaxSize;

    /**
     * Creates a new instance and sets class variables.
     * @param modelMaxSize Max size of a {@link TwitterModelWrapper#model} to contain. If this size is exceeded by a {@link TwitterModelWrapper#model} it will be written into a file.
     */
    Twitter7ResultCollector(long modelMaxSize) {
        this.modelMaxSize = modelMaxSize;
    }

    @Override
    public synchronized void onSuccess(TwitterModelWrapper result) {

        LOGGER.info("Collected result model.");

        this.currentModel.model.add(result.model);

        if (this.currentModel.model.size() >= this.modelMaxSize) {
            writeModel();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        LOGGER.error(t.getMessage());
    }

    void writeModel() {

        LOGGER.info("Writing result model {}.", this.currentModel);

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("C:/Git/TWIG/RDF/tmp.rdf")))) {
            this.currentModel.model.write(writer, "TURTLE");
            writer.flush();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }

        this.currentModel = new TwitterModelWrapper();
    }
}
