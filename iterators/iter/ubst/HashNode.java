import java.util.concurrent.atomic.*;
import java.util.*;

class HashNode
{
    public AtomicReference<Integer> mark = new AtomicReference<Integer>(0);
    public int key;
}
