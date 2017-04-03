package testers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import diskUtilities.DiskUnit;
import diskUtilities.DiskUtils;
import diskUtilities.VirtualDiskBlock;

public class diskDebugger {

	public static void main(String[] args) throws IOException {
		RandomAccessFile raf = new RandomAccessFile("moyi","rw");
		DiskUnit moyi = DiskUnit.mount("moyi");
		VirtualDiskBlock vdb = new VirtualDiskBlock(moyi.getBlockSize());
		int i=0;
		for (int x=0;x<24;x++){
			vdb.setElement(x, raf.readByte());
			if(x%4==0&&x!=0){
			int currentElement = DiskUtils.getIntFromBlock(vdb, i);
			i+=4;
			}
		}
		for (int x=0;x<moyi.getCapacity();x++){
			moyi.read(x, vdb);
		}
		//reads every virtual disk block in disk for reading through debugger.
	}
}
