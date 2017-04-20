import java.util.concurrent.atomic.*;
import java.util.*;

class HashNode
{
    public AtomicReference<Integer> frameMark = new AtomicReference<Integer>(0);
    public int key;
}
