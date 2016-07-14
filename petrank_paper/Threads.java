import java.util.List;
import java.util.ArrayList;
import java.util.function.BiFunction;

class Threads extends Thread {
	// thread properties
	private String threadName;  
	public int tid;

	// list on which thread operates on
	public ListIT list;

	// function which threads execute when being run
	// function accepts two arguments: Threads object and a list
	// list contains the items that are to be inserted or deleted from ListIT list
	public Object output;
	BiFunction<Threads, List<Integer>, Object> f;
	// this is the list that f will work on
	List<Integer> listInteger = new ArrayList<Integer>();

	// sort of a function pointer
	public static BiFunction<Threads, List<Integer>, Object> testInsert = (Threads t, List<Integer> l) -> {
		for(Integer element : l) {
			//System.out.println("Inserting "+ element);
			t.list.insert(t.tid, element);
		}
		return true; 
	};

	// sort of a function pointer
	public static BiFunction<Threads, List<Integer>, Object> testDelete = (Threads t, List<Integer> l) -> {   
		for(Integer element : l) {
			t.list.delete(t.tid, element);
		}
		return true; 
	};

	// List l is dummy. Not used
	public static BiFunction<Threads, List<Integer>, Object> testIterator = (Threads t, List<Integer> l) -> {
		List<Integer> iterationNodes = new<Integer> ArrayList();
		iterationNodes = t.list.iterate(t.tid);
		
		StringBuilder finalString = new StringBuilder();
		finalString.append("Iteration by thread ");
		finalString.append(t.threadName + " : ");
		for(Integer element : iterationNodes) {
			finalString.append(element + " ");
		}
		System.out.println(finalString);
		return true; 
	};

	public Threads(String name, int threadID, BiFunction<Threads, List<Integer>, Object> f, ListIT listIterator, List<Integer> listInteger) {
		this.threadName = name;
		this.tid = threadID;
		this.f = f;
		this.list = listIterator;
		this.listInteger = listInteger;
	}

	/*
   public static Function<Threads, Object> test2(int magic) {
	   return (Threads t) -> {
		   t.list.insert(t.tid, magic);
		   return magic;
	   };
   }
	 */

	public void run() {
		//System.out.println("Running thread" + this.tid);
		//System.out.println(this + " ooooo " + this.list);
		//System.out.println(this.listInteger);
		output = f.apply(this, this.listInteger);
		//System.out.println("Exiting thread" + this.tid);
	}
}