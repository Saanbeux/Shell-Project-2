package diskUtilities;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidParameterException;

import disk_Exceptions.ExistingDiskException;
import disk_Exceptions.InvalidBlockException;
import disk_Exceptions.InvalidBlockNumberException;
import disk_Exceptions.NonExistingDiskException;

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
	 *    �disk� with the specified name is found.
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
	 * @return capacity
	 */
	public int getCapacity(){
		return capacity;
	}

	/**
	 * Returns the size of the blocks in bytes available in the Disk Unit.
	 * @return blockSize
	 */
	public int getBlockSize(){
		return blockSize;
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
		if (blockNum<0||blockNum>=capacity){//Skips block 0; where the capacity and blockSize is held.
			throw new InvalidBlockNumberException("Block number is not a valid index: " + blockNum);
		}
		else if (b==null||b.getCapacity()!=blockSize){
			throw new InvalidBlockException("This block is not of compatible size with the disk unit!");
		}else{
			try {
				disk.seek(blockSize*(blockNum+1));
				for (int x=0; x<b.getCapacity();x++){
					disk.write(b.getElement(x));
				}//write content of block onto index
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;
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
		else if (b==null || b.getCapacity()!=blockSize){
			throw new InvalidBlockException("This block is not of compatible size with Disk Unit!");
		}else{
			try {
				disk.seek(blockSize*(blockNum+1)); //Starting from the 2nd block onward, block 1. Block 0 is reserved.
				for (int x=0; x<b.getCapacity();x++){
					b.setElement(x,disk.readByte());
				} // write content of disk on to new block.
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;
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
