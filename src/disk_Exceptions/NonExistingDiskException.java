package disk_Exceptions;
/**
 * Thrown whenever there is a Disk that does not exist in Disk Unit
 * @author Moises Garip
 *
 */
@SuppressWarnings("serial")
public class NonExistingDiskException extends RuntimeException{
	
	public NonExistingDiskException()
	{
	}
	public NonExistingDiskException(String exceptionMessage)
	{
		super(exceptionMessage);
	}
	
}
