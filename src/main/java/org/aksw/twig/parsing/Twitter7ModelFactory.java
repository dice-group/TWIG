package org.aksw.twig.parsing;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;

import java.util.HashMap;
import java.util.Map;

public final class Twitter7ModelFactory {

    public static final String FOAF_IRI = "http://xmlns.com/foaf/0.1/";

    public static final String FOAF_PREF = "foaf";

    public static final String TWIG_IRI = "http://aksw.org/twig";

    public static final String TWIG_PREF = "twig";

    private static final Map<String, String> PREFIXES_MAPPING = new HashMap<>();

    static
    {
        PREFIXES_MAPPING.put(FOAF_PREF, FOAF_IRI);
        PREFIXES_MAPPING.put(TWIG_PREF, TWIG_IRI);
    }

    public static Model createModel() {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefixes(PREFIXES_MAPPING);
        return model;
    }
}
