package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Twitter7ResultCollector implements FutureCallback<Model> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7ResultCollector.class);

    private Model currentModel = Twitter7ModelFactory.createModel();

    private Twitter7ModelWriter writer = new Twitter7ModelWriter();

    private long modelMaxSize;

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
