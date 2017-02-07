# compile the data-structure codes
#cd hash
#javac -cp .:lib/java-getopt-1.0.13.jar IteratorTest.java
#cd ../ubst
#javac -cp .:lib/java-getopt-1.0.13.jar IteratorTest.java
#cd ../linked_list
#javac -cp .:lib/java-getopt-1.0.13.jar IteratorTest.java
#cd ..

lscpu > lscpu_info.txt
cat /proc/cpuinfo > cpuinfo_info.txt

make -f Makefile

mkdir -p Output

# create a folder with the current time in the Output directory
currentDate=`date +"%Y-%m-%d-%H-%M"`
mkdir Output/$currentDate/original_plots
chmod 777 Output/$currentDate/original_plots

# run the tests
python runTests.py
#create the plots
gnuplot -e "outputDir='Output/$currentDate/original_plots'" plot_updater_com.gp.sh
gnuplot -e "outputDir='Output/$currentDate/original_plots'" plot_updater.gp.sh
gnuplot -e "outputDir='Output/$currentDate/original_plots'" plot_iterator_com.gp.sh
gnuplot -e "outputDir='Output/$currentDate/original_plots'" plot_iterator.gp.sh

# create data for plots for paper
sh ./parse.sh

# create plots for paper
gnuplot -e "outputDir='Output/$currentDate'" plot_updater_com_paper.gp.sh
gnuplot -e "outputDir='Output/$currentDate/'" plot_updater_paper.gp.sh
gnuplot -e "outputDir='Output/$currentDate/'" plot_iterator_com_paper.gp.sh
gnuplot -e "outputDir='Output/$currentDate/'" plot_iterator_paper.gp.sh

mv parse_updater_output.txt parse_updater_com_output.txt parse_iterator_output.txt parse_iterator_com_output.txt Output/$currentDate

# move the output to the correct directory in Output
mv config.txt output.txt output_verbose.txt lscpu_info.txt cpuinfo_info.txt error.txt Output/original_plots/$currentDate


