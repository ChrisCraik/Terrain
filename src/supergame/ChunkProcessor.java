package supergame;

import java.util.Vector;

public interface ChunkProcessor {
	public void processChunks(Vector<Chunk> chunksForProcessing);
	public Chunk getChunk(ChunkIndex i);
	public void swapChunks(Vector<Chunk> chunksForSwapping);
}