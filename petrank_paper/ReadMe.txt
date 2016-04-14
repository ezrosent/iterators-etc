Lock-Free Data-Structure Iterators: source code.

Please contact Shahar Timnat (shahar.timnat@gmail.com) with any
questions / remarks / corrections.


The classes ListIT and SkiplistIT both support four linearizable methods:
public boolean contains(int tid, int key);
public boolean insert(int tid, int key);
public boolean delete(int tid, int key);
public int size(int tid);

TID is a thread id that should be unique for each thread and expected to be
between 0...NUM_THREADS-1.
NUM_THREADS is defined inside the SnapCollector.java file.

The size method is implemented via iteration.
In case you desire to use an iteration not size,
you should easily be able to manipulate (a copy of)the size method 
for other purposes.


