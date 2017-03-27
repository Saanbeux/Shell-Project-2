package disk_Exceptions;
/**
 * Thrown when there is an invalid block in the Disk Unit
 * @author Moises Garip
 *
 */
public class InvalidBlockException extends RuntimeException {
	public InvalidBlockException()
	{
	}
	/**
	 *
	 * @param exceptionMessage printed message when the exception is thrown.
	 */
	public InvalidBlockException(String exceptionMessage)
	{
		super(exceptionMessage);
	}
	
}
