package freenet.pluginmanager;

import java.net.InetAddress;

/**
 * Base class for all transports
 * @see PacketTransportPlugin
 * @see StreamTransportPlugin
 * @author chetan
 *
 */
public abstract class TransportPlugin implements Runnable {
	
	public String transportName;
	
	public PluginAddress pluginAddress;
	
	public enum TransportType{
		streams, packets
	}
	
	/**
	 * Modes the plugin can work in. The plugin might be specifically designed to work on a particular mode
	 */
	public final boolean opennet, darknet;
	
	/**Method to stop a plugin, not terminate it.  
	 * Can be used for temporarily stopping a plugin, or putting it to sleep state.
	 * Don't know if this method is really necessary. 
	 */
	public abstract void stopTransportPlugin();
	
	public TransportPlugin(final boolean opennet, final boolean darknet){
		this.opennet = opennet;
		this.darknet = darknet;
	}
}

class PluginAddress{
	InetAddress address;
	int portNumber;
	
	public PluginAddress(InetAddress address, int portNumber){
		this.address = address;
		this.portNumber = portNumber;
	}
	
	public String getAddress(){
		return address.getHostAddress();
	}
}