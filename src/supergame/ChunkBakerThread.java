package supergame;

import java.util.PriorityQueue;
import java.util.Vector;


/*
 * Thread that aids in rendering volume data to polygons
 */
public class ChunkBakerThread implements Runnable {
	private PriorityQueue<Chunk> dirtyChunks;
	private Vector<Chunk> cleanChunks;
	
	ChunkBakerThread(PriorityQueue<Chunk> dirtyChunks, Vector<Chunk> cleanChunks) {
		this.dirtyChunks = dirtyChunks;
		this.cleanChunks = cleanChunks;
	}
	
	/*
	 * Take one dirty chunk(for now) and clean it by re-initializing it 
	 */
	public void run() {
		Chunk c = dirtyChunks.poll();
		if (c != null) {
			c.initialize();
			cleanChunks.add(c);
		}
	}
}
