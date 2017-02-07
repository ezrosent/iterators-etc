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
	public boolean retVal; // return value
    	
    	public Wrapper() {
    		arr = null;
    		node = null;
		retVal = false;
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
    
    public int invoke(int tid, boolean insert, int key, SnapCollector<HashNode> sc)
    {
	Object h = head;
	Wrapper ret;
	while (h instanceof HashNode []) {
	    HashNode [] o = (HashNode [])h;
	    ret = insert ? arrayInsert(o, key) : arrayRemove(o, key);
	    if (insert) {
		if (ret.retVal == true) { // key was not present, thus inserted
		     if (casHead(h, ret.arr)) { // CAS is successful
			 sc.Report(tid, ret.node, ReportType.add, ret.node.key);
			 return ret.arr.length; // return the new length
		     } else { // CAS failed on the bucket, thus restart
			 h = head;
			 continue;
		     }
		} else { // key was already present ; insert failed
		     if (ret.node.mark.get() == 0) {
		        sc.Report(tid, ret.node, ReportType.add, ret.node.key);
		     } else {
			 sc.Report(tid, ret.node, ReportType.remove, ret.node.key);
		     }
		     return -(ret.arr.length); // return the old length
		}
	    } else { // delete operation
		if (ret.retVal == false) { // key was not present ; delete failed
		    return -1; // return false (-ve is false)
		} 
		else { // key was present
		     if (ret.node.mark.compareAndSet(0, 1)) { // if mark successful
			 sc.Report(tid, ret.node, ReportType.remove, ret.node.key);
			 tryDelete(h, ret.node);
			 return 1; // it's ok not to return length because resizing only happens on insert
		     } else {
			 sc.Report(tid, ret.node, ReportType.remove, ret.node.key);
			 tryDelete(h, ret.node);
			 h = head;
			 continue; // restart
		     }
		}
	    }
	}
	return 0;
    }


    // TODO: possible error: what happens when the set freezes while trying to delete
    public void tryDelete(Object h, HashNode node) {
	    //System.out.println("In tryDelete");
	    while(h instanceof HashNode[]) {
			//System.out.println("In tryDelete loop");
			HashNode [] o = (HashNode [])h;
			boolean found = false;
			for (int i = 0; i < o.length; i++) {
				if (o[i] == node) {
					found = true;
					break;
				}
			}
			if (!found) { // physical delete of node already happened
				return;
			}
			// else create a new array without node and try CASing
			HashNode [] n = new HashNode[o.length - 1];
			int j = 0;
			for (int i = 0; i < o.length; i++) {
				if (o[i] != node) {
					n[j++] = o[i];
				}
			}
			if (casHead(h, n)) { // physical delete successful
				return;
			} else {
				h = head; // update head and continue
			}
		}
		return;			
	}

    public boolean hasMember(int tid, int key, SnapCollector<HashNode> sc)
    {
        Object h = head;
        HashNode [] arr = (h instanceof HashNode [])
            ? (HashNode [])h
            : ((FreezeMarker)h).arr;
        HashNode node =  arrayContains(arr, key);
		if (node == null) {
			return false;
		} else {
			if (node.mark.get() == 1) {
				sc.Report(tid, node, ReportType.remove, node.key);
				return false;
			} else {
				sc.Report(tid, node, ReportType.add, node.key);
				return true;
			}
		}
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

    private static HashNode arrayContains(HashNode [] o, int key)
    {
        for (int i = 0; i < o.length; i++) {
            if (o[i].key == key)
                return o[i];
        }
        return null;
    }

    // TODO: take one more parameter, say LFArrayFSetNode node
    // this parameter returns the node which is inserted
    private static Wrapper arrayInsert(HashNode [] o, int key)
    {
		Wrapper ret = new Wrapper();
		HashNode node;
		// key already present
		if ((node = arrayContains(o, key)) != null) {
			ret.arr = o;
			ret.node = node;
			ret.retVal = false;
			return ret;
		}
		// key not present
		HashNode [] n = new HashNode[o.length + 1];
		for (int i = 0; i < o.length; i++)
			n[i] = o[i];

		n[n.length - 1] = new HashNode();
		n[n.length - 1].key = key;

		ret.arr = n;
		ret.node = n[n.length - 1];
		ret.retVal = true;
		return ret;
    }

    // TODO: take one more parameter, say LFArrayFSetNode node
    // this parameter returns the node which is being deleted
    private static Wrapper arrayRemove(HashNode [] o, int key)
    {
		Wrapper ret = new Wrapper();
		HashNode node;
		if ((node = arrayContains(o, key)) == null) { // key is not present
			ret.arr = o;
			ret.node = null;
			ret.retVal = false;
			return ret;
		}
		// key is present
		HashNode [] n = new HashNode[o.length - 1];
		int j = 0;
		ret.arr = n;
		ret.node = node;
		ret.retVal = true;
		// copy all but the node to be deleted
		for (int i = 0; i < o.length; i++) {
			if (o[i].key != key) {
				n[j++] = o[i];
			}
		}
		return ret;
    }
}
