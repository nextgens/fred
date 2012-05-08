package freenet.node;

import java.util.HashMap;

public class TransportManager {
	
	final boolean isOpennet;
	private HashMap<String, PacketTransportPlugin> packetTransportMap = new HashMap<String, PacketTransportPlugin> ();
	private HashMap<String, StreamTransportPlugin> streamTransportMap = new HashMap<String, StreamTransportPlugin> ();
	
	public TransportManager(final boolean isOpennet){
		this.isOpennet = isOpennet;
	}
	
	/**The register method will allow a transport to register with the manager, so freenet can use it
	 * @param transportPlugin 
	 */
	public void register(PacketTransportPlugin transportPlugin){
		//Check if the transport was for a particular mode and was registering in another mode
		if(isOpennet == true)
			if(transportPlugin.opennet == true)
				throw new RuntimeException("Transport does not support opennet mode");
		else
			if(transportPlugin.darknet == false)
				throw new RuntimeException("Transport does not support darknet mode");
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
		//Check if the transport was for a particular mode and was registering in another mode
		if(isOpennet == true)
			if(transportPlugin.opennet == true)
				throw new RuntimeException("Transport does not support opennet mode");
		else
			if(transportPlugin.darknet == false)
				throw new RuntimeException("Transport does not support darknet mode");
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
