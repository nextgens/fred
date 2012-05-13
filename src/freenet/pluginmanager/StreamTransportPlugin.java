package freenet.pluginmanager;

import java.io.InputStream;
import java.io.OutputStream;

import freenet.io.comm.IncomingStreamHandler;

/**
 * 
 * @author chetan
 *
 */
public abstract class StreamTransportPlugin extends TransportPlugin {
	
	public StreamTransportPlugin(boolean opennet, boolean darknet) {
		super(opennet, darknet);
	}
	
	public final TransportType transportType = TransportType.streams;
	
	/**
	 * Method to connect to a peer
	 * @param destination The peer address to connect to
	 * @return A handle that contains the stream objects and more methods as required
	 */
	public abstract PluginStreamHandler connect(PluginAddress destination);
	
	/**
	 * Method to make a stream plugin listen to connections
	 * @param handle Object to pass new connections
	 * @param pluginAddress Address to listen at
	 * @return A handle that can be used to terminate the listener
	 */
	public abstract PluginConnectionListener listen(IncomingStreamHandler handle, PluginAddress pluginAddress);
	
}

/**
 * This object will used by the node to read and write data using streams.
 * The plugin provides these streams and the plugin will take care of parsing the data.
 * @author chetan
 * 
 */
abstract class PluginStreamHandler{
	
	public abstract InputStream getInputStream();
	
	public abstract OutputStream getOutputStream();
	
	public abstract void disconnect();
	
}
/**
 * A handle for the listener object, used by node to terminate.
 * Further methods maybe needed
 * @author chetan
 *
 */
abstract class PluginConnectionListener{
	
	public abstract void close();
	
}

