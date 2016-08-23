import java.util.Random;
import java.text.DecimalFormat;
import gnu.getopt.Getopt;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

class Bench {
    // Note that the total number of threads should be
    // no more than NUM_THREADS defined in SnapCollector.java

    public static int ITERATORS_NUM = 1;
    public static int UPDATERS_NUM = 24;
    public static int DURATION = 5;

    public static int KEY_RANGE = 4096;
    public static int INIT_SIZE = 1024;
    public static String ALG_NAME = "list";
    public static boolean DEACTIVATE = true;
    public static String OP_FILE = "op_file.txt";
    public static String INIT_FILE = "init_file.txt";
    public static volatile SetInterface set = null;
    public static volatile List<List<String>> ops = null;
    public static volatile boolean begin = false;
    public static volatile boolean stop = false;

    SnapCollector<Integer> test = new SnapCollector<Integer>();

    private static boolean ParseArgs(String [] args) {
        Getopt g = new Getopt("", args, "a:i:u:d:o:n:M:s:T?h");
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
              case 'o':
                OP_FILE = g.getOptarg();
                break;
              case 'n':
                INIT_FILE = g.getOptarg();
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
		DEACTIVATE = false;
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
        System.out.println("  -o      operations file");
        System.out.println("  -n      initialization file");
        System.out.println("  -M      maximum key value");
        System.out.println("  -s      initial size of hash table");
        System.out.println("  -h      print this help text");
    }

    private static void InitializeSet() throws IOException, FileNotFoundException {
        if (ALG_NAME.equals("ubst"))
            Bench.set = new BinarySearchTree(DEACTIVATE);
        else if (ALG_NAME.equals("hash"))
            Bench.set = new CHashSet(DEACTIVATE);
        else
            Bench.set = new CLinkedList(DEACTIVATE);

        // Read values from file
        BufferedReader br = new BufferedReader(new FileReader(Bench.INIT_FILE));

        String line = null;
        while ((line = br.readLine()) != null) {
            int key = Integer.parseInt(line);
            set.insert(key, 0);
        }

        br.close();
    }

    // Move file of operations into a list, for fast access
    // Divide them among the updater threads
    private static void InitializeOps() throws IOException, FileNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(Bench.OP_FILE));
        ops = new ArrayList<List<String>>();
        for (int id = 0; id < UPDATERS_NUM; id++) ops.add(new ArrayList<String>());

        int id = 0;
        String line = null;
        while ((line = br.readLine()) != null) {
            ops.get(id).add(line);
            id = (id + 1) % UPDATERS_NUM;
        }

        br.close();
    }

    private static void RunTest(boolean warmup) throws InterruptedException, IOException {
        InitializeSet();
        InitializeOps();

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

    public static void main(String[] args) throws InterruptedException, IOException {
        // Get command line argument
        if (!ParseArgs(args)) return;

        // Run test
        RunTest(true);
        RunTest(false);
    }
}
