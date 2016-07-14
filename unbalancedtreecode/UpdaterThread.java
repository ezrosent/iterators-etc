import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class UpdaterThread extends Thread {
    private int id;
    private Random oprng;
    private Random keyrng;

    public AtomicInteger inserts = new AtomicInteger();
    public AtomicInteger removals = new AtomicInteger();
    public AtomicInteger contains = new AtomicInteger();

    public UpdaterThread(int i) {
        this.id = i;
        this.oprng = new Random(i);
        this.keyrng = new Random(i);
    }

    public void run() {
        int insertThresh = IteratorTest.INSERT_PERCENT;
        int removeThresh = IteratorTest.INSERT_PERCENT + IteratorTest.REMOVE_PERCENT;
        BinarySearchTree set = IteratorTest.set;

        while (!IteratorTest.begin);

        while (!IteratorTest.stop) {
            int op = oprng.nextInt(100);
            int key = keyrng.nextInt(IteratorTest.KEY_RANGE);

            if (op < insertThresh) {
                set.insert(id, key);
                inserts.getAndIncrement();
            }
            else if (op < removeThresh) {
                set.delete(id, key);
                removals.getAndIncrement();
            }
            else {
                set.search(id, key);
                contains.getAndIncrement();
            }
        }
    }
}
