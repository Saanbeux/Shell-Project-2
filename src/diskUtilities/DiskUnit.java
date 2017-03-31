package diskUtilities;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidParameterException;

import disk_Exceptions.ExistingDiskException;
import disk_Exceptions.InvalidBlockException;
import disk_Exceptions.InvalidBlockNumberException;
import disk_Exceptions.NonExistingDiskException;
import listsManagementClasses.DiskManager;

/**
 * A sequence of virtual disk blocks physically stored within a 
 *file of definite size; minimum size of 2 blocks, standard size of 1024
 *blocks.
 * @author Moises Garip
 */
public class DiskUnit { 
	private final static int DEFAULT_CAPACITY = 1024;  // default number of blocks     
	private final static int DEFAULT_BLOCK_SIZE = 256; // default number of bytes per block

	private int capacity;         // number of blocks in current disk instance
	private int blockSize;     // size of each block of current disk instance

	// the file representing the simulated  disk, where all the disk blocks
	// are stored
	private RandomAccessFile disk;

	// the constructor -- PRIVATE
	/**
    @param name is the name of the disk created
	 **/
	private DiskUnit(String name) {
		try {
			disk = new RandomAccessFile(name, "rw");
		}
		catch (IOException e) {
			System.err.println ("Unable to start the disk");
			System.exit(1);
		}
	}


	/** Simulates shutting-off the disk. Just closes the corresponding RAF. **/
	public void shutdown() {
		try {
			disk.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Turns on an existing disk unit whose name is given. If successful, it makes
	 * the particular disk unit available for operations suitable for a disk unit.
	 * @param name the name of the disk unit to activate
	 * @return the corresponding DiskUnit object
	 * @throws NonExistingDiskException whenever no
	 *    disk with the specified name is found.
	 */
	public static DiskUnit mount(String name)
			throws NonExistingDiskException
	{
		File file=new File(name);
		if (!file.exists())
			throw new NonExistingDiskException("No disk has name : " + name);

		DiskUnit dUnit = new DiskUnit(name);
		// get the capacity and the block size of the disk from the file
		// representing the disk
		try {
			dUnit.disk.seek(0);
			dUnit.capacity = dUnit.disk.readInt();
			dUnit.blockSize = dUnit.disk.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dUnit;         
	}

	/***
	 * Creates a new disk unit with the given name. The disk is formatted
	 * as having default capacity (number of blocks), each of default
	 * size (number of bytes). Those values are: DEFAULT_CAPACITY and
	 * DEFAULT_BLOCK_SIZE. The created disk is left as in off mode.
	 * @param name the name of the file that is to represent the disk.
	 * @throws ExistingDiskException whenever the name attempted is
	 * already in use.
	 * @throws IOException 
	 */

	public static void createDiskUnit(String name) throws ExistingDiskException
	{
		try {
			createDiskUnit(name, DEFAULT_CAPACITY, DEFAULT_BLOCK_SIZE);
		} catch (InvalidParameterException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Creates a new disk unit with the given name. The disk is formatted
	 * as with the specified capacity (number of blocks), each of specified
	 * size (number of bytes).  The created disk is left as in off mode.
	 * @param name the name of the file that is to represent the disk.
	 * @param capacity number of blocks in the new disk
	 * @param blockSize size per block in the new disk
	 * @throws ExistingDiskException whenever the name attempted is
	 * already in use.
	 * @throws InvalidParameterException whenever the values for capacity
	 *  or blockSize are not valid according to the specifications
	 * @throws IOException 
	 */
	public static void createDiskUnit(String name, int capacity, int blockSize)
			throws ExistingDiskException, InvalidParameterException
	{
		File file=new File(name);
		if (file.exists())
			throw new ExistingDiskException("Disk name is already in use: " + name);

		RandomAccessFile disk = null;
		if (capacity < 2 || blockSize < 8 || // Capacity has a minimum of 2, blockSize has a minimum of 8.
				!((capacity&-capacity)==capacity) || !((blockSize&-blockSize)==blockSize)) // checking if blockSize and capacity are powers of two
			throw new InvalidParameterException("Invalid values: " +
					" capacity = " + capacity + " block size = " +
					blockSize);
		// disk parameters are valid... hence create the file to represent the
		// disk unit.
		try {
			disk = new RandomAccessFile(name, "rw");
		}
		catch (IOException e) {
			System.err.println ("Unable to start the disk");
			System.exit(1);
		}
		reserveDiskSpace(disk, capacity, blockSize);

		// after creation, just leave it in shutdown mode - just
		// close the corresponding file
		try {
			disk.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the amount of blocks available in the Disk Unit.
	 * Measured in bytes.
	 * @return capacity
	 */
	public int getCapacity(){
		return capacity;
	}

	/**
	 * Returns the size of the blocks in bytes available in the Disk Unit.
	 * Measured in bytes.
	 * @return blockSize
	 */
	public int getBlockSize(){
		return blockSize;
	}
	/**
	 * Returns the index of the current subtree that holds part of the free blocks.
	 * Measured by block indexes.
	 * @return freeBlockIndex
	 */
	public int getFreeBlockIndex(){
		int itr=0;
		try{
			disk.seek(8);
			itr = disk.readInt();
		}catch(IOException e){}
		return itr;
	}
	/**
	 * Returns the last child of the subtree that holds part o the free blocks. Returns
	 * the subtree root if there are no other children.
	 * Measured by index within a block, ranging rom 0 to blockSize.
	 * @return endOfFreeBlockIndex
	 */
	public int getEndOfFreeBlockIndex(){
		int itr=0;
		try{
			disk.seek(12);
			itr = disk.readInt();
		}catch(IOException e){}
		return itr;
	}
	/**
	 * Returns the next available INode for managing files.
	 * Measured in indexes from 0(root) to totalINodes-1().
	 * @return firstFreeINode
	 */
	public int getFirstFreeINode(){
		int itr=0;
		try{
			disk.seek(16);
			itr = disk.readInt();
		}catch(IOException e){}
		return itr;
	}
	/**
	 * Returns the total amount of INodes available to the Disk Unit.
	 * @return totalINodes
	 */
	public int getTotalINodes(){
		int itr=0;
		try{
			disk.seek(20);
			itr = disk.readInt();
		}catch(IOException e){}
		return itr;
	}

	public void setFreeBlockIndex(int freeBlockIndex){
		try{
			disk.seek(8);
			disk.writeInt(freeBlockIndex);
		}catch(IOException e){}
	}

	public void setEndOfFreeBlockIndex(int endOfFreeBlockIndex){
		try{
			disk.seek(12);
			disk.writeInt(endOfFreeBlockIndex);
		}catch(IOException e){}
	}
	public void setFirstFreeINode(int firstFreeINode){
		try{
			disk.seek(16);
			disk.writeInt(firstFreeINode);
		}catch(IOException e){}
	}



	private static void reserveDiskSpace(RandomAccessFile disk, int capacity,
			int blockSize)
	{
		try {
			disk.setLength(blockSize * (capacity+1)); // One extra block will be added for saving Capacity and blockSize
		} catch (IOException e) {
			e.printStackTrace();
		}

		// write disk parameters (number of blocks, bytes per block) in
		// block 0 of disk space

		try {
			disk.seek(0);
			disk.writeInt(capacity);  
			disk.writeInt(blockSize);
			disk.writeInt(0); //no registered blocks yet.
			disk.writeInt(0); //index is empty; the only one available is itself. Measured block indexes, from 0 to blockSize
			disk.writeInt(0); //first available i-Node; Measured in position of INode
			if (((capacity*blockSize)/900)< blockSize/9){
				disk.writeInt(blockSize/9);//total iNodes available
			}else{
				disk.writeInt((capacity*blockSize)/900);//total iNodes available
			}
			////////////////////////////////////////////////////
		} catch (IOException e) {
			e.printStackTrace();
		}     
	}


	/**
	 * Takes a separate VirtualDiskBlock and copies its contents on to the Disk Unit at a given block index, overwriting the block at that index.
	 * @param blockNum index of block that will be modified within Disk Unit.
	 * @param b Virtual Disk Block that will overwrite the block at index.
	 * @throws InvalidBlockNumberException whenever blockNum is not a valid block index within the Disk Unit.
	 * @throws InvalidBlockException whenever the overwriting block is not of compatible size with the Disk Unit.
	 */
	public void write(int blockNum, VirtualDiskBlock b) throws InvalidBlockNumberException, InvalidBlockException{
		if (blockNum<0||blockNum>=capacity){
			throw new InvalidBlockNumberException("Block number is not a valid index: " + blockNum);
		}
		else if (b.equals(null)||b.getCapacity()!=blockSize){
			throw new InvalidBlockException("This block is not of compatible size with the disk unit!");
		}else{
			try {
				disk.seek(blockSize*(blockNum+1));//Skips block 0; where the capacity and blockSize is held.
				for (int x=0; x<b.getCapacity();x++){
					disk.skipBytes(x);
					disk.writeByte(b.getElement(x));
					disk.seek(blockSize*(blockNum+1));
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Takes a block at a given index within the Disk Unit and copies its contents on to a separate VirtualDiskBlock.
	 * @param blockNum index of block that will be copied within Disk Unit.
	 * @param b Virtual Disk Block that will be overwritten by the Disk Unit.
	 * @throws InvalidBlockNumberException whenever blockNum is not a valid block index within the Disk Unit.
	 * @throws InvalidBlockException whenever the overwriting block is not of compatible size with the Disk Unit.
	 */
	public void read(int blockNum, VirtualDiskBlock b) throws InvalidBlockNumberException, InvalidBlockException{
		if (blockNum<0||blockNum>=capacity){
			throw new InvalidBlockNumberException("Block number not a valid index: " + blockNum);
		}
		else if (b.getCapacity()!=blockSize){
			throw new InvalidBlockException("This block is not of compatible size with Disk Unit!");
		}else{
			try {
				disk.seek(blockSize*(blockNum+1)); //Starting from the 2nd block onward, block 1. Block 0 is reserved.
				for (int x=0; x<b.getCapacity();x++){
					disk.skipBytes(x);
					b.setElement(x, disk.readByte());
					disk.seek(blockSize*(blockNum+1));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * Replaces all bytes within the Disk Unit with 0; excludes the reserved block where Capacity and Block Size are held.
	 * @throws IOException
	 */
	public void lowLevelFormat() throws IOException{
		disk.seek(blockSize); // The capacity and blockSize are stored in the first block; these are not formatted.
		for (int x=0; x<capacity*blockSize;x++){ // format each byte in disk to 0.
			disk.write(0);
		}
	}
}
