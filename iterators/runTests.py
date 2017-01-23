from subprocess import Popen, PIPE
import itertools
import argparse

parser = argparse.ArgumentParser(description='')
parser.add_argument('-s', action='store_true')
args = parser.parse_args()

# function that eliminates whitespace when printing tuples
# to make things easier when plotting things in gnuplot
def to_str(data):
	if type(data) != type(()):
		return str(data)
	else:
		return_str = "("
		return_str += (','.join(map(str, data))).strip(',') + ')'
		return return_str

ALGS = ["hash", "ubst"]#, "list"] 
ITERATORS_NUM = [1, 2, 3, 4, 5, 6, 7] # Compare against 0
UPDATERS_NUM = [1, 3, 5, 7, 9]#, 11, 13, 15]
DURATION = [2]#, 4] #, 10]
PERCENTAGES = [(25, 25, 50), (50, 50, 0)]
RANGE_SIZE = [(4096, 2048), (16384, 8192), (65536, 32768)]
runs = 10

op_prefix = "op_file"
init_prefix = "init_file"

# Write configurations used to a file
configfile = open("config.txt", 'w')
configfile.write(' '.join(map(str, ITERATORS_NUM)) + '\n')
configfile.write(' '.join(map(str, UPDATERS_NUM)) + '\n')
configfile.write(' '.join(map(str, DURATION)) + '\n')
#configfile.write(' '.join(map(str, PERCENTAGES)) + '\n')
perc_string = ""
for perc in PERCENTAGES:
	perc_string += "(%d,%d,%d) " % (perc[0], perc[1], perc[2])
perc_string = perc_string.strip() + '\n'
configfile.write(perc_string)
range_string = ""
for range in RANGE_SIZE:
	range_string += "(%d,%d) " % (range[0], range[1])
range_string = range_string.strip() + '\n'
configfile.write(range_string)
#configfile.write(' '.join(map(str, RANGE_SIZE)) + '\n')
configfile.close()

# Open file, write header
header_end = reduce(lambda x, y: x + y, map(lambda s: '\t' + s.upper(), ALGS)) + '\n'
outputfile = open("output.txt", 'w')
outputfile.write("ITER\tUPDT\tTIME\tCFIG\tSIZE" + header_end)
verbose = open("output_verbose.txt", 'w')
verbose.write("ITER\tUPDT\tTIME\tCFIG\tSIZE\tRUN" + header_end)

PARAMETER_COMBINATIONS = [ITERATORS_NUM, UPDATERS_NUM, DURATION, PERCENTAGES, RANGE_SIZE]

# Make argument list for Popen to start a Java execution
def makeargs(param, alg, i, path):
	args = ["java", "-cp", (".:lib/java-getopt-1.0.13.jar:" + path), "Bench"]
	args += ["-a", alg]
	args += ["-i", str(i)]
	args += ["-u", str(param[1])]
	args += ["-d", str(param[2])]
	args += ["-o", op_prefix + "_%d_%d_%d.txt" % (param[3][0], param[3][1], param[4][0])]
	args += ["-n", init_prefix + "_%d_%d.txt" % (param[4][0], param[4][1])]
	args += ["-M", str(param[4][0])]
	args += ["-s", str(param[4][1])]
	return args

# for keeping track of progress
count = 0
total = runs * len([j for j in itertools.product(*PARAMETER_COMBINATIONS)])

# initialize files for reading
if not args.s:
        for r, s in RANGE_SIZE:
                pInit = Popen(["python", "generate_init.py", str(r), str(s)])
                pInit.communicate()
                for i, d, c in PERCENTAGES:
                        pOps = Popen(["python", "generate_ops.py", str(i), str(d), str(r)])
                        pOps.communicate()

# main loop
for param in itertools.product(*PARAMETER_COMBINATIONS):
	accum = {a:0 for a in ALGS}
	accum_iter = {a:0 for a in ALGS}
	for r in xrange(runs):
		result_str = ""
		for alg in ALGS:
			pTest0 = Popen(makeargs(param, alg, 0, "orig"), stdout=PIPE)
			result0 = int(pTest0.communicate()[0].strip().split("+")[0]) # without iterators
			pTest1 = Popen(makeargs(param, alg, param[0], "iter"), stdout=PIPE)
			temp = pTest1.communicate()[0].strip().split("+")
			result1 = int(temp[0]) # with iterators
			total_iter = int(temp[1])
			accum[alg] += float(result1) / result0
			accum_iter[alg] += float(total_iter) # compute iterations 
			result_str += '\t' + str(float(result1) / result0) + '\t' + str(float(total_iter))

		# calculate/write verbose output
		line = reduce(lambda x, y: x + y, map(lambda x: to_str(x) + '\t', param + (r+1,))).strip()
		line += result_str + '\n'
		verbose.write(line)

		count += 1
		print "%d of %d done" % (count, total)

	# write averages
	line = reduce(lambda x, y: x + y, map(lambda x: to_str(x) + '\t', param)).strip()
	for alg in ALGS:
		line += '\t' + str(accum[alg]/runs) + '\t' + str(accum_iter[alg]/runs)
	line += '\n'

	outputfile.write(line)

outputfile.close()
verbose.close()
