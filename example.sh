
# Each command follows the pattern: java -jar JAR-PATH MAIN-CLASS-NAME ARGS...

java -jar /home/TWIG/target/twig-parent-0.0.1-SNAPSHOT.jar Twitter7Parser --out=/home/TWIG_data /home/data/tweets.txt.gz

java -jar /home/TWIG/target/twig-parent-0.0.1-SNAPSHOT.jar MessageCounterHandler --out=/home/TWIG_data_analysis --in=/home/TWIG_data

java -jar /home/TWIG/target/twig-parent-0.0.1-SNAPSHOT.jar TimeCounterHandler --out=/home/TWIG_data_analysis --in=/home/TWIG_data

java -jar /home/TWIG/target/twig-parent-0.0.1-SNAPSHOT.jar WordMatrixHandler --out=/home/TWIG_data_analysis --in=/home/TWIG_data

# First three arguments are serialized objects resulting from calls in line 6, 8 and 10.
# 50 -> number of users
# 60 -> period to simulate in days
# 2010-01-01 -> start date of simulation, i. e. it will be simulated from 2010-01-01 to 2010-02-03
# 1 -> seed value
# /home/TWIG_output -> Folder for results
java -jar /home/TWIG/target/twig-parent-0.0.1-SNAPSHOT.jar Automaton /home/TWIG_data_analysis/word_matrix_0.obj /home/TWIG_data_analysis/message_count_0.obj /home/TWIG_data_analysis/time_count_0.obj 50 60 2010-01-01 1 /home/TWIG_output
