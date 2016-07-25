package org.aksw.twig.misc;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiscTest implements Callable<Void> {

    public final Model model = ModelFactory.createDefaultModel();

    public MiscTest() {
        if (model == null) {
            throw new ExceptionInInitializerError();
        }
    }

    @Override
    public Void call() throws Exception {
        new MiscTest();
        System.getProperties().propertyNames();
        return null;
    }

    @Test
    public void test() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Callable<Void>> callables = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            callables.add(new MiscTest());
        }
        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }
}
