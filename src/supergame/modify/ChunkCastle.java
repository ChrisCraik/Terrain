package supergame.modify;

import javax.vecmath.Vector3f;

public class ChunkCastle {

	/**
	 * Convenience method for creating blocks of different rotations in a loop
	 */
	private static void rotatedBlock(int rotate, boolean flip,
			float widthOffset, float heightOffset, float depthOffset,
			float width, float height, float depth) {
		if (flip)
			widthOffset *= -1;

		if (rotate == 0) {
			new BlockChunkModifier(
					new Vector3f(widthOffset, heightOffset, depthOffset),
					new Vector3f(width, height, depth), 5);
		} else if (rotate == 90) {
			// z <= x, x <= -z
			new BlockChunkModifier(
					new Vector3f(-depthOffset, heightOffset, widthOffset),
					new Vector3f(depth, height, width), 5);
		} else if (rotate == 180) {
			// x <= -x, z <= -z
			new BlockChunkModifier(
					new Vector3f(-widthOffset, heightOffset, -depthOffset),
					new Vector3f(width, height, depth), 5);
		} else if (rotate == 270) {
			// z <= -x, x <= z
			new BlockChunkModifier(
					new Vector3f(depthOffset, heightOffset, -widthOffset),
					new Vector3f(depth, height, width), 5);
		}
	}


	/**
	 * Demo for ChunkModifier
	 */
	public static void create() {
		final int wallDistance = 30;
		final int wallHeight = 15;
		final int wallThickness = 4;
		final int wallRailHeight = 1;
		final int turretHeight = 30;
		final int turretThickness = 10;

		for (int r : new int[] {0,90,180,270}) {
			// wall
			rotatedBlock(r, false, 0, 0, wallDistance,
					wallDistance, wallHeight, wallThickness);

			// wall rails
			rotatedBlock(r, false, 0, wallHeight, wallDistance + wallThickness,
					wallDistance, wallRailHeight, 1);
			rotatedBlock(r, false, 0, wallHeight, wallDistance - wallThickness,
					wallDistance, wallRailHeight, 1);

			// wall bumpies
			for (int i = -wallDistance; i < wallDistance; i+=4) {
				rotatedBlock(r, false, i, wallHeight+wallRailHeight, wallDistance + wallThickness,
						1, 1, 1);
				rotatedBlock(r, false, i, wallHeight+wallRailHeight, wallDistance - wallThickness,
						1, 1, 1);
			}

			// turret
			rotatedBlock(r, false, wallDistance, 0, wallDistance,
					turretThickness, turretHeight, turretThickness);
		}
	}
}
