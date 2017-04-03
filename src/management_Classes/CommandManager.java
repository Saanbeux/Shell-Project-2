package management_Classes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import diskUtilities.DiskUnit;
/**
 * Creates an object that manages files outside of the command prompt, altering the list that stores disk units.
 * @author Moises
 *
 */
public class CommandManager {
	RandomAccessFile diskNameList;
	private int size;
	ArrayList<String> names;
	/**
	 * Default constructor for the Disk Manager
	 * It initializes and creates and empty list of disks
	 * @throws FileNotFoundException 
	 */
	public CommandManager (){
		try{
			if(new File("DiskNames.txt").exists()){
				names = new ArrayList<String>();
				diskNameList = new RandomAccessFile("DiskNames.txt","rw");
				while (diskNameList.getFilePointer()<diskNameList.length()){ //Go through the whole file
					names.add(diskNameList.readLine()); //add each new line, which is a disk name, to the list
				}
				size=names.size();
			}else{//creates disk if it doesnt exist
				diskNameList = new RandomAccessFile("DiskNames.txt","rw");
				names = new ArrayList<String>();
				size=0;
			}
		}catch (Exception e){}
	}
	/**
	 * Adds disk to file that stores disk names.
	 * @param diskName disk to add.
	 */
	public void addNewDiskToManager(String diskName) {
		try{
			diskNameList.seek(diskNameList.length());//appends disk name to the end of the file
			diskNameList.writeBytes(diskName+"\n");
			size++;
			names.add(diskName);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

/**
 * Deletes a disk from the file that stores disk names.
 * @param diskName disk to delete.
 */
	public void deleteDisk(String diskName){
		try{
			size--;
			diskNameList.seek(0);
			names.remove(diskName);
			diskNameList.close();
			File file = new File("DiskNames.txt"); //for deleting old names
			file.delete();
			diskNameList = new RandomAccessFile("DiskNames.txt","rw"); //creates file with new names.
			for (String name : names){
				diskNameList.writeBytes(name+"\n");
			}
			File fileToDelete = new File(diskName); //created to actually delete the disk
			fileToDelete.delete(); // deletes DiskUnit
		}catch (Exception e){}
	}



	//Getters, setters and verifiers.
	/**
	 * Temporarily opens disk to read block size.
	 * @param diskName disk to read block size.
	 * @return block size of disk.
	 */
	public int getDiskBlockSize(String diskName){
		DiskUnit tempDisk = DiskUnit.mount(diskName); //temporarily mounts disk to read blockSize
		int etr = tempDisk.getBlockSize();
		tempDisk.shutdown();
		return etr;
	}
	/**
	 * Temporarily opens disk to read capacity.
	 * @param diskName disk to read capacity.
	 * @return capacity of disk.
	 */
	public int getDiskBlockAmount(String diskName){
		DiskUnit tempDisk = DiskUnit.mount(diskName); //temporarily mounts disk to read capacity
		int etr = tempDisk.getCapacity();
		tempDisk.shutdown();
		return etr;
	}
	/**
	 * Verifies if disk exists in name list.
	 * @param diskName disk to check.
	 * @return True if disk exists, false otherwise.
	 */
	public boolean nameExists(String diskName) { //checks if disk is saved in the list
		for (String name : names){
			if (name.equals(diskName)){
				return true;
			}
		}
		return false;
	}
	/**
	 * Returns disk name at index within file.
	 * @param index index to read.
	 * @return disk name.
	 */
	public String getNameAtIndex(int index){ //returns index of name in list.
		return names.get(index);
	}
	/**
	 * Returns amount of disks registered in system.
	 * @return number of disks
	 */
	public int getNumberOfDisks(){
		return size;
	}
	/**
	 * Returns whether or not there are no disks registered in the system.
	 * @return True if list is empty, false otherwise.
	 */
	public boolean isEmpty(){
		return size==0;
	}
}
