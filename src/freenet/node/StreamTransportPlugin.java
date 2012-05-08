package freenet.node;

import java.io.InputStream;
import java.io.OutputStream;

import freenet.io.comm.Peer;

public abstract class StreamTransportPlugin extends TransportPlugin {
	
	public final TransportType transportType = TransportType.streams;
	
	/**Allow the node to install the streams, so that the plugin can read or write. 
	 * For every connection to a new peer, a new pair of streams must be installed
	 * @param destination The peer we are connected to
	 * @param nodeInputStream
	 * @param nodeOutputStream
	 */
	public abstract void installNodeStream(Peer destination, InputStream nodeInputStream, OutputStream nodeOutputStream);
	
	public abstract void checkConnection(Peer peer);
	
	public abstract void closeConnection(Peer disconnectPeer);
	
}
