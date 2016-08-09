from subprocess import Popen, PIPE
import itertools

ALGS = ["hash", "ubst", "list"]
UPDATERS_NUM = [1, 2, 3, 4]  #[1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31] #[1, 2, 3, 4, 5, 6, 7]
DURATION = [2, 4]
PERCENTAGES = [(25, 25, 50), (50, 50, 0)]
KEY_RANGE = [4000]
INIT_SIZE = [1000]
runs = 10

# Maybe sanitize inputs here

# Write configurations used to a file
configfile = open("config.txt", 'w')
configfile.write(' '.join(map(str, UPDATERS_NUM)) + '\n')
configfile.write(' '.join(map(str, DURATION)) + '\n')
#configfile.write(' '.join(map(str, PERCENTAGES)) + '\n')
perc_string = ""
for perc in PERCENTAGES:
	perc_string += "(%d,%d,%d) " % (perc[0], perc[1], perc[2])
perc_string = perc_string.strip() + '\n'
configfile.write(perc_string)
configfile.write(' '.join(map(str, KEY_RANGE)) + '\n')
configfile.write(' '.join(map(str, INIT_SIZE)) + '\n')
configfile.close()

# Open file, write header
header_end = reduce(lambda x, y: x + y, map(lambda s: '\t' + s.upper(), ALGS)) + '\n'
outputfile = open("output.txt", 'w')
outputfile.write("UP\tTIME\tCFG\tKEYR\tINIT" + header_end)
verbose = open("output_verbose.txt", 'w')
verbose.write("UP\tTIME\tCFG\tKEYR\tINIT\tRUN" + header_end)

PARAMETER_COMBINATIONS = [UPDATERS_NUM, DURATION, PERCENTAGES, KEY_RANGE, INIT_SIZE]

# Iterate through all combinations
def makeargs(param, alg, deact):
	args = ["java", "-cp", (".:lib/java-getopt-1.0.13.jar"), "Bench"]
	args += ["-a", alg]
	args += ["-i", "0"] # no iterators
	args += ["-u", str(param[0])]
	args += ["-d", str(param[1])]
	args += ["-I", str(param[2][0])]
	args += ["-R", str(param[2][1])]
	args += ["-M", str(param[3])]
	args += ["-s", str(param[4])]
	if deact:
		args += ["-T"]
	return args

# for keeping track of progress
count = 0
total = runs * len([j for j in itertools.product(*PARAMETER_COMBINATIONS)])

def to_str(data):
	if type(data) != type(()):
		return str(data)
	else:
		return_str = "("
		return_str += (','.join(map(str, data))).strip(',') + ')'
		return return_str

# main loop
for param in itertools.product(*PARAMETER_COMBINATIONS):
	accum = {a:0 for a in ALGS}
	for r in xrange(runs):
		result_str = ""
		for alg in ALGS:
			pTest0 = Popen(makeargs(param, alg, True), stdout=PIPE) # denom, no reporting
			result0 = int(pTest0.communicate()[0].strip())
			pTest1 = Popen(makeargs(param, alg, False), stdout=PIPE) # numer, reporting
			result1 = int(pTest1.communicate()[0].strip())
			accum[alg] += float(result1) / result0
			result_str += '\t' + str(float(result1) / result0)

		# calculate/write verbose output
		line = reduce(lambda x, y: x + y, map(lambda x: to_str(x) + '\t', param + (r+1,))).strip()
		line += result_str + '\n'
		verbose.write(line)

		count += 1
		print "%d of %d done" % (count, total)

	# write averages
	line = reduce(lambda x, y: x + y, map(lambda x: to_str(x) + '\t', param)).strip()
	for alg in ALGS:
		line += '\t' + str(accum[alg]/runs)
	line += '\n'

	outputfile.write(line)

outputfile.close()
verbose.close()
