reset 

# uncomment the following lint to get the output on the terminal
# set term dumb

# number of lines in one plot : ie, number of data structures
nDS = 2 # should be 3 when including LL
nIterators = "`head -1 config.txt`"
nUpdaters = "`head -2 config.txt | tail -1`"
nDuration = "`head -3 config.txt | tail -1`"
nWeightConfig = "`head -4 config.txt | tail -1`"
nRange = "`head -5 config.txt | tail -1`"
nLines = 2 #words(nDS) * words(nIterators)

# TODO: fix this. Should automatically be filled
rangeTitle = "[0,4096] [0,16384] [0,65536]"

flineI = 1   # first line iterators
flineD = flineI # first line duration
incI = words(nUpdaters) * words(nDuration) * words(nWeightConfig) * words(nRange)
incU = words(nDuration) * words(nWeightConfig) * words(nRange)
incD = words(nWeightConfig) * words(nRange)
incC = words(nRange)
incR = 1
fline = flineI
lline = flineI + incI - 1

print "incI = " . incI . " incU = " . incU . " incD = " . incD . " incC = " . incC . " incR = " . incR


# the starting column of time parameter. Before this column are the benchmark parameters.
startCol = "`wc -l < config.txt`" + 1
nPlotsY = words(nRange)
nPlotsX = words(nWeightConfig)

do for [l=1:words(nUpdaters)] {
  do for [m=1:words(nDuration)] {

    #colors = "red green blue violet pink"
    titles = "HS UBST"
    #markers = "1 2 3 5 6"  # ["cross", "3 lines cross", "filled square"]
    #linetype = "1 2 3 4" # ["solid", "dashed", "smaller dashes", "smaller dashes"]
    #columns(x) = x + (startCol - 1)
    columns(x) = ((2 * x) - 1) + (startCol)
    titleMargin = 0.03
    originX = 0
    originY = 1 - titleMargin  # 0.01 to accommodate the heading and legend
    deltaX = 1.0/nPlotsX 
    deltaY = (1.0-titleMargin)/nPlotsY
    sizeX = deltaX
    sizeY = deltaY

    #set the name of the output file
    set output outputDir."/iterators_" . word(nUpdaters, l) . "_dur_" . word(nDuration, m) . ".png" 
    set term png size 1200,1200

    # size x, y tells the percentage of width and height of the plot window.
    # x, y are multiplicative factors of 100%
    set size 1,1
    set multiplot title #"Number of iterators = " . word(nIterators, l)
    unset key

    # fix the values on x-axis
    set xtics nIterators

    # fix the values on y-axis
    #set yrange[0.0:1.5]

    set xlabel "Number of iterators" font ", 16" #font "Times New Roman, 8"
    set ylabel "Total operations" font ", 16" #font "Times New Roman, 8"
    #set key at 0,0 horizontal box
    unset key
    set size sizeX,sizeY
    set tics font ", 16"

    do for [k=1:nPlotsY] { # 2 rows = duration
        originX = 0;
        originY = originY - deltaY;
        do for [j=1:nPlotsX] { # 3 cols
	    set title "dis = " . word(nWeightConfig,j) . ", range = " . word(rangeTitle,k) font ", 14"
	    set origin originX,originY
            originX = originX + deltaX;
	    #print "fline = ". fline . " lline = ".lline
	    plot for [i=1:nLines] "output.txt" using 1:columns(i) every incI::fline title word(titles,i) with linespoints linewidth 2 pointsize 1
	    
	    fline = fline + incC
       }
       flineD = flineD + incR
       fline = flineD
    }

    #############################################

    # create the legend
    unset origin
    unset border
    unset tics
    unset label
    unset arrow
    unset title
    unset object

    set size 4,4

    set key box 
    set key horizontal samplen 1 width 5 height 0.5 maxrows 1 maxcols 12
    set key at screen 0.5,screen 0.99 center top
    set key font ", 16"

    set xrange [-1:1]
    set yrange [-1:1]

    plot for [i=1:nLines] NaN title word(titles,i) with linespoints linewidth 2 pointsize 1

    unset multiplot

    # set everything back for the next plot
    reset
  } # closing for nRange

  flineI = flineI + incU
  fline = flineI
#  lline = flineI + incI - 1
  flineD = flineI
} # closing for nIterators
