package listsManagementClasses;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import diskUtilities.DiskUnit;
import disk_Exceptions.FullDiskException;
import lists.INode;
import lists.SLINodeList;

public class DiskManager {
	RandomAccessFile diskUnitRAF;
	private DiskUnit currentDirectory;// mounted disk
	private String mountName; //name of mounted disk

	private int blockSize;
	private int capacity;

	private int freeBlockIndex; //index of the current subtree root that holds the rest of the free block indexes.
	private int endOfFreeBlockIndex; //Pointer to the next index available in subtree. 

	private int firstFreeINode; // Next available I-Node
	private int totalINodes;//Total number of I-Nodes
	private int iNodeBlockAmount;//how many blocks in total INodes will take up
	private int iNodePerBlock; //how many INodes fit in a block

	private int intsPerBlock; //how many separate numbers fit in a block

	private SLINodeList iNodeList; //Singly Linked List for the INodes

	public DiskManager(){
		currentDirectory = null;
		mountName = null;
	}



	public void prepareDiskUnit(String diskName, boolean beingMounted) throws IOException{
		//constants; will always be in the diskunit regardless if it was created recently.
		diskUnitRAF = new RandomAccessFile(diskName,"rw");
		capacity = diskUnitRAF.readInt();
		blockSize = diskUnitRAF.readInt();
		iNodeList = new SLINodeList(); //initialize linked list
		intsPerBlock = blockSize/4; //Used for amount of free block indexes that can be stored within a single block;
		totalINodes = calculateTotalINodes(); //total I-Nodes is 1% of diskUnit size
		iNodeBlockAmount = ((9*totalINodes)/blockSize); //how many blocks in total vINodes will take up
		iNodePerBlock = blockSize/9;


		if(beingMounted){ //Extract all the control information
			currentDirectory = DiskUnit.mount(diskName);
			mountName = diskName;

			freeBlockIndex = diskUnitRAF.readInt();
			endOfFreeBlockIndex = diskUnitRAF.readInt();
			firstFreeINode = diskUnitRAF.readInt();
			totalINodes = diskUnitRAF.readInt();
			for (int x=1;x<=iNodeBlockAmount;x++){ // for each INode assigned block; starting at one to skip control block
				diskUnitRAF.seek(x*blockSize); //seek the next INode block
				for (int i=0;i<iNodePerBlock;i++){ //fit as many INodes into that block as possible
					byte type = diskUnitRAF.readByte();
					int size = diskUnitRAF.readInt();
					int fileIndex = diskUnitRAF.read();
					iNodeList.addFirstNode(new INode(fileIndex,size,type)); //add iNode to list
				}
			}


		}else{
			//DiskUnit was recently created, RAF lacks required details in control other than Capacity and BlockSize.
			freeBlockIndex = iNodeBlockAmount+blockSize;//+1 to skip last INodesBlock index
			endOfFreeBlockIndex = freeBlockIndex; //The block starts empty
			firstFreeINode = blockSize+9; //First free INode in a formated disk is the first block after control, offsetting root I-Node.
			formatINodeSpace(); // reserves the iNode blocks
			formatFreeBlockSpace();		
			diskUnitRAF.seek(8); // skips Capacity and BlockSize

			//Write all details onto control block
			diskUnitRAF.writeInt(freeBlockIndex); 
			diskUnitRAF.writeInt(endOfFreeBlockIndex);
			diskUnitRAF.writeInt(firstFreeINode);
			diskUnitRAF.writeInt(totalINodes);
		}
	}








	//////////free block managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public int getFreeBlockIndex() throws FullDiskException{
		int availableBlockIndex = 0; //0 so that it won't throw errors
		if (freeBlockIndex==0){ // the disk is full.
			throw new FullDiskException ("Disk is full!");
		}
		if (endOfFreeBlockIndex!=freeBlockIndex){
			try {
				diskUnitRAF.seek(endOfFreeBlockIndex);//set pointer at the next int to read within this block's "array"
				availableBlockIndex = diskUnitRAF.readInt(); //read next int; the index of the next free block.
			}catch(Exception e){}
			endOfFreeBlockIndex=endOfFreeBlockIndex-4; // move pointer to the next index in the array
		}else{
			availableBlockIndex = freeBlockIndex; // this block is the next one that's available;
			try{
				diskUnitRAF.seek(freeBlockIndex); // seek this block's index
				freeBlockIndex = diskUnitRAF.readInt(); //The next block that stores free blocks is at this block's root. (Returns 0 if it is the last free block)
			}catch (Exception e){}
			endOfFreeBlockIndex = freeBlockIndex+blockSize-4; //endOfFreeBlockIndex+blockSize yields the next block; -4 yields the last int of this block. Reset necessary for next block's array.
		}
		try {
			diskUnitRAF.seek(8); // write the new indexes for the free block manager and its current pointer.
			diskUnitRAF.writeInt(freeBlockIndex);
			diskUnitRAF.writeInt(endOfFreeBlockIndex);
		} catch (IOException e) {}

		return availableBlockIndex;
	}





	void registerFreeBlocks(int blockIndex) { 
		if (freeBlockIndex == 0)  { //There were no free blocks previous to this
			freeBlockIndex = blockIndex; // This new registered block will become tree root.
			try{
				diskUnitRAF.seek(freeBlockIndex); // write on the tree root's array's first index 0, indicating there is no parent.
				diskUnitRAF.writeInt(0);
			}catch(Exception e){}
			endOfFreeBlockIndex = freeBlockIndex; //The array is currently empty; so the next available free block is itself.
		}  
		else if (endOfFreeBlockIndex == freeBlockIndex+blockSize-4) {      // the root node in the tree is full
			try{
				diskUnitRAF.seek(blockIndex); //This block will become a new root. 
				diskUnitRAF.writeInt(freeBlockIndex); //First index references parent node. 
			}catch (Exception e){}
			endOfFreeBlockIndex = blockIndex; //The array is currently empty; so the next available free block is itself.
			freeBlockIndex = blockIndex; //Next free block will be picked from the lowest tree root. 
		}  
		else { //there is space on the current subtree root.
			endOfFreeBlockIndex=endOfFreeBlockIndex+4; //the current index has an int; move it to the next index.
			try{
				diskUnitRAF.seek(endOfFreeBlockIndex); //write on current index the location of the next free block.
				diskUnitRAF.writeInt(blockIndex);
			}catch(Exception e){}
		}
		try {
			diskUnitRAF.seek(8); // write the new indexes for the free block manager and its current pointer.
			diskUnitRAF.writeInt(freeBlockIndex);
			diskUnitRAF.writeInt(endOfFreeBlockIndex);
		} catch (IOException e) {} 
	}     


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public int getFreeINode() throws FullDiskException{ //returns index of an available I-Node
		if (firstFreeINode==(iNodeBlockAmount+(iNodePerBlock*9))||totalINodes==1){ //i-Nodes are full; only root iNode available
			throw new FullDiskException ("Disk does not have space for a new file!");
		}
		//check if I Node is at end of block; account for space leftover.
		return 0;
	}


	///////Constructor helpers//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void formatFreeBlockSpace() throws IOException{
		int stillFreeSpace = capacity-iNodeBlockAmount; //Amount of FreeBlocks left in an empty DiskUnit.
		int currentBlock = freeBlockIndex; //Start at the first block after INodes
		while (stillFreeSpace>0){ //while there is still free space
			registerFreeBlocks(currentBlock); //register the next block
			currentBlock=currentBlock+blockSize; //go to the next block
			stillFreeSpace = stillFreeSpace-1; //reduce the amount remaining by 1 block
		}
	}




	private void formatINodeSpace() throws IOException{
		byte emptyByte = -1; // type byte that will indicate if the INode is being used or not with -1
		int emptyInt=0; //int to fill 4 bytes
		for (int x=1;x<=iNodeBlockAmount;x++){ // for each INode assigned block; starting at one to skip control block
			diskUnitRAF.seek(x*blockSize); //seek the next INode block
			for (int i=0;i<iNodePerBlock;i++){ //fit as many INodes into that block as possible
				diskUnitRAF.writeByte(emptyByte);//1 byte
				diskUnitRAF.writeInt(emptyInt);//4 bytes
				diskUnitRAF.writeInt(emptyInt); // 4 bytes; 9 bytes total
				iNodeList.addFirstNode(new INode(emptyInt,emptyInt,emptyByte));
			}
		}
		diskUnitRAF.seek(blockSize+8); //Setting the root file; offsets control.
		diskUnitRAF.writeInt(1); //Root i-Node is a directory.
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int calculateTotalINodes() {
		if (((blockSize*capacity)/900)<1){
			return 1;
		}
		return ((blockSize*capacity)/900);
	}
	public String getDiskName(){
		return mountName;
	}
	public boolean isMounted(){
		return currentDirectory!=null;
	}

	public void stop() {
		try {
			diskUnitRAF.close(); //close this raf
		} catch (IOException e) {}
		currentDirectory.shutdown(); //only useful if we're actually using diskunit methods
		currentDirectory=null;
		mountName=null;
	}

	public boolean fileExists() {
		// TODO Auto-generated method stub
		return false;
	}
}
