package freenet.node;

public abstract class TransportPlugin implements Runnable {
	
	public String transportName; 
	
	public enum TransportType{
		streams, packets
	}
	
	//Modes the plugin can work in. The plugin might be specifically designed to work on a particular mode
	public boolean opennet;
	public boolean darknet;
	
	/**Method to stop a plugin, not terminate it.  
	 * Can be used for temporarily stopping a plugin, or putting it to sleep state.
	 * Don't know if this method is really necessary. 
	 */
	public abstract void stopTransportPlugin();
}
