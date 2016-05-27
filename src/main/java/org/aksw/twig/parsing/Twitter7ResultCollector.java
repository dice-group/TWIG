package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class will handle parsed models. It will do so by collecting results of {@link com.google.common.util.concurrent.ListenableFuture} to which this collector has been added.
 * Results will be merged into one {@link Model} that is then handed to {@link Twitter7ModelWriter}.
 */
public class Twitter7ResultCollector implements FutureCallback<Model> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7ResultCollector.class);

    private Model currentModel = Twitter7ModelFactory.createModel();

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
    public synchronized void onSuccess(Model result) {
        this.currentModel.add(result);

        if (this.currentModel.size() >= modelMaxSize) {
            writer.write(this.currentModel);
            this.currentModel = Twitter7ModelFactory.createModel();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        LOGGER.error(t.getMessage());
    }
}
