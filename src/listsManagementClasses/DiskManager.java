package listsManagementClasses;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import diskUtilities.DiskUnit;
import diskUtilities.DiskUtils;
import diskUtilities.VirtualDiskBlock;
import disk_Exceptions.FullDirectoryException;
import disk_Exceptions.FullDiskException;
import lists.INode;
import lists.SLINodeList;

public class DiskManager {
	DiskUnit diskUnit;
	private INode currentDirectory;
	private String mountName; //name of mounted disk

	private int iNodeBlockAmount;//how many blocks in total INodes will take up
	private int iNodesPerBlock; //how many INodes fit in a block

	private SLINodeList iNodesList; //Singly Linked List for the INodes

	public DiskManager(){
		mountName = null;
	}



	public void prepareDiskUnit(String diskName, boolean beingMounted) throws IOException{
		//constants; will always be in the diskunit regardless if it was created recently.
		diskUnit = DiskUnit.mount(diskName);
		iNodesList = new SLINodeList(); //initialize linked list
		iNodeBlockAmount = ((9*diskUnit.getTotalINodes())/diskUnit.getBlockSize()); //how many blocks in total vINodes will take up
		iNodesPerBlock = diskUnit.getBlockSize()/9;


		if(beingMounted){ //Extract all the control information

			currentDirectory = getINode(0);//root file of disk
			mountName = diskName;

			//replace with get INodeAt
		}else{
			//DiskUnit was recently created, RAF lacks required details in control other than Capacity and BlockSize.
			currentDirectory = null; //file not being mounted; just created
			mountName = null;
			formatINodeSpace(); // reserves the iNode blocks
			formatFreeBlockSpace();
		}
	}
	/////////File Managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean fileExists(String fileName){
		boolean atEnd = false;
		int filesPerBlock = diskUnit.getBlockSize()/24; //20, 2 for each char; 4 for the INode index
		int currentIndex = currentDirectory.getIndex();//start at current directory
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		while (!atEnd){ // while still within directory
			
			
			
			
//			diskUnitRAF.seek(currentIndex); //start of block
//			for (int x=0; x<filesPerBlock;x++){ //for each possible file
//				if(getFileNameAtIndex((int)diskUnitRAF.getFilePointer()).equals(fileName)){//checks if first 40 bytes translated into 20 characters equals fileName, advances 4 extra
//					return true;
//				}
//			}
//			diskUnitRAF.seek(currentIndex+blockSize-4);
//			currentIndex=diskUnitRAF.readInt();
//			if (currentIndex==0){
//				atEnd=true;
//			}
//		}
//		return false;
		}
	}

	public String getFileNameAtIndex(int index){//must advance pointer 44 bytes
		String fileName = "";
		try{
			for (int x=0;x<40;x++){
				long currentPos = diskUnitRAF.getFilePointer();
				if(!(diskUnitRAF.readByte()==0)){//reads one byte and checks if the space is free
					diskUnitRAF.seek(currentPos); //if it wasnt free, go back to read correctly
					fileName = fileName+diskUnitRAF.readChar();
				}
				diskUnitRAF.readByte(); //if it was free, read the extra byte it would have to advance the pointer
			}
			diskUnitRAF.readInt();//just to skip int and advance the pointer.
		}catch (Exception e){}
		return fileName;
	}

	public void addFileToDirectory(byte type,int fileSize,String fileName) throws FullDirectoryException, FullDiskException{
		INode fileToAdd =  new INode(type, fileSize ,prepareFreeBlocksForUse(fileSize));
		if(diskUnit.getFirstFreeINode()==-1){//disk is full
			throw new FullDiskException ("No more available space in disk!");
		}
		int index = getDirectorySpace();
		if (index==-1){
			throw new FullDirectoryException("No more available space in this directory!");
		}else{
			int INodeIndex = addINode(fileToAdd); //adds to linked list
			writeFileToDirectory(fileName, INodeIndex, index); //Adds to RAF
		}
	}



	private void writeFileToDirectory(String fileName, int INodeIndex, int directoryIndex){
		try{
			diskUnitRAF.seek(directoryIndex);
			for (int x=0; x<fileName.length();x++){
				diskUnitRAF.writeChar(fileName.charAt(x));
			}
			diskUnitRAF.seek(directoryIndex+40);
			diskUnitRAF.writeInt(INodeIndex);
		}catch (Exception e){}
	}



	private int getDirectorySpace() { //get index of space within directory
		boolean atEnd=false;
		int currentIndex = currentDirectory.getIndex();
		int filesPerBlock = diskUnit.getBlockSize()/24; //20 bytes for chars, 4 for iNode index int



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
	}



	//////////free block managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public int getNextFreeBlock() throws FullDiskException{
		int availableBlockIndex = 0; //0 so that it won't throw errors
		int freeBlockIndex = diskUnit.getFreeBlockIndex();
		int endOfFreeBlockIndex = diskUnit.getEndOfFreeBlockIndex();
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		if (freeBlockIndex==0){ // the disk is full.
			throw new FullDiskException ("Disk is full!");
		}
		if (endOfFreeBlockIndex!=0){
			availableBlockIndex = DiskUtils.getIntFromBlock(vdb, endOfFreeBlockIndex);
			diskUnit.setEndOfFreeBlockIndex(endOfFreeBlockIndex-4); // move pointer to the next index in the array
		}else{
			availableBlockIndex = freeBlockIndex; // this block is the next one that's available;
			diskUnit.read(freeBlockIndex, vdb);
			diskUnit.setFreeBlockIndex(DiskUtils.getIntFromBlock(vdb, 0));
			diskUnit.setEndOfFreeBlockIndex(freeBlockIndex+diskUnit.getBlockSize()-4); //freeBlockIndex+blockSize yields the next block; -4 yields the last int of this block. Reset necessary for next block's array.
		}
		return availableBlockIndex;
	}


	private int prepareFreeBlocksForUse(int fileSize) {//returns index of first block.
		int blockSize = diskUnit.getBlockSize();
		int totalBlocksUsed = (int)Math.ceil((double)fileSize/((double)blockSize-4.0));//formula used to determine how many blocks per file depending on file's size.
		int currentBlock = 0;
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		for(int x=0;x<totalBlocksUsed;x++){ // for each block needed, register a new free block.
			int index = getNextFreeBlock();
			DiskUtils.copyIntToBlock(vdb, blockSize-4, currentBlock);
			diskUnit.write(index, vdb);
			currentBlock = index;
		}
		return currentBlock;
	}



	void registerFreeBlocks(int block) {//check if copy to block doesnt rewrite what was previously on
		int freeBlockIndex = diskUnit.getFreeBlockIndex();
		int endOfFreeBlockIndex = diskUnit.getEndOfFreeBlockIndex();
		int blockSize = diskUnit.getBlockSize();
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		if (freeBlockIndex == 0)  { //There were no free blocks previous to this
			DiskUtils.copyIntToBlock(vdb, 0, 0); //Copy 0 to root of this block.
			diskUnit.setFreeBlockIndex(block); // This new registered block will become tree root.
			diskUnit.setEndOfFreeBlockIndex(block); //The array is currently empty; so the next available free block is itself.
			diskUnit.write(block, vdb);
		}  
		else if (endOfFreeBlockIndex==blockSize) {      // the root node in the tree is full
			DiskUtils.copyIntToBlock(vdb, 0, freeBlockIndex); //Copy parent as root of this block
			diskUnit.setFreeBlockIndex(block); //Next free block will be picked from the subtree. 
			diskUnit.setEndOfFreeBlockIndex(block); //The array is currently empty; so the next available free block is itself.
			diskUnit.write(block, vdb);
		}  
		else { //there is space on the current subtree root.
			DiskUtils.copyIntToBlock(vdb, endOfFreeBlockIndex, block); //Copy next free block's index onto this block
			diskUnit.setEndOfFreeBlockIndex(endOfFreeBlockIndex+4); //the current index has an int; move it to the next index.
			diskUnit.write(freeBlockIndex, vdb);
		}
	}     


	//////INode Managers//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int addINode(INode nta) {//returns index in RAF
		writeINodeAtIndex(nta ,diskUnit.getFirstFreeINode());
		setNewFreeINode();
		boolean resolved = false;
		INode start = iNodesList.getFirstNode();
		int iNodeNumber=1;//starts at one, inode 0 is root.
		while (!resolved){
			if (start.getNext().getType()==-1){
				INode ntr = start.getNext();
				start.setNext(nta);
				nta.setNext(ntr.getNext());
				ntr.clean();
				resolved=true;
			}else{
				start=start.getNext();
				iNodeNumber++;
			}
		}
		return getINodeIndex(iNodeNumber);
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

	private int getINodeIndex(int iNodeNumber) { //returns index in block of INode in this position.
		int remainingINodes = iNodeNumber;
		int blockSize = diskUnit.getBlockSize();
		int blockPos=blockSize;//offsets control; index by block
		while(remainingINodes>iNodesPerBlock){ //the i node isn't in this block
			blockPos+=blockSize; //go to the next block
			remainingINodes=remainingINodes-iNodesPerBlock; //account for all nodes from previous block
		}
		return (remainingINodes*9)+blockPos; //returns which block and the index within that block where iNode resides.
	}


	private void setNewFreeINode() {
		if (diskUnit.getTotalINodes()==usedINodes){
			diskUnit.setFirstFreeINode(-1);
		}else{
			VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
			for (int x=1;x<=iNodeBlockAmount;x++){ // for each INode assigned block; starting at one to skip control block
				diskUnit.read(x, vdb);
				for (int i=0;i<iNodesPerBlock*9;){ //fit as many INodes into that block as possible
					if (vdb.getElement(i)==-1){
						diskUnit.setFirstFreeINode((x*diskUnit.getBlockSize())+i);
						return;
					}else{
						i+=9;
					}
				}
			}
		}
	}



	//still worked on
	public int getFreeINodeIndex() throws FullDiskException{ //returns index of an available I-Node
		if (diskUnit.getFirstFreeINode()==-1||diskUnit.getTotalINodes()==1){ //i-Nodes are full; only root iNode available
			throw new FullDiskException ("Disk does not have space for a new file!");
		}
		int itr = diskUnit.getFirstFreeINode();

		writeINodeAtIndex(new INode((byte)0, 0,0),itr); //remove -1 to run setNewINode.
		setNewFreeINode();
		return itr;
	}

	private void writeINodeAtIndex(INode nta, int index){
		int remainingINodes = (index-diskUnit.getBlockSize())/9;
		int blockPos=1;//offsets control; index by block
		while(remainingINodes>iNodesPerBlock){ //the i node isn't in this block
			blockPos++; //go to the next block
			remainingINodes=remainingINodes-iNodesPerBlock; //account for all nodes from previous block
		}
		VirtualDiskBlock vdb = new VirtualDiskBlock(diskUnit.getBlockSize());
		remainingINodes = remainingINodes*9;//index within block.
		vdb.setElement(remainingINodes,nta.getType());
		DiskUtils.copyIntToBlock(vdb, remainingINodes+1, nta.getSize());
		DiskUtils.copyIntToBlock(vdb, remainingINodes+5, nta.getIndex());
		diskUnit.write(blockPos, vdb);
	}


	///////Constructor helpers//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void formatFreeBlockSpace() {
		int stillFreeSpace = diskUnit.getCapacity()-iNodeBlockAmount-1; //Amount of FreeBlocks left in an empty DiskUnit, -1 to account for freeBlockIndex
		int currentBlock = diskUnit.getFreeBlockIndex()+1; //Start at the first block after INodes, freeBlockIndex is already registered in prepareDiskUnit
		while (stillFreeSpace>0){ //while there is still free space
			registerFreeBlocks(currentBlock); //register the next block
			currentBlock+=1; //go to the next block
			stillFreeSpace=-1; //reduce the amount remaining by 1 block
		}
	}




	private void formatINodeSpace() throws IOException{//-1 represents an unused iNode
		int maxINodesPerBlock = diskUnit.getBlockSize()/diskUnit.getTotalINodes();
		VirtualDiskBlock formatedINodes = new VirtualDiskBlock(diskUnit.getBlockSize());
		int counter=0;
		for(int x=0;x<maxINodesPerBlock;x++){//for every iNode that can fit in the VirtualDiskBlock
			formatedINodes.setElement(counter, (byte)-1); //at the beginning
			DiskUtils.copyIntToBlock(formatedINodes, counter+1, 0); //after type
			DiskUtils.copyIntToBlock(formatedINodes, counter+5, 0); //after size
			counter +=9;//continue to next iNode
		}
		for (int i=1;i<=Math.ceil(diskUnit.getCapacity()/100);i++){
			diskUnit.write(i, formatedINodes);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getDiskName(){
		return mountName;
	}
	public boolean isMounted(){
		return mountName!=null;
	}

	public void stop() {//still being worked on
		//		try {
		//			formatINodeSpace(); //format all I-Nodes for restructuring
		//			int tempCounter = 0; //counts how many INodes have been added to current block
		//			int currentBlock = blockSize; //starts at first INodeBlock
		//			while(!iNodesList.isEmpty()){
		//				diskUnitRAF.seek(currentBlock);
		//				while (tempCounter<iNodesPerBlock){
		//					INode ntr = iNodesList.removeFirstNode(); //remove first to maintain order of original INodes, namely, preserving root file as first INode.
		//					diskUnitRAF.writeByte(ntr.getType());
		//					diskUnitRAF.writeInt(ntr.getSize());
		//					diskUnitRAF.writeInt(ntr.getIndex());
		//					tempCounter++; // One more INode has been added
		//				}
		//				currentBlock=currentBlock+blockSize; //next block
		//			}
		//			diskUnitRAF.close(); //close this raf
		//		} catch (IOException e) {}
		mountName=null;
		currentDirectory=null;
	}




}
