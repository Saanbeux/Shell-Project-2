package lists;

public class INode{
	private int index; 
	private int size;
	private byte type;
	private INode next; 
	public INode() { 
		next = null; 
	}
	public INode(byte type,int size,int index, INode next) { 
		this.index = index; 
		this.size = size;
		this.type = type;
		this.next = next; 
	}
	public INode(byte type,int size,int index)  { 
		this.index = index; 
		next = null; 
	}
	public int getIndex() {
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
	public void clean() {
		index = (Integer) null; 
		size = (Integer) null;
		type = (Byte) null;
		next = null; 
	}
}