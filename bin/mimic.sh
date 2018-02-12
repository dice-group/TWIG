#!/bin/bash
cd ..
# First three arguments are serialized objects resulting from calls in line 6, 8 and 10.
# 10 -> number of users
# 14 -> period to simulate in days
# 2010-01-01 -> start date of simulation, i. e. it will be simulated from 2010-01-01 to 2010-02-03
# 1 -> seed value
# sample/output -> Folder for results
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar Automaton sample/analysis/word_matrix_0.obj sample/analysis/message_count_0.obj sample/analysis/time_count_0.obj 10 14 2009-09-29 1 sample/output