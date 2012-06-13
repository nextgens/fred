package freenet.node;

import java.util.LinkedList;
import java.util.Vector;

import freenet.io.comm.Peer;
/**
 * Base object for PeerPacketTransport and PeerStreamTransport. This includes common JKF fields and some others.
 * @author chetan
 *
 */
public class PeerTransport {
	
	protected final String transportName;
	
	/*
	 * 
	 */
	protected Peer detectedTransportPeer;
	protected Vector<Peer> nominalTransportPeer = new Vector<Peer> ();
	protected Peer remoteDetectedTransportPeer;
	
	/*
	 * JFK specific fields.
	 */
	protected byte[] jfkKa;
	protected byte[] incommingKey;
	protected byte[] jfkKe;
	protected byte[] outgoingKey;
	protected byte[] jfkMyRef;
	protected byte[] hmacKey;
	protected byte[] ivKey;
	protected byte[] ivNonce;
	protected int ourInitialSeqNum;
	protected int theirInitialSeqNum;
	protected int ourInitialMsgID;
	protected int theirInitialMsgID;
	
	protected long jfkContextLifetime = 0;
	
	/**
	 * For FNP link setup:
	 *  The initiator has to ensure that nonces send back by the
	 *  responder in message2 match what was chosen in message 1
	 */
	protected final LinkedList<byte[]> jfkNoncesSent = new LinkedList<byte[]>();
	
	protected boolean isTransportConnected;
	/** Are we rekeying ? */
	protected boolean isTransportRekeying = false;
	/** Number of handshake attempts since last successful connection or ARK fetch */
	protected int transportHandshakeCount;
	
	/** Transport input */
	protected long totalTransportInputSinceStartup;
	/** Transport output */
	protected long totalTransportOutputSinceStartup;
	
	public PeerTransport(String transportName){
		this.transportName = transportName;
	}

}
