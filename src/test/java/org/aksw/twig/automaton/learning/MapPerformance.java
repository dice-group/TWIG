package org.aksw.twig.automaton.learning;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MapPerformance {

    private static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger(MapPerformance.class);

    @Test
    public void testMaps() {
        testMapPerformance(HashMap::new);
        testMapPerformance(TreeMap::new);
    }

    private static final int TEST_COUNT = 10;
    private static final int TESTS_PER_MAP_COUNT = 1000000;

    public void testMapPerformance(Supplier<Map<String, Double>> mapSupplier) {
        for (int i = 0; i < TEST_COUNT; i++) {
            long start = System.currentTimeMillis();
            Map<String, Double> mapToTest = mapSupplier.get();

            for (int j = 0; j < TESTS_PER_MAP_COUNT; j++) {
                fillMap(mapToTest);
            }

            mapToTest.keySet().stream()
                    .forEach(key -> mapToTest.get(key));

            LOGGER.info("Map test took {} milliseconds to complete. Map was of type {}.", System.currentTimeMillis() - start, mapToTest.getClass().getName());
        }
    }

    private static final int STRING_LENGTH = 10;
    private void fillMap(Map<String, Double> map) {
        String key = RandomStringUtils.random(STRING_LENGTH);
        map.put(key, Math.random());
    }
}
