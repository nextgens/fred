package freenet.io.comm;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketOptions;
import java.net.SocketTimeoutException;
import java.util.Random;

import freenet.io.AddressTracker;
import freenet.io.comm.Peer.LocalAddressException;
import freenet.node.Node;
import freenet.node.PrioRunnable;
import freenet.support.Logger;
import freenet.support.io.NativeThread;
import freenet.support.transport.ip.IPUtil;

public class UdpSocketHandler implements PrioRunnable, PacketSocketHandler, PortForwardSensitiveSocketHandler {

	private final DatagramSocket _sock;
	private final InetAddress _bindTo;
	private final AddressTracker tracker;
	private IncomingPacketFilter lowLevelFilter;
	/** RNG for debugging, used with _dropProbability.
	 * NOT CRYPTO SAFE. DO NOT USE FOR THINGS THAT NEED CRYPTO SAFE RNG!
	 */
	private Random dropRandom;
	/** If &gt;0, 1 in _dropProbability chance of dropping a packet; for debugging */
	private int _dropProbability;
	// Icky layer violation, but we need to know the Node to work around the EvilJVMBug.
	private final Node node;
        private static volatile boolean logMINOR;
	private static volatile boolean logDEBUG;
	private boolean _isDone;
	private volatile boolean _active = true;
	private final int listenPort;
	private final String title;
	private boolean _started;
	private long startTime;
	private final IOStatisticCollector collector;

        static {
            Logger.registerClass(UdpSocketHandler.class);
        }

	private void doMagic() {
		try{
		if(_sock.getLocalAddress() instanceof Inet6Address ) {
			Field inner_socket = _sock.getClass().getDeclaredField("impl");
			inner_socket.setAccessible(true);
			DatagramSocketImpl
					dsi =
					(DatagramSocketImpl) inner_socket.get(_sock);
			// see /usr/include/linux/in6.h
			// IPV6_ADDR_PREFERENCES   72
			// IPV6_PREFER_SRC_PUBLIC          0x0002
			// IPV6_PREFER_SRC_HOME            0x0400
			System.out.println(dsi.getOption(SocketOptions.IP_TOS));
			System.out.println(dsi.getOption(72));
			dsi.setOption(new Integer(72), new Integer(0x0002 | 0x0400));
		} else {
			System.out.println(_sock.getLocalAddress().getHostAddress());
		}
		} catch (NoSuchFieldException e) {
			throw new Error(e);
		} catch (IllegalStateException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		}  catch (SocketException e) {
			throw new Error(e);
		}
	}

	public UdpSocketHandler(int listenPort, InetAddress bindto, Node node, long startupTime, String title, IOStatisticCollector collector) throws SocketException {
		this.node = node;
		this.collector = collector;
		this.title = title;
		_bindTo = bindto;
		// Keep the Updater code in, just commented out, for now
		// We may want to be able to do on-line updates.
//		if (Updater.hasResource()) {
//			_sock = (DatagramSocket) Updater.getResource();
//		} else {
		this.listenPort = listenPort;
		_sock = new DatagramSocket(listenPort, bindto);
		int sz = _sock.getReceiveBufferSize();
		if(sz < 65536) {
			_sock.setReceiveBufferSize(65536);
		}
		try {
			// Exit reasonably quickly
			_sock.setReuseAddress(true);
			_sock.setTrafficClass(0x38);
		} catch (SocketException e) {
			throw new Error(e);
		}
//		}
		// Only used for debugging, no need to seed from Yarrow
		dropRandom = node.fastWeakRandom;
		tracker = AddressTracker.create(node.lastBootID, node.runDir(), listenPort);
		tracker.startSend(startupTime);
	}

	/** Must be called, or we will NPE in run() */
	@Override
	public void setLowLevelFilter(IncomingPacketFilter f) {
		lowLevelFilter = f;
	}

	public InetAddress getBindTo() {
		return _bindTo;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public void run() { // Listen for packets
		tracker.startReceive(System.currentTimeMillis());
		try {
			runLoop();
		} catch (Throwable t) {
			// Impossible? It keeps on exiting. We get the below,
			// but not this...
			try {
				System.err.print(t.getClass().getName());
				System.err.println();
			} catch (Throwable tt) {}
			try {
				System.err.print(t.getMessage());
				System.err.println();
			} catch (Throwable tt) {}
			try {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
			} catch (Throwable tt) {}
			try {
				Runtime r = Runtime.getRuntime();
				System.err.print(r.freeMemory());
				System.err.println();
				System.err.print(r.totalMemory());
				System.err.println();
			} catch (Throwable tt) {}
			try {
				t.printStackTrace();
			} catch (Throwable tt) {}
		} finally {
			System.err.println("run() exiting for UdpSocketHandler on port "+_sock.getLocalPort());
			Logger.error(this, "run() exiting for UdpSocketHandler on port "+_sock.getLocalPort());
			synchronized (this) {
				_isDone = true;
				notifyAll();
			}
		}
	}

	private void runLoop() {
		byte[] buf = new byte[MAX_RECEIVE_SIZE];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while (_active) {
			try {
				realRun(packet);
			} catch (Throwable t) {
				System.err.println("Caught "+t);
				t.printStackTrace(System.err);
				Logger.error(this, "Caught " + t, t);
			}
		}
	}

	private void realRun(DatagramPacket packet) {
		// Single receiving thread
		boolean gotPacket = getPacket(packet);
		long now = System.currentTimeMillis();
		if (gotPacket) {
			long startTime = System.currentTimeMillis();
			Peer peer = new Peer(packet.getAddress(), packet.getPort());
			tracker.receivedPacketFrom(peer);
			long endTime = System.currentTimeMillis();
			if(endTime - startTime > 50) {
				if(endTime-startTime > 3000) {
					Logger.error(this, "packet creation took "+(endTime-startTime)+"ms");
				} else {
					if(logMINOR) Logger.minor(this, "packet creation took "+(endTime-startTime)+"ms");
				}
			}
			byte[] data = packet.getData();
			int offset = packet.getOffset();
			int length = packet.getLength();
			try {
				if(logMINOR) Logger.minor(this, "Processing packet of length "+length+" from "+peer);
				startTime = System.currentTimeMillis();
				lowLevelFilter.process(data, offset, length, peer, now);
				endTime = System.currentTimeMillis();
				if(endTime - startTime > 50) {
					if(endTime-startTime > 3000) {
						Logger.error(this, "processing packet took "+(endTime-startTime)+"ms");
					} else {
						if(logMINOR) Logger.minor(this, "processing packet took "+(endTime-startTime)+"ms");
					}
				}
				if(logMINOR) Logger.minor(this,
						"Successfully handled packet length " + length);
			} catch (Throwable t) {
				Logger.error(this, "Caught " + t + " from "
						+ lowLevelFilter, t);
			}
		} else {
			if(logDEBUG) Logger.debug(this, "No packet received");
		}
	}

	private static final int MAX_RECEIVE_SIZE = 1500;

	private boolean getPacket(DatagramPacket packet) {
		try {
			_sock.receive(packet);
			InetAddress address = packet.getAddress();
			boolean isLocal = !IPUtil.isValidAddress(address, false);
			collector.addInfo(address, packet.getPort(),
					getHeadersLength(address) + packet.getLength(), 0, isLocal);
		} catch (SocketTimeoutException e1) {
			return false;
		} catch (IOException e2) {
			if (!_active) { // closed, just return silently
				return false;
			} else {
				throw new RuntimeException(e2);
			}
		}
		if(logMINOR) Logger.minor(this, "Received packet");
		return true;
	}

	/**
	 * Send a block of encoded bytes to a peer. This is called by
	 * send, and by IncomingPacketFilter.processOutgoing(..).
	 * @param blockToSend The data block to send.
	 * @param destination The peer to send it to.
	 */
	@Override
	public void sendPacket(byte[] blockToSend, Peer destination, boolean allowLocalAddresses) throws LocalAddressException {
		assert(blockToSend != null);
		if(!_active) {
			Logger.error(this, "Trying to send packet but no longer active");
			// It is essential that for recording accurate AddressTracker data that we don't send any more
			// packets after shutdown.
			return;
		}
		// there should be no DNS needed here, but go ahead if we can, but complain doing it
		if( destination.getAddress(false, allowLocalAddresses) == null ) {
			Logger.error(this, "Tried sending to destination without pre-looked up IP address(needs a real Peer.getHostname()): null:" + destination.getPort(), new Exception("error"));
			if( destination.getAddress(true, allowLocalAddresses) == null ) {
				Logger.error(this, "Tried sending to bad destination address: null:" + destination.getPort(), new Exception("error"));
				return;
			}
		}
		if (_dropProbability > 0) {
			if (dropRandom.nextInt() % _dropProbability == 0) {
				Logger.normal(this, "DROPPED: " + _sock.getLocalPort() + " -> " + destination.getPort());
				return;
			}
		}
		InetAddress address = destination.getAddress(false, allowLocalAddresses);
		assert(address != null);
		if(address instanceof Inet6Address)
			doMagic();
		int port = destination.getPort();
		DatagramPacket packet = new DatagramPacket(blockToSend, blockToSend.length);
		packet.setAddress(address);
		packet.setPort(port);

		try {
			_sock.send(packet);
			tracker.sentPacketTo(destination);
			boolean isLocal = (!IPUtil.isValidAddress(address, false)) && (IPUtil.isValidAddress(address, true));
			collector.addInfo(address, port, 0, getHeadersLength(address) + blockToSend.length, isLocal);
			if(logMINOR) Logger.minor(this, "Sent packet length "+blockToSend.length+" to "+address+':'+port);
		} catch (IOException e) {
			if(packet.getAddress() instanceof Inet6Address) {
				Logger.normal(this, "Error while sending packet to IPv6 address: "+destination+": "+e);
			} else {
				Logger.error(this, "Error while sending packet to " + destination+": "+e, e);
			}
		}
	}

	// CompuServe use 1400 MTU; AOL claim 1450; DFN@home use 1448.
	// http://info.aol.co.uk/broadband/faqHomeNetworking.adp
	// http://www.compuserve.de/cso/hilfe/linux/hilfekategorien/installation/contentview.jsp?conid=385700
	// http://www.studenten-ins-netz.net/inhalt/service_faq.html
	// officially GRE is 1476 and PPPoE is 1492.
	// unofficially, PPPoE is often 1472 (seen in the wild). Also PPPoATM is sometimes 1472.
	static final int MAX_ALLOWED_MTU = 1280;
	static final int UDPv4_HEADERS_LENGTH = 28;
	static final int UDPv6_HEADERS_LENGTH = 48;
	// conservative estimation when AF is not known
	public static final int UDP_HEADERS_LENGTH = UDPv6_HEADERS_LENGTH;

	static final int MIN_IPv4_MTU = 576;
	static final int MIN_IPv6_MTU = 1280;
	// conservative estimation when AF is not known
	public static final int MIN_MTU = MIN_IPv4_MTU;

	private volatile int maxPacketSize = MAX_ALLOWED_MTU;
	
	/**
	 * @return The maximum packet size supported by this SocketManager, not including transport (UDP/IP) headers.
	 */
	@Override
	public int getMaxPacketSize() {
		return maxPacketSize;
	}

	public int calculateMaxPacketSize() {
		int oldSize = maxPacketSize;
		int newSize = innerCalculateMaxPacketSize();
		maxPacketSize = newSize;
		if(oldSize != newSize)
			System.out.println("Max packet size: "+newSize);
		return maxPacketSize;
	}
	
	/** Recalculate the maximum packet size */
	int innerCalculateMaxPacketSize() { //FIXME: what about passing a peerNode though and doing it on a per-peer basis? How? PMTU would require JNI, although it might be worth it...
		final int minAdvertisedMTU = node.getMinimumMTU();
		return maxPacketSize = Math.min(MAX_ALLOWED_MTU, minAdvertisedMTU) - UDP_HEADERS_LENGTH;
	}

	@Override
	public int getPacketSendThreshold() {
		return getMaxPacketSize() - 100;
	}

	public void start() {
		if(!_active) return;
		synchronized(this) {
			_started = true;
			startTime = System.currentTimeMillis();
		}
		node.executor.execute(this, "UdpSocketHandler for port "+listenPort);
	}

	public void close() {
		Logger.normal(this, "Closing.", new Exception("error"));
		synchronized (this) {
			_active = false;
			_sock.close();

			if(!_started) return;
			while (!_isDone) {
				try {
					wait(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		tracker.storeData(node.bootID, node.runDir(), listenPort);
	}

	public int getDropProbability() {
		return _dropProbability;
	}

	public void setDropProbability(int dropProbability) {
		_dropProbability = dropProbability;
	}

	public int getPortNumber() {
		return _sock.getLocalPort();
	}

	@Override
	public String toString() {
		return _sock.getLocalAddress() + ":" + _sock.getLocalPort();
	}

	@Override
	public int getHeadersLength() {
		return UDP_HEADERS_LENGTH;
	}

	@Override
	public int getHeadersLength(Peer peer) {
		return getHeadersLength(peer.getAddress(false));
	}

	int getHeadersLength(InetAddress addr) {
		return addr == null || addr instanceof Inet6Address ? UDPv6_HEADERS_LENGTH : UDPv4_HEADERS_LENGTH;
	}

	public AddressTracker getAddressTracker() {
		return tracker;
	}

	@Override
	public void rescanPortForward() {
		tracker.rescan();
	}

	@Override
	public AddressTracker.Status getDetectedConnectivityStatus() {
		return tracker.getPortForwardStatus();
	}

	@Override
	public int getPriority() {
		return NativeThread.MAX_PRIORITY;
	}

	public long getStartTime() {
		return startTime;
	}

}
