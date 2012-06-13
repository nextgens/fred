package freenet.node;

import java.util.Vector;

import freenet.io.comm.Peer;
/**
 * Base object of PeerPacketConnection and PeerStreamConnection. Fields common to both are present.
 * @author chetan
 *
 */
public class PeerConnection {
	
	protected final String transportName; 
	
	/** Every connection can belong to only one peernode. */
	protected PeerNode pn;
	
	/** The peer it connects to */
	protected Peer detectedPeer;
	
	/** List of keys for every connection. Multiple setups might complete simultaneously.
	 * This will also be used to replace current, previous and unverified to make it more generic.
	 */
	private Vector<SessionKey> keys;
	
	public PeerConnection(String transportName, PeerNode pn){
		this.transportName = transportName;
		this.pn = pn;
	}
	
	public synchronized void addKey(SessionKey key){
		keys.add(key);
	}
	
	public synchronized Vector<SessionKey> getKeys(){
		return keys;
	}
	
	public int getKeysSize(){
		return keys.size();
	}
	
	/*
	 * Compatibility for the old system of session keys
	 */

	public synchronized SessionKey getCurrentKeyTracker() {
		return keys.elementAt(keys.size() - 1);
	}
	
	public synchronized SessionKey getPreviousKeyTracker() {
		return keys.elementAt(keys.size() - 2);
	}
	
	public synchronized SessionKey getUnverifiedKeyTracker() {
		return keys.elementAt(keys.size() - 3);
	}
	
	
}
