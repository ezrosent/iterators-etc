from subprocess import Popen, PIPE
import itertools

ITERATORS_NUM = [1, 2] # Compare against 0
UPDATERS_NUM = [1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31] #[1, 2, 3, 4, 5, 6, 7]
DURATION = [2, 4]
PERCENTAGES = [(25, 25, 50), (20, 10, 70), (50, 50, 0)]
KEY_RANGE = [4096]
INIT_SIZE = [1024]
runs = 1

# Maybe sanitize inputs here

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
configfile.write(' '.join(map(str, KEY_RANGE)) + '\n')
configfile.write(' '.join(map(str, INIT_SIZE)) + '\n')
configfile.close()

# Open file, write header
outputfile = open("output.txt", 'w')
outputfile.write("IT\tUP\tTIME\tCFG\tKEYR\tINIT\tHASH\tUBST\n")
verbose = open("output_verbose.txt", 'w')
verbose.write("IT\tUP\tTIME\tCFG\tKEYR\tINIT\tRUN\tHASH\tUBST\n")

PARAMETER_COMBINATIONS = [ITERATORS_NUM, UPDATERS_NUM, DURATION, PERCENTAGES, KEY_RANGE, INIT_SIZE]

# Iterate through all combinations
def makeargs(param, alg, i):
	args = ["java", "-cp", ("%s:%s/lib/java-getopt-1.0.13.jar") % (alg, alg), "IteratorTest"]
	args += ["-i", str(i)]
	args += ["-u", str(param[1])]
	args += ["-d", str(param[2])]
	args += ["-I", str(param[3][0])]
	args += ["-R", str(param[3][1])]
	args += ["-M", str(param[4])]
	args += ["-s", str(param[5])]
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
	accum_hash = 0
	accum_ubst = 0
	# accum_list
	for r in xrange(runs):
		# Compare each run against identical run with no iterators
		# hash table
		pTest0h = Popen(makeargs(param, "hash", 0), stdout=PIPE)
		result0h = int(pTest0h.communicate()[0].strip())
		pTest1h = Popen(makeargs(param, "hash", param[0]), stdout=PIPE)
		result1h = int(pTest1h.communicate()[0].strip())

		# unbalanced binary search tree
		pTest0b = Popen(makeargs(param, "ubst", 0), stdout=PIPE)
		result0b = int(pTest0b.communicate()[0].strip())
		pTest1b = Popen(makeargs(param, "ubst", param[0]), stdout=PIPE)
		result1b = int(pTest1b.communicate()[0].strip())
		
		#Add linked list code here

		# calculate/write verbose output
		line = reduce(lambda x, y: x + y, map(lambda x: to_str(x) + '\t', param + (r+1,)))
		line += str(float(result1h)/result0h) + '\t'
		line += str(float(result1b)/result0b) + '\n' # change this to a tab
		# add line here
		verbose.write(line)

		# accumulate to calculate an average over runs
		accum_hash += float(result1h) / result0h
		accum_ubst += float(result1b) / result0b
		# accum_list

		count += 1
		print "%d of %d done" % (count, total)

	# write averages
	line = reduce(lambda x, y: x + y, map(lambda x: to_str(x) + '\t', param))
	line += str(accum_hash/runs) + '\t'
	line += str(accum_ubst/runs) + '\n' # changethis to a tab
	# add line here
	outputfile.write(line)

outputfile.close()
verbose.close()
