package listsManagementClasses;

import java.io.IOException;

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

	private int iNodeBlockAmount;//how many blocks in total INodes will take up
	private int iNodesPerBlock; //how many INodes fit in a block

	public DiskManager(){
		mountName = null;
	}



	public void prepareDiskUnit(String diskName, boolean beingMounted) throws IOException{
		//constants; will always be in the diskunit regardless if it was created recently.
		diskUnit = DiskUnit.mount(diskName);
		iNodeBlockAmount = ((9*diskUnit.getTotalINodes())/diskUnit.getBlockSize()); //how many blocks in total INodes will take up
		iNodesPerBlock = diskUnit.getBlockSize()/9;


		if(beingMounted){ //Extract all the control information
			currentDirectory = getINode(diskUnit.getBlockSize());//root file of disk
			mountName = diskName;

			//replace with get INodeAt
		}else{
			//DiskUnit was recently created, RAF lacks required details in control other than Capacity and BlockSize.
			formatFreeBlockSpace();
			formatINodeSpace();
			int rootFileSize = diskUnit.getBlockSize()*8;
			addINode(new INode((byte)0,rootFileSize,prepareFreeBlocksForUse(rootFileSize)));
			VirtualDiskBlock test = new VirtualDiskBlock(diskUnit.getBlockSize());
			diskUnit.read(1, test);
			System.out.println(diskUnit.getFirstFreeINode());
			currentDirectory = null; //file not being mounted; just created
			diskUnit.shutdown();
		}
	}


	public void formatINodeSpace(){
		int blockSize = diskUnit.getBlockSize();
		int maxINodesPerBlock = blockSize/9;
		int totalINodeBlocks = (int)Math.ceil((double)diskUnit.getCapacity()/100);
		int counter=0;
		VirtualDiskBlock formatedINodes = new VirtualDiskBlock(blockSize);
		for(int x=0;x<maxINodesPerBlock;x++){//prepare empty array, for every INode that can fit in a block
			formatedINodes.setElement(counter,(byte)-1);
			DiskUtils.copyIntToBlock(formatedINodes, counter+1, 0); //after type
			DiskUtils.copyIntToBlock(formatedINodes, counter+5, 0); //after size
			counter +=9;//continue to next iNode
		}
		for (int i=1;i<=totalINodeBlocks;i++){//starts at block 1
			diskUnit.write(i,formatedINodes);
		}
	}

	public void formatFreeBlockSpace(){
		int capacity = diskUnit.getCapacity();
		int totalINodeBlocks = (int)Math.ceil((double)capacity/100);
		int stillFreeSpace = capacity-totalINodeBlocks-1; //Amount of FreeBlocks left in an empty DiskUnit
		int currentBlock = totalINodeBlocks+1; //freeBlocksIndex is already registered.
		while (stillFreeSpace>0){ //while there is still free space
			registerFreeBlocks(currentBlock); //register the next block
			currentBlock+=1; //go to the next block
			stillFreeSpace--; //reduce the amount remaining by 1 block
		}
	}


	/////////File Managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean fileExists(String fileName){
		boolean atEnd = false;
		int currentIndex = currentDirectory.getIndex();//start at current directory
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		while (!atEnd){ // while still within directory
			diskUnit.read(currentIndex, vdb); //read current block in directory
			int counter=0; //counter for total bytes in blocks
			while(counter<diskUnit.getBlockSize()){//there are still files in directory
				String tempName = ""; //empty string to concat chars.
				int stringCounter=0; //counter for current string
				while(stringCounter<20){
					if(DiskUtils.getCharFromBlock(vdb, counter)=='#'){//flag for end of name.
						break;//file name has been completely read
					}else{ //concat the char to tempName
						tempName = tempName+DiskUtils.getCharFromBlock(vdb, counter);
					}
				}
				if (tempName.equals(fileName)){//file exists
					return true;
				}
				counter+=24;//advance to next file 
				currentIndex = DiskUtils.getIntFromBlock(vdb, diskUnit.getBlockSize()-4); //get next block in file by reading end of block's int
				if(currentIndex==0){ //if end int is 0, there are no more blocks in file.
					atEnd=true;
				}
			}
		}
		return false;
	}

	//	public String getFileNameAtIndex(int index){//must advance pointer 44 bytes
	//		String fileName = "";
	//		try{
	//			for (int x=0;x<40;x++){
	//				long currentPos = diskUnitRAF.getFilePointer();
	//				if(!(diskUnitRAF.readByte()==0)){//reads one byte and checks if the space is free
	//					diskUnitRAF.seek(currentPos); //if it wasnt free, go back to read correctly
	//					fileName = fileName+diskUnitRAF.readChar();
	//				}
	//				diskUnitRAF.readByte(); //if it was free, read the extra byte it would have to advance the pointer
	//			}
	//			diskUnitRAF.readInt();//just to skip int and advance the pointer.
	//		}catch (Exception e){}
	//		return fileName;
	//	}
	//
	//	public void addFileToDirectory(byte type,int fileSize,String fileName) throws FullDirectoryException, FullDiskException{
	//		INode fileToAdd =  new INode(type, fileSize ,prepareFreeBlocksForUse(fileSize));
	//		if(diskUnit.getFirstFreeINode()==-1){//disk is full
	//			throw new FullDiskException ("No more available space in disk!");
	//		}
	//		int index = getDirectorySpace();
	//		if (index==-1){
	//			throw new FullDirectoryException("No more available space in this directory!");
	//		}else{
	//			int INodeIndex = addINode(fileToAdd); //adds to linked list
	//			writeFileToDirectory(fileName, INodeIndex, index); //Adds to RAF
	//		}
	//	}
	//
	//
	//
	//	private void writeFileToDirectory(String fileName, int INodeIndex, int directoryIndex){
	//		try{
	//			diskUnitRAF.seek(directoryIndex);
	//			for (int x=0; x<fileName.length();x++){
	//				diskUnitRAF.writeChar(fileName.charAt(x));
	//			}
	//			diskUnitRAF.seek(directoryIndex+40);
	//			diskUnitRAF.writeInt(INodeIndex);
	//		}catch (Exception e){}
	//	}


	//
	//	private int getDirectorySpace() { //get index of space within directory
	//		boolean atEnd=false;
	//		int currentIndex = currentDirectory.getIndex();
	//		int filesPerBlock = diskUnit.getBlockSize()/24; //20 bytes for chars, 4 for iNode index int



	//		int filesPerBlock = blockSize/44; //40; 2 for each char; 4 for the INode index
	//		try{
	//			while (!atEnd){ // while still within directory
	//				diskUnitRAF.seek(currentIndex); //start of block
	//				for (int x=0; x<filesPerBlock;x++){ //for each possible file
	//					diskUnitRAF.skipBytes(40); //skip the first 40 characters
	//					if (diskUnitRAF.readByte()==0){ //check if file space has an INode allocated to it. 0 = no INode
	//						return (int)diskUnitRAF.getFilePointer()-41; //if not, space is free. return index.
	//					}else{
	//						diskUnitRAF.skipBytes(3);//skip remaining bytes to get to next file.
	//					}
	//				}
	//				diskUnitRAF.seek(currentIndex+blockSize-4); //check where the next block is
	//				currentIndex=diskUnitRAF.readInt();
	//				if (currentIndex==0){ //if next block index is 0, no more remaining blocks
	//					atEnd=true;
	//				}
	//			}
	//		}catch (Exception e){}
	//		return -1; //no free space in directory was found.
	//}



	//////////free block managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//should be good
	public int getNextFreeBlock() throws FullDiskException{
		int availableBlockIndex; 
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		if (diskUnit.getFreeBlockIndex()==0){ // the disk is full.
			throw new FullDiskException ("Disk is full!");
		}
		else if (diskUnit.getEndOfFreeBlockIndex()!=4){ //if there are available indexes to choose from
			diskUnit.read(diskUnit.getFreeBlockIndex(), vdb); //gets the free block array
			diskUnit.setEndOfFreeBlockIndex(diskUnit.getEndOfFreeBlockIndex()-4); // move pointer to the next index in the array
			availableBlockIndex = DiskUtils.getIntFromBlock(vdb, diskUnit.getEndOfFreeBlockIndex()); //end of free block index points to next available position
		}else{ //the only available block is this one.
			availableBlockIndex = diskUnit.getFreeBlockIndex(); // this block is the next one that's available;
			diskUnit.read(diskUnit.getFreeBlockIndex(), vdb);
			diskUnit.setFreeBlockIndex(DiskUtils.getIntFromBlock(vdb, 0)); //next free block array is at root of this block
			diskUnit.setEndOfFreeBlockIndex(diskUnit.getBlockSize()); //freeBlockIndex+blockSize would be the end of this array.
		}
		return availableBlockIndex;
	}



	//should be good
	public void registerFreeBlocks(int block) {//check if copy to block doesnt rewrite what was previously on
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		if (diskUnit.getFreeBlockIndex() == 0)  { //There were no free blocks previous to this
			DiskUtils.copyIntToBlock(vdb, 0, 0); //Copy 0 to root of this block.
			diskUnit.setFreeBlockIndex(block); // This new registered block will become tree root.
			diskUnit.setEndOfFreeBlockIndex(4); //The array is currently empty; so the next available free block is itself.
			diskUnit.write(block, vdb);
		}  
		else if (diskUnit.getEndOfFreeBlockIndex()>=diskUnit.getBlockSize()) {      // the root node in the tree is full
			DiskUtils.copyIntToBlock(vdb, 0, diskUnit.getFreeBlockIndex()); //Copy parent as root of this block
			diskUnit.setFreeBlockIndex(block); //Next free block will be picked from the subtree. 
			diskUnit.setEndOfFreeBlockIndex(4); //The array is currently empty; so the next available free block is itself.
			diskUnit.write(block, vdb);
		}  
		else { //there is space on the current subtree root.
			diskUnit.read(diskUnit.getFreeBlockIndex(), vdb); //get all previous block information
			DiskUtils.copyIntToBlock(vdb, diskUnit.getEndOfFreeBlockIndex(), block); //Copy next free block's index onto this block
			diskUnit.setEndOfFreeBlockIndex(diskUnit.getEndOfFreeBlockIndex()+4); //the current index will have an int; move it to the next index.
			diskUnit.write(diskUnit.getFreeBlockIndex(), vdb); //return original block plus new data.
		}
	}     


	private int prepareFreeBlocksForUse(int fileSize) {//returns index of first block.
		int blockSize = diskUnit.getBlockSize();
		int totalBlocksUsed = (int)Math.ceil((double)fileSize/((double)blockSize-4.0));//formula used to determine how many blocks per file depending on file's size.
		int currentBlock = 0;
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		for(int x=0;x<totalBlocksUsed;x++){ // for each block needed, register a new free block.
			int index = getNextFreeBlock();
			diskUnit.read(index, vdb);
			DiskUtils.copyIntToBlock(vdb, blockSize-4, currentBlock);
			diskUnit.write(index, vdb);
			currentBlock = index;
		}
		return currentBlock;
	}

	//////INode Managers//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void addINode(INode nta){
		int firstFreeINode=diskUnit.getFirstFreeINode()-diskUnit.getBlockSize();
		int blockCounter=1;
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		while(firstFreeINode>(iNodesPerBlock*9)){
			blockCounter++;
			firstFreeINode-=(iNodesPerBlock*9);
		}
		diskUnit.read(blockCounter, vdb);
		vdb.setElement(firstFreeINode, nta.getType());
		DiskUtils.copyIntToBlock(vdb, firstFreeINode+1, nta.getSize());
		DiskUtils.copyIntToBlock(vdb, firstFreeINode+5, nta.getIndex());
		diskUnit.write(blockCounter, vdb);
		setNewFreeINode();
	}
	
	private void setNewFreeINode(){
		int blockCounter=1;
		int remainingINodes = diskUnit.getTotalINodes();
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		while(remainingINodes>0){
			diskUnit.read(blockCounter, vdb);
			int currentINode=0;
			while (currentINode<diskUnit.getBlockSize()){
				if(vdb.getElement(currentINode)==-1){
					diskUnit.setFirstFreeINode((blockCounter*diskUnit.getBlockSize())+currentINode);
					return;
				}
				currentINode+=9;
				remainingINodes--;
			}
			blockCounter++;
		}
		diskUnit.setFirstFreeINode(-1);
	}



	private INode getINode(int iNodeNumber) { //returns iNode at this position
		int remainingINodes = iNodeNumber;
		int blockPos=1;//offsets control; index by block
		while(remainingINodes>iNodesPerBlock){ //the i node isn't in this block
			blockPos++; //go to the next block
			remainingINodes=remainingINodes-iNodesPerBlock; //account for all nodes from previous block
		}
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		remainingINodes = remainingINodes*9;//index within block.
		diskUnit.read(blockPos, vdb);
		return new INode(vdb.getElement(remainingINodes),DiskUtils.getIntFromBlock(vdb, remainingINodes+1),DiskUtils.getIntFromBlock(vdb, remainingINodes+5));
	}




	private INode getINodeAtIndex(int iNodeIndex) { //returns index in block of INode in this position.
		int remainingINodes = iNodeIndex;
		int blockPos=1;//offsets control; index by block
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		while(remainingINodes>iNodesPerBlock*9){ //the i node isn't in this block
			blockPos+=1; //go to the next block
			remainingINodes-=iNodesPerBlock*9; //account for all nodes from previous block
		}
		diskUnit.read(blockPos, vdb);
		return new INode(vdb.getElement(remainingINodes),DiskUtils.getIntFromBlock(vdb, remainingINodes+1),DiskUtils.getIntFromBlock(vdb, remainingINodes+5));
	}





	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getDiskName(){
		return mountName;
	}
	public boolean isMounted(){
		return mountName!=null;
	}

	public void stop(){
		mountName=null;
		currentDirectory=null;
	}




}
