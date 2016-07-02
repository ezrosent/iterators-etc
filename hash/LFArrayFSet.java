import java.util.concurrent.atomic.*;
import java.util.*;

class LFArrayFSet
{
    // field updater for head
    private static AtomicReferenceFieldUpdater<LFArrayFSet, Object> headUpdater
        = AtomicReferenceFieldUpdater.newUpdater(LFArrayFSet.class, Object.class, "head");

    public volatile Object head;

    static class FreezeMarker
    {
        public Node [] arr;

        public FreezeMarker(Node [] a)
        {
            arr = a;
        }
    }
    
    // This class is just used to return multiple values from arrayInsert and arrayRemove
    static class Wrapper 
    {
    	public Node[] arr; // new FSet created by arrayInsert and arrayRemove
    	public Node node; // node that is being inserted or deleted
    	
    	public Wrapper() {
    		arr = null;
    		node = null;
    	}
    	
    }

    private boolean casHead(Object o, Object n)
    {
        return headUpdater.compareAndSet(this, o, n);
    }

    // default constructor
    public LFArrayFSet()
    {
        head = new Node[0];
    }

    // copy constructor
    public LFArrayFSet(Node [] arr)
    {
        head = arr;
    }

    // TODO: pass snapcollector here as a parameter
    public int invoke(int tid, boolean insert, int key, SnapCollector<Node> sc)
    {
        Object h = head;
        Wrapper ret;
        while (h instanceof Node []) {
        	Node [] o = (Node [])h;
        	ret = insert ? arrayInsert(o, key) : arrayRemove(o, key);
        	Node [] n = ret.arr;
            if (n == o) 
                return -(n.length + 1);
            else if (casHead(h, n)) {
            	// TODO: if CAS successful "report" the "node"
            	if (insert) {
            	    sc.Report(tid, ret.node, ReportType.add, ret.node.key);
            	} else {
            		sc.Report(tid, ret.node, ReportType.remove, ret.node.key);
            	}
                return n.length + 1;
            }
            h = head;
        }
        return 0;
    }

    public boolean hasMember(int key)
    {
        Object h = head;
        Node [] arr = (h instanceof Node [])
            ? (Node [])h
            : ((FreezeMarker)h).arr;
        return arrayContains(arr, key);
    }

    public void freeze()
    {
        while (true) {
            Object h = head;
            if (h instanceof FreezeMarker)
                return;
            FreezeMarker m = new FreezeMarker((Node [])h);
            if (casHead(h, m))
                return;
        }
    }

    public LFArrayFSet split(int size, int remainder)
    {
    	Node [] o = ((FreezeMarker)head).arr;

        int count = 0;
        for (int i = 0; i < o.length; i++)
            if (o[i].key % size == remainder)
                count++;

        Node [] n = new Node[count];
        int j = 0;
        for (int i = 0; i < o.length; i++) {
            if (o[i].key % size == remainder)
                n[j++] = o[i];
        }

        return new LFArrayFSet(n);
    }
    
    public LFArrayFSet merge(LFArrayFSet t2)
    {
    	Node [] p = ((FreezeMarker)head).arr;
    	Node [] q = ((FreezeMarker)t2.head).arr;

    	Node [] n = new Node[p.length + q.length];
        int j = 0;
        for (int i = 0; i < p.length; i++)
            n[j++] = p[i];
        for (int i = 0; i < q.length; i++)
            n[j++] = q[i];

        return new LFArrayFSet(n);
    }
    
    
    public LFArrayFSet splitForIterate(int size, int remainder)
    {
    	Object h = head;
    	Node[] o;
    	if (h instanceof FreezeMarker) {
    	    o = ((FreezeMarker)h).arr;
    	} else {
    		o = (Node [])h;
    	}

        int count = 0;
        for (int i = 0; i < o.length; i++)
            if (o[i].key % size == remainder)
                count++;

        Node [] n = new Node[count];
        int j = 0;
        for (int i = 0; i < o.length; i++) {
            if (o[i].key % size == remainder)
                n[j++] = o[i];
        }

        return new LFArrayFSet(n);
    }

    public LFArrayFSet mergeForIterate(LFArrayFSet t2)
    {
    	Object h = head;
    	Node[] p;
    	if (h instanceof FreezeMarker) {
    	    p = ((FreezeMarker)h).arr;
    	} else {
    		p = (Node [])h;
    	}
   
    	h = t2.head;
    	Node [] q;
    	if (h instanceof FreezeMarker) {
    	    q = ((FreezeMarker)h).arr;
    	} else {
    		q = (Node [])h;
    	}

    	Node [] n = new Node[p.length + q.length];
        int j = 0;
        for (int i = 0; i < p.length; i++)
            n[j++] = p[i];
        for (int i = 0; i < q.length; i++)
            n[j++] = q[i];

        return new LFArrayFSet(n);
    }

    public void print()
    {
        Object h = head;
        Node [] arr = null;

        if (h instanceof FreezeMarker) {
            System.out.print("(F) ");
            arr = ((FreezeMarker)h).arr;
        }
        else {
            arr = (Node [])h;
        }

        for (Node i : arr)
            System.out.print(Integer.toString(i.key) + " ");
        System.out.println();
    }

    private static boolean arrayContains(Node [] o, int key)
    {
        for (int i = 0; i < o.length; i++) {
            if (o[i].key == key)
                return true;
        }
        return false;
    }

    // TODO: take one more parameter, say LFArrayFSetNode node
    // this parameter returns the node which is inserted
    private static Wrapper arrayInsert(Node [] o, int key)
    {
    	Wrapper ret = new Wrapper();
        if (arrayContains(o, key)) {
        	ret.arr = o;
        	ret.node = null;
            return ret;
        }
        Node [] n = new Node[o.length + 1];
        for (int i = 0; i < o.length; i++)
            n[i] = o[i];
        
        n[n.length - 1] = new Node();
        n[n.length - 1].key = key;
        
        ret.arr = n;
        ret.node = n[n.length - 1];
        return ret;
    }

    // TODO: take one more parameter, say LFArrayFSetNode node
    // this parameter returns the node which is being deleted
    private static Wrapper arrayRemove(Node [] o, int key)
    {
    	Wrapper ret = new Wrapper();
        if (!arrayContains(o, key)) {
        	ret.arr = o;
        	ret.node = null;
            return ret;
        }
        Node [] n = new Node[o.length - 1];
        int j = 0;
        for (int i = 0; i < o.length; i++) {
            if (o[i].key != key) {
                n[j++] = o[i];
            } else {
            	ret.arr = n;
            	ret.node = o[i];
            }
        }
        return ret;
    }
}
