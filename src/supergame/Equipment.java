package supergame;

import javax.vecmath.Vector3f;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import supergame.modify.BlockChunkModifier;

public class Equipment {
	static final int TROWEL = 1;
	static final int SHOVEL = 2;
	static final int ROCKET = 3;
	static final int MIN_TARGET = 1;
	static final int MAX_TARGET = 10;

	private int mTool = 1;
	private final int mTargetDistance = 5;

	private Vector3f mTargetPos;

	private static int msSinceShoot = 0;

	public void updatePositionLook(Vector3f position, float heading, float pitch) {
		mTargetPos = Vec3.HPVector(180 - heading, pitch);
		mTargetPos.scale(mTargetDistance);
		mTargetPos.add(position);

		// align the target to a voxel
		mTargetPos.set(
				(float)Math.floor(mTargetPos.x + 0.5),
				(float)Math.floor(mTargetPos.y + 0.5),
				(float)Math.floor(mTargetPos.z + 0.5));
	}

	public void render() {
		GL11.glPushMatrix();
		GL11.glTranslatef(mTargetPos.x, mTargetPos.y, mTargetPos.z);
		Game.collision.drawCube(new Vector3f(0.1f,0.1f,0.1f));
		GL11.glPopMatrix();
	}

	public void pollInput() {
		if (Keyboard.isKeyDown(Keyboard.KEY_1))
			mTool = 1;
		if (Keyboard.isKeyDown(Keyboard.KEY_2))
			mTool = 2;

		msSinceShoot += Game.delta;
		if (msSinceShoot > 200 && (Mouse.isButtonDown(0) || Mouse.isButtonDown(1))) {
			msSinceShoot = 0;

			float increment = 0.5f;
			if (Mouse.isButtonDown(1))
				increment *= -1;

			switch (mTool) {
			case(TROWEL) :
				new BlockChunkModifier(mTargetPos, new Vector3f(0.5f, 0.5f, 0.5f), increment);
				break;

			case(SHOVEL) :
				new BlockChunkModifier(mTargetPos, new Vector3f(1, 1, 1), increment);
				break;

			case(ROCKET) :
				// TODO: fire cube!
				break;
			}
		}
	}
}
