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
            set.iterate(id);
            iterations += 1;
        }
    }
}
