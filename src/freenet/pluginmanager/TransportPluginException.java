package freenet.pluginmanager;

public class TransportPluginException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public final String errorMessage;
	
	public TransportPluginException(String errorMessage){
		super(errorMessage);
		this.errorMessage = errorMessage;
	}
	

}
