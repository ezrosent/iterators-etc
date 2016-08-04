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
        int insertThresh = Bench.INSERT_PERCENT;
        int removeThresh = Bench.INSERT_PERCENT + Bench.REMOVE_PERCENT;
        SetInterface set = Bench.set;

        while (!Bench.begin);

        while (!Bench.stop) {
            int op = oprng.nextInt(100);
            int key = keyrng.nextInt(Bench.KEY_RANGE);

            if (op < insertThresh) {
                set.insert(key, id);
                inserts += 1;
            }
            else if (op < removeThresh) {
                set.delete(key, id);
                removals += 1;
            }
            else {
                set.search(key, id);
                contains += 1;
            }
        }
    }
}
