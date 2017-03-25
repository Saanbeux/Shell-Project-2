/**
 * 
 */
package theSystem;

import java.io.IOException;

import systemGeneralClasses.SystemController;

/**
 * @author Pedro I. Rivera-Vega
 *
 */
public class MySystem {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException  {
		SystemController system = new SystemController(); 
		system.start(); 
		// the system is shutting down...
		System.out.println("+++++ SYSTEM SHUTDOWN +++++"); 
	}

}
