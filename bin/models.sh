#!/bin/bash

cd ..

# create models
java -jar target/twig-parent-0.0.4-SNAPSHOT.jar MessageCounterHandler --out=sample/analysis --in=sample/data
java -jar target/twig-parent-0.0.4-SNAPSHOT.jar TimeCounterHandler --out=sample/analysis --in=sample/data
java -jar target/twig-parent-0.0.4-SNAPSHOT.jar WordMatrixHandler --out=sample/analysis --in=sample/data
