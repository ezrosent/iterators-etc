from sys import argv
from random import randrange

INS = int(argv[1])
DEL = int(argv[2])

RANGE = 4096
N = 1000000

OUTPUT = "op_file.txt"
file = open(OUTPUT, 'w')

for i in xrange(N):
	op = randrange(100)
	if op < INS:
		op_str = "ins"
	elif op < INS + DEL:
		op_str = "del"
	else:
		op_str = "sea"
	file.write("%s\t%d\n" % (op_str, randrange(RANGE)))

file.close()
