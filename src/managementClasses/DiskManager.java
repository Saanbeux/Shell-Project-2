package managementClasses;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import diskUtilities.DiskUnit;
import diskUtilities.DiskUtils;
import diskUtilities.VirtualDiskBlock;
import disk_Exceptions.FullDiskException;
/**
 * Creates an object to manage disk units separately.
 * @author Moises Garip
 */
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
	/**
	 * Prepares a recently created disk unit for formatting, preparing iNodes and free blocks for future use.
	 * @param diskName recently created disk's name
	 */
	public void prepareDiskUnit(String diskName){
		//constants; will always be in the disk unit regardless if it was created recently.
		diskUnit = DiskUnit.mount(diskName);

		capacity = diskUnit.getCapacity();
		blockSize = diskUnit.getBlockSize();
		freeBlockIndex = diskUnit.getFreeBlockIndex();
		endOfFreeBlockIndex = diskUnit.getEndOfFreeBlockIndex();
		firstFreeINode = diskUnit.getFirstFreeINode();
		totalINodes = diskUnit.getTotalINodes();


		formatFreeBlockSpace(); //prepare free blocks in DiskUnit
		formatINodeSpace(); // prepare INodes in DiskUnit

		//test size for root file in directory
		//creates a root of size blockSize, sets the file's size to how many blocks it actually takes up.
		int directoryIndex = prepareFreeBlocksForUse(blockSize);
		int rootSize = ((int)Math.ceil((double)blockSize/((double)blockSize-4.0)))*blockSize;
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		vdb.setElement(0, (byte)0); //hardcoding in since getINode throws index out of bounds at 0.
		DiskUtils.copyIntToBlock(vdb, 1, rootSize);
		DiskUtils.copyIntToBlock(vdb, 5, directoryIndex);
		currentDirectory=new INode((byte)0,rootSize,directoryIndex); //create root file's INode.
		diskUnit.write(0, vdb);
		stop();
	}








	////////formatters////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Prepares iNodes for use. Every unused iNode's index parameter points towards the next free iNode. The last iNode points to 0; meaning there are no more iNodes.
	 */
	private void formatINodeSpace(){
		int remainingINodes = totalINodes;
		int nextINode = blockSize+9; //offset the first iNode, it is being accounted for as the head with "nextFreeINode"
		int currentBlock = 0; //start at first block
		while (remainingINodes>0){ //while there are INodes left to register
			int currentIndex = 5; //start at first INode-in-block's "fileIndex" property; resets counter.
			int maxINodesPerBlock = blockSize/9; // reset counter
			VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize); 
			while (maxINodesPerBlock>1){ // while there is still space in block
				if(remainingINodes==0){
					diskUnit.write(currentBlock, vdb);
					return;
				}
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
			nextINode+= blockSize%(blockSize/9); //Account for extra space at end of block
			DiskUtils.copyIntToBlock(vdb, currentIndex, nextINode); //copy first INode of next block onto lastiNode of previous block.
			diskUnit.write(currentBlock, vdb);
		}
	}




	/**
	 * Prepares free blocks for use. Every block that is not used for control or iNodes will be put into a tree-like structure,
	 *  where each free block is either used to store other free blocks or indexed into 4 of another free block's bytes.
	 */
	private void formatFreeBlockSpace(){ 
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
	/**
	 * Finds a file name in the current directory.
	 * @param fileName file to find in directory.
	 * @return Index of file in directory (total bytes). -1 if no file was found.
	 */
	public int findInDirectory(String fileName){ 
		int currentBlock = currentDirectory.getIndex();//start at current directory
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		String emptyChar="";
		emptyChar+=DiskUtils.getCharFromBlock(vdb, 0);
		while (currentBlock!=0){ // while still within directory
			diskUnit.read(currentBlock, vdb); //read current block in directory
			int counter=0; //counter for total bytes in blocks
			while(counter<blockSize-4){//there are still files in directory
				String tempName = ""; //empty string to concat chars.
				int stringCounter=0; //counter for current string
				while(stringCounter<20){
					if(Character.toString(DiskUtils.getCharFromBlock(vdb, counter)).equals(emptyChar)){
						break;
					}
					tempName = tempName+DiskUtils.getCharFromBlock(vdb, counter+stringCounter);
					stringCounter++;
				}
				if (tempName.equals(fileName)){//at file.
					return ((currentBlock*blockSize)+counter);
				}
				counter+=24;//advance to next file
			}
			currentBlock = DiskUtils.getIntFromBlock(vdb, blockSize-4); //get next block in file by reading end of block's int
		}
		return -1;//file does not exist
	}
	/**
	 * Finds a file in current disk directory, deletes name, iNode, and file associated to iNode.
	 * @param fileName file to find in directory.
	 * @return Index of free file space within directory. -1 if no such file was found.
	 */
	private int deleteFileInDirectory(String fileName){ 
		int fileIndex = findInDirectory(fileName);
		if(fileIndex==-1){
			return -1;
		}
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read((int) getPosition(fileIndex).getX(), vdb); //read block where file is held
		int indexOfINode = DiskUtils.getIntFromBlock(vdb, fileIndex+20); //get where iNode of file is held
		int index = (int) getPosition(fileIndex).getY();
		for(int x=0; x<24;x++){//deletes from directory
			vdb.setElement(index,(byte) 0);
			index++;
		}
		reclaimFreeBlocks(getINodeAtIndex(indexOfINode).getIndex());//Searches for iNode, gets the position of its file outside of directory, reclaims blocks used.
		reclaimINode(indexOfINode);//adds file as available file
		return fileIndex+24;//return former position of file for rewritting
	}

	/**
	 * Takes file in directory and copies it contents into new blocks.
	 * @param fileName file to copy in directory.
	 * @return Index of new file copy created. -1 if file not found.
	 */
	private int copyFileContents(String fileName){

		//gets file in directory, reads it's iNode, returning the location of the iNode's file
		int position = findInDirectory(fileName);
		if (position ==-1){
			return -1;
		}
		int fileIndex = getINodeAtIndex(position+20).getIndex(); //gets index of file in directory
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
	/**
	 * Takes a file, verifies if there is space in disk and directory to overwrite a second file, and overwrites it.
	 * @param fileToRead file to be copied.
	 * @param fileToOverwrite file replaced by copy
	 * @return 0 if successful, 1 if unsuccessful.
	 */
	public int loadFile(String fileToRead,String fileToOverwrite){
		INode ntc = getINodeAtIndex(findInDirectory(fileToRead)+20); //gets iNode of file being read within directory
		if(!isAvailableSpace(findInDirectory(fileToRead))){
			return 1;
		}
		int fileToOverwriteIndex = findInDirectory(fileToOverwrite);
		deleteFileInDirectory(fileToOverwrite);//reclaim space used by file that is being overwritten; name is deleted,
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		Point2D position = getPosition(fileToOverwriteIndex);
		diskUnit.read((int)position.getX(), vdb);
		for(int x=0; x<fileToOverwrite.length();x++){//rewrites name onto block.
			DiskUtils.copyCharToBlock(vdb, x+(int)position.getY(), fileToOverwrite.charAt(x));
		}
		ntc.setIndex(copyFileContents(fileToRead));//leave iNode as is, since most of the information is the same, but set new index to the recently created copy.
		setINodeAtIndex(fileToOverwriteIndex+20,ntc);//place copied file's iNode to the overwritten file's iNode location.
		return 0;
	}
	/**
	 * Takes a file, verifies if there is space in disk and directory to create a second file with a new name, then creates it.
	 * @param fileToRead file to be copied.
	 * @param fileToOverwrite file replaced by copy
	 * @return 0 if successful, 1 if unsuccessful.
	 */
	public int duplicateFile(String fileToRead, String fileToOverwrite){//copies file onto a new file with a new name; creating a new file.
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		int index = getAvailableFileSpaceInDirectory(); //find space within directory
		if (!isAvailableSpace(findInDirectory(fileToRead))){
			return 1;
		}
		diskUnit.read((index/blockSize),vdb);
		for(int x=0; x<fileToOverwrite.length();x++){
			DiskUtils.copyCharToBlock(vdb,(index%blockSize)+x, fileToOverwrite.charAt(x));
		}
		DiskUtils.copyIntToBlock(vdb, (index%blockSize)+20, copyFileContents(fileToRead));
		diskUnit.write(index, vdb);
		return 0;
	}
	/**
	 * Takes a file and verifies if there is space to make a copy of it by comparing free blocks required to
	 * recreate it and available iNodes.
	 * @param fileToBeReadIndex file to verify space.
	 * @return True if there is space to make a copy, false otherwise..
	 */
	public boolean isAvailableSpace(int fileToBeReadIndex) {
		if(!(firstFreeINode==0)&&getAvailableFileSpaceInDirectory()!=-1){ //if 0; there are no INodes, no new file may be created.
			VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
			diskUnit.read((int)getPosition(fileToBeReadIndex).getX(), vdb);
			int iNodeIndex = DiskUtils.getIntFromBlock(vdb, (int)getPosition(fileToBeReadIndex).getY()+20);//20 to offset characters, read next 4 bytes as int
			//gets amount of blocks needed and compares to how many are available
			return ((getINodeAtIndex(iNodeIndex)).getSize()/blockSize)<=totalFreeBlocks();
		}
		return false;
	}
	/**
	 * Verifies if the file exists in current directory.
	 * @param fileName file to verify
	 * @return True if file exists, false otherwise.
	 */
	public boolean fileExistsInDirectory(String fileName){
		return findInDirectory(fileName)!=-1;
	}
	/**
	 * Looks for next free 24 bytes in directory for a new file and iNode location.
	 * @return Index of free space in directory, -1 if no space was found.
	 */
	public int getAvailableFileSpaceInDirectory(){
		return findInDirectory("");
	}
	/**
	 * Lists all files along with their sizes in this directory.
	 * @return List of files in current directory
	 */
	public ArrayList<String> listFiles(){
		return listFilesAtINode(currentDirectory);
	}
	/**
	 * Lists all files along with their sizes in an indicated directory.
	 * @param iNode Directory iNode to list files.
	 * @return List of files in an indicated directory.
	 */
	public ArrayList<String> listFilesAtINode(INode iNode){
		ArrayList<String> files = new ArrayList<>();
		int currentBlock = iNode.getIndex();//start at current directory
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		String emptyChar="";
		emptyChar+=DiskUtils.getCharFromBlock(vdb, 0);
		while (currentBlock!=0){ // while still within directory
			diskUnit.read(currentBlock, vdb); //read current block in directory
			int counter=0; //counter for total bytes in blocks
			while(counter<blockSize-4){//there are still files in directory
				String tempName = ""; //empty string to concat chars.
				int stringCounter=0; //counter for current string
				while(stringCounter<20){
					if(Character.toString(DiskUtils.getCharFromBlock(vdb, counter+stringCounter)).equals(emptyChar)){
						break;
					}
					tempName = tempName+DiskUtils.getCharFromBlock(vdb, counter+stringCounter);
					stringCounter++;
				}
				if (!(tempName.equals(""))){//at file.
					files.add(tempName+" - size: "+getINodeAtIndex((currentBlock*blockSize)+counter+20).getSize()+" bytes");
				}
				counter+=24;//advance to next file
			}
			currentBlock = DiskUtils.getIntFromBlock(vdb, blockSize-4); //get next block in file by reading end of block's int
		}
		return files;
	}
	/**
	 * Verifies if file is a directory or a data file.
	 * @param fileToBeRead file to verify.
	 * @return True if it is a directory, false otherwise.
	 */
	public boolean isDirectory(String fileToBeRead) {
		return getINodeAtIndex(findInDirectory(fileToBeRead)+20).getType()==1;
	}

	/**
	 * Reads a data file.
	 * @param fileName file to read.
	 * @return Array of file's data.
	 */
	public ArrayList<String> readDataFile(String fileName) {

		return null;
	}

	/**
	 * creates a dummy file for testing file methods.
	 * @param fileSize size of file
	 * @param fileName name of file
	 */
	public void testFiles(String fileName, int fileSize){
		int fileIndex = prepareFreeBlocksForUse(fileSize);
		int iNodeIndex = addINode(new INode((byte)0,fileSize,fileIndex));
		int directoryIndex = getAvailableFileSpaceInDirectory();
		int blockPos = (int)getPosition(directoryIndex).getX();
		int index = (int)getPosition(directoryIndex).getY();
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read(blockPos, vdb);
		for(int x=0; x<fileName.length();x++){
			DiskUtils.copyCharToBlock(vdb, x+index, fileName.charAt(x));
		}
		DiskUtils.copyIntToBlock(vdb, index+20, iNodeIndex);
		diskUnit.write(blockPos, vdb);
	}








	//////////free block managers/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Finds and returns a free block within the free block tree.
	 * @return index of a free block.
	 * @throws FullDiskException when there are no free blocks.
	 */
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
		diskUnit.setEndOfFreeBlockIndex(endOfFreeBlockIndex);
		diskUnit.setFreeBlockIndex(freeBlockIndex);
		return availableBlockIndex;
	}

	/**
	 * Returns a block that was in use to the free block tree.
	 ** @param BlockToRegisterIndex block that will be added back into tree.
	 */
	private void registerFreeBlocks(int BlockToRegisterIndex) {//check if copy to block doesnt rewrite what was previously on
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		if (freeBlockIndex == 0)  { //There were no free blocks previous to this
			DiskUtils.copyIntToBlock(vdb, 0, 0); //Copy 0 to root of this block.
			freeBlockIndex=BlockToRegisterIndex; // This new registered block will become tree root.
			endOfFreeBlockIndex = 4; //The array is currently empty; so the next available free block is itself.
			diskUnit.write(BlockToRegisterIndex, vdb);
		}  
		else if (endOfFreeBlockIndex==blockSize) {      // the root node in the tree is full
			DiskUtils.copyIntToBlock(vdb, 0, freeBlockIndex); //Copy parent as root of this block
			freeBlockIndex=BlockToRegisterIndex; //Next free block will be picked from the subtree. 
			endOfFreeBlockIndex=4; //The array is currently empty; so the next available free block is itself.
			diskUnit.write(BlockToRegisterIndex, vdb);
		}  
		else { //there is space on the current subtree root.
			diskUnit.read(freeBlockIndex, vdb); //get all previous block information
			DiskUtils.copyIntToBlock(vdb, endOfFreeBlockIndex, BlockToRegisterIndex); //Copy next free block's index onto this block
			endOfFreeBlockIndex+=4; //the current index will have an int; move it to the next index.
			diskUnit.write(freeBlockIndex, vdb); //return original block plus new data.
		}
		diskUnit.setEndOfFreeBlockIndex(endOfFreeBlockIndex);
		diskUnit.setFreeBlockIndex(freeBlockIndex);
	}     
	/**
	 * Prepares free blocks for use in a file, allocating the minimum required blocks for the file and setting the last 4 bytes
	 * to point towards the next block in file.
	 * @param fileSize size of file to prepare blocks for.
	 * @return index of free block where file will begin to be stored.
	 */
	private int prepareFreeBlocksForUse(int fileSize) {//returns index of first block. Gets as many free blocks as the file needs and sets links up.
		int totalBlocksUsed = (int)Math.ceil((double)fileSize/((double)blockSize-4.0));//formula used to determine how many blocks per file depending on file's size.
		int currentBlock = 0;
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		while(totalBlocksUsed>0){ // for each block needed, register a new free block.
			int index = getNextFreeBlock();
			DiskUtils.copyIntToBlock(vdb, blockSize-4, currentBlock); //write previous block's reference into current node's last int
			diskUnit.write(index, vdb);
			currentBlock = index;
			totalBlocksUsed--;
		}
		return currentBlock;
	}
	/**
	 * Calculates total free blocks for use.
	 * @return amount of free blocks.
	 */
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
	/**
	 * Returns all blocks of a file to the free block tree.
	 * @param blockIndex first index of file to reclaim
	 */
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

	/**
	 * Returns iNode to iNode linked list for later use.
	 * @param iNodeIndex index of a file's iNode
	 */
	private void reclaimINode(int iNodeIndex){//returns iNode to used linked list. (Doesn't account for if the file is a directory yet)
		int index =(int) getPosition(iNodeIndex).getY();
		int blockPos = (int)getPosition(iNodeIndex).getX();
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read(blockPos, vdb);
		DiskUtils.copyIntToBlock(vdb, index+5, firstFreeINode); //sets this iNode's next to the head
		diskUnit.write(blockPos, vdb);
		firstFreeINode = iNodeIndex; //this iNode is the new head.
		diskUnit.setFirstFreeINode(firstFreeINode);
	}
	/**
	 * Gets iNode at an index.
	 * @param iNodeIndex index of a file's iNode.
	 * @return file's iNode.
	 */
	public INode getINodeAtIndex(int iNodeIndex) { //returns iNode at this index.
		int index =(int) getPosition(iNodeIndex).getY();
		int blockPos = (int)getPosition(iNodeIndex).getX();
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read(blockPos, vdb);
		return new INode(vdb.getElement(index),DiskUtils.getIntFromBlock(vdb, index+1),DiskUtils.getIntFromBlock(vdb, index+5));
	}
	/**
	 * Overwrites an iNode at an index; used for creating and overwriting files.
	 * @param indexToAdd index of iNode to overwrite.
	 * @param nta iNode that will overwrite.
	 * @return index of added iNode.
	 */
	private int setINodeAtIndex(int indexToAdd, INode nta) { //sets new iNode given an i node index; overwrites an iNode at that position
		int index =(int) getPosition(indexToAdd).getY();
		int blockPos = (int)getPosition(indexToAdd).getX();
		VirtualDiskBlock vdb = new VirtualDiskBlock(blockSize);
		diskUnit.read(blockPos, vdb); //save previous information
		vdb.setElement(index,nta.getType()); //Set new type
		DiskUtils.copyIntToBlock(vdb, index+1,nta.getSize()); //set new Size
		DiskUtils.copyIntToBlock(vdb, index+5,nta.getIndex()); //set new Index
		diskUnit.write(blockPos, vdb);
		return indexToAdd;
	}
	/**
	 * Creates a file's iNode.
	 * @param nta iNode to add.
	 * @return node's index
	 */
	private int addINode(INode nta) { //Adds iNode to list
		int temp = firstFreeINode;
		firstFreeINode = getINodeAtIndex(firstFreeINode).getIndex();
		diskUnit.setFirstFreeINode(firstFreeINode);
		return setINodeAtIndex(temp,nta);
	}



	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Gets disk name.
	 * @return Disk's name.
	 */
	public String getDiskName(){
		return mountName;
	}
	/**
	 * Verifies if a disk is mounted.
	 * @return True if there is a mounted disk, false otherwise.
	 */
	public boolean isMounted(){
		return mountName!=null;
	}
	/**
	 * Splits an index into two components: blockIndex at getX(); index at getY().
	 * blockIndex for reading block containing index 
	 * and index for reading index within that block.
	 * @param totalByteIndex index to split.
	 * @return a Point 2D object containing the split index.
	 */
	private Point2D getPosition(int totalByteIndex){
		//first is block index, second is index within that block's array.
		return new Point2D.Double((int)Math.floor(totalByteIndex/blockSize)-1,(totalByteIndex%blockSize));
	}
	/**
	 * Prepares diskManager for method use.
	 * @param diskName disk to manage.
	 */
	public void mount(String diskName){
		diskUnit = DiskUnit.mount(diskName);
		capacity = diskUnit.getCapacity();
		blockSize = diskUnit.getBlockSize();
		freeBlockIndex = diskUnit.getFreeBlockIndex();
		endOfFreeBlockIndex = diskUnit.getEndOfFreeBlockIndex();
		firstFreeINode = diskUnit.getFirstFreeINode();
		totalINodes = diskUnit.getTotalINodes();
		currentDirectory = getINodeAtIndex(blockSize);
		mountName = diskName;
	}
	/**
	 * Shuts down mount
	 */
	public void stop(){
		diskUnit.shutdown();
		mountName=null;
		currentDirectory=null;
	}
}
