package freenet.node;


import java.util.Vector;

import freenet.io.comm.Peer;
import freenet.pluginmanager.TransportPlugin;
/**
 * This object will be used by PeerNode to maintain sessions keys for various transports. 
 * A list of PeerConnection will be maintained by PeerNode to handle multiple transports 
 * @author chetan
 *
 */
public class PeerPacketConnection {
	
	/** Every connection can belong to only one peernode. */
	protected PeerNode pn;
	/** The transport this connection is using. */
	protected TransportPlugin transportPlugin;
	
	/** Mangler to handle connections for different transports */
	protected OutgoingPacketMangler packetMangler;
	
	protected PacketFormat packetFormat;
	
	/** The peer it connects to */
	protected Peer detectedPeer;
	
	/** List of keys for every connection. Multiple setups might complete simultaneously.
	 * This will also be used to replace current, previous and unverified to make it more generic
	 */
	protected Vector<SessionKey> keys;
	
	PeerPacketConnection(PeerNode pn, TransportPlugin transportPlugin, OutgoingPacketMangler packetMangler, PacketFormat packetFormat, Peer detectedPeer){
		this.pn = pn;
		this.transportPlugin = transportPlugin;
		this.packetMangler = packetMangler;
		this.packetFormat = packetFormat;
		this.detectedPeer = detectedPeer;
	}
	
	public void addKey(SessionKey key){
		keys.add(key);
	}
	
	public Vector<SessionKey> getKeys(){
		return keys;
	}
	
}
