package disk_Exceptions;
/**
 * Thrown whenever there is a Disk that does not exist in Disk Unit
 * @author Moises Garip
 *
 */
@SuppressWarnings("serial")
public class FullDiskException extends RuntimeException{

	public FullDiskException(){}
	public FullDiskException(String exceptionMessage)
	{
		super(exceptionMessage);
	}
}