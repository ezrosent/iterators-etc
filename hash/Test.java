import java.util.ArrayList;
import java.util.List;


public class Test {

	private static final int NUM_UPDATORS = 4;
	private static final int NUM_ITERATORS = 5;
	private static Threads[] threadUpdators = new Threads[NUM_UPDATORS];
	private static Threads[] threadIterators = new Threads[NUM_ITERATORS];
	private static  LFArrayHashSet hashSet = new LFArrayHashSet();

	// following functions are creating threads which will call specific functions when run
	public static Threads createThreadsForInsert(int i, List<Integer> listInteger) {
		return (new Threads(Integer.toString(i), i, Threads.testInsert, hashSet, listInteger));
	}

	public static Threads createThreadsForDelete(int i, List<Integer> listInteger) {
		return (new Threads(Integer.toString(i), i, Threads.testDelete, hashSet, listInteger));
	}

	public static Threads createThreadsForIterator(int i) {
		return (new Threads(Integer.toString(i), i, Threads.testIterator, hashSet, new ArrayList<Integer>()));
	}

	public static void runUpdators() {
		for (int i = 0; i < NUM_UPDATORS; i++) {
			threadUpdators[i].start();
		}
	}

	public static void waitForUpdators() {
		for (int i = 0; i < NUM_UPDATORS; i++) {
			try {
				threadUpdators[i].join();
			} catch (InterruptedException e) {
				System.out.println("Could not join");
			}
		}
	}

	public static void iterate(LFArrayHashSet hashSet) {
		// iterate over the list
		List<Integer> iterationNodes = new ArrayList<Integer>();
		iterationNodes = hashSet.iterate(0);
		System.out.println("Iteration");
		int count = 0;
		for(Integer element : iterationNodes) {
			Integer key = element;
			System.out.println("Item " + count + ": " + key);
			count++;
		}
		System.out.println("Iteration Complete (found " + count + " items).");
	}

	public static void main(String[] args)
	{
		List<Integer> listInteger = new ArrayList<Integer>();
		for(int i = 0; i < 100; i++) {
			listInteger.add(i);
		}

		// Test1: Do some insertion : Every thread tries to insert the same value
		System.out.println("**Test 1**");
		for (int i = 0; i < NUM_UPDATORS; i++) {
			threadUpdators[i] = createThreadsForInsert(i, listInteger);
		}
		runUpdators();
		waitForUpdators();
		System.out.println("Finished inserting");
		//System.out.println("Size = " + bst.size(0));
		iterate(hashSet);

		// Test2: Do some deletion : Every thread tries to delete the same value

		System.out.println("\n**Test 2**");
		for (int i = 0; i < NUM_UPDATORS; i++) {
			threadUpdators[i] = createThreadsForDelete(i, listInteger);
		}
		runUpdators();
		waitForUpdators();
		//System.out.println("Size = " + bst.size(0));
		iterate(hashSet);

		//Test3: Insertion : All threads insert different values
		System.out.println("\n**Test 3**");
		for (int i = 0; i < NUM_UPDATORS; i++) {
			listInteger.clear();
			for(int j = 1; j <= 10; j++) {
				listInteger.add(10 * i + j);
			}
			threadUpdators[i] = createThreadsForInsert(i, new ArrayList<Integer>(listInteger));
		}
		runUpdators();
		waitForUpdators();
		//System.out.println("Size = " + bst.size(1));
		iterate(hashSet);

		//Test4 Deletion: All threads delete different values
		System.out.println("\n**Test 4**");
		for (int i = 0; i < NUM_UPDATORS; i++) {
			listInteger.clear();
			for(int j = 1; j <= 10; j++) {
				listInteger.add(10 * i + j);
			}
			threadUpdators[i] = createThreadsForDelete(i, new ArrayList<Integer>(listInteger));
		}
		runUpdators();
		waitForUpdators();
		//System.out.println("Size = " + bst.size(1));
		iterate(hashSet);	

		// Test 5: Do insertions and iterations together
		System.out.println("\n**Test 5**");

		// Create multiple threads: multiple trying to insert and others iterating
		// insert threads
		for (int i = 0; i < NUM_UPDATORS; i++) {
			listInteger.clear();
			for(int j = i; j <= 10; j++) {
				listInteger.add(10 * i + j);
			}
			threadUpdators[i] = createThreadsForInsert(i, new ArrayList<Integer>(listInteger));
		}

		// iterator threads
		for (int i = 0; i < NUM_ITERATORS; i++) {
			threadIterators[i] = createThreadsForIterator(i + NUM_UPDATORS);
		}


		// start insertion by the updators
		int k = 0;
		for (k = 0; k < NUM_UPDATORS ; k++) {
			threadUpdators[k].start();
		}


		// start iteration by all the iterators
		int m = 0;
		for (m = 0; m < NUM_ITERATORS; m++) {
			threadIterators[m].start();
		}
	}
}
