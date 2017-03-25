package diskUtilities;

/**
 * 
 *Sections of a whole "Disk Unit". Minimum capacity of
 *8 bytes, standard capacity of 256 bytes; stored in an internal array.
 * @author Moises Garip
 */
public class VirtualDiskBlock {
	//variables
	private int blockCapacity;
	/**
	 * Internal array where each block's bytes are stored. Of size blockCapacity.
	 */
	private byte[] blockContent;

	//constructors
	/**
	 * Creates a VirtualDiskBlock with an internal array of default capacity.
	 */
	public VirtualDiskBlock(){ //default block size is 256
		blockCapacity = 256;
		blockContent = new byte[256];
	}
	/**
	 * Creates a VirtualDiskBlock with an internal array of a specified capacity.
	 */
	public VirtualDiskBlock(int blockSize){ //minimum is 8 bytes, Disk Units cannot be created with less than 8 per block.
		this.blockCapacity = blockSize;
		this.blockContent = new byte[blockSize];
	}
	//Methods
	/**
	 * Returns the size of the VirtualDiskBlock in bytes.
	 */
	public int getCapacity(){ // returns the capacity of the block
		return blockCapacity;
	}
	/**
	 * Sets byte within the VirtualDiskBlock's internal array's index to nuevo.
	 */
	public void setElement(int index, byte nuevo){ // sets the byte at index as nuevo
		blockContent[index] = nuevo;
	}
	/**
	 * Returns the byte at the VirtualDiskBlock's internal array's index.
	 * @return Byte at blockContent[index].
	 */
	public byte getElement(int index){ // returns the byte at index
		return blockContent[index];
	}
}
