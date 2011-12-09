package supergame.test;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.lwjgl.Sys;

import supergame.ChunkManager;
import supergame.Collision;
import supergame.Game;

public class ChunkGenTest {
	private static double MAX_TIME = 20;

	@Test
	public void chunkGen() {
		Game.collision = new Collision();
		ChunkManager chunkManager = new ChunkManager(0, 0, 0, 6);
		long start = Sys.getTime();
		double secondsTaken;
		do {
			chunkManager.renderChunks(null);
			secondsTaken = ((Sys.getTime()-start)*1.0)/Sys.getTimerResolution();
		} while (chunkManager.workRemains() && secondsTaken < MAX_TIME);

		System.err.println("Chunk generation took " + secondsTaken + " seconds");
		if (secondsTaken >= MAX_TIME) {
			fail("Chunk generation was too slow");
		}
	}
}
