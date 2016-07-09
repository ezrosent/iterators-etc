import java.util.concurrent.atomic.*;
import java.util.*;

class Node
{
    public int key;
}
/*
    public boolean casState(int o, int n)
    {
        return stateUpdater.compareAndSet(this, o, n);
    }

    public boolean casNext(LFListNode o, LFListNode n)
    {
        return nextUpdater.compareAndSet(this, o, n);
    }

    public boolean immutable()
    {
        return state == FREEZE;
    }

    public LFListNode(int k, int s)
    {
        this.key = k;
        this.state = s;
    }
}
*/