package org.aksw.twig.automaton.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

public class WordMatrixTest {

  @Test(expected = IllegalArgumentException.class)
  public void emptyTest() {
    new WordMatrix().getChance("a", "a");
  }

  @Test
  public void readWriteTest() {
    final WordMatrix matrix = new WordMatrix();
    prepareMatrix(matrix);
    assertMatrix(matrix);
  }

  @Test
  public void serializeTest() {

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      WordMatrix matrix = new WordMatrix();
      prepareMatrix(matrix);

      // write
      outputStream.writeObject(matrix);

      // read
      final ObjectInputStream inputStream =
          new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
      matrix = (WordMatrix) inputStream.readObject();

      //
      assertMatrix(matrix);
    } catch (IOException | ClassNotFoundException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void truncateTest() {
    final WordMatrix matrix = new WordMatrix();
    prepareMatrix(matrix);
    matrix.truncateTo(1.0);
    Assert.assertTrue(matrix.getPredecessors().isEmpty());
  }

  private void prepareMatrix(final WordMatrix matrix) {
    matrix.alterFrequency("a", "a", 1);
    matrix.alterFrequency("a", "b", 1);
  }

  private void assertMatrix(final WordMatrix matrix) {
    Assert.assertEquals(0.5, matrix.getChance("a", "a"), 0.0);
    Assert.assertEquals(0.5, matrix.getChance("a", "b"), 0.0);

    final Map<String, Double> mappings = matrix.getMappings("a");
    Assert.assertTrue(mappings.containsKey("a"));
    Assert.assertEquals(0.5, mappings.get("a"), 0.0);
    Assert.assertTrue(mappings.containsKey("b"));
    Assert.assertEquals(0.5, mappings.get("b"), 0.0);

    matrix.matrix.entrySet().forEach(entry -> {
      final Pair<Long, Map<String, Long>> value = entry.getValue();
      final Long sum =
          value.getRight().entrySet().stream().map(Map.Entry::getValue).reduce(0L, Long::sum);
      Assert.assertEquals(value.getLeft(), sum);
    });

    Assert.assertEquals(0.5, matrix.getMeanChance(), 0.0);
    Assert.assertEquals(0.0, matrix.getChanceStdDeviation(), 0.0);
  }
}
