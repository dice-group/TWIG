package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Twitter7RDFBuilder implements FutureCallback<String> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7RDFBuilder.class);

    @Override
    public synchronized void onSuccess(String result) {

    }

    @Override
    public void onFailure(Throwable t) {
        LOGGER.error(t.getMessage());
    }
}
