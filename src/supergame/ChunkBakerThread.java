/*
 * Thread that aids in rendering volume data to polygons
 */

package supergame;

interface ChunkProvider {
    public Chunk getChunkToProcess() throws InterruptedException;
}

public class ChunkBakerThread extends Thread {
    @SuppressWarnings("unused")
    private final int mId;
    private final ChunkProvider mChunkProvider;

    ChunkBakerThread(int id, ChunkProvider chunkProvider) {
        mId = id;
        mChunkProvider = chunkProvider;
    }

    /*
     * Take one dirty chunk and (re-)initialize it so that it can be rendered,
     * and collided
     */
    @Override
    public void run() {
        Chunk current;
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Object buffers = Chunk.parallel_workerBuffersInit();
        while (Game.isRunning()) {
            try {
                current = mChunkProvider.getChunkToProcess();
                // System.out.println(id + " took chunk " + current);
                current.parallel_process(buffers);
            } catch (InterruptedException e) {
                System.out.println("interruptedexception ignored");
            } catch (Exception e) {
                System.out.println("ERROR: Worker thread experienced exception");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
