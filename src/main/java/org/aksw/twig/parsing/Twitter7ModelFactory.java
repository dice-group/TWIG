package org.aksw.twig.parsing;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public final class Twitter7ModelFactory {

    public static Model get() {
        return ModelFactory.createDefaultModel();
    }
}
