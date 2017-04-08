
// This class is just used to return multiple values from arrayInsert and arrayRemove
public class DummyWrapper 
{
    public HashNode[] arr; // new FSet created by arrayInsert and arrayRemove
    public HashNode node; // node that is being inserted or deleted
    public int retVal; // return value
    	
    public DummyWrapper() {
    	arr = null;
    	node = null;
	retVal = 0;
    }
    	
}
