import sun.font.CreatedFontTracker;
import java.util.List;
import java.util.ArrayList;

public class Tester {

	private static final int NUM_UPDATORS = 4;
	private static final int NUM_ITERATORS = 5;
	private static Threads[] threadUpdators = new Threads[NUM_UPDATORS];
	private static Threads[] threadIterators = new Threads[NUM_ITERATORS];
	private static ListIT listIT = new ListIT();

	// following functions are creating threads which will call specific functions when run
	public static Threads createThreadsForInsert(int i, List<Integer> listInteger) {
		return (new Threads(Integer.toString(i), i, Threads.testInsert, listIT, listInteger));
	}

	public static Threads createThreadsForDelete(int i, List<Integer> listInteger) {
		return (new Threads(Integer.toString(i), i, Threads.testDelete, listIT, listInteger));
	}
		
	public static Threads createThreadsForIterator(int i) {
		return (new Threads(Integer.toString(i), i, Threads.testIterator, listIT, new ArrayList<Integer>()));
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

	public static void iterate(ListIT list) {
		// iterate over the list
		List<Integer> iterationNodes = new<Integer> ArrayList();
		iterationNodes = list.iterate(0);
		System.out.println("Iteration");
		for(Integer element : iterationNodes) {
			Integer key = element;
			System.out.print(key + " ");
		}
		System.out.println("");
	}
	
	public static void main(String[] args)
	{
		List<Integer> listInteger = new ArrayList<Integer>();
		for(int i = 0; i < 10; i++) {
			listInteger.add(i);
		}
		
		// Test1: Do some insertion : Every thread tries to insert the same value
		System.out.println("**Test 1**");
		for (int i = 0; i < NUM_UPDATORS; i++) {
			threadUpdators[i] = createThreadsForInsert(i, listInteger);
		}
		runUpdators();
		waitForUpdators();
		System.out.println("Size = " + listIT.size(0));
		iterate(listIT);
		
		// Test2: Do some deletion : Every thread tries to delete the same value
		System.out.println("\n**Test 2**");
		for (int i = 0; i < NUM_UPDATORS; i++) {
			threadUpdators[i] = createThreadsForDelete(i, listInteger);
		}
		runUpdators();
		waitForUpdators();
		System.out.println("Size = " + listIT.size(0));
		iterate(listIT);
		
		
		//Test3: Insertion : All threads insert different values
		System.out.println("\n**Test 3**");
		for (int i = 0; i < NUM_UPDATORS; i++) {
			listInteger.clear();
			for(int j = 1; j <= 5; j++) {
				listInteger.add(5 * i + j);
			}
			threadUpdators[i] = createThreadsForInsert(i, new ArrayList<Integer>(listInteger));
		}
		runUpdators();
		waitForUpdators();
		System.out.println("Size = " + listIT.size(1));
		iterate(listIT);
		
		//Test4 Deletion: All threads delete different values
		System.out.println("\n**Test 4**");
		for (int i = 0; i < NUM_UPDATORS; i++) {
			listInteger.clear();
			for(int j = 1; j <= 5; j++) {
				listInteger.add(5 * i + j);
			}
			threadUpdators[i] = createThreadsForDelete(i, new ArrayList<Integer>(listInteger));
		}
		runUpdators();
		waitForUpdators();
		System.out.println("Size = " + listIT.size(1));
		iterate(listIT);
		
		// Test 5: Do insertions and iterations together
		System.out.println("**Test 5**");
		
		// Create multiple threads: multiple trying to insert and others iterating
		// insert threads
		for (int i = 0; i < NUM_UPDATORS; i++) {
			listInteger.clear();
			for(int j = i; j <= 20; j++) {
				listInteger.add(20 * i + j);
			}
		    threadUpdators[i] = createThreadsForInsert(i, new ArrayList<Integer>(listInteger));
		}
		
		// iterator threads
		for (int i = 0; i < NUM_ITERATORS; i++) {
			threadIterators[i] = createThreadsForIterator(i + NUM_UPDATORS);
		}
		// start insertion by half the threads
		int k = 0;
		for (k = 0; k < NUM_UPDATORS ; k++) {
		    threadUpdators[k].start();
		}
		// start iteration by all the iterators
		int m = 0;
		for (m = 0; m < NUM_ITERATORS; m++) {
			threadIterators[m].start();
		}
		/*
		 // uncomment if some sort of interleaving between the 
		 // starting of iterators and updators is desired
		
		// start insertion by rest of the threads
		for (; k < NUM_UPDATORS; k++) {
			threadUpdators[k].start();
		}
		for (; m < NUM_ITERATORS; m++) {
			threadIterators[m].start();
		}*/
		//waitForUpdators();
		//iterate(listIT);
	}
}
