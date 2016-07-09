from subprocess import Popen, PIPE
import itertools

ITERATORS_NUM = [1, 2] # Compare against 0
UPDATERS_NUM = [1, 2, 3, 4, 5, 6, 7]
DURATION = [2, 4]
PERCENTAGES = [(25, 25, 50), (20, 10, 70), (50, 50, 0)]
KEY_RANGE = [4096]
INIT_SIZE = [1024]

configfile = open("config.txt", 'w')
configfile.write(' '.join(map(str, ITERATORS_NUM)) + '\n')
configfile.write(' '.join(map(str, UPDATERS_NUM)) + '\n')
configfile.write(' '.join(map(str, DURATION)) + '\n')
configfile.write(' '.join(map(str, PERCENTAGES)) + '\n')
configfile.write(' '.join(map(str, KEY_RANGE)) + '\n')
configfile.write(' '.join(map(str, INIT_SIZE)) + '\n')
configfile.close()

outputfile = open("output.txt", 'w')
outputfile.write("IT\tUP\tTIME\tCFG\tKEYR\tINIT\tHASH\tUBST\n")

PARAMETER_COMBINATIONS = [ITERATORS_NUM, UPDATERS_NUM, DURATION, PERCENTAGES, KEY_RANGE, INIT_SIZE]

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

count = 0
total = len([j for j in itertools.product(*PARAMETER_COMBINATIONS)])

for param in itertools.product(*PARAMETER_COMBINATIONS):
	pTest0h = Popen(makeargs(param, "hash", 0), stdout=PIPE)
	result0h = int(pTest0h.communicate()[0].strip())
	pTest1h = Popen(makeargs(param, "hash", param[0]), stdout=PIPE)
	result1h = int(pTest1h.communicate()[0].strip())

	pTest0b = Popen(makeargs(param, "ubst", 0), stdout=PIPE)
	result0b = int(pTest0b.communicate()[0].strip())
	pTest1b = Popen(makeargs(param, "ubst", param[0]), stdout=PIPE)
	result1b = int(pTest1b.communicate()[0].strip())

	line = reduce(lambda x, y: x + y, map(lambda x: str(x) + '\t', param))
	line += str(float(result1h)/result0h) + '\t'
	line += str(float(result1b)/result0b) + '\n'
	outputfile.write(line)
	count += 1
	print "%d of %d done" % (count, total)

outputfile.close()
