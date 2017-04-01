package listsManagementClasses;

import java.awt.geom.Point2D;

import diskUtilities.DiskUnit;
import diskUtilities.DiskUtils;
import diskUtilities.VirtualDiskBlock;
import disk_Exceptions.FullDirectoryException;
import disk_Exceptions.FullDiskException;
import lists.INode;

public class DiskManager {
	DiskUnit diskUnit;
	private INode currentDirectory;
	private String mountName; //name of mounted disk

	private int capacity;
	private int blockSize;
	private int freeBlockIndex;
	private int endOfFreeBlockIndex;
	private int firstFreeINode;
	private int totalINodes;

	public DiskManager(){}

	public void prepareDiskUnit(String diskName){ //prepares recently created DiskUnit
		//constants; will always be in the diskunit regardless if it was created recently.
		diskUnit = DiskUnit.mount(diskName);

		capacity = diskUnit.getCapacity();
		blockSize = diskUnit.getBlockSize();
		freeBlockIndex = diskUnit.getFreeBlockIndex();
		endOfFreeBlockIndex = diskUnit.getEndOfFreeBlockIndex();
		firstFreeINode = diskUnit.getFirstFreeINode();
		totalINodes = diskUnit.getTotalINodes();


		formatFreeBlockSpace(); //prepare free blocks in DiskUnit
		formatINodeSpace(); // prepare INodes in DiskUnit
		int rootFileSize = blockSize; //test size for root file in directory

		//creates a root of size blockSize, sets the file's size to how many blocks it actually takes up.
		addINode(new INode((byte)0,((int)Math.ceil((double)rootFileSize/((double)blockSize-4.0)))*blockSize,prepareFreeBlocksForUse(rootFileSize))); //create root file's INode.
		stop();
		diskUnit.shutdown();
	}

	public void mount(String diskName){
		diskUnit = DiskUnit.mount(diskName);

		capacity = diskUnit.getCapacity();
		blockSize = diskUnit.getBlockSize();
		freeBlockIndex = diskUnit.getFreeBlockIndex();
		endOfFreeBlockIndex = diskUnit.getEndOfFreeBlockIndex();
		firstFreeINode = diskUnit.getFirstFreeINode();
		totalINodes = diskUnit.getTotalINodes();


		currentDirectory = getINodeIndexAtDirectoryIndex(0);
		mountName = diskName;
	}

	////////formatters////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void formatINodeSpace(){ //creates a stacked linked-list of iNodes. The last iNode will always point to 0.
		int remainingINodes = totalINodes;
		int nextINode = blockSize+9; //next available INode
		int currentBlock = 0; //start at first block
		while (remainingINodes>0){ //while there are INodes left to register
			int currentIndex = 5; //start at first INode-in-block's "fileIndex" property; resets counter.
			int maxINodesPerBlock = blockSize/9; // reset counter
			VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize); 
			while (maxINodesPerBlock>1){ // while there is still space in block
				DiskUtils.copyIntToBlock(vdb, currentIndex, nextINode); //write next available INode on to current iNode's fileINdex property.
				currentIndex+=9; //advance to next index within block
				nextINode+=9; //Advance to next INode to store
				maxINodesPerBlock--; //counters are reduced
				remainingINodes--;
			}//The while loop stops at one to be able to account for missing space at end of block, last INode in block has to point at first INode in next block during a format.
			remainingINodes--;
			if (remainingINodes==0){//there are no more INodes; last iNode in block points to nothing.
				diskUnit.write(currentBlock, vdb);
				return;
			}
			currentBlock++;
			nextINode = (currentBlock*blockSize); //get firstINode at next block.
			DiskUtils.copyIntToBlock(vdb, currentIndex, nextINode); //copy first INode of next block onto lastiNode of previous block.
			diskUnit.write(currentBlock, vdb);
		}
	}


	private void formatFreeBlockSpace(){ //creates a tree of free blocks, where the root's parent is 0. 
		int totalINodeBlocks = (int)Math.ceil((double)capacity/100);
		int stillFreeSpace = capacity-totalINodeBlocks; //Amount of FreeBlocks left in an empty DiskUnit
		int currentBlock = totalINodeBlocks; //block after iNodes
		while (stillFreeSpace>0){ //while there is still free space
			registerFreeBlocks(currentBlock); //register the next block
			currentBlock+=1; //go to the next block
			stillFreeSpace--; //reduce the amount remaining by 1 block
		}
	}


	/////////File Managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public int findInDirectory(String fileName){
		int currentBlock = currentDirectory.getIndex();//start at current directory
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		while (currentBlock!=0){ // while still within directory
			diskUnit.read(currentBlock, vdb); //read current block in directory
			int counter=0; //counter for total bytes in blocks
			while(counter<=blockSize){//there are still files in directory
				String tempName = ""; //empty string to concat chars.
				int stringCounter=0; //counter for current string
				while(stringCounter<20){
					tempName = tempName+DiskUtils.getCharFromBlock(vdb, counter);
					stringCounter++;
				}
				if (tempName.equals(fileName)){//at file.
					return (((currentBlock*blockSize)-blockSize)+counter);
				}
				counter+=24;//advance to next file
			}
			currentBlock = DiskUtils.getIntFromBlock(vdb, blockSize-4); //get next block in file by reading end of block's int
		}
		vdb=null;
		return -1;//file does not exist
	}

	private int deleteFileInDirectory(String fileName){
		int fileIndex = findInDirectory(fileName);
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read((int) getBlockIndexWithPosition(fileIndex).getX(), vdb); //read block where file is held
		int indexOfINode = DiskUtils.getIntFromBlock(vdb, fileIndex+20); //get where iNode of file is held
		int index = (int) getBlockIndexWithPosition(fileIndex).getY();
		for(int x=0; x<24;x++){//deletes from directory
			vdb.setElement(index,(byte) 0);
			index++;
		}
		reclaimFreeBlocks(getINodeIndexAtDirectoryIndex(indexOfINode).getIndex());//deletes INode's file
		reclaimINode(indexOfINode);//adds file as available file
		return fileIndex+24;//return former position of file for rewritting
	}


	private int copyFileContents(String fileName){//copies file's contents, returns index of copy blocks.
		//gets file in directory, reads it's iNode, returning the location of the iNode's file
		int fileIndex = getINodeIndexAtDirectoryIndex(findInDirectory(fileName)+20).getIndex(); //gets index of file in directory
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		int currentBlock = getNextFreeBlock();
		int btr=currentBlock; //first block in copied file
		while(fileIndex!=0){ //while there are still blocks in the file
			diskUnit.read(fileIndex,vdb); //read block
			fileIndex = DiskUtils.getIntFromBlock(vdb, blockSize-4); //queue next block
			if (fileIndex!=0){ //While not at end
				DiskUtils.copyIntToBlock(vdb, blockSize-4,getNextFreeBlock()); //the next block has to be registered
			}
			diskUnit.write(currentBlock, vdb);//Write contents of other block plus reference to the next one
			currentBlock = DiskUtils.getIntFromBlock(vdb, blockSize-4); //queue next copied block.
		}
		return btr;
	}

	public void loadFileInDirectory(String fileToRead,String fileToOverwrite){//copies file onto new file; overwritting an existing file.
		INode ntc = getINodeIndexAtDirectoryIndex(findInDirectory(fileToRead)+20); //gets index of iNode to read within directory
		ntc.setIndex(copyFileContents(fileToRead));//leave iNode as is, but set new index to the recently created copy.
		setINodeAtIndex(findInDirectory(fileToOverwrite)+20,ntc);//replace file to overwrite's iNode with file to read's iNode that now points to new copy of file.
	}
	
	public void duplicateFile(String fileToRead, String fileToOverwrite) throws FullDirectoryException{//copies file onto a new file with a new name; creating a new file.
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		int index = getAvailableFileSpaceInDirectory(); //find space within directory
		if (index ==-1){
			throw new FullDirectoryException ("Directory is full!");
		}
		diskUnit.read((index/blockSize),vdb);
		for(int x=0; x<fileToOverwrite.length();x++){
			DiskUtils.copyCharToBlock(vdb,(index%blockSize)+x, fileToOverwrite.charAt(x));
		}
		DiskUtils.copyIntToBlock(vdb, (index%blockSize)+20, copyFileContents(fileToRead));
		diskUnit.write(index, vdb);
	}

	public boolean isAvailableSpace(int fileToBeReadIndex) {
		if(!(firstFreeINode==0)&&!(getAvailableFileSpaceInDirectory()==-1)){ //if 0; there are no INodes, no new file may be created.
			VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
			diskUnit.read((int)getBlockIndexWithPosition(fileToBeReadIndex).getX(), vdb);
			int iNodeIndex = DiskUtils.getIntFromBlock(vdb, (int)getBlockIndexWithPosition(fileToBeReadIndex).getY()+20);//20 to offset characters, read next 4 bytes as int
			//gets amount of blocks needed and compares to how many are available
			return ((getINodeIndexAtDirectoryIndex(iNodeIndex)).getSize()/blockSize)<=totalFreeBlocks();
		}
		return false;
	}
	
	public boolean fileExistsInDirectory(String fileName){
		return !(findInDirectory(fileName)==-1);
	}
	public int getAvailableFileSpaceInDirectory(){
		return findInDirectory("");
	}







	//////////free block managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int getNextFreeBlock() throws FullDiskException{
		int availableBlockIndex; 
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		if (freeBlockIndex==0){ // the disk is full.
			throw new FullDiskException ("Disk is full!");
		}
		else if (endOfFreeBlockIndex!=4){ //if there are available indexes to choose from
			diskUnit.read(freeBlockIndex, vdb); //gets the free block array
			endOfFreeBlockIndex-=4; // move pointer to the next index in the array
			availableBlockIndex = DiskUtils.getIntFromBlock(vdb, endOfFreeBlockIndex); //end of free block index points to next available position
		}else{ //the only available block is this one.
			availableBlockIndex = freeBlockIndex; // this block is the next one that's available;
			diskUnit.read(freeBlockIndex, vdb);
			freeBlockIndex = DiskUtils.getIntFromBlock(vdb, 0); //next free block array is at root of this block
			endOfFreeBlockIndex = blockSize; //freeBlockIndex+blockSize would be the end of this array.
		}
		return availableBlockIndex;
	}


	private void registerFreeBlocks(int registeredBlockIndex) {//check if copy to block doesnt rewrite what was previously on
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		if (freeBlockIndex == 0)  { //There were no free blocks previous to this
			DiskUtils.copyIntToBlock(vdb, 0, 0); //Copy 0 to root of this block.
			freeBlockIndex=registeredBlockIndex; // This new registered block will become tree root.
			endOfFreeBlockIndex = 4; //The array is currently empty; so the next available free block is itself.
			diskUnit.write(registeredBlockIndex, vdb);
		}  
		else if (endOfFreeBlockIndex==blockSize) {      // the root node in the tree is full
			DiskUtils.copyIntToBlock(vdb, 0, freeBlockIndex); //Copy parent as root of this block
			freeBlockIndex=registeredBlockIndex; //Next free block will be picked from the subtree. 
			endOfFreeBlockIndex=4; //The array is currently empty; so the next available free block is itself.
			diskUnit.write(registeredBlockIndex, vdb);
		}  
		else { //there is space on the current subtree root.
			diskUnit.read(freeBlockIndex, vdb); //get all previous block information
			DiskUtils.copyIntToBlock(vdb, endOfFreeBlockIndex, registeredBlockIndex); //Copy next free block's index onto this block
			endOfFreeBlockIndex+=4; //the current index will have an int; move it to the next index.
			diskUnit.write(freeBlockIndex, vdb); //return original block plus new data.
		}
	}     

	private int prepareFreeBlocksForUse(int fileSize) {//returns index of first block. Gets as many free blocks as the file needs and sets links up.
		int totalBlocksUsed = (int)Math.ceil((double)fileSize/((double)blockSize-4.0));//formula used to determine how many blocks per file depending on file's size.
		int currentBlock = 0;
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		for(int x=0;x<totalBlocksUsed;x++){ // for each block needed, register a new free block.
			int index = getNextFreeBlock();
			DiskUtils.copyIntToBlock(vdb, blockSize-4, currentBlock); //write previous block's reference into current node's last int
			diskUnit.write(index, vdb);
			currentBlock = index;
		}
		return currentBlock;
	}

	private int totalFreeBlocks() {
		int counter = 0;
		int currentFreeBlock = freeBlockIndex;
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		while (currentFreeBlock!=0){
			diskUnit.read(currentFreeBlock,vdb);
			currentFreeBlock = DiskUtils.getIntFromBlock(vdb, 0);
			counter++;
		}
		return counter;
	}

	private void reclaimFreeBlocks(int blockIndex){
		VirtualDiskBlock empty = new VirtualDiskBlock(blockSize);
		VirtualDiskBlock current = new VirtualDiskBlock(blockSize);
		while(blockIndex!=0){//as long as there are blocks referring to this file.
			diskUnit.read(blockIndex, current); //for getting next block
			diskUnit.write(blockIndex, empty); //resetting block
			registerFreeBlocks(blockIndex);//register block into tree
			blockIndex = DiskUtils.getIntFromBlock(current, blockSize-4);//get next block
		}
	}
	//////INode Managers//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	private void reclaimINode(int iNodeIndex){//returns iNode to used linked list. (Doesn't account for if the file is a directory yet)
		int index =(int) getBlockIndexWithPosition(iNodeIndex).getY();
		int blockPos = (int)getBlockIndexWithPosition(iNodeIndex).getX();
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read(blockPos, vdb);
		int currentFreeINodeIndex = firstFreeINode;
		DiskUtils.copyIntToBlock(vdb, index+5, currentFreeINodeIndex); //sets this iNode's next to the head
		diskUnit.write(blockPos, vdb);
		firstFreeINode = iNodeIndex; //this iNode is the new head.
	}

	private INode getINodeIndexAtDirectoryIndex(int iNodeIndex) { //returns iNode at this index.
		int index =(int) getBlockIndexWithPosition(iNodeIndex).getY();
		int blockPos = (int)getBlockIndexWithPosition(iNodeIndex).getX();
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read(blockPos, vdb);
		firstFreeINode=DiskUtils.getIntFromBlock(vdb, index+5);//sets the new free INode using "getNext"
		return new INode(vdb.getElement(index),DiskUtils.getIntFromBlock(vdb, index+1),DiskUtils.getIntFromBlock(vdb, index+5));
	}
	private void setINodeAtIndex(int freeINodeIndex, INode nta) { //sets new iNode given a free i node index.
		int index =(int) getBlockIndexWithPosition(freeINodeIndex).getY();
		int blockPos = (int)getBlockIndexWithPosition(freeINodeIndex).getX();
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read(blockPos, vdb); //save previous information
		firstFreeINode=DiskUtils.getIntFromBlock(vdb, index+5); //set the new free iNode head using the "getNext()"

		vdb.setElement(index,nta.getType()); //Set new type
		DiskUtils.copyIntToBlock(vdb, index+1,nta.getSize()); //set new Size
		DiskUtils.copyIntToBlock(vdb, index+5,nta.getIndex()); //set new Index
		diskUnit.write(blockPos, vdb);
	}
	private void addINode(INode nta) { //sets new iNode given a free i node index.
		setINodeAtIndex(firstFreeINode,nta);
	}



	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getDiskName(){
		return mountName;
	}
	public boolean isMounted(){
		return mountName!=null;
	}

	private Point2D getBlockIndexWithPosition(int totalByteIndex){
		//first is block index, second is index within that block's array.
		return new Point2D.Double((int)Math.floor(totalByteIndex/blockSize),totalByteIndex%blockSize);
	}

	public void stop(){

		diskUnit.setFreeBlockIndex(freeBlockIndex);
		diskUnit.setEndOfFreeBlockIndex(endOfFreeBlockIndex);
		diskUnit.setFirstFreeINode(firstFreeINode);

		mountName=null;
		currentDirectory=null;
	}

}
