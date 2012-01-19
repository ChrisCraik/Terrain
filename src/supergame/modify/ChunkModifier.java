
package supergame.modify;

import supergame.Chunk;
import supergame.ChunkIndex;
import supergame.ChunkProcessor;
import supergame.Config;

import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class used for processing of terrain modifications. the engine's ChunkManager
 * requires only
 */
public abstract class ChunkModifier implements ChunkModifierInterface {
    private static final LinkedList<ChunkModifier> sChangeList = new LinkedList<ChunkModifier>();

    private static boolean sServerMode = false;
    private static final LinkedList<Chunk> sChunksModifiedByServer = new LinkedList<Chunk>();

    public static void setServerMode(boolean serverMode, ChunkProcessor cp) {
        while (!sChangeList.isEmpty()) {
            System.err.println("trying to step...");
            step(cp);
        }
        sServerMode = serverMode;
    }

    public static LinkedList<Chunk> getServerModified() {
        return sChunksModifiedByServer;
    }

    private AtomicInteger mDirtyCount = null;
    private Vector<Chunk> mChunkList = null;
    private State mState = State.CREATED;

    private enum State {
        CREATED, STARTED, FINISHED
    };

    /**
     * Tell the chunk modifier system to process modifications and kick off new
     * chunk generation.
     *
     * It returns two things: 1) new replacement chunks that need to start
     * processing 2) completed replacement chunks that should be swapped in
     * atomically
     *
     * TODO: Multiple independent modifiers processing in parallel
     */
    public static void step(ChunkProcessor cp) {
        ChunkModifier currentModifier = sChangeList.peekFirst();

        // no modifications, quick return
        if (currentModifier != null) {
            // start chunk processing, if needed
            currentModifier.tryStart(cp);

            // if modifications are complete, swap them in and remove the
            // modification
            if (currentModifier.tryFinish(cp))
                sChangeList.removeFirst();
        }
    }

    public ChunkModifier() {
        sChangeList.addLast(this);
    }

    /**
     * Kick off re-generation of all modified chunks through the supplied
     * ChunkProcessor if generation had not already started.
     */
    final public void tryStart(ChunkProcessor cp) {
        if (mState != State.CREATED)
            return;

        mState = State.STARTED;

        Vector<ChunkIndex> indexList = getIndexList();
        mChunkList = new Vector<Chunk>(indexList.size());

        for (ChunkIndex index : indexList) {
            mChunkList.add(new Chunk(index, cp.getChunk(index), this));
        }

        mDirtyCount = new AtomicInteger(mChunkList.size());
        cp.processChunks(mChunkList);
    }

    /**
     * If the ChunkModifier has completed, swap it out through the supplied
     * ChunkProcessor and return true.
     *
     * @param cp ChunkProcessor used to swap new chunks with old ones
     * @return true if the ChunkModifier has completed chunk generation
     */
    final public boolean tryFinish(ChunkProcessor cp) {
        if (mState != State.FINISHED)
            return false;

        cp.swapChunks(mChunkList);
        return true;
    }

    /**
     * Calculate a list of chunk coordinates that will need to be regenerated in
     * applying this modification
     *
     * @return vector of chunk indices affected by the modification
     */
    protected abstract Vector<ChunkIndex> getIndexList();

    /**
     * Helper method for getIndexList, when a simple boundingBox will suffice
     *
     * @return vector of chunk indices containing the bounding box
     */
    protected static Vector<ChunkIndex> getBoundingIndexList(float xPos,
            float yPos, float zPos, float xSize, float ySize, float zSize) {
        Vector<ChunkIndex> list = new Vector<ChunkIndex>();
        long minX = (long) Math.floor((xPos - xSize - 1) / Config.CHUNK_DIVISION);
        long maxX = (long) Math.floor((xPos + xSize + 1) / Config.CHUNK_DIVISION);
        long minY = (long) Math.floor((yPos - ySize - 1) / Config.CHUNK_DIVISION);
        long maxY = (long) Math.floor((yPos + ySize + 1) / Config.CHUNK_DIVISION);
        long minZ = (long) Math.floor((zPos - zSize - 1) / Config.CHUNK_DIVISION);
        long maxZ = (long) Math.floor((zPos + zSize + 1) / Config.CHUNK_DIVISION);

        for (long x = minX; x <= maxX; x++)
            for (long y = minY; y <= maxY; y++)
                for (long z = minZ; z <= maxZ; z++)
                    list.add(new ChunkIndex(x, y, z));

        return list;
    }

    // Methods for worker threads

    /**
     * Called by a modified chunk that has completed processing. Once all chunks
     * to be modified by a ChunkModification have called this, the next call to
     * ChunkModifier.step() will swap in the new chunks.
     */
    @Override
    public void chunkCompletion(Chunk c) {
        if (mDirtyCount.decrementAndGet() == 0) {
            mState = State.FINISHED;
        }

        if (sServerMode) {
            sChunksModifiedByServer.addLast(c);
        }
    }
}
