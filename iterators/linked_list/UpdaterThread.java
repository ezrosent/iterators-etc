import java.util.Random;

class UpdaterThread extends Thread {
    private int id;
    private Random oprng;
    private Random keyrng;

    public int inserts;
    public int removals;
    public int contains;

    public UpdaterThread(int i) {
        this.id = i;
        this.oprng = new Random(i);
        this.keyrng = new Random(i);
        inserts = 0;
        removals = 0;
        contains = 0;
    }

    public void run() {
        int insertThresh = IteratorTest.INSERT_PERCENT;
        int removeThresh = IteratorTest.INSERT_PERCENT + IteratorTest.REMOVE_PERCENT;
        ListIT set = IteratorTest.set;

        while (!IteratorTest.begin);

        while (!IteratorTest.stop) {
            int op = oprng.nextInt(100);
            int key = keyrng.nextInt(IteratorTest.KEY_RANGE);

            if (op < insertThresh) {
                set.insert(id, key);
                inserts += 1;
            }
            else if (op < removeThresh) {
                set.delete(id, key);
                removals += 1;
            }
            else {
                set.contains(id, key);
                contains += 1;
            }
        }
    }
}