package freenet.node;

import java.util.LinkedList;
import java.util.Vector;

import freenet.io.comm.Peer;
import freenet.pluginmanager.StreamTransportPlugin;
/**
 * This class will be used to store keys, timing fields, etc. by PeerNode for each transport for handshaking. 
 * Once handshake is completed a PeerStreamConnection object is used to store the session keys.<br><br>
 * 
 * Unlike the PeerPacketTransport this object will be used differently. It will have a StreamMangler and StreamFormat.
 * It will be driven by separate threads and not by PacketSender.
 * 
 * <b>Convention:</b> The "Transport" word is used in fields that are transport specific, and are also present in PeerNode.
 * These fields will allow each Transport to behave differently. The existing fields in PeerNode will be used for 
 * common functionality.
 * The fields without "Transport" in them are those which in the long run must be removed from PeerNode.
 * <br> e.g.: <b>isTransportRekeying</b> is used if the individual transport is rekeying;
 * <b>isRekeying</b> will be used in common to all transports in PeerNode.
 * <br> e.g.: <b>jfkKa</b>, <b>incommingKey</b>, etc. should be transport specific and must be moved out of PeerNode 
 * once existing UDP is fully converted to the new TransportPlugin format.
 * @author chetan
 *
 */
public class PeerStreamTransport {
	
	protected StreamTransportPlugin transportPlugin;
	
	protected OutgoingStreamMangler streamMangler;
	
	/*
	 * 
	 */
	protected Peer detectedTransportPeer;
	protected Vector<Peer> nominalTransportPeer;
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
	
	
	/*
	 * Time related fields
	 */
	/** When did we last send a Stream? */
	protected long timeLastSentTransportStream;
	/** When did we last receive a Stream? */
	protected long timeLastReceivedTransportStream;
	/** When did we last receive a non-auth Stream? */
	protected long timeLastReceivedTransportDataStream;
	/** When did we last receive an ack? */
	protected long timeLastReceivedTransportAck;
	/** When was isConnected() last true? */
	protected long timeLastConnectedTransport;
	/** Time added or restarted (reset on startup unlike peerAddedTime) */
	protected long timeAddedOrRestartedTransport;
	/** Time at which we should send the next handshake request */
	protected long sendTransportHandshakeTime;
	/** The time at which we last completed a connection setup for this transport. */
	protected long transportConnectedTime;
	
	
	protected boolean isTransportConnected;
	/** Are we rekeying ? */
	protected boolean isTransportRekeying = false;
	/** Number of handshake attempts since last successful connection or ARK fetch */
	protected int transportHandshakeCount;
	
	/** Transport input */
	protected long totalTransportInputSinceStartup;
	/** Transport output */
	protected long totalTransportOutputSinceStartup;
	

	

	
	public PeerStreamTransport (StreamTransportPlugin transportPlugin, OutgoingStreamMangler streamMangler){
		this.transportPlugin = transportPlugin;
		this.streamMangler = streamMangler;
	}
	
}