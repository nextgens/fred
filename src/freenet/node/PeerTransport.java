package freenet.node;

import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Vector;

import freenet.io.comm.Peer;
import freenet.io.comm.PeerParseException;
import freenet.support.Logger;
import freenet.support.transport.ip.HostnameSyntaxException;
/**
 * Base object for PeerPacketTransport and PeerStreamTransport. This includes common JKF fields and some others.<br><br>
 * 
 *  * <b>Convention:</b> The "Transport" word is used in fields that are transport specific, and are also present in PeerNode.
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
public class PeerTransport {
	
	protected final String transportName;
	
	/** We need the PeerNode as the PeerTransport is PeerNode specific, while TransportBundle is not */
	protected final PeerNode pn;
	
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
	
	public PeerTransport(String transportName, PeerNode pn){
		this.transportName = transportName;
		this.pn = pn;
	}
	
	/**
	 * Set the transport Peer from a given String array.
	 * @param physical
	 * @param fromLocal
	 */
	public void setTransportPeer(String[] physical, boolean fromLocal, boolean checkHostnameOrIPSyntax){
		for(String physicalPeer:physical){
			Peer p = getTransportPeer(physicalPeer, fromLocal, checkHostnameOrIPSyntax);
			if((p != null) && (!nominalTransportPeer.contains(p)))
				nominalTransportPeer.addElement(p);
		}
		if(nominalTransportPeer.isEmpty()){
			Logger.normal(this, "No IP addresses found for identity '" + pn.identityAsBase64String + "', possibly at location '" + Double.toString(pn.getLocation()) + ": " + pn.userToString());
			detectedTransportPeer = null;
		} else {
			detectedTransportPeer = nominalTransportPeer.firstElement();
		}
		
	}
	
	/**
	 * Set the detected peer directly from previous detected IP address.
	 */
	public void setMetadataTransportPeer(String physicalPeer, boolean fromLocal, boolean checkHostnameOrIPSyntax){
		Peer p = getTransportPeer(physicalPeer, fromLocal, checkHostnameOrIPSyntax);
		if(p != null)
			detectedTransportPeer = p;
	}
	
	private Peer getTransportPeer(String physicalPeer, boolean fromLocal, boolean checkHostnameOrIPSyntax){
		Peer p;
		try{
			p = new Peer(physicalPeer, true, checkHostnameOrIPSyntax);
		} catch(HostnameSyntaxException e){
			if(fromLocal)
				Logger.error(this, "Invalid hostname or IP Address syntax error while parsing peer reference in local peers list: " + physicalPeer);
			System.err.println("Invalid hostname or IP Address syntax error while parsing peer reference: " + physicalPeer);
			return null;
		} catch (PeerParseException e){
			if(fromLocal)
				Logger.error(this, "Invalid hostname or IP Address syntax error while parsing peer reference in local peers list: " + physicalPeer);
			System.err.println("Invalid hostname or IP Address syntax error while parsing peer reference: " + physicalPeer);
			return null;
		} catch (UnknownHostException e){
			if(fromLocal)
				Logger.error(this, "Invalid hostname or IP Address syntax error while parsing peer reference in local peers list: " + physicalPeer);
			System.err.println("Invalid hostname or IP Address syntax error while parsing peer reference: " + physicalPeer);
			return null;
		}
		return p;
	}

}
