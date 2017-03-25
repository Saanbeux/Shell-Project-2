package listsManagementClasses;

import java.io.IOException;
import java.io.RandomAccessFile;

import lists.INode;
import lists.SLINodeList;

public class DiskManager {
	RandomAccessFile diskUnitRAF;
	private int blockSize;
	private int capacity;
	private int freeBlockIndex; //index where the free block tree root is located
	private int endOfFreeBlockIndex; //last index in free block tree
	private int firstFreeINode; // Next available I-Node
	private int totalINodes;//Total number of I-Nodes
	private int iNodeBlockAmount;//how many blocks in total INodes will take up
	private int iNodePerBlock; //how many INodes fit in a block
	private int intsPerBlock; //how many separate numbers fit in a block
	private SLINodeList iNodeList; //Singly Linked List for the INodes

	public DiskManager(String diskName, boolean recentlyCreated) throws IOException{
		//constants; will always be in the diskunit regardless if it was created recently.
		diskUnitRAF = new RandomAccessFile(diskName,"rw");
		capacity = diskUnitRAF.readInt();
		blockSize = diskUnitRAF.readInt();
		iNodeList = new SLINodeList(); //initialize linked list
		intsPerBlock = blockSize/4;
		totalINodes = ((blockSize*capacity)/900); //total I-Nodes is 1% of diskUnit size
		iNodeBlockAmount = ((9*totalINodes)/blockSize); //how many blocks in total vINodes will take up
		iNodePerBlock = blockSize/9;
		
		if(!recentlyCreated){ //Extract all the control information
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
			endOfFreeBlockIndex = freeBlockIndex+blockSize-4; //the end of this block,-4 to obtain last int.
			firstFreeINode = blockSize; //First free INode in a formated disk is the first block after control***check
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















	//Constructor helpers
	private void formatFreeBlockSpace() throws IOException{

		int currentBlockIndex = 0;//file pointer variable
		int lastBlockAccessed = freeBlockIndex+blockSize; //last block that was added to the tree
		int stillFreeSpace = capacity-iNodeBlockAmount; //remaining free space
		int previousRoot = 0; //dummy to reference last block that stored free blocks
		int endOfBlock = 0;


		diskUnitRAF.seek(freeBlockIndex); //makes sure that first block tree node returns 0 as last index; meaning there is no more space
		diskUnitRAF.writeInt(0);
		currentBlockIndex=currentBlockIndex+4;//offsets first hardcoded int

		while (stillFreeSpace>0){ //while there are still free blocks to manage
			endOfBlock = blockSize+currentBlockIndex-4;
			while (currentBlockIndex<endOfBlock){ //while this block still has space to store ints

				diskUnitRAF.writeInt(lastBlockAccessed); // write next available free block onto tree.
				lastBlockAccessed=lastBlockAccessed+blockSize; //Set next block up for adding to tree
				stillFreeSpace=stillFreeSpace-blockSize; //Less blocks to add to tree
				currentBlockIndex = (int) diskUnitRAF.getFilePointer(); // Less space in block to store ints.
			}
			endOfFreeBlockIndex=currentBlockIndex;// This is the last of the free blocks stored in the block
			previousRoot = currentBlockIndex-blockSize;//If here, we are at the end of the last block. Subtracting blockSize yields it's index
			currentBlockIndex=lastBlockAccessed; //Sets pointer to "0" with respect to the new block that stores free blocks.
			diskUnitRAF.seek(currentBlockIndex);//Sets pointer
			diskUnitRAF.writeInt(previousRoot);//hard codes root to be the previous tree.
			freeBlockIndex=currentBlockIndex;
			currentBlockIndex=lastBlockAccessed+4;//offsetting root.
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
	}
}
