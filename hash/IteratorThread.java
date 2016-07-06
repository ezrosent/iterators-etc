class IteratorThread extends Thread {
    private int id;

    public IteratorThread(int i) {
        this.id = i;
    }

    public void run() {
        ISet set = IteratorTest.set;

        while (!IteratorTest.begin);

        while (!IteratorTest.stop)
            set.iterate(id);
    }
}
