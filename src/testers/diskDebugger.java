package testers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import diskUtilities.DiskUnit;
import diskUtilities.VirtualDiskBlock;

public class diskDebugger {

	public static void main(String[] args) throws IOException {
		VirtualDiskBlock vdb = new VirtualDiskBlock(32);
		RandomAccessFile raf = new RandomAccessFile("moyi","rw");
		DiskUnit moyi = DiskUnit.mount("moyi");
		for (int x=0;x<moyi.getBlockSize();x++){
			vdb.setElement(x, raf.readByte());
		}
		for (int x=0;x<moyi.getBlockSize();x++){
			moyi.read(x, vdb);
		}
		//reads every virtual disk block in disk for reading through debugger.
	}
}
