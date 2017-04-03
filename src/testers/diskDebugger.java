package testers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import diskUtilities.DiskUnit;
import diskUtilities.DiskUtils;
import diskUtilities.VirtualDiskBlock;
import management_Classes.INode;

@SuppressWarnings("unused")
public class diskDebugger {

	public static void main(String[] args) throws IOException {
		@SuppressWarnings("resource")
		RandomAccessFile raf = new RandomAccessFile("moyi","rw");
		DiskUnit moyi = DiskUnit.mount("moyi");
		VirtualDiskBlock vdb = new VirtualDiskBlock(moyi.getBlockSize());
		int i=0;
		for (int x=0;x<25;x++){
			vdb.setElement(x, raf.readByte());
			if(x%4==0&&x!=0){
			int currentElement = DiskUtils.getIntFromBlock(vdb, i);
			i+=4;
			}
		}
		for (int x=0;x<moyi.getCapacity();x++){
			moyi.read(x, vdb);
			INode iNode;
			if (x<Math.ceil(moyi.getCapacity()/100)&&x!=0){
				int h=0;
				for(int j=0; j<moyi.getBlockSize()*moyi.getCapacity()/900;){
					iNode = new INode(vdb.getElement(h),DiskUtils.getIntFromBlock(vdb, h+1),DiskUtils.getIntFromBlock(vdb, h+5));
					j++;
					h+=9;
				}
			}
		}
		//reads every virtual disk block in disk for reading through debugger.
	}
}
