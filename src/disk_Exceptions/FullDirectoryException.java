package disk_Exceptions;
/**
 * Thrown whenever a file has no more available space for another data file or directory.
 * @author Moises Garip
 *
 */
public class FullDirectoryException extends RuntimeException{

	public FullDirectoryException(){}
	public FullDirectoryException(String exceptionMessage)
	{
		super(exceptionMessage);
	}
}