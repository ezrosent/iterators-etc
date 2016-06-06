import java.util.concurrent.atomic.AtomicStampedReference;

//Node class with integer keys
public class Node 
{
	public final int key;
	
	//the least significant bit of the stamp is tag -> sibling getting deleted
	//and the second to least significant bit is flag -> I am getting deleted
	public volatile AtomicStampedReference<Node> left; 
	public volatile AtomicStampedReference<Node> right;
	
	public Node(int nodeKey)
	{
		this.key = nodeKey;
		left = new AtomicStampedReference<Node>(null, 0);
		right = new AtomicStampedReference<Node>(null, 0);
	}
}
