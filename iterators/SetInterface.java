import java.util.List;

interface SetInterface
{
    boolean insert(int key, int tid);
    boolean delete(int key, int tid);
    boolean search(int key, int tid);
    List<Integer> iterate(int tid);

    // These are specific to hash table
    //boolean simpleInsert(int key, int tid);
    //boolean simpleDelete(int key, int tid);

    //boolean grow();
    //boolean shrink();
    //int getBucketSize();
}
