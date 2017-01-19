import java.util.concurrent.atomic.*;
import java.util.List;
import java.util.ArrayList;

public class CLinkedList implements SetInterface{

	class Node
	{
		int key;
		AtomicMarkableReference<Node> next;
		
		public Node (int val)
		{
			key = val;
		}
	}
	
	Node head;
	Node tail;
	AtomicReference<SnapCollector<Node>> snapPointer;
	
	public CLinkedList(boolean deactivate) {

		head = new Node(Integer.MIN_VALUE);
		tail = new Node(Integer.MAX_VALUE);
		head.next = new AtomicMarkableReference<Node>(tail, false);
		tail.next = new AtomicMarkableReference<Node>(tail, false);
		
		SnapCollector<Node> dummy = new SnapCollector<Node>();
		dummy.BlockFurtherReports();
		if (deactivate) {
		    dummy.Deactivate();
		}
		snapPointer = new AtomicReference<SnapCollector<Node>>(dummy);

	}
	
	class Window {
		public Node pred, curr;
		Window(Node myPred, Node myCurr)
		{
			pred = myPred; curr = myCurr;
		}		
	}
	
	public Window find(Node head, int key, int tid)
	{
		Node pred = null, curr = null, succ = null;
		boolean[] marked = {false};
		boolean snip;
		retry : while (true) {
			pred = head;
			curr = pred.next.getReference();
			while (true)
			{
				succ = curr.next.get(marked);
				while (marked[0])
				{
					SnapCollector<Node> sc = snapPointer.get();
					if (sc.IsActive()) {
						sc.Report(tid, curr, ReportType.remove, curr.key);
					}
					snip = pred.next.compareAndSet(curr, succ, false, false);
					if (!snip) continue retry;
					curr = succ;
					succ = curr.next.get(marked);					
				}
				if (curr.key >= key)
				{
					return new Window(pred, curr);
				}
				pred = curr;
				curr = succ;
			}
		}
	}
	
	public boolean insert(int key, int tid) 
	{
		while (true)
		{
			Window window = find(head, key, tid);
			Node pred = window.pred, curr = window.curr;
			if (curr.key == key)
			{
				SnapCollector<Node> sc = snapPointer.get();
				if (sc.IsActive()) {
					// report only if you are not going to be deleted
					if (!curr.next.isMarked())
						sc.Report(tid, curr, ReportType.add, curr.key);
				}
				return false;
			}
			else
			{
				Node node = new Node(key);
				node.next = new AtomicMarkableReference<Node>(curr, false);
				if (pred.next.compareAndSet(curr, node, false, false)){
					SnapCollector<Node> sc = snapPointer.get();
					if (sc.IsActive()) {
						// report only if you are not going to be deleted
						if (!node.next.isMarked())
							sc.Report(tid, node, ReportType.add, node.key);
					}
					return true;
				}				
			}
		}
	}

	public boolean delete(int key, int tid)
	{
		boolean snip;
		while (true)
		{
			Window window = find(head, key, tid);
			Node pred = window.pred, curr = window.curr;
			if (curr.key != key)
			{
				return false;
			}
			else
			{
				Node succ = curr.next.getReference();
				// just marking the next pointer 
				snip = curr.next.compareAndSet(succ, succ, false, true);
				if (!snip)
					continue;
				SnapCollector<Node> sc = snapPointer.get();
				if (sc.IsActive()) {
					sc.Report(tid, curr, ReportType.remove, curr.key);
				}
				// physically removing curr
				if (!pred.next.compareAndSet(curr, succ, false, false))
					 find(head,key,tid);
				return true;
			}
		}
	}

	public boolean search(int key, int tid) {
		boolean[] marked = {false};
		Node curr = head;
		while (curr.key < key) { // search for the key
			curr = curr.next.getReference();
			curr.next.get(marked);
		}
		if (curr.key != key)
			return false;
		SnapCollector<Node> sc = snapPointer.get();
		if (sc.IsActive()) {
			if (curr.next.isMarked())
				sc.Report(tid, curr, ReportType.remove, curr.key);
			else
				sc.Report(tid, curr, ReportType.add, curr.key);
		}
		return (curr.key == key && !marked[0]); // the key is found and is logically in the list.		
	}
	
	public SnapCollector<Node> GetSnapshot(int tid) {
		SnapCollector<Node> sc = AcquireSnapCollector();
		CollectSnapshot(sc);
		sc.Prepare(tid);
		return sc;
	}
	
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
	
	private void CollectSnapshot(SnapCollector<Node> sc) {
		Node curr = head.next.getReference();
		while (sc.IsActive()) {
			if (!curr.next.isMarked())
				curr = sc.AddNode(curr, curr.key);
			if (curr == null) {
				sc.BlockFurtherPointers();
				sc.Deactivate();
				break;
			}
			if (curr.key == Integer.MAX_VALUE) {
				sc.BlockFurtherPointers();
				sc.Deactivate();
			}
			curr = curr.next.getReference();
		}
		sc.BlockFurtherReports();
	}

	
	// Calculates size via iteration.
	public int size(int tid) {
		SnapCollector<Node> snap = GetSnapshot(tid);
		int result = 0;
		Node curr;
		while ((curr = snap.GetNext(tid)) != null) {
			result++;
		}
		return result;
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
