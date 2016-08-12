from random import sample
from sys import argv

f = open("init_file_%s_%s.txt" % (argv[1], argv[2]), 'w')
numbers = sample(xrange(int(argv[1])), int(argv[2]))
for n in numbers:
	f.write("%d\n" % n)	
