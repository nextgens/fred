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
		if( (transportPlugin.transportName == null) && (transportPlugin.transportName.length() < 1) )
			throw new RuntimeException("Transport name can't be null");
		
		//Check if socketMap already has the same transport loaded.
		if(packetTransportMap.containsKey(transportPlugin.transportName))
			throw new RuntimeException("A transport type by the name of " + transportPlugin.transportName + " already exists!");
		
		packetTransportMap.put(transportPlugin.transportName, transportPlugin);
		
		//FIXME finish calling nodecrypto
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
		if( (transportPlugin.transportName == null) && (transportPlugin.transportName.length() < 1) )
			throw new RuntimeException("Transport name can't be null");
		
		//Check if socketMap already has the same transport loaded.
		if(streamTransportMap.containsKey(transportPlugin.transportName))
			throw new RuntimeException("A transport type by the name of " + transportPlugin.transportName + " already exists!");
		
		streamTransportMap.put(transportPlugin.transportName, transportPlugin);
		
		//FIXME finish calling nodecrypto
		return true;
	}
	
	public HashMap<String, PacketTransportPlugin> getPacketTransportMap(){
		return packetTransportMap;
	}
	
	public HashMap<String, StreamTransportPlugin> getStreamTransportMap(){
		return streamTransportMap;
	}
	
	
}
