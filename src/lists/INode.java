package lists;

public class INode{
	private int index; 
	private int size;
	private byte type;
	public INode(byte type,int size,int index)  { 
		this.index = index; 
		this.type = type;
		this.size = size;
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
	public void clean() {
		index = (Integer) null; 
		size = (Integer) null;
		type = (Byte) null;
	}
}