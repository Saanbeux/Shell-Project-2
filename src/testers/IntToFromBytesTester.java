package testers;
import diskUtilities.DiskUtils;


public class IntToFromBytesTester {
              // testing with array of byte ... similar for the VirtualDiskBlock
	public static void main(String[] args) {
		byte[] bytes = new byte[500];   // array to play with...
				
		test(bytes, 20, 787);
		test(bytes, 80, 50);
		test(bytes, 10, -1);
		test(bytes, 30, -989);
		test(bytes, 0, 800); 
	}
	
	private static void test(byte[] bytes, int index, int value) { 
		// Save value (its bytes) in bytes[index..index+3]
		DiskUtils.copyIntToBytesArray(bytes, index, value);
		
		// Extract integer saved in bytes[index..index+3]
		int recValue = DiskUtils.getIntFromBytesArray(bytes, index); 
		
		// Display results....
		System.out.print("Recovered value: " + recValue + 
				"  \tExpected value: " + value);
		System.out.println((recValue==value?"  \tSUCCESS!":"  \tTest FAILED"));
	}
}
