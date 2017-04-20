
// This class is just used to return multiple values from arrayInsert and arrayRemove
public class DummyWrapper 
{
    public HashNode[] arr; // new FSet created by arrayInsert and arrayRemove

    // seek record
    public FSet fset;
    public HashNode node; //used to return a HashNode around
    public int retVal; // any integer that has to be moved around. In seek record, it is the bucket index
    	
    public DummyWrapper() {
    	arr = null;
    	node = null;
	retVal = 0;
    }
    	
}
