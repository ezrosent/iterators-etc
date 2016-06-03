import sun.font.CreatedFontTracker;
import java.util.List;
import java.util.ArrayList;

public class Tester {

	private static final int NUMTHREADS = 4;
	private static Threads[] threadObj = new Threads[NUMTHREADS];
	private static ListIT listIT = new ListIT();

	// following functions are creating threads which will call specific functions when run
	public static void createThreadsForInsert(int i, List<Integer> listInteger) {
		threadObj[i] = new Threads(Integer.toString(i), i, Threads.testInsert, listIT, listInteger);
	}

	public static void createThreadsForDelete(int i, List<Integer> listInteger) {
		threadObj[i] = new Threads(Integer.toString(i), i, Threads.testDelete, listIT, listInteger);
	}
		
	
	public static void runThreads() {
		for (int i = 0; i < NUMTHREADS; i++) {
			threadObj[i].start();
		}
	}

	public static void waitForThreads() {
		for (int i = 0; i < NUMTHREADS; i++) {
			try {
				threadObj[i].join();
			} catch (InterruptedException e) {
				System.out.println("Could not join");
			}
		}
	}

	public static void iterate(ListIT list) {
		// iterate over the list
		List<Integer> iterationNodes = new<Integer> ArrayList();
		iterationNodes = list.iterate(0);

		for(Integer element : iterationNodes) {
			Integer key = element;
			System.out.println(key);
		}
	}
	
	public static void main(String[] args)
	{
		List<Integer> listInteger = new ArrayList<Integer>();
		/*for(int i = 0; i < 10; i++) {
			listInteger.add(i);
		}
		
		// Test1: Do some insertion : Every thread tries to insert the same value
		for (int i = 0; i < NUMTHREADS; i++) {
		    createThreadsForInsert(i, listInteger);
		}
		runThreads();
		waitForThreads();
		System.out.println("Size = " + listIT.size(0));
		iterate(listIT);
		
		// Test2: Do some deletion : Every thread tries to delete the same value
		for (int i = 0; i < NUMTHREADS; i++) {
		    createThreadsForDelete(i, listInteger);
		}
		runThreads();
		waitForThreads();
		System.out.println("Size = " + listIT.size(0));
		iterate(listIT);
		*/
		
		//Test3: Insertion : All threads insert different values
		for (int i = 0; i < NUMTHREADS; i++) {
			listInteger.clear();
			for(int j = 1; j <= 5; j++) {
				listInteger.add(5 * i + j);
			}
		    createThreadsForInsert(i, listInteger);
		    /*
		    threadObj[i].start();
		    try {
				threadObj[i].join();
			} catch (InterruptedException e) {
				System.out.println("Could not join");
			}
		    System.out.println("Size = " + listIT.size(1));
			iterate(listIT);	*/
		}
		runThreads();
		waitForThreads();
		System.out.println("Size = " + listIT.size(1));
		iterate(listIT);
	}
}
