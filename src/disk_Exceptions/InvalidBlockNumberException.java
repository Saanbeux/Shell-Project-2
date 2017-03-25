package disk_Exceptions;
/**
 * Thrown when there is an invalid block number to search in the Disk Unit
 * @author Francisco Diaz
 *
 */
public class InvalidBlockNumberException extends RuntimeException {
	public InvalidBlockNumberException()
	{
	}
	public InvalidBlockNumberException(String exceptionMessage)
	{
		super(exceptionMessage);
	}
	
}
