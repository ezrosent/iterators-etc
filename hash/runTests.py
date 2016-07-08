from subprocess import Popen, PIPE
import itertools

ITERATORS_NUM = [0, 1, 4]
UPDATERS_NUM = [1, 2, 4, 8]
DURATION = [2]
PERCENTAGES = [(25, 25)]
KEY_RANGE = [65536]
INIT_SIZE = [1024]

PARAMETER_COMBINATIONS = [ITERATORS_NUM, UPDATERS_NUM, DURATION, PERCENTAGES, KEY_RANGE, INIT_SIZE]

for param in itertools.product(*PARAMETER_COMBINATIONS):
	args = ["java", "-cp", ".:lib/java-getopt-1.0.13.jar" , "IteratorTest"]
	args += ["-i", str(param[0])]
	args += ["-u", str(param[1])]
	args += ["-d", str(param[2])]
	args += ["-I", str(param[3][0])]
	args += ["-R", str(param[3][1])]
	args += ["-M", str(param[4])]
	args += ["-s", str(param[5])]
	pTest = Popen(args, stdout=PIPE)
	result = pTest.communicate()[0]
	print result
