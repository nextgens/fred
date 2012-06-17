package freenet.node;

import java.util.HashMap;

import freenet.pluginmanager.PacketTransportPlugin;
import freenet.pluginmanager.StreamTransportPlugin;
/**
 * This class maintains a record of packet transports and stream transports available. 
 * For every mode(opennet, darknet, etc.) a separate manager is created at the node
 * The plugin lets the node know about its presence by registering here with either a packet transport or a stream transport
 * The register method calls the corresponding NodeCrypto object to handle the new transport.
 * In case the NodeCrpyto is non-existent, 
 * then on creation it can get a list of available transports and call the initPlugin method.
 * @author chetan
 *
 */
public class TransportManager {
	
	private final Node node;
	
	/** The mode of operation - opennet, darknet, etc. */
	public enum TransportMode{
		opennet, darknet
	};
	public final TransportMode transportMode;
	
	private HashMap<String, PacketTransportPlugin> packetTransportMap = new HashMap<String, PacketTransportPlugin> ();
	private HashMap<String, StreamTransportPlugin> streamTransportMap = new HashMap<String, StreamTransportPlugin> ();
	
	public TransportManager(Node node, TransportMode transportMode){
		this.node = node;
		this.transportMode = transportMode;
	}
	
	/**The register method will allow a transport to register with the manager, so freenet can use it
	 * @param transportPlugin 
	 * @return <b>true-</b> if the corresponding mode(opennet, darknet) is active. <br />
	 * <b>false-</b> the transport plugin must wait for the initPlugin method to be called explicitly.
	 * The transport may decide to initialise or not to initialise its sockets until the corresponding mode is active
	 */
	public boolean register(PacketTransportPlugin transportPlugin){
		
		//Check for valid mode of operation
		if(transportMode != transportPlugin.transportMode)
			throw new RuntimeException("Wrong mode of operation");
		
		//Check for a valid transport name
		if( (transportPlugin.transportName == null) || (transportPlugin.transportName.length() < 1) )
			throw new RuntimeException("Transport name can't be null");
		
		//Check if socketMap already has the same transport loaded.
		if(packetTransportMap.containsKey(transportPlugin.transportName))
			throw new RuntimeException("A transport type by the name of " + transportPlugin.transportName + " already exists!");
		
		packetTransportMap.put(transportPlugin.transportName, transportPlugin);
		
		if(transportMode == TransportMode.opennet){
			if(node.opennet != null){
				node.opennet.crypto.handleNewTransport(transportPlugin);
				return true;
			}
			else
				return false;
		}
		//Assuming only two modes exist currently. darknet mode is created by default
		node.darknetCrypto.handleNewTransport(transportPlugin);
		return true;
	
	}
	
	/**The register method will allow a transport to register with the manager, so freenet can use it
	 * @param transportPlugin 
	 * @return <b>true</b> if the corresponding mode(opennet, darknet) is active. <br />
	 * <b>false</b> the transport plugin must wait for the initPlugin method to be called explicitly.
	 * The transport may decide to initialise or not to initialise its sockets until the corresponding mode is active
	 */
	public boolean register(StreamTransportPlugin transportPlugin){
		
		//Check for valid mode of operation
		if(transportMode != transportPlugin.transportMode)
			throw new RuntimeException("Wrong mode of operation");
		
		//Check for a valid transport name
		if( (transportPlugin.transportName == null) || (transportPlugin.transportName.length() < 1) )
			throw new RuntimeException("Transport name can't be null");
		
		//Check if socketMap already has the same transport loaded.
		if(streamTransportMap.containsKey(transportPlugin.transportName))
			throw new RuntimeException("A transport type by the name of " + transportPlugin.transportName + " already exists!");
		
		streamTransportMap.put(transportPlugin.transportName, transportPlugin);
		
		if(transportMode == TransportMode.opennet){
			if(node.opennet != null){
				node.opennet.crypto.handleNewTransport(transportPlugin);
				return true;
			}
			else
				return false;
		}
		//Assuming only two modes exist currently. darknet mode is created by default
		node.darknetCrypto.handleNewTransport(transportPlugin);
		return true;
		
	}
	
	//Do not change to public. We might end up allowing plugins to access other plugins.
	
	HashMap<String, PacketTransportPlugin> getPacketTransportMap(){
		return packetTransportMap;
	}
	
	HashMap<String, StreamTransportPlugin> getStreamTransportMap(){
		return streamTransportMap;
	}
	
	/**
	 * This is for transports that are loaded by default. The code is present in the core of fred. For e.g. existing UDPSocketHandler
	 * The advantage is that freenet can load faster for normal users who don't want to use plugins
	 * The visibility of this method is default, package-locale access. We won't allow other classes to add here.
	 * The default plugin is added at the beginning. For UDPSocketHandler it is created in the NodeCrypto object.
	 * @param transportPlugin
	 */
	void addDefaultTransport(PacketTransportPlugin transportPlugin){
		if(transportPlugin.transportName != Node.defaultPacketTransportName)
			throw new RuntimeException("Not the default transport");
		else if(packetTransportMap.containsKey(transportPlugin.transportName))
			throw new RuntimeException("Default transport already added");
		packetTransportMap.put(transportPlugin.transportName, transportPlugin);
	}
	/**
	 * This is for transports that are loaded by default. The code is present in the core of fred. For e.g. existing UDPSockethandler
	 * The advantage is that freenet can load faster for normal users who don't want to use plugins
	 * The visibility of this method is default, package-locale access. We won't allow other classes to add here.
	 * We still don't have a TCP default plugin. But we will need one.
	 * The default plugin is added at the beginning.
	 * @param transportPlugin
	 */
	void addDefaultTransport(StreamTransportPlugin transportPlugin){
		if(transportPlugin.transportName != Node.defaultStreamTransportName)
			throw new RuntimeException("Not the default transport");
		else if(streamTransportMap.containsKey(transportPlugin.transportName))
			throw new RuntimeException("Default transport already added");
		streamTransportMap.put(transportPlugin.transportName, transportPlugin);
	}
	
}
