package freenet.node;

import freenet.io.comm.PacketSocketHandler;

public abstract class PacketTransportPlugin extends TransportPlugin implements PacketSocketHandler{
	
	public final TransportType transportType = TransportType.packets;
	
}
