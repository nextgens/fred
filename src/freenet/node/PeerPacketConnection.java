package freenet.node;


import freenet.io.comm.Peer;
import freenet.pluginmanager.PacketTransportPlugin;

/**
 * This object will be used by PeerNode to maintain sessions keys for various transports. 
 * A list of PeerConnection will be maintained by PeerNode to handle multiple transports 
 * @author chetan
 *
 */
public class PeerPacketConnection extends PeerConnection {
	

	/** The transport this connection is using. */
	protected PacketTransportPlugin transportPlugin;
	
	/** Mangler to handle connections for different transports */
	protected OutgoingPacketMangler packetMangler;
	
	protected PacketFormat packetFormat;
	
	
	public PeerPacketConnection(PeerNode pn, PacketTransportPlugin transportPlugin, OutgoingPacketMangler packetMangler, PacketFormat packetFormat, Peer detectedPeer){
		super(transportPlugin.transportName, pn);
		this.transportPlugin = transportPlugin;
		this.packetMangler = packetMangler;
		this.packetFormat = packetFormat;
		this.detectedPeer = detectedPeer;
	}

}
