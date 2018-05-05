#!/bin/bash

export MAVEN_OPTS="-Xmx50G"

ARGS="Automaton data/word_matrix_1.obj data/message_count_0.obj data/time_count_0.obj 10 14 2009-09-29 1 mimic"

nohup mvn exec:java -Dexec.mainClass="org.aksw.twig.Main" -Dexec.args="$ARGS" > mimic.log &