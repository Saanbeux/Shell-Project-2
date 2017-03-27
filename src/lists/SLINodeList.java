/**
 * 
 */
package lists;

/**
 * @author pirvos
 *
 */
public class SLINodeList {
	private INode head; 
	private int length; 

	public SLINodeList() { 
		head = null; 
		length = 0; 
	}
	
	public void addFirstNode(INode nuevo) {
		// Pre: nuevo is not a node in the list
		nuevo.setNext(head); 
		head =nuevo; 
		length++; 
	}

	public void addNodeAfter(INode target, INode nuevo) {
		// Pre: target is a node in the list
		// Pre: nuevo is not a node in the list
		nuevo.setNext(target.getNext()); 
		target.setNext(nuevo); 
		length++; 
	}

	public void addNodeBefore(INode target, INode nuevo) {
		// Pre: target is a node in the list
		// Pre: nuevo is not a node in the list

		if (target == head)
			this.addFirstNode(nuevo); 
		else { 
			INode prevNode = findNodePrevTo(target);  
			this.addNodeAfter(prevNode, nuevo); 
		}
	}

	private INode findNodePrevTo(INode target) {
		// Pre: target is a node in the list
		if (target == head) 
			return null; 
		else { 
			INode prev = head; 
			while (prev != null && prev.getNext() != target) 
				prev = prev.getNext();  
			return prev; 
		}
	}

	public INode getLastNode() 
	throws NodeOutOfBoundsException 
	{
		if (head == null)
			throw new NodeOutOfBoundsException("getLastNode(): Empty list."); 
		else { 
			INode curr = head; 
			while ( curr.getNext() != null)
				curr = curr.getNext(); 
			return curr; 
		}
	}

	public INode getNodeAfter(INode target) 
	throws NodeOutOfBoundsException 
	{
		// Pre: target is a node in the list
		INode aNode = target.getNext(); 
		if (aNode == null)  
			throw new NodeOutOfBoundsException("getNextNode(...) : target is the last node."); 
		else 
			return aNode;
	}


	public INode getNodeBefore(INode target) 
	throws NodeOutOfBoundsException 
	{
		// Pre: target is a node in the list
		if (target == head)  
			throw new NodeOutOfBoundsException("getPrevNode(...) : target is the first node."); 
		else 
			return findNodePrevTo(target);
	}

	public int length() {
		return this.length;
	}

	public INode removeFirstNode() 
	throws NodeOutOfBoundsException 
	{
		if (head == null) 
			throw new NodeOutOfBoundsException("removeFirstNode(): linked list is empty."); 

		// the list is not empty....
		INode ntr = head; 
		head = head.getNext(); 
		ntr.setNext(null);      // notice that the node keeps its data..
		length--; 
		return ntr;
	}

	public INode removeLastNode() 
	throws NodeOutOfBoundsException 
	{
		if (head == null) 
			throw new NodeOutOfBoundsException("removeFirstNode(): linked list is empty."); 

		// the list is not empty....
		if (head.getNext() == null)
			return this.removeFirstNode(); 
		else { 
			INode prevNode = head; 
			INode ntr = prevNode.getNext(); 
			while (ntr.getNext() != null)
			{
				prevNode = ntr; 
				ntr = ntr.getNext(); 
			}
			prevNode.setNext(ntr.getNext()); 
			length--; 
			return ntr;
		} 
	}

	public void removeNode(INode target) {
		// Pre: target is a node in the list
		
		if (target == head) 
			this.removeFirstNode(); 
		else { 
			INode prevNode =  this.getNodeBefore(target); 
			prevNode.setNext(target.getNext()); 
			length--; 
		}
		
	}

	public INode removeNodeAfter(INode target) 
	throws NodeOutOfBoundsException 
	{
		// Pre: target is a node in the list
		if ( target.getNext() == null)
			throw new NodeOutOfBoundsException("removeNodeAfter(...) : target is the last node...");			

		INode ntr = target.getNext(); 
		target.setNext(ntr.getNext()); 
		ntr.setNext(null); 
		length--; 
		return ntr;
	}

	public INode removeNodeBefore(INode target) 
	throws NodeOutOfBoundsException 
	{
		// Pre: target is a node in the list
		if (target == head)
			throw new NodeOutOfBoundsException("removeNodeBrfore(...) : target is the first node...");			

		// head is not the first node. 
		// need to find the node before target and the one before it, if any...
		INode prevNode = this.getNodeBefore(target); 
		this.removeNode(prevNode); 
		return prevNode;
	}
	
	public INode getFirstNode() 
	throws NodeOutOfBoundsException {
		if (head == null)
			throw new NodeOutOfBoundsException("getFirstNode() : linked list is empty..."); 
		
		// the linked list is not empty....
		return head;
	}
	
	/**
	 * Prepares every node so that the garbage collector can free 
	 * its memory space, at least from the point of view of the
	 * list. This method is supposed to be used whenever the 
	 * list object is not going to be used anymore. Removes all
	 * physical nodes (data nodes and control nodes, if any)
	 * from the linked list
	 */
	private void removeAll() {
		while (head != null) { 
			INode nnode = head.getNext();
			head.setNext(null); 
			head = nnode; 
		}
	}
	
	/**
	 * The execution of this method removes all the data nodes
	 * from the current instance of the list. 
	 */
	public boolean isEmpty() { 
		return length==0;
	}
		
	protected void finalize() throws Throwable {
	    try {
			System.out.println("GC is WORKING!");
			System.out.println("Number of nodes to remove is: "+ this.length); 
			this.removeAll(); 
	    } finally {
	        super.finalize();
	    }
	}
	
	public INode createNewNode() {
		return new INode();
	}

}
