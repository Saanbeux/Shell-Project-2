package disk_Exceptions;
/**
 * Thrown whenever there is an Invalid Parameter
 * @author Francisco Diaz
 *
 */
public class InvalidParameterException extends RuntimeException {
	public InvalidParameterException()
	{
	}
	public InvalidParameterException(String exceptionMessage)
	{
		super(exceptionMessage);
	}
	
}
