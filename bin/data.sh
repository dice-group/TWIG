#!/bin/bash

cd ..

#parse data
java -jar target/twig-parent-0.0.4-SNAPSHOT.jar Twitter7Parser --out=sample/data sample/sample.txt.gz
