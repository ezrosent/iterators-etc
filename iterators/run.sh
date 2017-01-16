# compile the data-structure codes
#cd hash
#javac -cp .:lib/java-getopt-1.0.13.jar IteratorTest.java
#cd ../ubst
#javac -cp .:lib/java-getopt-1.0.13.jar IteratorTest.java
#cd ../linked_list
#javac -cp .:lib/java-getopt-1.0.13.jar IteratorTest.java
#cd ..

make -f Makefile

mkdir -p Output

# create a folder with the current time in the Output directory
currentDate=`date +"%Y-%m-%d-%H-%M"`
mkdir Output/$currentDate
chmod 777 Output/$currentDate

# run the tests
python runTests.py
#create the plots
gnuplot -e "outputDir='Output/$currentDate'" plot_mul_iter.gp.sh
gnuplot -e "outputDir='Output/$currentDate'" plot.gp.sh

# move the output to the correct directory in Output
mv config.txt output.txt output_verbose.txt Output/$currentDate


