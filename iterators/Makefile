# define java compiler
JC = javac
ORIG_PATH = orig
ITER_PATH = iter
CURR_PATH = $(ORIG_PATH)

.SUFFIXES: .java .class

# This is a target entry that uses the suffix rule syntax:
#	DSTS:
#		rule
# 'TS' is the suffix of the target file, 'DS' is the suffix of the dependency 
# file, and 'rule'  is the rule for building a target	
# '$*' is a built-in macro that gets the basename of the current target
.java.class:
	$(JC) -cp .:lib/java-getopt-1.0.13.jar:$(CURR_PATH) -d $(CURR_PATH) $*.java

CLASSES_ITER = $(ITER_PATH)/ReportItem.java \
	  $(ITER_PATH)/SetInterface.java \
	  $(ITER_PATH)/SnapCollector.java \
	  $(ITER_PATH)/TreeNode.java \
	  $(ITER_PATH)/SeekRecord.java \
	  $(ITER_PATH)/BinarySearchTree.java \
	  $(ITER_PATH)/HashNode.java \
	  $(ITER_PATH)/FSet.java \
	  $(ITER_PATH)/CHashSet.java \
	  $(ITER_PATH)/CLinkedList.java \
	  $(ITER_PATH)/UpdaterThread.java \
	  $(ITER_PATH)/IteratorThread.java \
	  $(ITER_PATH)/Bench.java 

CLASSES_ORIG = $(ORIG_PATH)/SetInterface.java \
	  $(ORIG_PATH)/TreeNode.java \
	  $(ORIG_PATH)/SeekRecord.java \
	  $(ORIG_PATH)/BinarySearchTree.java \
	  $(ORIG_PATH)/HashNode.java \
	  $(ORIG_PATH)/FSet.java \
	  $(ORIG_PATH)/CHashSet.java \
	  $(ORIG_PATH)/CLinkedList.java \
	  $(ORIG_PATH)/UpdaterThread.java \
	  $(ORIG_PATH)/IteratorThread.java \
	  $(ORIG_PATH)/Bench.java

# do not change the order or else set $CURR_PATH accordingly
default: classes_orig classes_iter

# Below we are replacing the suffix .java of all words in the macro CLASSES 
# with the .class suffix
classes_iter: $(CLASSES_ITER:.java=.class)

# After building the data structures with no support of iterators, 
# set the CURR_PATH to the directory containing data structures supporting iterators
classes_orig: $(CLASSES_ORIG:.java=.class)
	$(eval CURR_PATH = $(ITER_PATH))

# RM is a predefined macro in make (RM = rm -f)
clean: clean_iter clean_orig

clean_iter:
	$(RM) $(ITER_PATH)/*.class 

clean_orig:
	$(RM) $(ORIG_PATH)/*.class

