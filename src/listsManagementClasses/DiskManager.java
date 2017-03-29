package listsManagementClasses;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import diskUtilities.DiskUnit;
import disk_Exceptions.FullDirectoryException;
import disk_Exceptions.FullDiskException;
import lists.INode;
import lists.SLINodeList;

public class DiskManager {
	RandomAccessFile diskUnitRAF;
	private INode currentDirectory;
	private String mountName; //name of mounted disk

	private int blockSize;
	private int capacity;

	private int freeBlockIndex; //index of the current subtree root that holds the rest of the free block indexes.
	private int endOfFreeBlockIndex; //Pointer to the next index available in subtree. 

	private int usedINodes;
	private int firstFreeINode; // Next available I-Node
	private int totalINodes;//Total number of I-Nodes
	private int iNodeBlockAmount;//how many blocks in total INodes will take up
	private int iNodesPerBlock; //how many INodes fit in a block

	private int intsPerBlock; //how many separate numbers fit in a block

	private SLINodeList iNodesList; //Singly Linked List for the INodes

	public DiskManager(){
		mountName = null;
	}



	public void prepareDiskUnit(String diskName, boolean beingMounted) throws IOException{
		//constants; will always be in the diskunit regardless if it was created recently.
		diskUnitRAF = new RandomAccessFile(diskName,"rw");

		capacity = diskUnitRAF.readInt();
		blockSize = diskUnitRAF.readInt();

		usedINodes=0;
		iNodesList = new SLINodeList(); //initialize linked list
		intsPerBlock = blockSize/4; //Used for amount of free block indexes that can be stored within a single block;
		totalINodes = calculateTotalINodes(); //total I-Nodes is 1% of diskUnit size
		iNodeBlockAmount = ((9*totalINodes)/blockSize); //how many blocks in total vINodes will take up
		iNodesPerBlock = blockSize/9;


		if(beingMounted){ //Extract all the control information
			freeBlockIndex = diskUnitRAF.readInt();
			endOfFreeBlockIndex = diskUnitRAF.readInt();
			firstFreeINode = diskUnitRAF.readInt();
			totalINodes = diskUnitRAF.readInt();
			diskUnitRAF.seek(blockSize);
			currentDirectory = new INode(diskUnitRAF.readByte(),diskUnitRAF.readInt(),diskUnitRAF.readInt());//root file of disk
			mountName = diskName;


			for (int x=1;x<=iNodeBlockAmount;x++){ // for each INode assigned block; starting at one to skip control block
				diskUnitRAF.seek(x*blockSize); //seek the next INode block
				for (int i=0;i<iNodesPerBlock;i++){ //for each I Node in block
					byte type = diskUnitRAF.readByte();
					int size = diskUnitRAF.readInt();
					int fileIndex = diskUnitRAF.read();
					if(type!=-1){
						usedINodes++;
					}
					iNodesList.addLast(new INode(type,size,fileIndex)); //add iNode to list
				}
			}


		}else{
			//DiskUnit was recently created, RAF lacks required details in control other than Capacity and BlockSize.
			currentDirectory = null; //file not being mounted; just created
			mountName = null;
			freeBlockIndex = iNodeBlockAmount*blockSize;//block after reserved INodes
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
	/////////File Managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean fileExists(String fileName){
		boolean atEnd = false;
		int filesPerBlock = blockSize/44; //40; 2 for each char; 4 for the INode index
		int currentIndex = currentDirectory.getIndex();//start at current directory
		try{
			while (!atEnd){ // while still within directory
				diskUnitRAF.seek(currentIndex); //start of block
				for (int x=0; x<filesPerBlock;x++){ //for each possible file
					if(getFileNameAtIndex((int)diskUnitRAF.getFilePointer()).equals(fileName)){//checks if first 40 bytes translated into 20 characters equals fileName, advances 4 extra
						return true;
					}
				}
				diskUnitRAF.seek(currentIndex+blockSize-4);
				currentIndex=diskUnitRAF.readInt();
				if (currentIndex==0){
					atEnd=true;
				}
			}
		}catch (Exception e){}
		return false;
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
		if(firstFreeINode==-1){//disk is full
			throw new FullDiskException ("No more available space in disk!");
		}
		int index = getDirectoryIndex();
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



	private int getDirectoryIndex() {
		boolean atEnd=false;
		int currentIndex = currentDirectory.getIndex();
		int filesPerBlock = blockSize/44; //40; 2 for each char; 4 for the INode index
		try{
			while (!atEnd){ // while still within directory
				diskUnitRAF.seek(currentIndex); //start of block
				for (int x=0; x<filesPerBlock;x++){ //for each possible file
					diskUnitRAF.skipBytes(40); //skip the first 40 characters
					if (diskUnitRAF.readByte()==0){ //check if file space has an INode allocated to it. 0 = no INode
						return (int)diskUnitRAF.getFilePointer()-41; //if not, space is free. return index.
					}else{
						diskUnitRAF.skipBytes(3);//skip remaining bytes to get to next file.
					}
				}
				diskUnitRAF.seek(currentIndex+blockSize-4); //check where the next block is
				currentIndex=diskUnitRAF.readInt();
				if (currentIndex==0){ //if next block index is 0, no more remaining blocks
					atEnd=true;
				}
			}
		}catch (Exception e){}
		return -1; //no free space in directory was found.
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


	private int prepareFreeBlocksForUse(int fileSize) {//returns index of first block.
		int totalBlocksUsed = (int)Math.ceil((double)fileSize/((double)blockSize-4.0));//formula used to determine how many blocks per file depending on file's size.
		int currentBlock = 0;
		for(int x=0;x<totalBlocksUsed;x++){ // for each block needed, register a new free block.
			int index = getFreeBlockIndex();
			try{
				diskUnitRAF.seek(index+blockSize-4);//write next block in file at end of block;
				diskUnitRAF.writeInt(currentBlock);
			}catch(Exception e){}
			currentBlock = index; //sets up for the next int to be written on new block to be this block's index.
		}
		return currentBlock;
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
		else if (endOfFreeBlockIndex == freeBlockIndex+blockSize) {      // the root node in the tree is full
			try{
				diskUnitRAF.seek(blockIndex); //This block will become a new subtree in hierarchy. 
				diskUnitRAF.writeInt(freeBlockIndex); //First index always references parent. 
			}catch (Exception e){}
			endOfFreeBlockIndex = blockIndex; //The array is currently empty; so the next available free block is itself.
			freeBlockIndex = blockIndex; //Next free block will be picked from the lowest tree root. 
		}  
		else { //there is space on the current subtree root.
			try{
				diskUnitRAF.seek(endOfFreeBlockIndex); //write on current index the location of the next free block.
				diskUnitRAF.writeInt(blockIndex);
			}catch(Exception e){}
			endOfFreeBlockIndex=endOfFreeBlockIndex+4; //the current index has an int; move it to the next index.
		}
		try {
			diskUnitRAF.seek(8); // write the new indexes for the free block manager and its current pointer.
			diskUnitRAF.writeInt(freeBlockIndex);
			diskUnitRAF.writeInt(endOfFreeBlockIndex);
		} catch (Exception e) {} 
	}     


	//////INode Managers//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private INode getINodeAtIndex(int index) throws IOException{
		diskUnitRAF.seek(index);
		return new INode(diskUnitRAF.readByte(),diskUnitRAF.readInt(),diskUnitRAF.readInt());
	}

	private int addINode(INode nta) {//returns index in RAF
		writeINodeAtIndex(nta ,firstFreeINode);
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

	private int getINodeIndex(int iNodeNumber) { //returns index in block of INode in this position.
		int remainingINodes = iNodeNumber;
		int blockPos=blockSize;//offsets control
		while(remainingINodes>iNodesPerBlock){ //the i node isn't in this block
			blockPos=blockPos+blockSize; //go to the next block
			remainingINodes=remainingINodes-iNodesPerBlock; //account for all nodes from previous block
		}
		return (remainingINodes*9)+blockPos;
	}



	private void setNewFreeINode() {
		if (totalINodes==usedINodes){
			firstFreeINode=-1;
		}
		try{
			for (int x=1;x<=iNodeBlockAmount;x++){ // for each INode assigned block; starting at one to skip control block
				diskUnitRAF.seek(x*blockSize); //seek the next INode block
				for (int i=0;i<iNodesPerBlock;i++){ //fit as many INodes into that block as possible
					if (diskUnitRAF.readByte()==-1){
						firstFreeINode = (int)diskUnitRAF.getFilePointer()-1;
						return;
					}else{
						diskUnitRAF.skipBytes(8);
					}
				}
			}
		}catch (Exception e){}
	}



	//still worked on
	public int getFreeINode() throws FullDiskException{ //returns index of an available I-Node
		if (firstFreeINode==(iNodeBlockAmount*blockSize+(iNodesPerBlock*9))||totalINodes==1){ //i-Nodes are full; only root iNode available
			throw new FullDiskException ("Disk does not have space for a new file!");
		}
		//check if I Node is at end of block; account for space leftover.
		return 0;
	}

	private void writeINodeAtIndex(INode nta, int index){
		try{
			diskUnitRAF.seek(index);
			diskUnitRAF.writeByte(nta.getType());
			diskUnitRAF.writeInt(nta.getSize());
			diskUnitRAF.writeInt(nta.getIndex());
		}catch (Exception e){}
	}


	///////Constructor helpers//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void formatFreeBlockSpace() {
		int stillFreeSpace = capacity-iNodeBlockAmount-1; //Amount of FreeBlocks left in an empty DiskUnit, -1 to account for freeBlockIndex
		int currentBlock = freeBlockIndex+blockSize; //Start at the first block after INodes, freeBlockIndex is already registered in prepareDiskUnit
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
			for (int i=0;i<iNodesPerBlock;i++){ //fit as many INodes into that block as possible
				diskUnitRAF.writeByte(emptyByte);//1 byte
				diskUnitRAF.writeInt(emptyInt);//4 bytes
				diskUnitRAF.writeInt(emptyInt); // 4 bytes; 9 bytes total
				iNodesList.addLast(new INode(emptyByte,emptyInt,emptyInt));
			}
		}
		diskUnitRAF.seek(blockSize); //Setting the root file; offsets control.
		diskUnitRAF.writeByte(1); //Root i-Node is a directory.
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
