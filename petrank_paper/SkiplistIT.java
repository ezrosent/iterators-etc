


public class SkiplistIT {
	
	LeaSourceWithIT2<Integer, Boolean> inner = new LeaSourceWithIT2<Integer,Boolean>();

	public boolean contains(int tid, int key) {
		return inner.containsKey(key, tid);
	}

	public boolean insert(int tid, int key) {
		return inner.putIfAbsent(key, true, tid) == null;
	}

	public boolean delete(int tid, int key) {
		return inner.remove(key, tid) != null;
	}

	public int size(int tid) {
		return inner.iterSize(tid);
	}
}
