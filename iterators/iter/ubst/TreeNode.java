import java.util.concurrent.atomic.*;

//Node class with integer keys
public class TreeNode 
{
	public volatile AtomicReference<Integer> frameMark = new AtomicReference<Integer>(0);	
	public final int key;
	
	//the least significant bit of the stamp is tag -> sibling getting deleted
	//and the second to least significant bit is flag -> I am getting deleted
	public volatile AtomicStampedReference<TreeNode> left; 
	public volatile AtomicStampedReference<TreeNode> right;
	
	public TreeNode(int nodeKey)
	{
		this.key = nodeKey;
		left = new AtomicStampedReference<TreeNode>(null, 0);
		right = new AtomicStampedReference<TreeNode>(null, 0);
	}
}
