import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicStampedReference;

public class BinarySearchTree implements SetInterface 
{
	public TreeNode root; //Node R
	public TreeNode leftChildOfRoot; //Node S 
	
	//The sentinel keys
	//Other keys must be smaller than the sentinel keys
	private final int infty_0 = Integer.MAX_VALUE - 2;
	private final int infty_1 = Integer.MAX_VALUE - 1;
	private final int infty_2 = Integer.MAX_VALUE;
	
	//The initial tree as in Figure 3 of the paper 
	// deactivate does nothing -- it is to match the implementation of the code with iterators
	// UNNECESSARY
	public BinarySearchTree(boolean deactivate)
	{
		this.root = new TreeNode(infty_2);						
		this.leftChildOfRoot = new TreeNode(infty_1);
		
		this.root.left.set(leftChildOfRoot, 0);
		this.root.right.set(new TreeNode(infty_2), 0);
		
		this.leftChildOfRoot.left.set(new TreeNode(infty_0), 0);
		this.leftChildOfRoot.right.set(new TreeNode(infty_1), 0);
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
			if ((parentField.getStamp() & 1) == 0) 
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
		
		if (seekRecord.leaf.key == key)
			return true;
		else
			return false;
	}
	
	public boolean insert(int key, int tid)
	{
		while (true)
		{
			SeekRecord seekRecord = seek(key);
			
			if (seekRecord.leaf.key != key)
			{
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

				if (key < leaf.key)
				{
					newInternal = new TreeNode(leaf.key);
					newInternal.left.set(newLeaf, 0);
					newInternal.right.set(leaf, 0);
				}
				else
				{
					newInternal = new TreeNode(key);
					newInternal.left.set(leaf, 0);
					newInternal.right.set(newLeaf, 0);				
				}
				
				boolean result = childAddr.compareAndSet(leaf, newInternal, 0, 0);
				
				if (result)
					return true;
				else
				{
					int[] marks = new int[1];
					TreeNode address;
					address = childAddr.get(marks);
					
					if (address == leaf && marks[0] > 0)
						cleanup(key, seekRecord);
				}
			}
			else
				return false;
		}
	}
	
	public boolean delete(int key, int tid)
	{
		boolean mode = true; //true == INJECTION, false == CLEANUP		
		TreeNode leaf = null;
		
		while (true)
		{
			SeekRecord seekRecord = seek(key);
			TreeNode parent = seekRecord.parent;
						
			AtomicStampedReference<TreeNode> childAddr;
			if (key < parent.key)
				childAddr = parent.left;
			else
				childAddr = parent.right;	
			
			if (mode == true) //mode == INJECTION
			{
				leaf = seekRecord.leaf;
				
				if (leaf.key != key)
					return false;
				
				//flag it
				boolean result = childAddr.compareAndSet(leaf, leaf, 0, 2);
				
				if (result)
				{
					mode = false; //mode = CLEANUP
					boolean done = cleanup(key, seekRecord);
					if (done)
						return true;
				}
				else
				{
					int[] marks = new int[1];
					TreeNode address;
					address = childAddr.get(marks);
					
					if (address == leaf && marks[0] > 0)
						cleanup(key, seekRecord);
				}
			}
			else //mode == CLEANUP
			{
				if (seekRecord.leaf != leaf)
					return true;
				else
				{
					boolean done = cleanup(key, seekRecord);
					if (done)
						return true;
				}
			}
		}
	}
	
	private boolean cleanup(int key, SeekRecord seekRecord)
	{
		TreeNode ancestor = seekRecord.ancestor;
		TreeNode successor = seekRecord.successor;
		TreeNode parent = seekRecord.parent;
		TreeNode leaf = seekRecord.leaf;
		
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
		
		int mark = childAddr.getStamp();
		if ((mark & 2) == 0) //if not flagged
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
			
			if ((marks[0] & 1) > 0)
				complete = true;
			else //do CAS instead of BTS
				complete = siblingAddr.compareAndSet(address, address, marks[0], marks[0] | 1);
		}
		//*****************************
		
		//Now line 107
		address = siblingAddr.get(marks);
		int flag = marks[0] & 2;
		
		//Make the sibling node a direct child of the ancestor node
		//and flag the edge
		return successorAddr.compareAndSet(successor, address, 0, flag);
	}

	// UNNECESSARY
	public List<Integer> iterate(int tid) {
	    System.out.println("Doea nothing");
	    return new ArrayList<Integer>();
	}
}
