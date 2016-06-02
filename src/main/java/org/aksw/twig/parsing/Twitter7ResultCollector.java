package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class will handle parsed models. It will do so by collecting results of {@link com.google.common.util.concurrent.ListenableFuture} to which this collector has been added.
 * Results will be merged into one {@link Model} that is then handed to {@link Twitter7ModelWriter}.
 * @author Felix Linker
 */
public class Twitter7ResultCollector implements FutureCallback<TwitterModelWrapper> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7ResultCollector.class);

    private TwitterModelWrapper currentModel = new TwitterModelWrapper();

    private Twitter7ModelWriter writer = new Twitter7ModelWriter();

    private long modelMaxSize;

    /**
     * Creates a new instance and sets class variables.
     * @param modelMaxSize Max size of a {@link Model} to contain. If this size is exceeded by a {@link Model} it will be written into a file.
     */
    public Twitter7ResultCollector(long modelMaxSize) {
        this.modelMaxSize = modelMaxSize;
    }

    @Override
    public synchronized void onSuccess(TwitterModelWrapper result) {
        this.currentModel.model.add(result.model);

        if (this.currentModel.model.size() >= modelMaxSize) {
            writer.write(this.currentModel);
            this.currentModel = new TwitterModelWrapper();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        LOGGER.error(t.getMessage());
    }
}
