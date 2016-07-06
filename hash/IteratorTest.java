import java.util.Random;
import java.text.DecimalFormat;
import gnu.getopt.Getopt;

class IteratorTest {
    public static int ITERATORS_NUM = 8;
    public static int UPDATERS_NUM = 24;
    public static int DURATION = 5;
    public static int INSERT_PERCENT = 25;
    public static int REMOVE_PERCENT = 25;
    public static int KEY_RANGE = 4096;
    public static int INIT_SIZE = 1024;
    
    public static volatile ISet set = null;
    public static volatile boolean begin = false;
    public static volatile boolean stop = false;
    
    private static boolean ParseArgs(String [] args) {
        Getopt g = new Getopt("", args, "i:u:d:I:R:M:s:h");
        int c;
        String arg = null;
        while ((c = g.getopt()) != -1)
        {
            switch(c)
            {
              case 'i':
                arg = g.getOptarg();
                ITERATORS_NUM = Integer.parseInt(arg);
                break;
              case 'u':
                arg = g.getOptarg();
                UPDATERS_NUM = Integer.parseInt(arg);
                break;
              case 'd':
                arg = g.getOptarg();
                DURATION = Integer.parseInt(arg);
                break;
              case 'I':
                arg = g.getOptarg();
                INSERT_PERCENT = Integer.parseInt(arg);
                break;
              case 'R':
                arg = g.getOptarg();
                REMOVE_PERCENT = Integer.parseInt(arg);
                break;
              case 'M':
                arg = g.getOptarg();
                KEY_RANGE = Integer.parseInt(arg);
                break;
              case 's':
                arg = g.getOptarg();
                INIT_SIZE = Integer.parseInt(arg);
                break;
              case 'h':
                printHelp();
                return false;
              default:
                return false;
            }
        }
        return true;
    }

    private static void printHelp() {
        System.out.println("This is some helpful text");

    }

    private static void InitializeSet() {
        IteratorTest.set = new LFArrayHashSet();

        Random rng = new Random();
        for (int i = 0; i < INIT_SIZE; i++) {
            while (true) {
                int key = rng.nextInt(KEY_RANGE);
                if (set.insert(key, 0))
                    break;
            }
        }
    }

    private static void RunTest() throws InterruptedException {
        InitializeSet();

        begin = false;
        stop = false;

        UpdaterThread [] updaterThreads = new UpdaterThread[UPDATERS_NUM];
        IteratorThread [] iteratorThreads = new IteratorThread[ITERATORS_NUM];

        for (int i = 0; i < updaterThreads.length; i++) {
            updaterThreads[i] = new UpdaterThread(i);
            updaterThreads[i].start();
        }

        for (int i = 0; i < iteratorThreads.length; i++) {
            iteratorThreads[i] = new IteratorThread(i + UPDATERS_NUM);
            iteratorThreads[i].start();
        }

        // record start time
        long startTime = System.currentTimeMillis();

        // signal threads to begin
        begin = true;

        // wait for duration
        Thread.sleep(DURATION * 1000);

        // call time
        stop = true;

        // wait for all threads to finish
        for (int i = 0; i < updaterThreads.length; i++)
            updaterThreads[i].join();
        for (int i = 0; i < iteratorThreads.length; i++)
            iteratorThreads[i].join();

        // record end time
        long endTime = System.currentTimeMillis();

        // compute elapsed time
        long elapsed = endTime - startTime;

        long totalOps = 0;
        for (int i = 0; i < updaterThreads.length; i++) {
            totalOps += updaterThreads[i].inserts.get();
            totalOps += updaterThreads[i].removals.get();
            totalOps += updaterThreads[i].contains.get();
        }

        System.out.print("Throughput (ops/ms): ");
        System.out.println(new DecimalFormat("#.##").format((double)totalOps / elapsed));

        System.gc();
    }

    public static void main(String[] args) throws InterruptedException {
        // Get command line argument
        ParseArgs(args);

        // Initialize the hash table
        InitializeSet();

        // Run test
        RunTest();
    }
}
