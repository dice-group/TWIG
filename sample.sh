#!/bin/bash

rm -r sample/{analysis,data,output}

mkdir -p sample/{analysis,data,output}

#parse data
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar Twitter7Parser --out=sample/data sample/sample.txt.gz

# create models
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar MessageCounterHandler --out=sample/analysis --in=sample/data
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar TimeCounterHandler --out=sample/analysis --in=sample/data
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar WordMatrixHandler --out=sample/analysis --in=sample/data

# First three arguments are serialized objects resulting from calls in line 6, 8 and 10.
# 2 -> number of users
# 2 -> period to simulate in days
# 2010-01-01 -> start date of simulation, i. e. it will be simulated from 2010-01-01 to 2010-02-03
# 1 -> seed value
# sample/output -> Folder for results
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar Automaton sample/analysis/word_matrix_0.obj sample/analysis/message_count_0.obj sample/analysis/time_count_0.obj 10 30 2009-09-29 1 sample/output