package freenet.node;

import java.util.Vector;

import freenet.io.comm.Peer;
import freenet.pluginmanager.StreamTransportPlugin;

public class PeerStreamConnection {
	
	/** Every connection can belong to only one peernode. */
	protected PeerNode pn;
	
	/** The transport this connection is using. */
	protected StreamTransportPlugin transportPlugin;
	
	/** Mangler to handle connections for different transports */
	protected OutgoingStreamMangler streamMangler;
	
	/** The object that runs this connection. Analogous to NewPacketFormat and PacketSender */
	protected StreamConnectionFormat streamConnection;
	
	/** The peer it connects to */
	protected Peer detectedPeer;
	
	/** List of keys for every connection. Multiple setups might complete simultaneously.
	 * This will also be used to replace current, previous and unverified to make it more generic
	 */
	protected Vector<SessionKey> keys;
	
	public void addKey(SessionKey key){
		keys.add(key);
	}
	
	public Vector<SessionKey> getKeys(){
		return keys;
	}
}
