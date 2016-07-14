import java.util.concurrent.atomic.AtomicInteger;

class IteratorThread extends Thread {
    private int id;
    public AtomicInteger iterations;

    public IteratorThread(int i) {
        this.id = i;
        iterations = new AtomicInteger();
    }

    public void run() {
        ISet set = IteratorTest.set;

        while (!IteratorTest.begin);

        while (!IteratorTest.stop) {
            set.iterate(id);
            iterations.getAndIncrement();
        }
    }
}
