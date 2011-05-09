package supergame;

import java.util.LinkedList;

/*
 * Thread that aids in rendering volume data to polygons
 */
public class ChunkBakerThread extends Thread {
	private int id;
	private LinkedList<Chunk> dirtyChunks;
	private LinkedList<Chunk> cleanChunks;

	ChunkBakerThread(int id, LinkedList<Chunk> dirtyChunks, LinkedList<Chunk> cleanChunks) {
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
			synchronized (dirtyChunks) {
				//System.out.println(id + "trying to poll, count=" + dirtyChunks.size());
				while (dirtyChunks.isEmpty()) {
					try {
						dirtyChunks.wait();
					} catch (InterruptedException ignored) {
						System.out.println("exception ignored");
					}
				}
				current = dirtyChunks.removeFirst();
			}
			//System.out.println(id + "got one!");
			current.initialize();
			synchronized (cleanChunks) {
				cleanChunks.add(current);
			}
		}
	}
}
