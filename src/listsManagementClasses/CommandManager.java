package listsManagementClasses;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import diskUtilities.DiskUnit;
import disk_Exceptions.NonExistingDiskException;
import theSystem.SystemCommandsProcessor;

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
	public int getDiskBlockSize(String diskName){
		DiskUnit tempDisk = DiskUnit.mount(diskName); //temporarily mounts disk to read blockSize
		int etr = tempDisk.getBlockSize();
		tempDisk.shutdown();
		return etr;
	}
	public int getDiskBlockAmount(String diskName){
		DiskUnit tempDisk = DiskUnit.mount(diskName); //temporarily mounts disk to read capacity
		int etr = tempDisk.getCapacity();
		tempDisk.shutdown();
		return etr;
	}
	public boolean nameExists(String diskName) { //checks if disk is saved in the list
		for (String name : names){
			if (name.equals(diskName)){
				return true;
			}
		}
		return false;
	}
	public String getNameAtIndex(int index){ //returns index of name in list.
		return names.get(index);
	}
	public int getNumberOfDisks(){
		return size;
	}
	public boolean isEmpty(){
		return size==0;
	}
}
