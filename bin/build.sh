#!/bin/bash

cd ..

mvn clean compile package

rm -r sample/{analysis,data,output}

mkdir -p sample/{analysis,data,output}

