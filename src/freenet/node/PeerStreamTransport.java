package freenet.node;

import java.util.Vector;

import freenet.io.comm.Peer;
import freenet.pluginmanager.StreamTransportPlugin;

public class PeerStreamTransport {
	
	protected StreamTransportPlugin transportPlugin;
	
	protected OutgoingPacketMangler packetMangler;
	
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
	
	protected boolean isConnected;
	
	protected Peer detectedPeer;
	protected Vector<Peer> nominalPeer;
	protected Peer remoteDetectedPeer;
	
	public PeerStreamTransport (StreamTransportPlugin transportPlugin, OutgoingPacketMangler packetMangler){
		this.transportPlugin = transportPlugin;
		this.packetMangler = packetMangler;
	}
	
}