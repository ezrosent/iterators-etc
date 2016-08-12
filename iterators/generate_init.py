from random import sample
from sys import argv

f = open("init_file.txt", 'w')
numbers = sample(xrange(int(argv[1])), int(argv[2]))
for n in numbers:
	f.write("%d\n" % n)	
