import java.util.concurrent.atomic.AtomicStampedReference;

public class BinarySearchTree 
{
	public Node root; //Node R
	public Node leftChildOfRoot; //Node S 
	
	//The sentinel keys
	//Other keys must be smaller than the sentinel keys
	private final int infty_0 = Integer.MAX_VALUE - 2;
	private final int infty_1 = Integer.MAX_VALUE - 1;
	private final int infty_2 = Integer.MAX_VALUE;
	
	//The initial tree as in Figure 3 of the paper 
	public BinarySearchTree()
	{
		this.root = new Node(infty_2);						
		this.leftChildOfRoot = new Node(infty_1);
		
		this.root.left.set(leftChildOfRoot, 0);
		this.root.right.set(new Node(infty_2), 0);
		
		this.leftChildOfRoot.left.set(new Node(infty_0), 0);
		this.leftChildOfRoot.right.set(new Node(infty_1), 0);
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
	
	public boolean search(int key)
	{
		SeekRecord seekRecord = seek(key);
		
		if (seekRecord.leaf.key == key)
			return true;
		else
			return false;
	}
	
	public boolean insert(int key)
	{
		while (true)
		{
			SeekRecord seekRecord = seek(key);
			
			if (seekRecord.leaf.key != key)
			{
				Node parent = seekRecord.parent;
				Node leaf = seekRecord.leaf;
				
				AtomicStampedReference<Node> childAddr;
				if (key < parent.key)
					childAddr = parent.left;
				else
					childAddr = parent.right;
				
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
				
				if (result)
					return true;
				else
				{
					int[] marks = new int[1];
					Node address;
					address = childAddr.get(marks);
					
					if (address == leaf && marks[0] > 0)
						cleanup(key, seekRecord);
				}
			}
			else
				return false;
		}
	}
	
	public boolean delete(int key)
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
					Node address;
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
		Node ancestor = seekRecord.ancestor;
		Node successor = seekRecord.successor;
		Node parent = seekRecord.parent;
		Node leaf = seekRecord.leaf;
		
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
		
		int mark = childAddr.getStamp();
		if ((mark & 2) == 0) //if not flagged
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
}
