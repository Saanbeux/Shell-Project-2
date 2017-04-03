package managementClasses;

/**
 * Object that conveniently stores a physical iNode's details.
 * @author Moises
 *
 */
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
	public void setIndex(int index) {
		this.index = index;
	}
}