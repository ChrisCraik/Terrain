package supergame;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import org.lwjgl.Sys;

import supergame.modify.ChunkModifier;

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

/**
 * The ChunkManager holds all of the chunks in the engine and serves two main
 * purposes
 *
 * 1 - To generate chunks surrounding the player, so that a player always has
 * terrain around them. This is managed through a HashMap of ChunkIndex to
 * chunk, where every time the player moves from one chunk to another, the chunk
 * manager creates any chunks now within CHUNK_LOAD_DISTANCE of the player
 *
 * 2 - To generate chunk replacements as the terrain is modified - from
 * building, digging, explosions or otherwise. This is done my creating a
 * ChunkModifier for each modification, and calling the static step method each
 * frame. Chunks being modified (as opposed to those just being loaded) are not
 * immediately added to the global pool of chunks, but instead swapped with the
 * chunks they replace atomically, once the generation of modified chunks is
 * complete.
 */
public class ChunkManager implements ChunkProvider, ChunkProcessor {

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

	/**
	 * Create a chunk at the parameter coordinates if it doesn't exist.
	 */
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

	/**
	 * Delete the chunk at the parameter coordinates, as it's no longer needed.
	 *
	 * Eventually, this will just cache the chunk and only delete a chunk if the cache is full.
	 */
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

	public void updateWithPosition(long x, long y, long z) {
		//NOTE: assumes constant CHUNK_LOAD_DISTANCE

		// process modified chunks, swap them into place as needed
		ChunkModifier.step(this);

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

	@Override
	public Chunk getChunkToProcess() throws InterruptedException {
		if (dirtyChunks.size() == 1)
			System.out.println("\t\t\tCompleted in " + ((Sys.getTime()-startTime)*1.0)/Sys.getTimerResolution());
		return dirtyChunks.take();
	}

	@Override
	public void processChunks(Vector<Chunk> chunksForProcessing) {
		dirtyChunks.addAll(chunksForProcessing);
	}

	@Override
	public void swapChunks(Vector<Chunk> chunksForSwapping) {
		for (Chunk newChunk : chunksForSwapping) {
			ChunkIndex i = newChunk.getChunkIndex();
			Chunk oldChunk = chunks.remove(i);

			if (oldChunk == null) {
				System.err.println("swapped out nonexistant chunk...");
				System.exit(1);
			}

			if (!oldChunk.processingIsComplete())
				oldChunk.cancelParallelProcessing();
			oldChunk.serial_clean();

			chunks.put(i, newChunk);
		}
	}

	@Override
	public Chunk getChunk(ChunkIndex i) {
		return chunks.get(i);
	}
}
