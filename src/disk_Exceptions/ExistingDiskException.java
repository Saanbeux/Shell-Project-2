package disk_Exceptions;
/**
 * Exception when there is a Disk
 * @author Moises Garip
 *
 */
//
@SuppressWarnings("serial")
public class ExistingDiskException extends RuntimeException{
	public ExistingDiskException()
	{
	}
	/**
	 * Thrown when there is an existing disk in the disk unit.
	 * @param exceptionMessage message to throw when an exception is thrown
	 */
	public ExistingDiskException(String exceptionMessage)
	{
		super(exceptionMessage);
	}

}
