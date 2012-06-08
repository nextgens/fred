package freenet.node;
/**
 * This class is analogous to the OutgoingPacketMangler.
 * Most of the code for the implementation should be taken from FNPPacketMangler class as we don't want to change
 * the protocol for handshaking and processing. However we need to keep in mind that they use streams and it 
 * should be relatively simpler implementing the JFK protocol.
 * @author chetan
 *
 */
public interface OutgoingStreamMangler {

	/**
	 * Send a handshake, if possible, to the node.
	 * @param pn
	 */
	public void sendHandshake(PeerNode pn, boolean notRegistered);

}
