package supergame;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.lwjgl.Sys;

/*
Serial dict contains all cubes.
Serial LRU containing 

Main thread:

for each of the 3*n^2 chunks that are now in InterestCube:
	if not in dict:
		allocate in dict, initialize state to INITIAL
		insert into unprocessed queue
	else if in LRU:
		remove from LRU(will still be in local area dict)

for each of the 3*n^2 chunks NO LONGER in InterestCube:
	add to LRU (Should not be present)
	
for each chunk in dict:
	if thread.state == RENDERABLE:
		RENDER

while LRU.length > CHUNK_CACHE_SIZE:
	pop
	remove from dict
 */
public class ChunkManager implements ChunkProvider {

	long lastx, lasty, lastz;

	private HashMap<ChunkIndex, Chunk> chunks;
	private LinkedBlockingQueue<Chunk> dirtyChunks;
	private LinkedHashSet<ChunkIndex> chunkCache;
	
	public static long startTime;

	ChunkManager(long x, long y, long z) {
		chunks = new HashMap<ChunkIndex, Chunk>();
		dirtyChunks = new LinkedBlockingQueue<Chunk>();
		chunkCache = new LinkedHashSet<ChunkIndex>();

		lastx = x;
		lasty = y;
		lastz = z;

		System.out.printf("Chunkmanager starting at position %d %d %d\n", x, y, z);

		for (int i = 0; i < Config.WORKER_THREADS; i++)
			new ChunkBakerThread(i, this).start();

		startTime = Sys.getTime();
		
		sweepNearby(x, y, z, 2, false);
		sweepNearby(x, y, z, Config.CHUNK_LOAD_DISTANCE, false);
	}

	private void sweepNearby(long x, long y, long z, int limit, boolean stall) {
		for (int i = -limit; i <= limit; i++)
			for (int j = -limit; j <= limit; j++)
				for (int k = -limit; k <= limit; k++)
					if (stall) {
						//Stall until chunk processed
						Chunk localChunk = chunks.get(new ChunkIndex(x + i, y + j, z + k));
						while (!localChunk.processingIsComplete());
						localChunk.serial_render(null, true, false);
					} else {
						//prioritize the chunk
						prioritizeChunk(x + i, y + j, z + k);
					}
	}

	public Chunk getChunkToProcess() throws InterruptedException {
		if (dirtyChunks.size() == 1)
			System.out.println("\t\t\tCompleted in " + ((Sys.getTime()-startTime)*1.0)/Sys.getTimerResolution());
		return dirtyChunks.take();
	}

	private void prioritizeChunk(long x, long y, long z) {
		ChunkIndex key = new ChunkIndex(x, y, z);
		//System.out.println("Prioritizing chunk " + key.getVec3());
		if (chunks.containsKey(key)) {
			//remove from LRU
			chunkCache.remove(key);
		} else {
			Chunk c = new Chunk(key);
			dirtyChunks.add(c);
			chunks.put(key, c);
		}
	}

	private void deprioritizeChunk(long x, long y, long z) {
		ChunkIndex key = new ChunkIndex(x, y, z);
		//System.out.println("DEPrioritizing chunk "+key.getVec3()+"chunkCache size is "+chunkCache.size());
		//insert to LRU (shouldn't be present)

		Chunk c = chunks.get(key);
		if (c != null) {
			if (!c.processingIsComplete())
				c.cancelParallelProcessing();
			c.serial_clean();
			chunks.remove(key);
		}
		/*
		if (chunkCache.contains(key))
			System.err.println("WARNING: non-local chunk removed from local pool");
		chunkCache.add(key);
		*/
	}

	public void updatePosition(long x, long y, long z) {
		//NOTE: assumes constant CHUNK_LOAD_DISTANCE
		long dx = x - lastx, dy = y - lasty, dz = z - lastz;

		if (dx == 0 && dy == 0 && dz == 0)
			return;

		//prioritize chunks now within range, deprioritize those out of range
		//moving chunks in one dimension means a 2d slice of chunks no longer in range.
		for (int i = -Config.CHUNK_LOAD_DISTANCE; i <= Config.CHUNK_LOAD_DISTANCE; i++)
			for (int j = -Config.CHUNK_LOAD_DISTANCE; j <= Config.CHUNK_LOAD_DISTANCE; j++) {
				if (dx == 1) {
					prioritizeChunk(x + Config.CHUNK_LOAD_DISTANCE, y + i, z + j);
					deprioritizeChunk(x - Config.CHUNK_LOAD_DISTANCE - 1, y + i, z + j);
				} else if (dx == -1) {
					prioritizeChunk(x - Config.CHUNK_LOAD_DISTANCE, y + i, z + j);
					deprioritizeChunk(x + Config.CHUNK_LOAD_DISTANCE + 1, y + i, z + j);
				}

				if (dy == 1) {
					prioritizeChunk(x + i, y + Config.CHUNK_LOAD_DISTANCE, z + j);
					deprioritizeChunk(x + i, y - Config.CHUNK_LOAD_DISTANCE - 1, z + j);
				} else if (dy == -1) {
					prioritizeChunk(x + i, y - Config.CHUNK_LOAD_DISTANCE, z + j);
					deprioritizeChunk(x + i, y + Config.CHUNK_LOAD_DISTANCE + 1, z + j);
				}

				if (dz == 1) {
					prioritizeChunk(x + i, y + j, z + Config.CHUNK_LOAD_DISTANCE);
					deprioritizeChunk(x + i, y + j, z - Config.CHUNK_LOAD_DISTANCE - 1);
				} else if (dz == -1) {
					prioritizeChunk(x + i, y + j, z - Config.CHUNK_LOAD_DISTANCE);
					deprioritizeChunk(x + i, y + j, z + Config.CHUNK_LOAD_DISTANCE + 1);
				}
			}

		lastx = x;
		lasty = y;
		lastz = z;

		Game.PROFILE("pos update");
		
		sweepNearby(x, y, z, 1, true);
		Game.PROFILE("loc chunk stall");

		System.out.println("NOW " + chunks.size() + " CHUNKS EXIST.");
	}

	public void renderChunks(Camera cam) {
		int newRenders = 10; // only render this many NEW chunks
		for (Chunk c : chunks.values())
			if (c.serial_render(cam, newRenders > 0, true))
				newRenders++;
	}
}
