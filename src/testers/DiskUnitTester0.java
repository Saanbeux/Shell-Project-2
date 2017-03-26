package testers;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import diskUtilities.DiskUnit;
import diskUtilities.VirtualDiskBlock;

//ignore tester
public class DiskUnitTester0 {
	public static void main(String[] args) throws IOException {
		DiskUnit test = DiskUnit.mount("moyi");
		RandomAccessFile testRaf = new RandomAccessFile("moyi","rw");
		VirtualDiskBlock reader = new VirtualDiskBlock(test.getBlockSize());
		int i=0;
		while (i<test.getCapacity()){
			for(int x=0;x<reader.getCapacity();x++){
				test.read(i, reader);
				System.out.print(reader.getElement(x));
				i+=1;
			}
		}
		testRaf.close();
	}
}

