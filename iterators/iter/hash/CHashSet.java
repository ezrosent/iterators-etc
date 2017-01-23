import java.util.concurrent.atomic.*;
import java.util.*;

class CHashSet implements SetInterface
{
	static class HNode
	{
		// points to old HNode
		public HNode old;

		// bucket array
		public AtomicReferenceArray<FSet> buckets;

		// store the size [for convenience]
		public final int size;

		// constructor
		public HNode(HNode o, int s)
		{
			old = o;
			size = s;
			buckets = new AtomicReferenceArray<FSet>(size);
		}
	}

	public final static int MIN_BUCKET_NUM = 1;
	public final static int MAX_BUCKET_NUM = 1 << 16;

	// points to the current hash table
	volatile HNode head;

	//Pointer to snap collector object
	AtomicReference<SnapCollector<HashNode>> snapPointer;

	// field updater for head
	private static AtomicReferenceFieldUpdater<CHashSet, HNode> headUpdater
	= AtomicReferenceFieldUpdater.newUpdater(CHashSet.class, HNode.class, "head");

	public CHashSet(boolean deactivate)
	{
		head = new HNode(null, MIN_BUCKET_NUM);
		head.buckets.set(0, new FSet());
		SnapCollector<HashNode> dummy = new SnapCollector<HashNode>();
		dummy.BlockFurtherReports();
		if (deactivate) {
		    dummy.Deactivate();
		}
		snapPointer = new AtomicReference<SnapCollector<HashNode>>(dummy);
	}

	public boolean insert(int key, int tid)
	{
		HNode h = head;
		int result = apply(tid, true, key);
		if (Math.abs(result) > 2)
			resize(h, true);
		return result > 0;
	}

	public boolean delete(int key, int tid)
	{
		int result = apply(tid, false, key);
		return result > 0;
	}

	public boolean search(int key, int tid)
	{
		HNode t = head;
		FSet b = t.buckets.get(key % t.size);
		// if the b is empty, use old table
		if (b == null) {
			HNode s = t.old;
			b = (s == null)
					? t.buckets.get(key % t.size)
							: s.buckets.get(key % s.size);
		}
		return b.hasMember(key);
	}

	public boolean simpleInsert(int key, int tid)
	{
		return apply(tid, true, key) > 0;
	}

	public boolean simpleDelete(int key, int tid)
	{
		return apply(tid, false, key) > 0;
	}

	public boolean grow()
	{
		HNode h = head;
		return resize(h, true);
	}

	public boolean shrink()
	{
		HNode h = head;
		return resize(h, false);
	}

	public int getBucketSize()
	{
		return head.size;
	}

	public void print()
	{
		HNode curr = head;
		int age = 0;
		while (curr != null) {
			System.out.println("HashTableNode #" + Integer.toString(age++));
			for (int i = 0; i < curr.size; i++) {
				System.out.print("  Bucket " + Integer.toString(i) + ": ");
				if (curr.buckets.get(i) != null)
					curr.buckets.get(i).print();
				else
					System.out.println();
			}
			curr = curr.old;
			System.out.println();
		}
	}

	private int apply(int tid, boolean insert, int key)
	{
		SnapCollector<HashNode> sc = snapPointer.get();
		while (true) {
			HNode       t = head;
			int         i = key % t.size;
			FSet b = t.buckets.get(i);

			// response value
			int ret = 0;

			// if the b is empty, help finish resize
			if (b == null)
				helpResize(t, i);
			// otherwise enlist at b
			else if ((ret = b.invoke(tid, insert, key, sc)) != 0)
				return ret;
		}
	}

	private boolean resize(HNode t, boolean grow)
	{
		if ((t.size == MAX_BUCKET_NUM && grow) ||
				(t.size == MIN_BUCKET_NUM && !grow))
			return false;

		if (t == head) {
			// make sure we can deprecate t's predecessor
			for (int i = 0; i < t.size; i++) {
				if (t.buckets.get(i) == null)
					helpResize(t, i);
			}
			// deprecate t's predecessor
			t.old = null;

			// switch to a new bucket array
			if (t == head) {
				HNode n = new HNode(t, grow ? t.size * 2 : t.size / 2);
				return casHead(t, n);
			}
		}
		return false;
	}

	private void helpResize(HNode t, int i)
	{
		FSet b = t.buckets.get(i);
		HNode s = t.old;
		if (b == null && s != null) {
			FSet set = null;
			if (s.size * 2 == t.size) /* growing */ {
				FSet p = s.buckets.get(i % s.size);
				p.freeze();
				set = p.split(t.size, i);
			}
			else /* shrinking */ {
				FSet p = s.buckets.get(i);
				FSet q = s.buckets.get(i + t.size);
				p.freeze();
				q.freeze();
				set = p.merge(q);
			}
			t.buckets.compareAndSet(i, null, set);

		}
	}

	private boolean casHead(HNode o, HNode n)
	{
		return headUpdater.compareAndSet(this, o, n);
	}

	// Snap collector code

	private void CollectSnapshot(SnapCollector<HashNode> sc) {
		// TODO: write the iterate logic here
		/* 
		 * cur = head // extract the current hash set
		 * start iterating over buckets
		 * if bucket is null
		 * extract old hash set (head.old)
		 * compute the corresponding bucket in old
		 * iterate over that
		 * if old is null come back to curr and read the original bucket in current (original bucket is guaranteed to not being null)
		 * continue with iteration on current 
		 */
		HNode t = head;
		HNode s = t.old;
		
		for (int i = 0; i < t.size && sc.IsActive(); i++) {
			// get the i-th bucket
			FSet b = t.buckets.get(i);
			if ((b == null) && (s != null)) {
				// compute the corresponding bucket in s
				if (s.size * 2 == t.size) /* growing */ {
					FSet p = s.buckets.get(i % s.size);
					b = p.splitForIterate(t.size, i);
				} else {
					FSet p = s.buckets.get(i);
					FSet q = s.buckets.get(i + t.size);
					b = p.mergeForIterate(q);
				}
			}
			else {
				// Make a copy of the bucket for iteration
				//b = new FSet((HashNode [])(t.buckets.get(i).head));
				b = new FSet(t.buckets.get(i).head);
			}
			// iterate b
                        //System.out.println("Bucket size: " + ((Node [])(b.head)).length);
			for (int j = 0; j < ((HashNode [])(b.head)).length; j++) {
				sc.AddNode(((HashNode [])(b.head))[j], ((HashNode [])(b.head))[j].key);
				//System.out.println("Succesfully added "+ ((Node [])(b.head))[j].key);
			}
		}
		sc.BlockFurtherPointers();
		sc.Deactivate();
		sc.BlockFurtherReports();
	}

	public SnapCollector<HashNode> GetSnapshot(int tid) {
		SnapCollector<HashNode> sc = AcquireSnapCollector();
		CollectSnapshot(sc);
		sc.Prepare(tid);
		sc.PrepareSnapshotNodes(tid);
		return sc;
	}

	private SnapCollector<HashNode> AcquireSnapCollector() {
		SnapCollector<HashNode> result = null;
		result = snapPointer.get();
		if (!result.IsActive()) {
			SnapCollector<HashNode> candidate = new SnapCollector<HashNode>();
			if (snapPointer.compareAndSet(result, candidate))
				result = candidate;
			else
				result = snapPointer.get();
		}
		return result;
	}

	// Calculates size via iteration.
	public int size(int tid) {
		SnapCollector<HashNode> snap = GetSnapshot(tid);
		int result = 0;
		HashNode curr;
		while ((curr = snap.GetNext(tid)) != null) {
			result++;
		}
		return result;
	}

	// iterator: calculates the nodes in the list via iteration
	public List<Integer> iterate(int tid) {
		List<Integer> list = new ArrayList<Integer>();

		SnapCollector<HashNode> snap = GetSnapshot(tid);
		HashNode curr;
		while ((curr = snap.GetNext(tid)) != null) {
			list.add(curr.key);
		}
		return list;
	}


}
