cd hash
javac -cp .:lib/java-getopt-1.0.13.jar IteratorTest.java
cd ubst
javac -cp .:lib/java-getopt-1.0.13.jar IteratorTest.java
python runTests.py
gnuplot plot.gp.sh 

