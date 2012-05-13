package freenet.pluginmanager;

import freenet.io.comm.PacketSocketHandler;

public abstract class PacketTransportPlugin extends TransportPlugin implements PacketSocketHandler{
	
	public PacketTransportPlugin(boolean opennet, boolean darknet) {
		super(opennet, darknet);
	}

	public final TransportType transportType = TransportType.packets;
	
}
