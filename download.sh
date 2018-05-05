#!/bin/bash
#
# This script will execute the following points:
# 
# 1. Clones the repository 'https://github.com/AKSW/TWIG'
# 2. Executes 'build.sh' to build the application.
# 3. Downloads models to '/TWIG/data'.
# 4. Runs the mimic by executing 'mimic.sh'
#
# Now in sample/output are the mimic data.
#
#################################################################
#
# 1.
git clone https://github.com/AKSW/TWIG.git
# 2.
cd TWIG
./build.sh
# 3. 
mkdir data
cd data
# 4. 
wget ftp://hobbitdata.informatik.uni-leipzig.de/TWIG/datasmall.tar.gz
tar -xzf datasmall.tar.gz
rm datasmall.tar.gz
# 5. 
cd ../
./mimic.sh
