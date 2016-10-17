import java.util.concurrent.atomic.*;
import java.util.List;
import java.util.ArrayList;

/*
interface IList
{
	public boolean insert (int tid, int key);
	public boolean delete (int tid, int key);
	public boolean contains(int tid, int key);
	public boolean IsEmpty ();
	public int size();
	public void PrintList();
}

interface CList
{
	public boolean insert (int tid, int key);
	public boolean delete (int tid, int key);
	public boolean contains(int tid, int key);
	public int size();
}

interface ITList
{
	public boolean insert (int tid, int key);
	public boolean delete (int tid, int key);
	public boolean contains(int tid, int key);
	public int size(int tid);
}*/
public class CLinkedList implements SetInterface {
	
	class Node
	{
		int key;
		AtomicMarkableReference<Node> next;
		
		public Node (int val)
		{
			key = val;
		}
	}
	
	Node head;
	Node tail;
	
	
	public CLinkedList(boolean dectivate)
	{
		head = new Node(Integer.MIN_VALUE);
		tail = new Node(Integer.MAX_VALUE);
		head.next = new AtomicMarkableReference<Node>(tail, false);
		tail.next = new AtomicMarkableReference<Node>(tail, false);
	}
	
	class Window {
		public Node pred, curr;
		Window(Node myPred, Node myCurr)
		{
			pred = myPred; curr = myCurr;
		}		
	}
	
	public Window find(Node head, int key)
	{
		Node pred = null, curr = null, succ = null;
		boolean[] marked = {false};
		boolean snip;
		retry : while (true) {
			pred = head;
			curr = pred.next.getReference();
			while (true)
			{
				succ = curr.next.get(marked);
				while (marked[0])
				{
					snip = pred.next.compareAndSet(curr, succ, false, false);
					if (!snip) continue retry;
					curr = succ;
					succ = curr.next.get(marked);					
				}
				if (curr.key >= key)
				{
					return new Window(pred, curr);
				}
				pred = curr;
				curr = succ;
			}
		}
	}
	
	public boolean insert(int key, int tid) 
	{
		while (true)
		{
			Window window = find(head, key);
			Node pred = window.pred, curr = window.curr;
			if (curr.key == key)
			{
				return false;
			}
			else
			{
				Node node = new Node(key);
				node.next = new AtomicMarkableReference<Node>(curr, false);
				if (pred.next.compareAndSet(curr, node, false, false))
					return true;
			}
		}
	}
	
	public boolean delete(int key, int tid)
	{
		boolean snip;
		while (true)
		{
			Window window = find(head, key);
			Node pred = window.pred, curr = window.curr;
			if (curr.key != key)
			{
				return false;
			}
			else
			{
				Node succ = curr.next.getReference();
				snip = curr.next.compareAndSet(succ, succ, false, true);
				if (!snip)
					continue;
				pred.next.compareAndSet(curr, succ, false, false);
				return true;
			}
		}
	}
	
	public boolean search(int key, int tid) {
		boolean[] marked = {false};
		Node curr = head;
		while (curr.key < key) { // search for the key
			curr = curr.next.getReference();
			curr.next.get(marked);
		}
		return (curr.key == key && !marked[0]); // the key is found and is logically in the list.
	}
	
	public List<Integer> iterate(int tid) {
	    List<Integer> list = new ArrayList<Integer>();
	    // simply go through the list

	    List<Node> list_node = new ArrayList<Node>();
	    Node curr = head.next.getReference();
	    while (curr != tail) {
		  //  System.out.println("Added " + curr.key);
		list_node.add(curr);
		curr = curr.next.getReference();
	    }
	    return list;
	}
}
	
	
