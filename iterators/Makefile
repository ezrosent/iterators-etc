# define java compiler
JC = javac

.SUFFIXES: .java .class

# This is a target entry that uses the suffix rule syntax:
#	DSTS:
#		rule
# 'TS' is the suffix of the target file, 'DS' is the suffix of the dependency 
# file, and 'rule'  is the rule for building a target	
# '$*' is a built-in macro that gets the basename of the current target
.java.class:
	$(JC) -cp .:lib/java-getopt-1.0.13.jar $*.java

CLASSES = BinarySearchTree.java \
	  CHashSet.java \
	  CLinkedList.java \
	  FSet.java \
	  HashNode.java \
	  IteratorThread.java \
	  ReportItem.java \
	  SeekRecord.java \
	  SetInterface.java \
	  SnapCollector.java \
	  TreeNode.java \
	  UpdaterThread.java

default: classes

# Below we are replacing the suffix .java of all words in the macro CLASSES 
# with the .class suffix
classes: $(CLASSES:.java=.class)

# RM is a predefined macro in make (RM = rm -f)
clean:
	$(RM) *.class

