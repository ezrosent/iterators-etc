# define java compiler
JC = javac
ORIG_PATH = orig
ITER_UBST_PATH = iter/ubst
ITER_HS_PATH = iter/hash
ITER_LL_PATH = iter/ll
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

CLASSES_UBST_ITER = $(ITER_UBST_PATH)/ReportItem.java \
	  $(ITER_UBST_PATH)/SetInterface.java \
	  $(ITER_UBST_PATH)/SnapCollector.java \
	  $(ITER_UBST_PATH)/TreeNode.java \
	  $(ITER_UBST_PATH)/SeekRecord.java \
	  $(ITER_UBST_PATH)/BinarySearchTree.java \
	  $(ITER_UBST_PATH)/HashNode.java \
	  $(ITER_UBST_PATH)/FSet.java \
	  $(ITER_UBST_PATH)/CHashSet.java \
	  $(ITER_UBST_PATH)/CLinkedList.java \
	  $(ITER_UBST_PATH)/UpdaterThread.java \
	  $(ITER_UBST_PATH)/IteratorThread.java

CLASSES_HS_ITER = $(ITER_HS_PATH)/ReportItem.java \
	  $(ITER_HS_PATH)/SetInterface.java \
	  $(ITER_HS_PATH)/SnapCollector.java \
	  $(ITER_HS_PATH)/TreeNode.java \
	  $(ITER_HS_PATH)/SeekRecord.java \
	  $(ITER_HS_PATH)/BinarySearchTree.java \
	  $(ITER_HS_PATH)/HashNode.java \
	  $(ITER_HS_PATH)/FSet.java \
	  $(ITER_HS_PATH)/CHashSet.java \
	  $(ITER_HS_PATH)/CLinkedList.java \
	  $(ITER_HS_PATH)/UpdaterThread.java \
	  $(ITER_HS_PATH)/IteratorThread.java

CLASSES_LL_ITER = $(ITER_LL_PATH)/ReportItem.java \
	  $(ITER_LL_PATH)/SetInterface.java \
	  $(ITER_LL_PATH)/SnapCollector.java \
	  $(ITER_LL_PATH)/TreeNode.java \
	  $(ITER_LL_PATH)/SeekRecord.java \
	  $(ITER_LL_PATH)/BinarySearchTree.java \
	  $(ITER_LL_PATH)/HashNode.java \
	  $(ITER_LL_PATH)/FSet.java \
	  $(ITER_LL_PATH)/CHashSet.java \
	  $(ITER_LL_PATH)/CLinkedList.java \
	  $(ITER_LL_PATH)/UpdaterThread.java \
	  $(ITER_LL_PATH)/IteratorThread.java

CLASSES_ORIG = $(ORIG_PATH)/SetInterface.java \
	  $(ORIG_PATH)/TreeNode.java \
	  $(ORIG_PATH)/SeekRecord.java \
	  $(ORIG_PATH)/BinarySearchTree.java \
	  $(ORIG_PATH)/HashNode.java \
	  $(ORIG_PATH)/FSet.java \
	  $(ORIG_PATH)/CHashSet.java \
	  $(ORIG_PATH)/CLinkedList.java \
	  $(ORIG_PATH)/UpdaterThread.java \
	  $(ORIG_PATH)/IteratorThread.java

# do not change the order or else set $CURR_PATH accordingly
default: classes_orig classes_ubst_iter classes_hs_iter classes_ll_iter

# Below we are replacing the suffix .java of all words in the macro CLASSES 
# with the .class suffix
classes_ubst_iter: $(CLASSES_UBST_ITER:.java=.class)
	$(eval CURR_PATH = $(ITER_HS_PATH))

classes_hs_iter: $(CLASSES_HS_ITER:.java=.class)
	$(eval CURR_PATH = $(ITER_LL_PATH))

classes_ll_iter: $(CLASSES_LL_ITER:.java=.class)

# After building the data structures with no support of iterators, 
# set the CURR_PATH to the directory containing data structures supporting iterators
classes_orig: $(CLASSES_ORIG:.java=.class)
	$(eval CURR_PATH = $(ITER_UBST_PATH))

# RM is a predefined macro in make (RM = rm -f)
clean: clean_iter clean_orig

clean_iter:
	$(RM) $(ITER_UBST_PATH)/*.class $(ITER_HS_PATH)/*.class $(ITER_LL_PATH)/*.class

clean_orig:
	$(RM) $(ORIG_PATH)/*.class

