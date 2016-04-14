import java.util.concurrent.atomic.*;

public class ListIT {

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
	
	public ListIT() {

		head = new Node(Integer.MIN_VALUE);
		tail = new Node(Integer.MAX_VALUE);
		head.next = new AtomicMarkableReference<Node>(tail, false);
		tail.next = new AtomicMarkableReference<Node>(tail, false);
		
		SnapCollector<Node> dummy = new SnapCollector<Node>();
		dummy.BlockFurtherReports();
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
	
	public boolean insert(int tid, int key) 
	{
		while (true)
		{
			Window window = find(head, key, tid);
			Node pred = window.pred, curr = window.curr;
			if (curr.key == key)
			{
				SnapCollector<Node> sc = snapPointer.get();
				if (sc.IsActive()) {
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
						if (!node.next.isMarked())
							sc.Report(tid, node, ReportType.add, node.key);
					}
					return true;
				}				
			}
		}
	}

	public boolean delete(int tid, int key)
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
				snip = curr.next.compareAndSet(succ, succ, false, true);
				if (!snip)
					continue;
				SnapCollector<Node> sc = snapPointer.get();
				if (sc.IsActive()) {
					sc.Report(tid, curr, ReportType.remove, curr.key);
				}
				if (!pred.next.compareAndSet(curr, succ, false, false))
					 find(head,key,tid);
				return true;
			}
		}
	}

	public boolean contains(int tid, int key) {
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

}
