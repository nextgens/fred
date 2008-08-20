/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package freenet.client.async;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import com.db4o.ObjectContainer;

import freenet.client.ArchiveContext;
import freenet.client.ClientMetadata;
import freenet.client.FetchContext;
import freenet.client.FetchException;
import freenet.client.FetchResult;
import freenet.client.Metadata;
import freenet.client.MetadataParseException;
import freenet.keys.CHKBlock;
import freenet.keys.ClientCHK;
import freenet.keys.NodeCHK;
import freenet.node.SendableGet;
import freenet.support.BloomFilter;
import freenet.support.Fields;
import freenet.support.Logger;
import freenet.support.OOMHandler;
import freenet.support.api.Bucket;
import freenet.support.compress.CompressionOutputSizeException;
import freenet.support.compress.Compressor;

/**
 * Fetch a splitfile, decompress it if need be, and return it to the GetCompletionCallback.
 * Most of the work is done by the segments, and we do not need a thread.
 */
public class SplitFileFetcher implements ClientGetState, HasKeyListener {

	final FetchContext fetchContext;
	final ArchiveContext archiveContext;
	final ArrayList decompressors;
	final ClientMetadata clientMetadata;
	final ClientRequester parent;
	final GetCompletionCallback cb;
	final int recursionLevel;
	/** The splitfile type. See the SPLITFILE_ constants on Metadata. */
	final short splitfileType;
	/** The segment length. -1 means not segmented and must get everything to decode. */
	final int blocksPerSegment;
	/** The segment length in check blocks. */
	final int checkBlocksPerSegment;
	/** Total number of segments */
	final int segmentCount;
	/** The detailed information on each segment */
	final SplitFileFetcherSegment[] segments;
	/** Maximum temporary length */
	final long maxTempLength;
	/** Have all segments finished? Access synchronized. */
	private boolean allSegmentsFinished;
	/** Override length. If this is positive, truncate the splitfile to this length. */
	private final long overrideLength;
	/** Preferred bucket to return data in */
	private final Bucket returnBucket;
	private boolean finished;
	private long token;
	final boolean persistent;
	private FetchException otherFailure;
	
	// A persistent hashCode is helpful in debugging, and also means we can put
	// these objects into sets etc when we need to.
	
	private final int hashCode;
	
	public int hashCode() {
		return hashCode;
	}
	
	// Bloom filter stuff
	/** The main bloom filter, which includes every key in the segment, is stored
	 * in this file. It is a counting filter and is updated when a key is found. */
	File mainBloomFile;
	/** The per-segment bloom filters are kept in this (slightly larger) file,
	 * appended one after the next. */
	File altBloomFile;
	/** Size of the main Bloom filter in bytes. */
	final int mainBloomFilterSizeBytes;
	/** Default mainBloomElementsPerKey. False positives is approx 
	 * 0.6185^[this number], so 19 gives us 0.01% false positives, which should
	 * be acceptable even if there are thousands of splitfiles on the queue. */
	static final int DEFAULT_MAIN_BLOOM_ELEMENTS_PER_KEY = 19;
	/** Number of hashes for the main filter. */
	final int mainBloomK;
	/** What proportion of false positives is acceptable for the per-segment
	 * Bloom filters? This is divided by the number of segments, so it is (roughly)
	 * an overall probability of any false positive given that we reach the 
	 * per-segment filters. IMHO 1 in 100 is adequate. */
	static final double ACCEPTABLE_BLOOM_FALSE_POSITIVES_ALL_SEGMENTS = 0.01;
	/** Size of per-segment bloom filter in bytes. This is calculated from the
	 * above constant and the number of segments, and rounded up. */
	final int perSegmentBloomFilterSizeBytes;
	/** Number of hashes for the per-segment bloom filters. */
	final int perSegmentK;
	private int keyCount;
	/** Salt used in the secondary Bloom filters if the primary matches. 
	 * The primary Bloom filters use the already-salted saltedKey. */
	private final byte[] localSalt;
	/** Reference set on the first call to makeKeyListener().
	 * NOTE: db4o DOES NOT clear transient variables on deactivation.
	 * So as long as this is paged in (i.e. there is a reference to it, i.e. the
	 * KeyListener), it will remain valid, once it is set by the first call
	 * during resuming. */
	private transient SplitFileFetcherKeyListener tempListener;
	
	public SplitFileFetcher(Metadata metadata, GetCompletionCallback rcb, ClientRequester parent2,
			FetchContext newCtx, ArrayList decompressors2, ClientMetadata clientMetadata, 
			ArchiveContext actx, int recursionLevel, Bucket returnBucket, long token2, ObjectContainer container, ClientContext context) throws FetchException, MetadataParseException {
		this.persistent = parent2.persistent();
		this.hashCode = super.hashCode();
		this.finished = false;
		this.returnBucket = returnBucket;
		this.fetchContext = newCtx;
		this.archiveContext = actx;
		this.decompressors = decompressors2;
		this.clientMetadata = clientMetadata;
		this.cb = rcb;
		this.recursionLevel = recursionLevel + 1;
		this.parent = parent2;
		localSalt = new byte[32];
		context.random.nextBytes(localSalt);
		if(parent2.isCancelled())
			throw new FetchException(FetchException.CANCELLED);
		overrideLength = metadata.dataLength();
		this.splitfileType = metadata.getSplitfileType();
		ClientCHK[] splitfileDataBlocks = metadata.getSplitfileDataKeys();
		ClientCHK[] splitfileCheckBlocks = metadata.getSplitfileCheckKeys();
		for(int i=0;i<splitfileDataBlocks.length;i++)
			if(splitfileDataBlocks[i] == null) throw new MetadataParseException("Null: data block "+i+" of "+splitfileDataBlocks.length);
		for(int i=0;i<splitfileCheckBlocks.length;i++)
			if(splitfileCheckBlocks[i] == null) throw new MetadataParseException("Null: check block "+i+" of "+splitfileCheckBlocks.length);
		long finalLength = 1L * splitfileDataBlocks.length * CHKBlock.DATA_LENGTH;
		if(finalLength > overrideLength) {
			if(finalLength - overrideLength > CHKBlock.DATA_LENGTH)
				throw new FetchException(FetchException.INVALID_METADATA, "Splitfile is "+finalLength+" but length is "+finalLength);
			finalLength = overrideLength;
		}
		long eventualLength = Math.max(overrideLength, metadata.uncompressedDataLength());
		cb.onExpectedSize(eventualLength, container);
		String mimeType = metadata.getMIMEType();
		if(mimeType != null)
			cb.onExpectedMIME(mimeType, container);
		if(metadata.uncompressedDataLength() > 0)
			cb.onFinalizedMetadata(container);
		if(eventualLength > 0 && newCtx.maxOutputLength > 0 && eventualLength > newCtx.maxOutputLength)
			throw new FetchException(FetchException.TOO_BIG, eventualLength, true, clientMetadata.getMIMEType());
		
		if(splitfileType == Metadata.SPLITFILE_NONREDUNDANT) {
			// Don't need to do much - just fetch everything and piece it together.
			blocksPerSegment = -1;
			checkBlocksPerSegment = -1;
			segmentCount = 1;
			if(splitfileCheckBlocks.length > 0) {
				Logger.error(this, "Splitfile type is SPLITFILE_NONREDUNDANT yet "+splitfileCheckBlocks.length+" check blocks found!! : "+this);
				throw new FetchException(FetchException.INVALID_METADATA, "Splitfile type is non-redundant yet have "+splitfileCheckBlocks.length+" check blocks");
			}
		} else if(splitfileType == Metadata.SPLITFILE_ONION_STANDARD) {
			byte[] params = metadata.splitfileParams();
			if((params == null) || (params.length < 8))
				throw new MetadataParseException("No splitfile params");
			blocksPerSegment = Fields.bytesToInt(params, 0);
			int checkBlocks = Fields.bytesToInt(params, 4);
			
			// FIXME remove this eventually. Will break compat with a few files inserted between 1135 and 1136.
			// Work around a bug around build 1135.
			// We were splitting as (128,255), but we were then setting the checkBlocksPerSegment to 64.
			// Detect this.
			if(checkBlocks == 64 && blocksPerSegment == 128 &&
					splitfileCheckBlocks.length == splitfileDataBlocks.length - (splitfileDataBlocks.length / 128)) {
				Logger.normal(this, "Activating 1135 wrong check blocks per segment workaround for "+this);
				checkBlocks = 127;
			}
			checkBlocksPerSegment = checkBlocks;
			
			if((blocksPerSegment > fetchContext.maxDataBlocksPerSegment)
					|| (checkBlocksPerSegment > fetchContext.maxCheckBlocksPerSegment))
				throw new FetchException(FetchException.TOO_MANY_BLOCKS_PER_SEGMENT, "Too many blocks per segment: "+blocksPerSegment+" data, "+checkBlocksPerSegment+" check");
			segmentCount = (splitfileDataBlocks.length / blocksPerSegment) +
				(splitfileDataBlocks.length % blocksPerSegment == 0 ? 0 : 1);
			// Onion, 128/192.
			// Will be segmented.
		} else throw new MetadataParseException("Unknown splitfile format: "+splitfileType);
		this.maxTempLength = fetchContext.maxTempLength;
		if(Logger.shouldLog(Logger.MINOR, this))
			Logger.minor(this, "Algorithm: "+splitfileType+", blocks per segment: "+blocksPerSegment+
					", check blocks per segment: "+checkBlocksPerSegment+", segments: "+segmentCount+
					", data blocks: "+splitfileDataBlocks.length+", check blocks: "+splitfileCheckBlocks.length);
		segments = new SplitFileFetcherSegment[segmentCount]; // initially null on all entries
		if(segmentCount == 1) {
			// splitfile* will be overwritten, this is bad
			// so copy them
			ClientCHK[] newSplitfileDataBlocks = new ClientCHK[splitfileDataBlocks.length];
			ClientCHK[] newSplitfileCheckBlocks = new ClientCHK[splitfileCheckBlocks.length];
			System.arraycopy(splitfileDataBlocks, 0, newSplitfileDataBlocks, 0, splitfileDataBlocks.length);
			if(splitfileCheckBlocks.length > 0)
				System.arraycopy(splitfileCheckBlocks, 0, newSplitfileCheckBlocks, 0, splitfileCheckBlocks.length);
			segments[0] = new SplitFileFetcherSegment(splitfileType, newSplitfileDataBlocks, newSplitfileCheckBlocks, 
					this, archiveContext, fetchContext, maxTempLength, recursionLevel, parent, 0);
			if(persistent) {
				container.set(segments[0]);
			}
		} else {
			int dataBlocksPtr = 0;
			int checkBlocksPtr = 0;
			for(int i=0;i<segments.length;i++) {
				// Create a segment. Give it its keys.
				int copyDataBlocks = Math.min(splitfileDataBlocks.length - dataBlocksPtr, blocksPerSegment);
				int copyCheckBlocks = Math.min(splitfileCheckBlocks.length - checkBlocksPtr, checkBlocksPerSegment);
				ClientCHK[] dataBlocks = new ClientCHK[copyDataBlocks];
				ClientCHK[] checkBlocks = new ClientCHK[copyCheckBlocks];
				if(copyDataBlocks > 0)
					System.arraycopy(splitfileDataBlocks, dataBlocksPtr, dataBlocks, 0, copyDataBlocks);
				if(copyCheckBlocks > 0)
					System.arraycopy(splitfileCheckBlocks, checkBlocksPtr, checkBlocks, 0, copyCheckBlocks);
				dataBlocksPtr += copyDataBlocks;
				checkBlocksPtr += copyCheckBlocks;
				segments[i] = new SplitFileFetcherSegment(splitfileType, dataBlocks, checkBlocks, this, archiveContext, 
						fetchContext, maxTempLength, recursionLevel+1, parent, i);
				if(persistent) {
					container.set(segments[i]);
				}
			}
			if(dataBlocksPtr != splitfileDataBlocks.length)
				throw new FetchException(FetchException.INVALID_METADATA, "Unable to allocate all data blocks to segments - buggy or malicious inserter");
			if(checkBlocksPtr != splitfileCheckBlocks.length)
				throw new FetchException(FetchException.INVALID_METADATA, "Unable to allocate all check blocks to segments - buggy or malicious inserter");
		}
		this.token = token2;
		parent.addBlocks(splitfileDataBlocks.length + splitfileCheckBlocks.length, container);
		parent.addMustSucceedBlocks(splitfileDataBlocks.length, container);
		parent.notifyClients(container, context);
		
		// Setup bloom parameters.
		if(persistent) {
			// FIXME: Should this be encrypted? It's protected to some degree by the salt...
			// Since it isn't encrypted, it's likely to be very sparse; we should name
			// it appropriately...
			try {
				mainBloomFile = context.fg.makeRandomFile();
				altBloomFile = context.fg.makeRandomFile();
			} catch (IOException e) {
				throw new FetchException(FetchException.BUCKET_ERROR, "Unable to create Bloom filter files", e);
			}
		} else {
			// Not persistent, keep purely in RAM.
			mainBloomFile = null;
			altBloomFile = null;
		}
		int mainElementsPerKey = DEFAULT_MAIN_BLOOM_ELEMENTS_PER_KEY;
		int origSize = splitfileDataBlocks.length + splitfileCheckBlocks.length;
		mainBloomK = (int) (mainElementsPerKey * 0.7);
		long elementsLong = origSize * mainElementsPerKey;
		// REDFLAG: SIZE LIMIT: 3.36TB limit!
		if(elementsLong > Integer.MAX_VALUE)
			throw new FetchException(FetchException.TOO_BIG, "Cannot fetch splitfiles with more than "+(Integer.MAX_VALUE/mainElementsPerKey)+" keys! (approx 3.3TB)");
		int mainSizeBits = (int)elementsLong; // counting filter
		if((mainSizeBits & 7) != 0)
			mainSizeBits += (8 - (mainSizeBits & 7));
		mainBloomFilterSizeBytes = mainSizeBits / 8 * 2; // counting filter
		double acceptableFalsePositives = ACCEPTABLE_BLOOM_FALSE_POSITIVES_ALL_SEGMENTS / segments.length;
		int perSegmentBitsPerKey = (int) Math.ceil(Math.log(acceptableFalsePositives) / Math.log(0.6185));
		int segBlocks = blocksPerSegment + checkBlocksPerSegment;
		if(segBlocks < origSize)
			segBlocks = origSize;
		int perSegmentSize = perSegmentBitsPerKey * segBlocks;
		if((perSegmentSize & 7) != 0)
			perSegmentSize += (8 - (perSegmentSize & 7));
		perSegmentBloomFilterSizeBytes = perSegmentSize / 8;
		perSegmentK = BloomFilter.optimialK(perSegmentSize, blocksPerSegment + checkBlocksPerSegment);
		keyCount = origSize;
		// Now create it.
		Logger.error(this, "Creating block filter for "+this+": keys="+(splitfileDataBlocks.length+splitfileCheckBlocks.length)+" main bloom size "+mainBloomFilterSizeBytes+" bytes, K="+mainBloomK+", filename="+mainBloomFile+" alt bloom filter: segments: "+segments.length+" each is "+perSegmentBloomFilterSizeBytes+" bytes k="+perSegmentK);
		try {
			tempListener = new SplitFileFetcherKeyListener(this, keyCount, mainBloomFile, altBloomFile, mainBloomFilterSizeBytes, mainBloomK, !fetchContext.cacheLocalRequests, localSalt, segments.length, perSegmentBloomFilterSizeBytes, perSegmentK, persistent, true);
			
			// Now add the keys
			int dataKeysIndex = 0;
			int checkKeysIndex = 0;
			int segNo = 0;
			while(dataKeysIndex < splitfileDataBlocks.length) {
				int dataKeysEnd = dataKeysIndex + blocksPerSegment;
				int checkKeysEnd = checkKeysIndex + checkBlocksPerSegment;
				dataKeysEnd = Math.min(dataKeysEnd, splitfileDataBlocks.length);
				checkKeysEnd = Math.min(checkKeysEnd, splitfileCheckBlocks.length);
				for(int j=dataKeysIndex;j<dataKeysEnd;j++)
					tempListener.addKey(splitfileDataBlocks[j].getNodeKey(), segNo, context);
				for(int j=checkKeysIndex;j<checkKeysEnd;j++)
					tempListener.addKey(splitfileCheckBlocks[j].getNodeKey(), segNo, context);
				segNo++;
				dataKeysIndex = dataKeysEnd;
				checkKeysIndex = checkKeysEnd;
			}
			tempListener.writeFilters();
		} catch (IOException e) {
			throw new FetchException(FetchException.BUCKET_ERROR, "Unable to write Bloom filters for splitfile");
		}
		if(persistent) {
			for(int i=0;i<segments.length;i++) {
				segments[i].deactivateKeys(container);
			}
		}
	}

	/** Return the final status of the fetch. Throws an exception, or returns a 
	 * Bucket containing the fetched data.
	 * @throws FetchException If the fetch failed for some reason.
	 */
	private Bucket finalStatus(ObjectContainer container, ClientContext context) throws FetchException {
		boolean logMINOR = Logger.shouldLog(Logger.MINOR, this);
		long finalLength = 0;
		for(int i=0;i<segments.length;i++) {
			SplitFileFetcherSegment s = segments[i];
			if(persistent)
				container.activate(s, 1);
			if(!s.succeeded()) {
				throw new IllegalStateException("Not all finished");
			}
			s.throwError();
			// If still here, it succeeded
			finalLength += s.decodedLength();
			if(logMINOR)
				Logger.minor(this, "Segment "+i+" decoded length "+s.decodedLength()+" total length now "+finalLength+" for "+s.dataBuckets.length+" blocks which should be "+(s.dataBuckets.length * NodeCHK.BLOCK_SIZE));
			// Healing is done by Segment
		}
		if(finalLength > overrideLength) {
			if(finalLength - overrideLength > CHKBlock.DATA_LENGTH)
				throw new FetchException(FetchException.INVALID_METADATA, "Splitfile is "+finalLength+" but length is "+finalLength);
			finalLength = overrideLength;
		}
		
		long bytesWritten = 0;
		OutputStream os = null;
		Bucket output;
		if(persistent) {
			container.activate(decompressors, 5);
			if(returnBucket != null)
				container.activate(returnBucket, 5);
		}
		try {
			if((returnBucket != null) && decompressors.isEmpty()) {
				output = returnBucket;
			} else
				output = context.getBucketFactory(parent.persistent()).makeBucket(finalLength);
			os = output.getOutputStream();
			for(int i=0;i<segments.length;i++) {
				SplitFileFetcherSegment s = segments[i];
				long max = (finalLength < 0 ? 0 : (finalLength - bytesWritten));
				bytesWritten += s.writeDecodedDataTo(os, max);
			}
		} catch (IOException e) {
			throw new FetchException(FetchException.BUCKET_ERROR, e);
		} finally {
			if(os != null) {
				try {
					os.close();
				} catch (IOException e) {
					// If it fails to close it may return corrupt data.
					throw new FetchException(FetchException.BUCKET_ERROR, e);
				}
			}
		}
		if(finalLength != output.size()) {
			Logger.error(this, "Final length is supposed to be "+finalLength+" but only written "+output.size());
		}
		return output;
	}

	public void segmentFinished(SplitFileFetcherSegment segment, ObjectContainer container, ClientContext context) {
		if(persistent)
			container.activate(this, 1);
		boolean logMINOR = Logger.shouldLog(Logger.MINOR, this);
		if(logMINOR) Logger.minor(this, "Finished segment: "+segment);
		boolean finish = false;
		synchronized(this) {
			boolean allDone = true;
			for(int i=0;i<segments.length;i++) {
				if(persistent)
					container.activate(segments[i], 1);
				if(!segments[i].succeeded()) {
					if(logMINOR) Logger.minor(this, "Segment "+segments[i]+" is not finished");
					allDone = false;
				}
			}
			if(allDone) {
				if(allSegmentsFinished) {
					Logger.error(this, "Was already finished! (segmentFinished("+segment+ ')', new Exception("debug"));
				} else {
					allSegmentsFinished = true;
					finish = true;
				}
			} else {
				for(int i=0;i<segments.length;i++) {
					if(segments[i] == segment) continue;
					container.deactivate(segments[i], 1);
				}
			}
			notifyAll();
		}
		if(persistent) container.set(this);
		if(finish) finish(container, context);
	}

	private void finish(ObjectContainer container, ClientContext context) {
		if(persistent) {
			container.activate(cb, 1);
		}
		context.getChkFetchScheduler().removePendingKeys(this, true);
		try {
			synchronized(this) {
				if(otherFailure != null) throw otherFailure;
				if(finished) {
					Logger.error(this, "Was already finished");
					return;
				}
				finished = true;
			}
			Bucket data = finalStatus(container, context);
			// Decompress
			if(persistent) {
				container.set(this);
				container.activate(decompressors, 5);
				container.activate(returnBucket, 5);
				container.activate(cb, 1);
				container.activate(fetchContext, 1);
			}
			while(!decompressors.isEmpty()) {
				Compressor c = (Compressor) decompressors.remove(decompressors.size()-1);
				long maxLen = Math.max(fetchContext.maxTempLength, fetchContext.maxOutputLength);
				try {
					Bucket out = returnBucket;
					if(!decompressors.isEmpty()) out = null;
					data = c.decompress(data, context.getBucketFactory(parent.persistent()), maxLen, maxLen * 4, out);
				} catch (IOException e) {
					cb.onFailure(new FetchException(FetchException.BUCKET_ERROR, e), this, container, context);
					return;
				} catch (CompressionOutputSizeException e) {
					if(Logger.shouldLog(Logger.MINOR, this))
						Logger.minor(this, "Too big: maxSize = "+fetchContext.maxOutputLength+" maxTempSize = "+fetchContext.maxTempLength);
					cb.onFailure(new FetchException(FetchException.TOO_BIG, e.estimatedSize, false /* FIXME */, clientMetadata.getMIMEType()), this, container, context);
					return;
				}
			}
			cb.onSuccess(new FetchResult(clientMetadata, data), this, container, context);
		} catch (FetchException e) {
			cb.onFailure(e, this, container, context);
		} catch (OutOfMemoryError e) {
			OOMHandler.handleOOM(e);
			System.err.println("Failing above attempted fetch...");
			cb.onFailure(new FetchException(FetchException.INTERNAL_ERROR, e), this, container, context);
		} catch (Throwable t) {
			Logger.error(this, "Caught "+t, t);
			cb.onFailure(new FetchException(FetchException.INTERNAL_ERROR, t), this, container, context);
		}
	}

	public void schedule(ObjectContainer container, ClientContext context) throws KeyListenerConstructionException {
		if(persistent)
			container.activate(this, 1);
		boolean logMINOR = Logger.shouldLog(Logger.MINOR, this);
		if(logMINOR) Logger.minor(this, "Scheduling "+this);
		SendableGet[] getters = new SendableGet[segments.length];
		for(int i=0;i<segments.length;i++) {
			if(logMINOR)
				Logger.minor(this, "Scheduling segment "+i+" : "+segments[i]);
			if(persistent)
				container.activate(segments[i], 1);
			getters[i] = segments[i].schedule(container, context);
			if(persistent)
				container.deactivate(segments[i], 1);
		}
		BlockSet blocks = fetchContext.blocks;
		context.getChkFetchScheduler().register(this, getters, persistent, true, blocks, false);
	}

	public void cancel(ObjectContainer container, ClientContext context) {
		if(persistent)
			container.activate(this, 1);
		boolean logMINOR = Logger.shouldLog(Logger.MINOR, this);
		for(int i=0;i<segments.length;i++) {
			if(logMINOR)
				Logger.minor(this, "Cancelling segment "+i);
			if(persistent)
				container.activate(segments[i], 1);
			segments[i].cancel(container, context);
		}
	}

	public long getToken() {
		return token;
	}

	/**
	 * Make our SplitFileFetcherKeyListener. Returns the one we created in the
	 * constructor if possible, otherwise makes a new one. We must have already
	 * constructed one at some point, maybe before a restart.
	 * @throws FetchException 
	 */
	public KeyListener makeKeyListener(ObjectContainer container, ClientContext context) throws KeyListenerConstructionException {
		synchronized(this) {
			if(tempListener != null) {
				// Recently constructed
				return tempListener;
			}
			try {
				tempListener =
					new SplitFileFetcherKeyListener(this, keyCount, mainBloomFile, altBloomFile, mainBloomFilterSizeBytes, mainBloomK, !fetchContext.cacheLocalRequests, localSalt, segments.length, perSegmentBloomFilterSizeBytes, perSegmentK, persistent, false);
			} catch (IOException e) {
				Logger.error(this, "Unable to read Bloom filter for "+this+" attempting to reconstruct...");
				mainBloomFile.delete();
				altBloomFile.delete();
				try {
					mainBloomFile = context.fg.makeRandomFile();
					altBloomFile = context.fg.makeRandomFile();
				} catch (IOException e1) {
					throw new KeyListenerConstructionException(new FetchException(FetchException.BUCKET_ERROR, "Unable to create Bloom filter files in reconstruction", e1));
				}

				try {
					tempListener = 
						new SplitFileFetcherKeyListener(this, keyCount, mainBloomFile, altBloomFile, mainBloomFilterSizeBytes, mainBloomK, !fetchContext.cacheLocalRequests, localSalt, segments.length, perSegmentBloomFilterSizeBytes, perSegmentK, persistent, true);
				} catch (IOException e1) {
					throw new KeyListenerConstructionException(new FetchException(FetchException.BUCKET_ERROR, "Unable to reconstruct Bloom filters: "+e1, e1));
				}
			}
			return tempListener;
		}
	}

	public synchronized boolean isCancelled(ObjectContainer container) {
		return finished;
	}

	public SplitFileFetcherSegment getSegment(int i) {
		return segments[i];
	}

	public void removeMyPendingKeys(SplitFileFetcherSegment segment, ObjectContainer container, ClientContext context) {
		keyCount = tempListener.killSegment(segment, container, context);
	}

	void setKeyCount(int keyCount2, ObjectContainer container) {
		this.keyCount = keyCount2;
		if(persistent)
			container.set(this);
	}

	public void onFailed(KeyListenerConstructionException e, ObjectContainer container, ClientContext context) {
		otherFailure = e.getFetchException();
		cancel(container, context);
	}

}
