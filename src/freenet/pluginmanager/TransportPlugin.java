package freenet.pluginmanager;

import java.net.InetAddress;

import freenet.io.comm.IOStatisticCollector;
import freenet.node.Node;
import freenet.node.TransportManager.TransportMode;

/**
 * Base class for all transports
 * @see PacketTransportPlugin
 * @see StreamTransportPlugin
 * @author chetan
 *
 */
public abstract class TransportPlugin implements Runnable {
	
	/**Unique name for every transport*/
	public final String transportName;
	
	public enum TransportType{
		streams, packets
	}
	
	/**
	 * Initialize the mode in which the instance of the plugin is to work in.
	 */
	public final TransportMode transportMode;
	
	public final Node node;
	
	public TransportPlugin(final String transportName, final TransportMode transportMode, Node node){
		this.transportName = transportName;
		this.transportMode = transportMode;
		this.node = node;
	}
	
	/**
	 * Method to initialise and start the plugin.
	 * @param pluginAddress If plugin is configurable then the pluginAddress is used to bind
	 * @param collector If plugin supports sharing statistics, then the object will be used
	 * @param startTime 
	 * @return Whether plugin is configurable by node
	 * FIXME Figure out other parameters needed
	 */
	public abstract boolean initPlugin(PluginAddress pluginAddress, IOStatisticCollector collector, long startTime);
	
	/**Method to stop a plugin, not terminate it.  
	 * Can be used for temporarily stopping a plugin, or putting it to sleep state.
	 * Don't know if this method is really necessary. 
	 */
	public abstract void stopTransportPlugin();

	/** The PluginAddress the plugin is bound to. */
	public abstract PluginAddress getPluginAddress();
	
	/** If we want to manually set the PluginAddress to bind to. 
	 * @return True if the plugin allows the node to configure it, false if the plugin is automatically configuring itself
	 */
	public abstract boolean setPluginAddress(PluginAddress pluginAddress);
	
	public abstract PluginAddress toPluginAddress(String address);
	
}

interface PluginAddress{
	
	public String toStringAddress();
	
}