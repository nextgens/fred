package freenet.pluginmanager;

import freenet.io.comm.PacketSocketHandler;
import freenet.io.comm.Peer;
import freenet.io.comm.Peer.LocalAddressException;

public abstract class PacketTransportPlugin extends TransportPlugin implements PacketSocketHandler{
	
	public PacketTransportPlugin(final String transportName, final boolean opennet, final boolean darknet) {
		super(transportName, opennet, darknet);
	}

	public final TransportType transportType = TransportType.packets;
	
	public abstract void sendPacket(byte[] blockToSend, Peer destination, boolean allowLocalAddresses, boolean isOpennet) throws LocalAddressException;
	
	/**
	 * The major issue is that multiple instances of UDPSocketHandler are created (one for opennet and one for darknet).
	 * But a plugin can choose to work for either one or both modes. However only a single plugin will be available.
	 * Therefore this method should not be used. 
	 * Instead the boolean isOpennet is used to distinguish between the two modes of operation.
	 * I have not changed PacketSocketHandler itself, instead overriding it with darknet mode as default
	 */
	@Override
	public void sendPacket(byte[] blockToSend, Peer destination, boolean allowLocalAddresses) throws LocalAddressException{
		sendPacket(blockToSend, destination, allowLocalAddresses, false);
	}
}
