package supergame;

/*
 * Thread that aids in rendering volume data to polygons
 */

interface ChunkProvider {
	public Chunk getChunkToProcess() throws InterruptedException;
}

public class ChunkBakerThread extends Thread {
	private int id;
	private ChunkProvider chunkProvider;

	ChunkBakerThread(int id, ChunkProvider chunkProvider) {
		this.id = id;
		this.chunkProvider = chunkProvider;
	}

	/*
	 * Take one dirty chunk(for now) and clean it by re-initializing it 
	 */
	public void run() {
		Chunk current;
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		Object buffers = Chunk.parallel_workerBuffersInit();
		while (Game.isRunning()) {
			try {
				current = chunkProvider.getChunkToProcess();
				//System.out.println(id + " took chunk " + current);
				current.parallel_process(buffers);
			} catch (InterruptedException e) {
				System.out.println("interruptedexception ignored");
			} /*catch (Exception e) {
				System.out.println("ERROR: Worker thread experienced exception");
				e.printStackTrace();
				System.exit(1);
			}*/
		}
	}
}
