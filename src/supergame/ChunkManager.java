
package supergame;

import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;

import supergame.modify.ChunkModifier;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

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
    public static long startTime;

    private long mLastX, mLastY, mLastZ;

    private final HashMap<ChunkIndex, Chunk> mChunks;
    private final LinkedBlockingQueue<Chunk> mDirtyChunks;
    private final LinkedHashSet<ChunkIndex> mChunkCache;
    private final int mLoadDistance;

    public ChunkManager(long x, long y, long z, int loadDistance) {
        mChunks = new HashMap<ChunkIndex, Chunk>();
        mDirtyChunks = new LinkedBlockingQueue<Chunk>();
        mChunkCache = new LinkedHashSet<ChunkIndex>();
        mLoadDistance = loadDistance;

        mLastX = x;
        mLastY = y;
        mLastZ = z;

        System.out.printf("Chunkmanager starting at position %d %d %d\n", x, y, z);

        for (int i = 0; i < Config.WORKER_THREADS; i++)
            new ChunkBakerThread(i, this).start();

        startTime = Sys.getTime();

        sweepNearby(x, y, z, 2, false);
        sweepNearby(x, y, z, mLoadDistance, false);
    }

    private void sweepNearby(long x, long y, long z, int limit, boolean stall) {
        for (int i = -limit; i <= limit; i++) {
            for (int j = -limit; j <= limit; j++) {
                for (int k = -limit; k <= limit; k++) {
                    if (stall) {
                        // Stall until chunk processed
                        Chunk localChunk = mChunks.get(new ChunkIndex(x + i, y + j, z + k));
                        while (!localChunk.processingIsComplete())
                            ;
                        localChunk.serial_render(null, true, false);
                    } else {
                        // prioritize the chunk
                        prioritizeChunk(x + i, y + j, z + k);
                    }
                }
            }
        }
    }

    /**
     * Create a chunk at the parameter coordinates if it doesn't exist.
     */
    private void prioritizeChunk(long x, long y, long z) {
        ChunkIndex key = new ChunkIndex(x, y, z);
        // System.out.println("Prioritizing chunk " + key.getVec3());
        if (mChunks.containsKey(key)) {
            // remove from LRU
            mChunkCache.remove(key);
        } else {
            Chunk c = new Chunk(key);
            mDirtyChunks.add(c);
            mChunks.put(key, c);
        }
    }

    /**
     * Delete the chunk at the parameter coordinates, as it's no longer needed.
     * Eventually, this will just cache the chunk and only delete a chunk if the
     * cache is full.
     */
    private void deprioritizeChunk(long x, long y, long z) {
        ChunkIndex key = new ChunkIndex(x, y, z);
        // System.out.println("DEPrioritizing chunk "+key.getVec3()+"chunkCache size is "+chunkCache.size());
        // insert to LRU (shouldn't be present)

        Chunk c = mChunks.get(key);
        if (c != null) {
            if (!c.processingIsComplete())
                c.cancelParallelProcessing();
            c.serial_clean();
            mChunks.remove(key);
        }

        // if (chunkCache.contains(key))
        // System.err.println("WARNING: non-local chunk removed from local pool");
        // chunkCache.add(key);
    }

    public void updateWithPosition(long x, long y, long z) {
        // NOTE: assumes constant CHUNK_LOAD_DISTANCE

        // process modified chunks, swap them into place as needed
        ChunkModifier.step(this);

        long dx = x - mLastX, dy = y - mLastY, dz = z - mLastZ;

        if (dx == 0 && dy == 0 && dz == 0)
            return;

        // prioritize chunks now within range, deprioritize those out of range
        // moving chunks in one dimension means a 2d slice of chunks no longer
        // in range.
        for (int i = -mLoadDistance; i <= mLoadDistance; i++)
            for (int j = -mLoadDistance; j <= mLoadDistance; j++) {
                if (dx == 1) {
                    prioritizeChunk(x + mLoadDistance, y + i, z + j);
                    deprioritizeChunk(x - mLoadDistance - 1, y + i, z + j);
                } else if (dx == -1) {
                    prioritizeChunk(x - mLoadDistance, y + i, z + j);
                    deprioritizeChunk(x + mLoadDistance + 1, y + i, z + j);
                }

                if (dy == 1) {
                    prioritizeChunk(x + i, y + mLoadDistance, z + j);
                    deprioritizeChunk(x + i, y - mLoadDistance - 1, z + j);
                } else if (dy == -1) {
                    prioritizeChunk(x + i, y - mLoadDistance, z + j);
                    deprioritizeChunk(x + i, y + mLoadDistance + 1, z + j);
                }

                if (dz == 1) {
                    prioritizeChunk(x + i, y + j, z + mLoadDistance);
                    deprioritizeChunk(x + i, y + j, z - mLoadDistance - 1);
                } else if (dz == -1) {
                    prioritizeChunk(x + i, y + j, z - mLoadDistance);
                    deprioritizeChunk(x + i, y + j, z + mLoadDistance + 1);
                }
            }

        mLastX = x;
        mLastY = y;
        mLastZ = z;

        Game.PROFILE("pos update");

        sweepNearby(x, y, z, 1, true);
        Game.PROFILE("loc chunk stall");

        System.out.println("NOW " + mChunks.size() + " CHUNKS EXIST.");
    }

    public void renderChunks(Camera cam) {
        if (cam != null)
            GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE,
                    Game.makeFB(new float[] {0.4f, 0.3f, 0.0f, 1}));
        int newRenders = 10; // only render this many NEW chunks
        for (Chunk c : mChunks.values())
            if (c.serial_render(cam, newRenders > 0, true))
                newRenders++;
    }

    @Override
    public Chunk getChunkToProcess() throws InterruptedException {
        if (mDirtyChunks.size() == 1 && startTime != 0) {
            double timeTaken = ((Sys.getTime() - startTime) * 1.0) / Sys.getTimerResolution();
            System.out.println("\t\t\tCompleted in " + timeTaken);
            startTime = 0;
        }
        return mDirtyChunks.take();
    }

    public boolean workRemains() {
        return mDirtyChunks.size() != 0;
    }

    @Override
    public void processChunks(Vector<Chunk> chunksForProcessing) {
        mDirtyChunks.addAll(chunksForProcessing);
    }

    @Override
    public void swapChunks(Vector<Chunk> chunksForSwapping) {
        for (Chunk newChunk : chunksForSwapping) {
            ChunkIndex i = newChunk.getChunkIndex();
            Chunk oldChunk = mChunks.remove(i);

            if (oldChunk == null) {
                System.err.println("swapped out nonexistant chunk...");
                System.exit(1);
            }

            if (!oldChunk.processingIsComplete())
                oldChunk.cancelParallelProcessing();
            oldChunk.serial_clean();

            mChunks.put(i, newChunk);
        }
    }

    @Override
    public Chunk getChunk(ChunkIndex i) {
        return mChunks.get(i);
    }
}
