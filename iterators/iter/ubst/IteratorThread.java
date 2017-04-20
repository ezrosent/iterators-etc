import java.util.List;

class IteratorThread extends Thread {
    private int id;
    public int iterations;

    public IteratorThread(int i) {
        this.id = i;
        iterations = 0;
    }

    public void run() {
        SetInterface set = Bench.set;

        while (!Bench.begin);

        while (!Bench.stop) {
            List<Integer> list = set.iterate(id);
	    //System.out.println("thread " + id + " snapshot " + list.toString());
            iterations += 1;
        }
    }
}
