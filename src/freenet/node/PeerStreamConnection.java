package freenet.node;

import java.util.Vector;

import freenet.io.comm.Peer;
import freenet.pluginmanager.TransportPlugin;

public class PeerStreamConnection {
	
	/** Every connection can belong to only one peernode. */
	protected PeerNode pn;
	
	/** The transport this connection is using. */
	protected TransportPlugin transportPlugin;
	
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
