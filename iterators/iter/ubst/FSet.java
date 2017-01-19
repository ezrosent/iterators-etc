import java.util.concurrent.atomic.*;
import java.util.*;

class FSet
{
    // field updater for head
    private static AtomicReferenceFieldUpdater<FSet, Object> headUpdater
        = AtomicReferenceFieldUpdater.newUpdater(FSet.class, Object.class, "head");

    public volatile Object head;

    static class FreezeMarker
    {
        public HashNode [] arr;

        public FreezeMarker(HashNode [] a)
        {
            arr = a;
        }
    }
    
    // This class is just used to return multiple values from arrayInsert and arrayRemove
    static class Wrapper 
    {
    	public HashNode[] arr; // new FSet created by arrayInsert and arrayRemove
    	public HashNode node; // node that is being inserted or deleted
    	
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
    public FSet()
    {
        head = new HashNode[0];
    }

    // copy constructor
    public FSet(HashNode [] arr)
    {
        head = arr;
    }

    // TODO: pass snapcollector here as a parameter
    public int invoke(int tid, boolean insert, int key, SnapCollector<HashNode> sc)
    {
        Object h = head;
        Wrapper ret;
        while (h instanceof HashNode []) {
        	HashNode [] o = (HashNode [])h;
        	ret = insert ? arrayInsert(o, key) : arrayRemove(o, key);
        	HashNode [] n = ret.arr;
            if (n == o) 
                return -(n.length + 1);
            else if (casHead(h, n)) {
            	// TODO: if CAS successful "report" the "node"
		if (sc.IsActive()) {
            	    if (insert) {
            	        sc.Report(tid, ret.node, ReportType.add, ret.node.key);
            	    } else {
            		sc.Report(tid, ret.node, ReportType.remove, ret.node.key);
            	    }
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
        HashNode [] arr = (h instanceof HashNode [])
            ? (HashNode [])h
            : ((FreezeMarker)h).arr;
        return arrayContains(arr, key);
    }

    public void freeze()
    {
        while (true) {
            Object h = head;
            if (h instanceof FreezeMarker)
                return;
            FreezeMarker m = new FreezeMarker((HashNode [])h);
            if (casHead(h, m))
                return;
        }
    }

    public FSet split(int size, int remainder)
    {
    	HashNode [] o = ((FreezeMarker)head).arr;

        int count = 0;
        for (int i = 0; i < o.length; i++)
            if (o[i].key % size == remainder)
                count++;

        HashNode [] n = new HashNode[count];
        int j = 0;
        for (int i = 0; i < o.length; i++) {
            if (o[i].key % size == remainder)
                n[j++] = o[i];
        }

        return new FSet(n);
    }
    
    public FSet merge(FSet t2)
    {
    	HashNode [] p = ((FreezeMarker)head).arr;
    	HashNode [] q = ((FreezeMarker)t2.head).arr;

    	HashNode [] n = new HashNode[p.length + q.length];
        int j = 0;
        for (int i = 0; i < p.length; i++)
            n[j++] = p[i];
        for (int i = 0; i < q.length; i++)
            n[j++] = q[i];

        return new FSet(n);
    }
    
    
    public FSet splitForIterate(int size, int remainder)
    {
    	Object h = head;
    	HashNode[] o;
    	if (h instanceof FreezeMarker) {
    	    o = ((FreezeMarker)h).arr;
    	} else {
    		o = (HashNode [])h;
    	}

        int count = 0;
        for (int i = 0; i < o.length; i++)
            if (o[i].key % size == remainder)
                count++;

        HashNode [] n = new HashNode[count];
        int j = 0;
        for (int i = 0; i < o.length; i++) {
            if (o[i].key % size == remainder)
                n[j++] = o[i];
        }

        return new FSet(n);
    }

    public FSet mergeForIterate(FSet t2)
    {
    	Object h = head;
    	HashNode[] p;
    	if (h instanceof FreezeMarker) {
    	    p = ((FreezeMarker)h).arr;
    	} else {
    		p = (HashNode [])h;
    	}
   
    	h = t2.head;
    	HashNode [] q;
    	if (h instanceof FreezeMarker) {
    	    q = ((FreezeMarker)h).arr;
    	} else {
    		q = (HashNode [])h;
    	}

    	HashNode [] n = new HashNode[p.length + q.length];
        int j = 0;
        for (int i = 0; i < p.length; i++)
            n[j++] = p[i];
        for (int i = 0; i < q.length; i++)
            n[j++] = q[i];

        return new FSet(n);
    }

    public void print()
    {
        Object h = head;
        HashNode [] arr = null;

        if (h instanceof FreezeMarker) {
            System.out.print("(F) ");
            arr = ((FreezeMarker)h).arr;
        }
        else {
            arr = (HashNode [])h;
        }

        for (HashNode i : arr)
            System.out.print(Integer.toString(i.key) + " ");
        System.out.println();
    }

    private static boolean arrayContains(HashNode [] o, int key)
    {
        for (int i = 0; i < o.length; i++) {
            if (o[i].key == key)
                return true;
        }
        return false;
    }

    // TODO: take one more parameter, say LFArrayFSetNode node
    // this parameter returns the node which is inserted
    private static Wrapper arrayInsert(HashNode [] o, int key)
    {
    	Wrapper ret = new Wrapper();
        if (arrayContains(o, key)) {
        	ret.arr = o;
        	ret.node = null;
            return ret;
        }
        HashNode [] n = new HashNode[o.length + 1];
        for (int i = 0; i < o.length; i++)
            n[i] = o[i];
        
        n[n.length - 1] = new HashNode();
        n[n.length - 1].key = key;
        
        ret.arr = n;
        ret.node = n[n.length - 1];
        return ret;
    }

    // TODO: take one more parameter, say LFArrayFSetNode node
    // this parameter returns the node which is being deleted
    private static Wrapper arrayRemove(HashNode [] o, int key)
    {
    	Wrapper ret = new Wrapper();
        if (!arrayContains(o, key)) {
        	ret.arr = o;
        	ret.node = null;
            return ret;
        }
        HashNode [] n = new HashNode[o.length - 1];
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
