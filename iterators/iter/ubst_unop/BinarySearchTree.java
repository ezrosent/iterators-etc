import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

// Line numbers in comments refer to ListIT.java

public class BinarySearchTree implements SetInterface
{
    public TreeNode root; //Node R
    public TreeNode leftChildOfRoot; //Node S

    //The sentinel keys
    //Other keys must be smaller than the sentinel keys
    private final int infty_0 = Integer.MAX_VALUE - 2;
    private final int infty_1 = Integer.MAX_VALUE - 1;
    private final int infty_2 = Integer.MAX_VALUE;

    //Pointer to snap collector object
    AtomicReference<SnapCollector<TreeNode>> snapPointer;

    //The initial tree as in Figure 3 of the paper 
    public BinarySearchTree(boolean deactivate)
    {
        this.root = new TreeNode(infty_2);                        
        this.leftChildOfRoot = new TreeNode(infty_1);

        this.root.left.set(leftChildOfRoot, 0);
        this.root.right.set(new TreeNode(infty_2), 0);

        this.leftChildOfRoot.left.set(new TreeNode(infty_0), 0);
        this.leftChildOfRoot.right.set(new TreeNode(infty_1), 0);

        SnapCollector<TreeNode> dummy = new SnapCollector<TreeNode>();
        dummy.BlockFurtherReports();
        if (deactivate) {
            dummy.Deactivate();
        }
        snapPointer = new AtomicReference<SnapCollector<TreeNode>>(dummy);

    }
    
    public void reportDelete(int tid, TreeNode node, int nodeKey) {
        SnapCollector<TreeNode> sc = snapPointer.get();
	if (sc.IsActive()) {
            sc.Report(tid, node, ReportType.remove, nodeKey);
	}
    }

    public void reportInsert(int tid, TreeNode node, int nodeKey) {
        SnapCollector<TreeNode> sc = snapPointer.get();
	if (sc.IsActive()) {
	    if (!isFrameMarked(node)) {
                sc.Report(tid, node, ReportType.add, nodeKey);
	    } else {
	        sc.Report(tid, node, ReportType.remove, nodeKey);
	    }
	}
    }

    public boolean isFlagged(AtomicStampedReference<TreeNode> ref) {
        return (!((ref.getStamp() & 2) == 0));
    }

    public boolean isTagged(AtomicStampedReference<TreeNode> ref) {
        return (!((ref.getStamp() & 1) == 0));
    }

    public boolean isMarked(AtomicStampedReference<TreeNode> ref) {
        return (ref.getStamp() > 0);
    }
    
    public boolean isFrameMarked(TreeNode node) {
        return (node.frameMark.get() == 1);
    }

    private SeekRecord seek(int key)
    {
        SeekRecord seekRecord = new SeekRecord();
        seekRecord.ancestor = this.root;
        seekRecord.successor = this.leftChildOfRoot;
        seekRecord.parent = this.leftChildOfRoot;
        seekRecord.leaf = this.leftChildOfRoot.left.getReference();

        AtomicStampedReference<TreeNode> parentField = seekRecord.parent.left;
        AtomicStampedReference<TreeNode> currentField = seekRecord.leaf.left;
        TreeNode current = currentField.getReference();

        while (current != null)
        {
            //if not tagged
            if (!isTagged(parentField)) 
            {
                seekRecord.ancestor = seekRecord.parent;
                seekRecord.successor = seekRecord.leaf;
            }

            seekRecord.parent = seekRecord.leaf;
            seekRecord.leaf = current;

            parentField = currentField;

            if (key < current.key)
                currentField = current.left;            
            else
                currentField = current.right;

            current = currentField.getReference();
        }

        return seekRecord;
    }

    public boolean search(int key, int tid)
    {
        SeekRecord seekRecord = seek(key);
	TreeNode leaf = seekRecord.leaf;

        if (leaf.key == key) { // node found
            if (isFrameMarked(seekRecord.leaf)) { // the node is flagged
	        reportDelete(tid, leaf, leaf.key);
		return false;
            } else {
		reportInsert(tid, leaf, leaf.key);
		return true;
            }
        } else { // node not found
            return false;
	}
    }

    public boolean insert(int key, int tid) {
	TreeNode newLeaf;
        SeekRecord seekRecord = seek(key);

	TreeNode leaf = seekRecord.leaf;
	if (leaf.key == key) { // node found
	    if (isFrameMarked(leaf)) {
	        reportDelete(tid, leaf, leaf.key);
		cleanup(leaf, seekRecord);
		return insert(key, tid);
	    } else {
	        reportInsert(tid, leaf, leaf.key);
		return false;
	    }
	} else { // node not found
	    if ((newLeaf = insert_old(key, seekRecord)) != null) {
	        reportInsert(tid, newLeaf, newLeaf.key);
		return true;
	    } else {
	        return insert(key, tid);
	    }
	} 
    }

    //  take tid as well
    public TreeNode insert_old(int key, SeekRecord seekRecord) {
        TreeNode parent = seekRecord.parent;
        TreeNode leaf = seekRecord.leaf;

        AtomicStampedReference<TreeNode> childAddr;
        if (key < parent.key)
            childAddr = parent.left;
        else
            childAddr = parent.right;

        //initialize newInternal and newLeaf appropriately
        TreeNode newLeaf = new TreeNode(key);
        TreeNode newInternal;

        if (key < leaf.key) {
            newInternal = new TreeNode(leaf.key);
            newInternal.left.set(newLeaf, 0);
            newInternal.right.set(leaf, 0);
         } else {
            newInternal = new TreeNode(key);
            newInternal.left.set(leaf, 0);
            newInternal.right.set(newLeaf, 0);                
         }

         boolean result = childAddr.compareAndSet(leaf, newInternal, 0, 0);

         if (result) { // successfully added
             return newLeaf;
         } else {// if failed to insert due to concurrent CAS (concurrent insert or delete)
            // we shouldn't be directly returning false, right => some sort of cleanup has to be done right before returning false?
	    int[] marks = new int[1];
            TreeNode address;
            address = childAddr.get(marks);
            if (address == leaf && isMarked(childAddr)) { 
                cleanup_old(key, seekRecord);
	    }
	    return null; // signal physical insertion failure to framework insert
         }
    }


    public boolean delete(int key, int tid)
    {
        TreeNode leaf = null;
        boolean result = false;

        SeekRecord seekRecord = seek(key);

        leaf = seekRecord.leaf;

        if (leaf.key != key) { // key not found
            return false;
        }
            // key found
        result = leaf.frameMark.compareAndSet(0, 1);        
        reportDelete(tid, leaf, leaf.key);
        cleanup(leaf, seekRecord); // cleanup seekRecord.leaf
        if (result == true) {
            return true;
        }
        else {
            return delete(key, tid);
        }
    }
    
    public void cleanup(TreeNode node, SeekRecord seekRecord) {
        int key = seekRecord.leaf.key;
        boolean mode = true; //true == INJECTION, false == CLEANUP        
        TreeNode leaf = null;
        TreeNode parent = null;

        while (true) {
            
            parent = seekRecord.parent;
            leaf = seekRecord.leaf;

            AtomicStampedReference<TreeNode> childAddr;
            if (key < parent.key) {
                childAddr = parent.left;
            } else {
                childAddr = parent.right;
            }    

	    // check is needed because of one thread fails in marking, it will cleanup and try again
	    // when trying again we do not want it to remove a new node with the same key
            if (leaf != node) { // node has already been deleted
		return;
	    }

            if (mode == true) {    
                //flag it
                boolean result = childAddr.compareAndSet(leaf, leaf, 0, 2);

                if (result) // succeeded in marking => return true
                {
                    mode = false; //mode = CLEANUP
                    if (cleanup_old(key, seekRecord)) {
                        return;
                    }
		    seekRecord = seek(key);
                }
                else // failed in marking
                {
                    int[] marks = new int[1];
                    TreeNode address;
                    address = childAddr.get(marks);
                    if (address == leaf && isMarked(childAddr)) // optimization -- make sure it still needs to be cleaned up
                        cleanup_old(key, seekRecord);
                        seekRecord = seek(key);
                }
            }
            else //mode == CLEANUP
            {
                // Mode becomes false only when a thread successfully marks a node for deletion
                // once mode becomes false, it's always false.
                // true is returned iff mode is false
                if (cleanup_old(key, seekRecord)) {
                    return;
                }
		seekRecord = seek(key);
            }
        }
    }

    private boolean cleanup_old(int key, SeekRecord seekRecord)
    {
        TreeNode ancestor = seekRecord.ancestor;
        TreeNode successor = seekRecord.successor;
        TreeNode parent = seekRecord.parent;
        //Node leaf = seekRecord.leaf;

        AtomicStampedReference<TreeNode> successorAddr;
        if (key < ancestor.key)
            successorAddr = ancestor.left;
        else
            successorAddr = ancestor.right;

        AtomicStampedReference<TreeNode> childAddr; 
        AtomicStampedReference<TreeNode> siblingAddr; 
        if (key < parent.key)
        {
            childAddr = parent.left;
            siblingAddr = parent.right;
        }
        else
        {
            childAddr = parent.right;
            siblingAddr = parent.left;
        }

	// if child is not flagged for deletion, then sibling must be
	// In this case we will delete the sibling and make the acestor point to the original child
	// thus switch the sibling to child
        if (!isFlagged(childAddr)) //if not flagged 
            siblingAddr = childAddr;

        //*****************************
        //Since BTS (line 106 of the pseudocode) is not supported, we use CAS instead
        //But it may have performance/progress issues?
        boolean complete = false;
        int[] marks = new int[1];
        TreeNode address;
        while (!complete)
        {
            address = siblingAddr.get(marks);
            // TODO: is this equivalent to isMarked?
            //if ((marks[0] & 1) > 0)
            if (isTagged(siblingAddr)) {
                complete = true;
            }
            else  {//do CAS instead of BTS
                complete = siblingAddr.compareAndSet(address, address, marks[0], marks[0] | 1);
                assert complete; // BTS should always succeed
            }
        }
        //*****************************

        //Now line 107
        address = siblingAddr.get(marks);
        int flag = marks[0] & 2;

        //Make the sibling node a direct child of the ancestor node
        //and flag the edge
        complete = successorAddr.compareAndSet(successor, address, 0, flag);
        return complete;
    }

    // Snapcollector stuff below this
    public SnapCollector<TreeNode> GetSnapshot(int tid) {
        SnapCollector<TreeNode> sc = AcquireSnapCollector();
        CollectSnapshot(sc);
        sc.Prepare(tid);
	sc.PrepareSnapshotNodes(tid);
        return sc;
    }

    // This function figures out if a new snap collector is to be created
    // or build on the old existing one
    private SnapCollector<TreeNode> AcquireSnapCollector() {
        SnapCollector<TreeNode> result = null;
        result = snapPointer.get();
        if (!result.IsActive()) {
            SnapCollector<TreeNode> candidate = new SnapCollector<TreeNode>();
            if (snapPointer.compareAndSet(result, candidate))
                result = candidate;
            else
                result = snapPointer.get();
        }
        return result;
    }

    // TODO: check for sc.isActive is missing -- Line 179
    public void dfs(AtomicStampedReference<TreeNode> node, SnapCollector<TreeNode> sc) {
        if(!sc.IsActive()) {
            return;
        }
        if (node.getReference().key == infty_0 && node.getReference().right.getReference() == null) {
            sc.BlockFurtherPointers();
            sc.Deactivate();
            return;
        }
        AtomicStampedReference<TreeNode> leftChild = node.getReference().left;
        AtomicStampedReference<TreeNode> rightChild = node.getReference().right;
        if ((leftChild.getReference() == null) && (rightChild.getReference() == null)) { // found a leaf
            if (!isFrameMarked(node.getReference())) {
                sc.AddNode(node.getReference(), node.getReference().key);
            }
            return;
        }
        if (leftChild.getReference() != null) {
            dfs(leftChild, sc);
        }
        if (rightChild.getReference() != null) {
            dfs(rightChild, sc);
        }
    }

    private void CollectSnapshot(SnapCollector<TreeNode> sc) {
        // get the root of the actual tree
        dfs(leftChildOfRoot.left, sc);
        sc.BlockFurtherReports();
    }

    // iterator: calculates the nodes in the list via iteration
    public List<Integer> iterate(int tid) {
        List<Integer> list = new ArrayList<Integer>();

        SnapCollector<TreeNode> snap = GetSnapshot(tid);
        TreeNode curr;
        while ((curr = snap.GetNext(tid)) != null) {
            list.add(curr.key);
        }
        return list;
    }
}
