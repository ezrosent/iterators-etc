class IteratorThread extends Thread {
    private int id;
    public int iterations;

    public IteratorThread(int i) {
        this.id = i;
        iterations = 0;
    }

    public void run() {
        ListIT set = IteratorTest.set;

        while (!IteratorTest.begin);

        while (!IteratorTest.stop) {
            set.iterate(id);
            iterations += 1;
        }
    }
}
