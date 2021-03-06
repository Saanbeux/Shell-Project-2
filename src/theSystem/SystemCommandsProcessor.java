package theSystem;

import java.io.File;
import java.util.ArrayList;

import diskUtilities.DiskUnit;
import managementClasses.CommandManager;
import managementClasses.DiskManager;
import managementClasses.IntStack;
import operandHandlers.OperandValidatorUtils;
import systemGeneralClasses.Command;
import systemGeneralClasses.CommandActionHandler;
import systemGeneralClasses.CommandProcessor;
import systemGeneralClasses.FixedLengthCommand;
import systemGeneralClasses.SystemCommand;


public class SystemCommandsProcessor extends CommandProcessor { 

	private ArrayList<String> resultsList; //for printing results
	private CommandManager commandManager = new CommandManager(); // for the processors
	private DiskManager diskManager = new DiskManager();
	boolean stopExecution; //shuts down program
	SystemCommand attemptedSC; 

	//NOTE: The HelpProcessor is inherited...

	// To initially place all lines for the output produced after a 
	// command is entered. The results depend on the particular command. 

	// The system command that looks like the one the user is
	// trying to execute. 


	// This field is false whenever the system is in execution
	// Is set to true when in the "administrator" state the command
	// "shutdown" is given to the system.

	////////////////////////////////////////////////////////////////
	// The following are references to objects needed for management 
	// of data as required by the particular octions of the command-set..
	// The following represents the object that will be capable of
	// managing the different lists that are created by the system
	// to be implemented as a lab exercise. 

	/**
	 *  Initializes the list of possible commands for each of the
	 *  states the system can be in. 
	 */
	public SystemCommandsProcessor() {

		// stack of states
		currentState = new IntStack(); 

		// The system may need to manage different states. For the moment, we
		// just assume one state: the general state. The top of the stack
		// "currentState" will always be the current state the system is at...
		currentState.push(GENERALSTATE); 

		// Maximum number of states for the moment is assumed to be 1
		// this may change depending on the types of commands the system
		// accepts in other instances...... 
		createCommandList(1);    // only 1 state -- GENERALSTATE

		// commands for the state GENtheIndexERALSTATE

		// the following are just for demonstration...

		/////////////////// commands/////////////////////////////////////


		add(GENERALSTATE, SystemCommand.getFLSC("createdisk name int int", new createDiskProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("deletedisk name", new deleteDiskProcessor()));
		add(GENERALSTATE, SystemCommand.getFLSC("loadfile name name", new loadFileProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("cp name name", new cpProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("mount name", new mountProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("unmount", new unmountProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("createfile name int", new CreateFileProcessor()));
		add(GENERALSTATE, SystemCommand.getFLSC("ls", new lsProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("cat name", new catProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("showdisks", new showdisksProcessor()));
		add(GENERALSTATE, SystemCommand.getFLSC("help", new HelpProcessor()));
		add(GENERALSTATE, SystemCommand.getFLSC("exit", new ShutDownProcessor()));


		// need to follow this pattern to add a SystemCommand for each
		// command that has been specified...
		// ...

		// set to execute....
		stopExecution = false; 

	}

	public ArrayList<String> getResultsList() { 
		return resultsList; 
	}
/**
 * Creates a processor for handling the printing of every disk available
 * within the system.
 * @author Moises
 *
 */
	private class showdisksProcessor implements CommandActionHandler{
		@Override
		public ArrayList<String> execute(Command c) {
			resultsList = new ArrayList<>();
			if(!commandManager.isEmpty()){
				resultsList.add("\n Disks that currently exist: \n");
				String result = "";
				String name = "";
				for(int i=0; i< commandManager.getNumberOfDisks();i++){
					name = commandManager.getNameAtIndex(i);
					result=Integer.toString(i+1)+") " + name + " -- Block Size:"
							+ " ["+commandManager.getDiskBlockSize(name)+"] -- Capacity:"
							+ " ["+commandManager.getDiskBlockAmount(name)+"] --";
					if (diskManager.isMounted() && diskManager.getDiskName().equals(name)){
						result = result+" [CURRENTLY MOUNTED]\n";
					}else{
						result = result+" [Not Mounted]\n";
					}
					resultsList.add(result);
				}
			}else{
				resultsList.add("\n No disks currently exist! \n");
			}
			return resultsList;
		}

	}
	/**
	 * Creates a processor that handles the creation of a disk and its formatting.
	 * @author Moises
	 *
	 */
	private class createDiskProcessor implements CommandActionHandler{

		@Override
		public ArrayList<String> execute(Command c) {
			resultsList = new ArrayList<String>();
			FixedLengthCommand fc = (FixedLengthCommand) c;
			String diskName = fc.getOperand(1); 														
			int numberOfBlocks = Integer.parseInt(fc.getOperand(2));									
			int bsize = Integer.parseInt(fc.getOperand(3));							 					

			// Verifies if the disk has valid name and that it does not exist 
			if (!OperandValidatorUtils.isValidName(diskName)){											
				resultsList.add("\n Invalid name formation: " + diskName +"\n");
			}
			else if(commandManager.nameExists(diskName))	{													
				resultsList.add("\n his disk already exists " + diskName+"\n");
			}
			else if(bsize<32 || !((bsize&-bsize)==bsize)){
				resultsList.add("\n Disk block size must be greater than 32 and a power of 2: " + bsize+"\n");
			}
			else if(numberOfBlocks<32 || !((numberOfBlocks&-numberOfBlocks)==numberOfBlocks)){
				resultsList.add("\n Disk block size must be greater than 32 and a power of 2: " +numberOfBlocks+"\n");
			}
			else {
				DiskUnit.createDiskUnit(diskName, numberOfBlocks, bsize);
				diskManager.prepareDiskUnit(diskName);
				commandManager.addNewDiskToManager(diskName);
				resultsList.add("\n Disk created!\n ");					 
			}
			return resultsList;
		} 
	}


	/**
	 * Creates a processor that handles the deletion of a disk from the system.
	 * @author Moises
	 *
	 */
	private class deleteDiskProcessor implements CommandActionHandler{

		@Override
		public ArrayList<String> execute(Command c) {
			resultsList = new ArrayList<>();
			FixedLengthCommand fc = (FixedLengthCommand)c;
			String diskName = fc.getOperand(1); 
			if (!OperandValidatorUtils.isValidName(diskName)){											
				resultsList.add("\n Invalid name formation: " + diskName+"\n"); 
			}
			else if (diskManager.isMounted() && diskManager.getDiskName().equals(diskName)){											
				resultsList.add("\n Cannot delete a mounted disk: " + diskName+" \n"); 
			}
			else if(!commandManager.nameExists(diskName))	{													
				resultsList.add("\n Disk does not exist. \n");
			}
			else {
				commandManager.deleteDisk(diskName);
				resultsList.add("\n "+diskName + " has been deleted. \n");
			}
			return resultsList;
		}
	}


/**
 * Creates a processor for handling the mounting of a disk, necessary for other commands.
 * @author Moises
 *
 */
	private class mountProcessor implements CommandActionHandler{

		public ArrayList<String> execute(Command c) {
			resultsList = new ArrayList<>();
			FixedLengthCommand fc = (FixedLengthCommand)c;
			String diskName = fc.getOperand(1);
			if (!OperandValidatorUtils.isValidName(diskName)){											
				resultsList.add("\n Invalid name formation: " + diskName+" \n");
			}

			else if (diskManager.isMounted()){
				if (diskManager.getDiskName().equals(diskName)){
					resultsList.add("\n That disk is already mounted! \n");
				}else{
					resultsList.add("\n A disk is already mounted: "+diskManager.getDiskName()+"! \n");
				}
			}
			else if(!(commandManager.nameExists(diskName))){
				resultsList.add("\n"+diskName+" does not exist! \n");
			}
			else{
				try{
					diskManager.mount(diskName);
					resultsList.add("\n"+diskName+" is Mounted. \n");
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			return resultsList;
		}
	}


/**
 * Creates a processor for handling the unmounting of a disk.
 * @author Moises
 *
 */
	private class unmountProcessor implements CommandActionHandler{
		public ArrayList<String> execute(Command c) {
			resultsList = new ArrayList<>();
			if(!diskManager.isMounted()){
				resultsList.add("\n No disk is currently mounted. \n");
			}
			else {
				diskManager.stop();
				resultsList.add("\n Successfully unmounted disk. \n");
			}
			return resultsList;
		}

	}


/**
 * Creates a processor for handling an import of an external file into a disk. 
 * @author Moises
 *
 */
	private class loadFileProcessor implements CommandActionHandler{

		@Override
		public ArrayList<String> execute(Command c) {
			ArrayList<String> resultsList = new ArrayList<>();
			FixedLengthCommand fc = (FixedLengthCommand)c;
			String fileToBeOverwritten = fc.getOperand(1);
			String fileToBeRead = fc.getOperand(2);
			//there must be a disk unit mounted.
			if (!diskManager.isMounted()){ 
				resultsList.add("\n No disk is currently mounted, no file could be found. \n");
				return resultsList;
			}
			//The file must exist
			if (!(new File(fileToBeRead).exists())){ 
				resultsList.add("\n No such file in directory: "+fileToBeRead+"! \n");
				return resultsList;
			}
			//there must be space for the file.
			if (!diskManager.checkForSpace(fileToBeRead)){
				resultsList.add("\n There is no more space left in disk! \n");
			}
			//overwrite file
			else if (diskManager.fileExistsInDirectory(fileToBeOverwritten)){
				resultsList.add("\n "+fileToBeRead+" has been overwritten! \n");
			}
			//create file
			else{
				resultsList.add("\n No such file name: "+fileToBeOverwritten+"! File has been created instead. \n");
			}
			return resultsList;
		}
	}





/**
 * Creates a processor that handles the creation of a dummy file within the directory.
 * @author Moises
 *
 */
	private class CreateFileProcessor implements CommandActionHandler{

		public ArrayList<String> execute(Command c) {
			ArrayList<String> resultsList = new ArrayList<>();
			FixedLengthCommand fc = (FixedLengthCommand)c;
			String fileName = fc.getOperand(1);
			int fileSize  = Integer.parseInt(fc.getOperand(2));
			//there must be a disk unit mounted.
			if (!diskManager.isMounted()){ 
				resultsList.add("\n No disk is currently mounted, no file could be found. \n");
			}else if(!diskManager.hasAvailableINodes()){
				resultsList.add("\n No more space in directory! \n");
			}else{
				diskManager.testFiles(fileName,fileSize);
				resultsList.add("\n File created! \n");
			}
			return resultsList;
		}

	}







/**
 * Creates a processor for handling the copy of an internal file.
 * @author Moises
 *
 */
	private class cpProcessor implements CommandActionHandler{

		@Override
		public ArrayList<String> execute(Command c) {
			ArrayList<String> resultsList = new ArrayList<>();
			FixedLengthCommand fc = (FixedLengthCommand)c;
			String fileToBeOverwritten = fc.getOperand(2);
			String fileToBeRead = fc.getOperand(1);
			//there must be a disk unit mounted.
			if (!diskManager.isMounted()){ 
				resultsList.add("\n No disk is currently mounted, no file could be found. \n");
				return resultsList;
			}//The file must exist
			
			if (!diskManager.fileExistsInDirectory(fileToBeRead)){ 
				resultsList.add("\n No such file name: "+fileToBeRead+"! \n");
				
			}else if (diskManager.isDirectory(fileToBeRead)){
				resultsList.add("\n File is not a data file! \n");
			}
			
			else if (!diskManager.fileExistsInDirectory(fileToBeOverwritten)){
				resultsList.add("\n "+fileToBeRead+" does not exist! \n");
				
			}else if(!diskManager.checkForSpace(fileToBeRead)){
				resultsList.add("\n There is no space in current directory! \n");
			}else{
				diskManager.duplicateFile(fileToBeRead, fileToBeOverwritten);
				resultsList.add("\n File has been copied! \n");
				}
			return resultsList;
		}
	}
	
/**
 * Creates a processor for handling the printing of all files within directory.
 * @author Moises
 *
 */
	private class lsProcessor implements CommandActionHandler{
		public ArrayList<String> execute(Command c) {
			resultsList = new ArrayList<>();
			if(!diskManager.isMounted()){
				resultsList.add("\n No disk is currently mounted. \n");
				return resultsList;
			}
			ArrayList<String> theNames = diskManager.listFiles();
			if (theNames.isEmpty()){
				resultsList.add("\n Directory is empty. \n");
			}else{
				return theNames;
			}
			return resultsList;
		}
	}
/**
 * Creates a processor that handles the reading of an internal file.
 * @author Moises
 *
 */
	private class catProcessor implements CommandActionHandler{

		public ArrayList<String> execute(Command c) {//check if exists in directory, check if is data or directory
			resultsList = new ArrayList<>();
			FixedLengthCommand fc = (FixedLengthCommand)c;
			String fileName = fc.getOperand(1);
			if (!OperandValidatorUtils.isValidName(fileName)){											
				resultsList.add("\n Invalid name : " +fileName+" \n");
			}else if (!diskManager.isMounted()){ //No disk mounted
				resultsList.add("\n No disk is currently mounted. \n");
			}else if(!(diskManager.fileExistsInDirectory(fileName))){ //file doesn't exist
				resultsList.add("\n"+fileName+" does not exist! \n");
			}else if (diskManager.isDirectory(fileName)){ //file is directory
				ArrayList<String>theNames = diskManager.listFilesAtINode(diskManager.getINodeAtIndex(diskManager.findInDirectory(fileName)+20));
				if(theNames.isEmpty()){
					resultsList.add("\n File is empty. \n");
				}else{
					return theNames;
				}
			}else{//file is a data file
				return diskManager.readDataFile(fileName);
			}
			return resultsList;
		}

	}


	private class ShutDownProcessor implements CommandActionHandler { 
		public ArrayList<String> execute(Command c) { 

			resultsList = new ArrayList<String>(); 
			resultsList.add("Thank you for using our prototype virtual disk operating system.");
			stopExecution = true;
			return resultsList; 
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean inShutdownMode() {
		return stopExecution;
	}
}		





