package disk_Exceptions;
/**
 * Thrown whenever there is an Invalid Parameter
 * @author Moises Garip
 *
 */
@SuppressWarnings("serial")
public class InvalidParameterException extends RuntimeException {
	public InvalidParameterException()
	{
	}
	public InvalidParameterException(String exceptionMessage)
	{
		super(exceptionMessage);
	}
	
}
