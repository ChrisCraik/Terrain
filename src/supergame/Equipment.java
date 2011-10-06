package supergame;

import javax.vecmath.Vector3f;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import supergame.modify.BlockChunkModifier;
import supergame.modify.SphereChunkModifier;

public class Equipment {
	static final int NOTHING = 0;
	static final int TROWEL = 1;
	static final int SHOVEL = 2;
	static final int ROCKET = 3;
	static final int MIN_TARGET = 1;
	static final int MAX_TARGET = 10;

	private int mTool = 0;
	private int mTargetDistance = 5;

	private final Vector3f mTargetPos =  new Vector3f();
	private final Vector3f mTargetVoxelPos = new Vector3f();

	private static int msSinceShoot = 0;

	public void updatePositionLook(Vector3f position, float heading, float pitch) {
		Vec3.HPVector(mTargetPos, 180 - heading, pitch);
		mTargetPos.scale(mTargetDistance);
		mTargetPos.add(position);

		// align the target to a voxel
		mTargetVoxelPos.set(
				(float)Math.floor(mTargetPos.x + 0.5),
				(float)Math.floor(mTargetPos.y + 0.5),
				(float)Math.floor(mTargetPos.z + 0.5));
	}

	public void render() {
		GL11.glPushMatrix();
		GL11.glTranslatef(mTargetPos.x, mTargetPos.y, mTargetPos.z);
		Game.collision.drawCube(0.1f,0.1f,0.1f);
		GL11.glPopMatrix();

		if (mTool == NOTHING)
			return;

		GL11.glPushMatrix();
		GL11.glTranslatef(mTargetVoxelPos.x, mTargetVoxelPos.y, mTargetVoxelPos.z);
		Game.collision.drawCube(1, 0.02f, 0.02f);
		Game.collision.drawCube(0.02f, 1, 0.02f);
		Game.collision.drawCube(0.02f, 0.02f, 1);
		GL11.glPopMatrix();
	}

	public void pollInput() {
		if (Keyboard.isKeyDown(Keyboard.KEY_0)) {
			mTool = 0;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_1)) {
			mTool = 1;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_2)) {
			mTool = 2;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_3)) {
			mTool = 3;
		}

		while (Keyboard.next()) {
			if (Keyboard.getEventKey() == Keyboard.KEY_UP
					&& Keyboard.getEventKeyState()) {
				mTargetDistance = Math.min(mTargetDistance + 1, MAX_TARGET);
			} else if (Keyboard.getEventKey() == Keyboard.KEY_DOWN
					&& Keyboard.getEventKeyState()) {
				mTargetDistance = Math.max(mTargetDistance - 1, MIN_TARGET);
			}
		}

		msSinceShoot += Game.delta;
		if (msSinceShoot > 200 && (Mouse.isButtonDown(0) || Mouse.isButtonDown(1))) {
			msSinceShoot = 0;

			float increment = 0.5f;
			if (Mouse.isButtonDown(1))
				increment *= -1;

			switch (mTool) {
			case(TROWEL) :
				new BlockChunkModifier(mTargetVoxelPos, new Vector3f(0.5f, 0.5f, 0.5f), increment);
				break;

			case(SHOVEL) :
				new SphereChunkModifier(mTargetVoxelPos, 1, Mouse.isButtonDown(1));
				break;

			case(ROCKET) :
				// TODO: fire cube!
				new SphereChunkModifier(mTargetVoxelPos, 3, false);
				break;
			}
		}
	}
}
