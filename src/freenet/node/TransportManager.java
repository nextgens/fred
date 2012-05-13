package freenet.node;

import java.util.HashMap;

import freenet.pluginmanager.PacketTransportPlugin;
import freenet.pluginmanager.StreamTransportPlugin;

public class TransportManager {
	
	private HashMap<String, PacketTransportPlugin> packetTransportMap = new HashMap<String, PacketTransportPlugin> ();
	private HashMap<String, StreamTransportPlugin> streamTransportMap = new HashMap<String, StreamTransportPlugin> ();
	
	public TransportManager(){
		
	}
	
	/**The register method will allow a transport to register with the manager, so freenet can use it
	 * @param transportPlugin 
	 */
	public void register(PacketTransportPlugin transportPlugin){
		//Check if the transport was available for at least one mode
		if(transportPlugin.opennet == false && transportPlugin.darknet == false)
			throw new RuntimeException("Transport must support one mode at least");
		//Check for a valid transport name
		if(transportPlugin.transportName.length() < 1)
			throw new RuntimeException("Transport name can't be null");
		//Check if socketMap already has the same transport loaded.
		if(packetTransportMap.containsKey(transportPlugin.transportName))
			throw new RuntimeException("A transport type by the name of " + transportPlugin.transportName + " already exists!");
		
		packetTransportMap.put(transportPlugin.transportName, transportPlugin);
	}
	/**The register method will allow a transport to register with the manager, so freenet can use it
	 * @param transportPlugin 
	 */
	public void register(StreamTransportPlugin transportPlugin){
		//Check if the transport was available for at least one mode
		if(transportPlugin.opennet == false && transportPlugin.darknet == false)
			throw new RuntimeException("Transport must support one mode at least");
		//Check for a valid transport name
		if(transportPlugin.transportName.length() < 1)
			throw new RuntimeException("Transport name can't be null");
		//Check if socketMap already has the same transport loaded.
		if(streamTransportMap.containsKey(transportPlugin.transportName))
			throw new RuntimeException("A transport type by the name of " + transportPlugin.transportName + " already exists!");
		
		streamTransportMap.put(transportPlugin.transportName, transportPlugin);
	}
	
	public HashMap<String, PacketTransportPlugin> getPacketTransportMap(){
		return packetTransportMap;
	}
	
	public HashMap<String, StreamTransportPlugin> getStreamTransportMap(){
		return streamTransportMap;
	}
	
	
}
