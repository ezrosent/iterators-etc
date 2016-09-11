import java.util.concurrent.atomic.*;
import java.util.*;

class FSet
{
    // field updater for head
    private static AtomicReferenceFieldUpdater<FSet, Object> headUpdater
        = AtomicReferenceFieldUpdater.newUpdater(FSet.class, Object.class, "head");

    private volatile Object head;

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
    public FSet(HashNode [] arr)
    {
        head = arr;
    }

    public int invoke(boolean insert, int key)
    {
        Object h = head;
        while (h instanceof HashNode []) {
            HashNode [] o = (HashNode [])h;
            HashNode [] n = insert ? arrayInsert(o, key) : arrayRemove(o, key);
            if (n == o)
                return -(n.length + 1);
            else if (casHead(h, n))
                return n.length + 1;
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

    private static HashNode [] arrayInsert(HashNode [] o, int key)
    {
        if (arrayContains(o, key))
            return o;
        HashNode [] n = new HashNode[o.length + 1];
        for (int i = 0; i < o.length; i++)
            n[i] = o[i];

	n[n.length - 1] = new HashNode();
        n[n.length - 1].key = key;
        return n;
    }

    private static HashNode [] arrayRemove(HashNode [] o, int key)
    {
        if (!arrayContains(o, key))
            return o;
        HashNode [] n = new HashNode[o.length - 1];
        int j = 0;
        for (int i = 0; i < o.length; i++) {
            if (o[i].key != key)
                n[j++] = o[i];
        }
        return n;
    }
}
