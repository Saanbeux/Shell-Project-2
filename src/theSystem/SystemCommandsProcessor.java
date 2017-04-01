package theSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import diskUtilities.DiskUnit;
import disk_Exceptions.NonExistingDiskException;
import listsManagementClasses.CommandManager;
import listsManagementClasses.DiskManager;
import operandHandlers.OperandValidatorUtils;
import stack.IntStack;
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
		//add(GENERALSTATE, SystemCommand.getFLSC("cp name name", new cpProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("mount name", new mountProcessor())); 
		add(GENERALSTATE, SystemCommand.getFLSC("unmount", new unmountProcessor())); 
		//add(GENERALSTATE, SystemCommand.getFLSC("ls", new lsProcessor())); 
		//add(GENERALSTATE, SystemCommand.getFLSC("cat name", new catProcessor())); 
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



	private class mountProcessor implements CommandActionHandler{

		@Override
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
			}//The file must exist
			if (!diskManager.fileExistsInDirectory(fileToBeRead)){ 
				resultsList.add("\n No such file name: "+fileToBeRead+"! \n");
				return resultsList;
			}//there must be space for the file.
			int fileToBeReadIndex = diskManager.findInDirectory(fileToBeRead);
			if (!diskManager.isAvailableSpace(fileToBeReadIndex)){
				resultsList.add("\n There is no more space left in disk! \n");
			}//overwrite file
			else if (diskManager.fileExistsInDirectory(fileToBeOverwritten)){
				diskManager.loadFileInDirectory(fileToBeRead, fileToBeOverwritten);
				resultsList.add("\n "+fileToBeRead+" has been overwritten! \n");
			}else{//create file
				diskManager.duplicateFile(fileToBeRead, fileToBeOverwritten);
				resultsList.add("\n No such file name: "+fileToBeOverwritten+"! File has been created instead. \n");
			}
			return resultsList;
		}

	}





	private class cpProcessor implements CommandActionHandler{

		@Override
		public ArrayList<String> execute(Command c) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	private class lsProcessor implements CommandActionHandler{

		@Override
		public ArrayList<String> execute(Command c) {
			// TODO Auto-generated method stub
			return null;
		}

	}
	private class catProcessor implements CommandActionHandler{

		@Override
		public ArrayList<String> execute(Command c) {
			// TODO Auto-generated method stub
			return null;
		}

	}




	// INNER CLASSES -- ONE FOR EACH VALID COMMAND --
	/**
	 *  The following are inner classes. Notice that there is one such class
	 *  for each command. The idea is that enclose the implementation of each
	 *  command in a particular unique place. Notice that, for each command, 
	 *  what you need is to implement the internal method "execute(Command c)".
	 *  In each particular case, your implementation assumes that the command
	 *  received as parameter is of the type corresponding to the particular
	 *  inner class. For example, the command received by the "execute(...)" 
	 *  method inside the "LoginProcessor" class must be a "login" command. 
	 *
	 */


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





