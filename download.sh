#!/bin/bash
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
mkdir mimic
mkdir data
cd data
# 4. 
wget http://hobbitdata.informatik.uni-leipzig.de/TWIG/data.tar.gz
tar -xzf data.tar.gz
rm data.tar.gz
# 5. 
cd ../
./mimic.sh
