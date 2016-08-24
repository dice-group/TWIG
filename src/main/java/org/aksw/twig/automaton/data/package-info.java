/**
 * This contains classes preparing data for usage with {@link org.aksw.twig.automaton.Automaton}. There are three main classes: {@link org.aksw.twig.automaton.data.MessageCounter}, {@link org.aksw.twig.automaton.data.TimeCounter} and {@link org.aksw.twig.automaton.data.WordMatrix}.
 * Those classes will be handed as argument to {@link org.aksw.twig.automaton.Automaton}.<br><br>
 * All classes with {@code Handler} as suffix will create instances of the main classes by parsing files as TWIG models according to {@link org.aksw.twig.model.TWIGModelWrapper}.
 */
package org.aksw.twig.automaton.data;