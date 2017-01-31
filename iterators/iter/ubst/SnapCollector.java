import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class SnapCollector<T> {

	// tids are expected to be 0...NUM_THREADS-1
	static int NUM_THREADS = 64;
	
	class NodeWrapper<V> {
		V node;
		AtomicReference<NodeWrapper<V>> next = new AtomicReference<NodeWrapper<V>>(null);
		int key;
	}
	
	ReportItem[] reportHeads;
	ReportItem[] reportTails;

	NodeWrapper<T> head;
	AtomicReference<NodeWrapper<T>> tail;
	ReportItem blocker = new ReportItem(null, ReportType.add,-1); 
	NodeWrapper<T> blocker_snapshot;
	volatile boolean active;

	public SnapCollector() {
		head = new NodeWrapper<T>(); // sentinel head.
		head.key = Integer.MIN_VALUE;
		tail = new AtomicReference<NodeWrapper<T>>(head);
		active = true;
		
		reportHeads = new ReportItem[NUM_THREADS];
		reportTails = new ReportItem[NUM_THREADS];
		for (int i = 0; i < NUM_THREADS; i++) {
			reportHeads[i] = new ReportItem(null, ReportType.add,-1); // sentinel head.
			reportTails[i] = reportHeads[i];
		}

		blocker_snapshot = new NodeWrapper<T>();
		blocker_snapshot.key = Integer.MAX_VALUE;
		blocker_snapshot.node = null;
	}

	// Implemented according to the optimization in A.3:
	// Only accept nodes whose key is higher than the last, and return the last node.
	public T AddNode(T node, int key) {
	/*	NodeWrapper<T> last = tail.get();
		if (last.key >= key) // trying to add an out of place node.
			return last.node;
		
		if (last.next.get() != null) {
			if (last == tail.get())
				tail.compareAndSet(last, last.next.get());
			return tail.get().node;
		}

		NodeWrapper<T> newNode = new NodeWrapper<T>();
		newNode.node = node;
		newNode.key = key;
		if (last.next.compareAndSet(null, newNode)) {
			tail.compareAndSet(last, newNode);
			return node;
		}
		else {
			return tail.get().node;
		}
		*/
		T uselessNode = node;
		NodeWrapper<T> newNode = new NodeWrapper<T>();
		newNode.node = node;
		newNode.key = key;
		while(true) {
		    NodeWrapper<T> last = tail.get();
		    if (last.key >= key) // trying to add an out of place node.
			    return uselessNode;
		    newNode.next.set(last);
		    if (tail.compareAndSet(last, newNode)) {
			    return uselessNode;
		    }
		}
	}
	
	public void Report(int tid, T Node, ReportType t, int key) {
		ReportItem tail = reportTails[tid];
		ReportItem newItem = new ReportItem(Node, t, key);
		if (tail.next.compareAndSet(null, newItem))
			reportTails[tid] = newItem;
	}
	
	public boolean IsActive() {
		return active;
	}
	
	public void BlockFurtherPointers() {
		/*
		NodeWrapper<T> blocker = new NodeWrapper<T>();
		blocker.node = null;
		blocker.key = Integer.MAX_VALUE; 
		tail.set(blocker);*/

		// I think this makes it idempotent
		blocker_snapshot.next.compareAndSet(null, tail.get());
		tail.set(blocker_snapshot);
	}
	
	public void Deactivate() {
		this.active = false;
	}

	public void BlockFurtherReports() {
		for (int i = 0; i < NUM_THREADS; i++) {
			ReportItem tail = reportTails[i];
			if (tail.next.get() == null)
				tail.next.compareAndSet(null, blocker);
		}
	}
	
	public NodeWrapper<T> ReadPointers() {
		return head;
	}
	
	public ReportItem[] ReadReports() {
		return reportHeads;
	}
	
	
	
	// What follows is functions that are used to work with the snapshot while it is
	// already taken. These functions are used to iterate over the nodes of the snapshot.
	Object[] currLocations = new Object[NUM_THREADS];
	int[] currRepLocations = new int[NUM_THREADS];
	AtomicReference<ArrayList<CompactReportItem>> gAllReports = 
			new AtomicReference<ArrayList<CompactReportItem>>(null);
		
	// An optimization: sort the reports and nodes.
	public void Prepare(int tid) {
		currLocations[tid] = head;
		currRepLocations[tid] = 0;
		if (gAllReports.get() != null)
			return;

		ArrayList<CompactReportItem> allReports = new ArrayList<CompactReportItem>();
		for (int i = 0; i < NUM_THREADS; i++) {
			AddReports(allReports, reportHeads[i]);
			if (gAllReports.get() != null) {
			    return;
			}
		}
		Collections.sort(allReports);
		//System.out.println("How many reports you ask?" + allReports.size());
		if (gAllReports.compareAndSet(null, allReports)) {
		    /*
		    //System.out.println("report start");
		    for (CompactReportItem c : allReports) {
			    if (c.t == ReportType.remove)
			    System.out.println(c.key + " " + c.t);
		    }
		    //System.out.println("report end");
		    */
		}
		return;
	}
	
	private void AddReports(ArrayList<CompactReportItem> allReports,
			ReportItem curr) {
		curr = curr.next.get();
		while (curr != null && curr != blocker) {
			allReports.add(new CompactReportItem(curr.node, curr.t, curr.key));
			curr = curr.next.get();
		}
	}

	public T GetNext(int tid) {
		//System.out.println("hello");
		NodeWrapper<T> currLoc = (NodeWrapper<T>)currLocations[tid];
		int currRepLoc = currRepLocations[tid];
		ArrayList<CompactReportItem> allReports = gAllReports.get();
		bigloop : while (true) { 
			CompactReportItem rep = null;
			int repKey = Integer.MIN_VALUE;
			if (allReports.size() > currRepLoc) {
				rep = allReports.get(currRepLoc);
				repKey = rep.key;
			}
			int nodeKey = Integer.MIN_VALUE;
			NodeWrapper<T> next = currLoc.next.get();
			if (next != null)
				nodeKey = next.key;

			// Option 1: node key < rep key. Return node.
			if (nodeKey > repKey) {
				currLocations[tid] = next;
				currRepLocations[tid] = currRepLoc; 
				return currLoc.node;
			}

			// Option 2: node key == rep key 
			if (nodeKey == repKey) {
				// 2.a - both are infinity - iteration done.
				if (nodeKey == Integer.MIN_VALUE) {
					currLocations[tid] = currLoc;
					currRepLocations[tid] = currRepLoc;
					return null;
				}
				// node and report with the same key ::

				// skip not-needed reports
				while (currRepLoc+1 < allReports.size()) {
					CompactReportItem nextRep = allReports.get(currRepLoc+1);
					// dismiss a duplicate, or an insert followed by a matching delete:
					if (rep.key == nextRep.key && rep.node == nextRep.node) {
						currRepLoc++;
						rep = nextRep;
					}
					else
						break;
				}
				// standing on an insert report to a node I am holding:
				// 1. Return the current node.
				// 2. Skip over rest of reports for that key.
				if (rep.t == ReportType.add && (T)rep.node == next.node) {
					while (currRepLoc < allReports.size() 
							&& allReports.get(currRepLoc).key == rep.key) {
						currRepLoc++;
					}
					currRepLocations[tid] = currRepLoc;
					currLocations[tid] = next;
					return next.node;
				}
				// standing on an insert report to a different node than I hold:
				// 1. Return the reported node.
				// 2. Skip over rest of reports for that key.
				if (rep.t == ReportType.add && (T)rep.node != next.node) {
					T returnValue = (T)rep.node;
					while (currRepLoc < allReports.size() 
							&& allReports.get(currRepLoc).key == rep.key) {
						currRepLoc++;
					}
					currRepLocations[tid] = currRepLoc;
					currLocations[tid] = next;
					return returnValue;
				}
				// standing on a delete report to a different node than I hold:
				// skip over it and continue the big loop.
				if (rep.t == ReportType.remove && (T)rep.node != next.node) {
					currRepLoc++;
					continue bigloop;
				}
				// standing on a delete report to the node that I hold:
				// 1. advance over the node that I hold.
				// 2. advance with the report.
				// 3. continue the bigloop
				currLoc = next;
				currRepLoc++;
				continue;
			}

			// Option 3: node key > rep key
			if (nodeKey < repKey) {
				// skip not-needed reports
				while (currRepLoc+1 < allReports.size()) {
					CompactReportItem nextRep = allReports.get(currRepLoc+1);
					// dismiss a duplicate, or an insert followed by a matching delete:
					if (rep.key == nextRep.key && rep.node == nextRep.node) {
						currRepLoc++;
						rep = nextRep;
					}
					else
						break;
				}
				// a delete report - skip over it.
				if (rep.t == ReportType.remove) {
					currRepLoc++;
					continue bigloop;
				}

				// an insert report:
				// 1. skip over rest of the reports for the same key.
				// 2. return the node.
				if (rep.t == ReportType.add) {
					T returnValue = (T)rep.node;
					while (currRepLoc < allReports.size() 
							&& allReports.get(currRepLoc).key == rep.key) {
						currRepLoc++;
					}
					currRepLocations[tid] = currRepLoc;
					currLocations[tid] = currLoc;
					return returnValue;
				}
			}
		}
	}

}

