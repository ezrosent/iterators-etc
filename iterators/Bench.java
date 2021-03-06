import java.util.Random;
import java.text.DecimalFormat;
import gnu.getopt.Getopt;

class Bench {
    // Note that the total number of threads should be
    // no more than NUM_THREADS defined in SnapCollector.java

    public static int ITERATORS_NUM = 1;
    public static int UPDATERS_NUM = 24;
    public static int DURATION = 5;
    public static int INSERT_PERCENT = 25;
    public static int REMOVE_PERCENT = 25;
    public static int KEY_RANGE = 4096;
    public static int INIT_SIZE = 1024;
    public static String ALG_NAME = "list";
    public static boolean DEACTIVATE = false;
    
    public static volatile SetInterface set = null;
    public static volatile boolean begin = false;
    public static volatile boolean stop = false;
    
    private static boolean ParseArgs(String [] args) {
        Getopt g = new Getopt("", args, "a:i:u:d:I:R:M:s:T?h");
        int c;
        String arg = null;
        while ((c = g.getopt()) != -1)
        {
            switch(c)
            {
              case 'a':
                ALG_NAME = g.getOptarg();
                break;
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
	      case 'T':
		DEACTIVATE = true;
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
        System.out.println("  -a      set implementation (list, ubst, hash)");
        System.out.println("  -i      number of iterators");
        System.out.println("  -u      number of updaters");
        System.out.println("  -d      execution time");
        System.out.println("  -I      percentage of updates that are inserts");
        System.out.println("  -D      percentage of updates that are deletes");
        System.out.println("  -M      maximum key value");
        System.out.println("  -s      initial size of hash table");
        System.out.println("  -h      print this help text");
    }

    private static void InitializeSet() {
        if (ALG_NAME.equals("ubst"))
            Bench.set = new BinarySearchTree(DEACTIVATE);
        else if (ALG_NAME.equals("hash"))
            Bench.set = new CHashSet(DEACTIVATE);
        else
            Bench.set = new CLinkedList(DEACTIVATE);

        Random rng = new Random();
        for (int i = 0; i < INIT_SIZE; i++) {
            while (true) {
                int key = rng.nextInt(KEY_RANGE);
                if (set.insert(key, 0))
                    break;
            }
	    //if (i % 10000 == 0) System.out.println(i + " elements initialized");
        }
    }

    private static void RunTest(boolean warmup) throws InterruptedException {
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
        if (warmup)
            Thread.sleep(500);
        else
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

        long inserts = 0;
        long removals = 0;
        long contains = 0;
        for (int i = 0; i < updaterThreads.length; i++) {
            inserts += updaterThreads[i].inserts;
            removals += updaterThreads[i].removals;
            contains += updaterThreads[i].contains;
        }

        int iterations = 0;
        for (int i = 0; i < iteratorThreads.length; i++)
            iterations += iteratorThreads[i].iterations;

        long totalOps = inserts + removals + contains;
        if (!warmup)
            System.out.println(totalOps);

        System.gc();
    }

    public static void main(String[] args) throws InterruptedException {
        // Get command line argument
        if (!ParseArgs(args)) return;

        // Run test
        RunTest(true);
        RunTest(false);
    }
}
