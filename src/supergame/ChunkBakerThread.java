package supergame;

import java.util.concurrent.LinkedBlockingQueue;

/*
 * Thread that aids in rendering volume data to polygons
 */
public class ChunkBakerThread extends Thread {
	private int id;
	private LinkedBlockingQueue<Chunk> dirtyChunks;
	private LinkedBlockingQueue<Chunk> cleanChunks;

	ChunkBakerThread(int id, LinkedBlockingQueue<Chunk> dirtyChunks, LinkedBlockingQueue<Chunk> cleanChunks) {
		this.id = id;
		this.dirtyChunks = dirtyChunks;
		this.cleanChunks = cleanChunks;
	}

	/*
	 * Take one dirty chunk(for now) and clean it by re-initializing it 
	 */
	public void run() {
		Chunk current;
		while (Game.isRunning()) {
			try {
				current = dirtyChunks.take();
				//System.out.println(id+"took chunk"+current);
				current.initialize();
				cleanChunks.offer(current);
			} catch (InterruptedException e) {
				System.out.println("interruptedexception ignored");
			}
		}
	}
}
