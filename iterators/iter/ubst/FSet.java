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
/*    public FSet(HashNode [] arr)
    {
        head = arr;
    }
*/

    // copy constructor
    public FSet(Object o) 
    {
	if (o instanceof HashNode[]) {
	    head = (HashNode [])o;
	} else {
	    head = ((FreezeMarker)o).arr;
	}
    }
    
    // Return values:
    // 0 -> FSet frozen
    // -ve -> unsuccessful
    // +ve -> successful
    // returns the new size + 1. +1 is needed to make sure distinguish from the case when on success the size has become 0 (deleting from a single node Fset)
    public DummyWrapper invokeInsert(int key)
    {
	Object h = head;
	DummyWrapper ret = new DummyWrapper();
	if (h instanceof HashNode []) {
	    HashNode [] o = (HashNode [])h;
	    ret = arrayInsert(o, key);
	    HashNode [] n = ret.arr;
	    ret.arr = null;
	    if (casHead(h, n)) {		    
		ret.retVal =  n.length + 1;
	    } else {
		ret.node = null;
	        ret.retVal =  -(n.length + 1);
	    }
	} else {
	    ret.retVal = 0;
	}
	return ret;	
    }

    
    public DummyWrapper invokeDelete(HashNode node)
    {
	Object h = head;
	DummyWrapper ret = new DummyWrapper();
	while (h instanceof HashNode []) {
	    HashNode [] o = (HashNode [])h;
	    
	    // check that node still exists in FSet
	    boolean found = false;
	    for (int i = 0; i < o.length; i++) {
	    	if (o[i] == node) {
	    		found = true;
	    		break;
	    	}
	    }
	    if (!found) {
	    	ret.retVal = 1;
	    	return ret;
	    }
	    
	    // if exists, try to remove it
	    ret = arrayRemove(o, node);
	    HashNode [] n = ret.arr;
	    ret.arr = null;
	    /*if (ret.node != node) {
	        ret.retVal = 1; // return any non-zero value to signal success
	        return ret;
	    }*/
	    if (casHead(h, n)) {		    
	    	ret.retVal =  1;
	    	return ret;
	    }
	    h = head;
	}
	ret.retVal = 0;
	return ret;	
    }


    public HashNode hasMember(int key)
    {
        Object h = head;
        HashNode [] arr = (h instanceof HashNode []) ? (HashNode [])h : ((FreezeMarker)h).arr;

        for (int i = 0; i < arr.length; i++) {
            if (arr[i].key == key)
                return arr[i];
        }
        return null;
	
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


    private static DummyWrapper arrayInsert(HashNode [] o, int key)
    {
	DummyWrapper ret = new DummyWrapper();
	HashNode [] n = new HashNode[o.length + 1];
	for (int i = 0; i < o.length; i++)
	    n[i] = o[i];

	n[n.length - 1] = new HashNode();
	n[n.length - 1].key = key;

	ret.arr = n;
	ret.node = n[n.length - 1];
	return ret;
    }

    // Invariant: is always called with node being present in o
    private static DummyWrapper arrayRemove(HashNode [] o, HashNode node)
    {
		DummyWrapper ret = new DummyWrapper();
		HashNode [] n = new HashNode[o.length - 1];
		
		int j = 0;
		
		ret.arr = n;
		// copy all but the node to be deleted
		for (int i = 0; i < o.length; i++) {
			if (o[i] != node) {
				n[j++] = o[i];
			}
		}
		return ret;
    }
}
