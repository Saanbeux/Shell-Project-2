package lists;

public class INode{
	private int index; 
	private int size;
	private byte type;
	private INode next; 
	public INode() { 
		next = null; 
	}
	public INode(int index,int size,byte type, INode next) { 
		this.index = index; 
		this.size = size;
		this.type = type;
		this.next = next; 
	}
	public INode(int index,int size,byte type)  { 
		this.index = index; 
		next = null; 
	}
	public int getElement() {
		return index;
	}
	public int getSize(){
		return size;
	}
	public byte getType(){
		return type;
	}
	public void setElement(int index) {
		this.index = index;
	}
	public INode getNext() {
		return next;
	}
	public void setNext(INode next) {
		this.next = next;
	}
}