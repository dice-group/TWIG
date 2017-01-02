#!/bin/bash

# create models
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar MessageCounterHandler --out=sample/analysis --in=sample/data
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar TimeCounterHandler --out=sample/analysis --in=sample/data
java -jar target/twig-parent-0.0.1-SNAPSHOT.jar WordMatrixHandler --out=sample/analysis --in=sample/data
