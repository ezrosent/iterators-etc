import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

// Line numbers in comments refer to ListIT.java

public class BinarySearchTree 
{
	public Node root; //Node R
	public Node leftChildOfRoot; //Node S

	//The sentinel keys
	//Other keys must be smaller than the sentinel keys
	private final int infty_0 = Integer.MAX_VALUE - 2;
	private final int infty_1 = Integer.MAX_VALUE - 1;
	private final int infty_2 = Integer.MAX_VALUE;

	//Pointer to snap collector object
	AtomicReference<SnapCollector<Node>> snapPointer;

	//The initial tree as in Figure 3 of the paper 
	public BinarySearchTree()
	{
		this.root = new Node(infty_2);						
		this.leftChildOfRoot = new Node(infty_1);

		this.root.left.set(leftChildOfRoot, 0);
		this.root.right.set(new Node(infty_2), 0);

		this.leftChildOfRoot.left.set(new Node(infty_0), 0);
		this.leftChildOfRoot.right.set(new Node(infty_1), 0);

		SnapCollector<Node> dummy = new SnapCollector<Node>();
		dummy.BlockFurtherReports();
		snapPointer = new AtomicReference<SnapCollector<Node>>(dummy);

	}

	public boolean isFlagged(AtomicStampedReference<Node> ref) {
		return (!((ref.getStamp() & 2) == 0));
	}

	public boolean isTagged(AtomicStampedReference<Node> ref) {
		return (!((ref.getStamp() & 1) == 0));
	}

	public boolean isMarked(AtomicStampedReference<Node> ref) {
		return (ref.getStamp() > 0);
	}

	private SeekRecord seek(int key)
	{
		SeekRecord seekRecord = new SeekRecord();
		seekRecord.ancestor = this.root;
		seekRecord.successor = this.leftChildOfRoot;
		seekRecord.parent = this.leftChildOfRoot;
		seekRecord.leaf = this.leftChildOfRoot.left.getReference();

		AtomicStampedReference<Node> parentField = seekRecord.parent.left;
		AtomicStampedReference<Node> currentField = seekRecord.leaf.left;
		Node current = currentField.getReference();

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

	public boolean search(int tid, int key)
	{
		SeekRecord seekRecord = seek(key);

		if (seekRecord.leaf.key == key) {
			// report -- 150
			/* check if seekRecord.parent.<whatever child key is> is flagged
			 * if flagged report "remove" for "leaf"
			 * else report "add" for "leaf"
			 * */
			AtomicStampedReference<Node> childPointer;
			SnapCollector<Node> sc = snapPointer.get();
			if (seekRecord.parent.left.getReference() == seekRecord.leaf) {
				childPointer = seekRecord.parent.left;
			} else {
				childPointer = seekRecord.parent.right;
			}

			if (sc.IsActive()) {
				if (isFlagged(childPointer)) { // the node is flagged
					sc.Report(tid, seekRecord.leaf, ReportType.remove, seekRecord.leaf.key);
				}
				else {
					sc.Report(tid, seekRecord.leaf, ReportType.add, seekRecord.leaf.key);
				}
			}
			return true;
		}
		else
			return false;
	}

	//  take tid as well
	public boolean insert(int tid, int key)
	{
		while (true)
		{
			SnapCollector<Node> sc = snapPointer.get();
			SeekRecord seekRecord = seek(key);

			Node parent = seekRecord.parent;
			Node leaf = seekRecord.leaf;

			AtomicStampedReference<Node> childAddr;
			if (key < parent.key)
				childAddr = parent.left;
			else
				childAddr = parent.right;

			if (leaf.key != key)
			{
				//initialize newInternal and newLeaf appropriately
				Node newLeaf = new Node(key);
				Node newInternal;

				if (key < leaf.key)
				{
					newInternal = new Node(leaf.key);
					newInternal.left.set(newLeaf, 0);
					newInternal.right.set(leaf, 0);
				}
				else
				{
					newInternal = new Node(key);
					newInternal.left.set(leaf, 0);
					newInternal.right.set(newLeaf, 0);				
				}

				boolean result = childAddr.compareAndSet(leaf, newInternal, 0, 0);

				if (result) {
					// Add the report to "newLeaf" -- 98
					if (sc.IsActive()) {
						// report only if you are not going to be deleted
						if (key < leaf.key) {
							if (!isFlagged(newInternal.left)) {
								sc.Report(tid, newLeaf, ReportType.add, newLeaf.key);
							}
						} else {
							if (!isFlagged(newInternal.right)) {
								sc.Report(tid, newLeaf, ReportType.add, newLeaf.key);
							}
						}
					}
					return true;
				}
				else
				{
					int[] marks = new int[1];
					Node address;
					address = childAddr.get(marks);

					if (address == leaf && marks[0] > 0)
						cleanup(tid, key, seekRecord);
				}
			}
			else {
				// key was already present
				// Report the "leaf" -- 85
				if (sc.IsActive()) {
					// report only if you are not going to be deleted
					if (!isFlagged(childAddr)) {
						sc.Report(tid, leaf, ReportType.add, leaf.key);
					}
				}
				return false;
			}
		}
	}

	public boolean delete(int tid, int key)
	{
		boolean mode = true; //true == INJECTION, false == CLEANUP		
		Node leaf = null;

		while (true)
		{
			SeekRecord seekRecord = seek(key);
			Node parent = seekRecord.parent;

			AtomicStampedReference<Node> childAddr;
			if (key < parent.key)
				childAddr = parent.left;
			else
				childAddr = parent.right;	

			if (mode == true) //mode == INJECTION
			{
				leaf = seekRecord.leaf;

				if (leaf.key != key) {
					return false;
				}

				//flag it
				boolean result = childAddr.compareAndSet(leaf, leaf, 0, 2);

				if (result)
				{
					mode = false; //mode = CLEANUP
					boolean done = cleanup(tid, key, seekRecord);
					if (done) {
						return true;
					}
				}
				else
				{
					int[] marks = new int[1];
					Node address;
					address = childAddr.get(marks);

					if (address == leaf && isMarked(childAddr))
						cleanup(tid, key, seekRecord);
				}
			}
			else //mode == CLEANUP
			{
				if (seekRecord.leaf != leaf) {
					return true;
				}
				else
				{
					boolean done = cleanup(tid, key, seekRecord);
					if (done) {
						return true;
					}
				}
			}
		}
	}

	private boolean cleanup(int tid, int key, SeekRecord seekRecord)
	{
		Node ancestor = seekRecord.ancestor;
		Node successor = seekRecord.successor;
		Node parent = seekRecord.parent;
		//Node leaf = seekRecord.leaf;

		AtomicStampedReference<Node> successorAddr;
		if (key < ancestor.key)
			successorAddr = ancestor.left;
		else
			successorAddr = ancestor.right;

		AtomicStampedReference<Node> childAddr; 
		AtomicStampedReference<Node> siblingAddr; 
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

		if (!isFlagged(childAddr)) //if not flagged
			siblingAddr = childAddr;

		//*****************************
		//Since BTS (line 106 of the pseudocode) is not supported, we use CAS instead
		//But it may have performance/progress issues?
		boolean complete = false;
		int[] marks = new int[1];
		Node address;
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
		if (complete) {
			// Physical removal: Report childAddr.node
			SnapCollector<Node> sc = snapPointer.get();
			if (sc.IsActive()) {
				sc.Report(tid, childAddr.getReference(), ReportType.add, key);
			}
		}
		return complete;
	}

	// Snapcollector stuff below this
	public SnapCollector<Node> GetSnapshot(int tid) {
		SnapCollector<Node> sc = AcquireSnapCollector();
		CollectSnapshot(sc);
		sc.Prepare(tid);
		return sc;
	}

	// This function figures out if a new snap collector is to be created
	// or build on the old existing one
	private SnapCollector<Node> AcquireSnapCollector() {
		SnapCollector<Node> result = null;
		result = snapPointer.get();
		if (!result.IsActive()) {
			SnapCollector<Node> candidate = new SnapCollector<Node>();
			if (snapPointer.compareAndSet(result, candidate))
				result = candidate;
			else
				result = snapPointer.get();
		}
		return result;
	}

	// TODO: check for sc.isActive is missing -- Line 179
	public void dfs(AtomicStampedReference<Node> node, SnapCollector<Node> sc) {
		if(!sc.IsActive()) {
			return;
		}
		if (node.getReference().key == infty_0 && node.getReference().right.getReference() == null) {
			sc.BlockFurtherPointers();
			sc.Deactivate();
			return;
		}
		AtomicStampedReference<Node> leftChild = node.getReference().left;
		AtomicStampedReference<Node> rightChild = node.getReference().right;
		if ((leftChild.getReference() == null) && (rightChild.getReference() == null)) { // found a leaf
			if (!isFlagged(node)) {
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

	private void CollectSnapshot(SnapCollector<Node> sc) {
		// get the root of the actual tree
		dfs(leftChildOfRoot.left, sc);
		sc.BlockFurtherReports();
	}

	// iterator: calculates the nodes in the list via iteration
	public List<Integer> iterate(int tid) {
		List<Integer> list = new ArrayList<Integer>();

		SnapCollector<Node> snap = GetSnapshot(tid);
		Node curr;
		while ((curr = snap.GetNext(tid)) != null) {
			list.add(curr.key);
		}
		return list;
	}
}
