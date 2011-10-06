package supergame;

import javax.vecmath.Vector3f;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import supergame.Camera.CameraControllable;

public class Character implements CameraControllable {

	public final long mCharacterId;
	private float mHeading, mPitch;
	private final Equipment mEquipment = new Equipment();

	// temporary vectors used for intermediate calculations. should not be
	// queried outside of the functions that set them.
	private final Vector3f mPosition = new Vector3f();
	private final Vector3f mForwardDir = new Vector3f();
	private final Vector3f mStrafeDir = new Vector3f();

	Character(long characterId) {
		mCharacterId = characterId;
		mHeading = 0;
		mPitch = 0;
	}

	void render() {
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE,
				Game.makeFB(new float[] { 0.5f, 0.5f, 0.5f, 0.5f }));

		Game.collision.physics.queryCharacterPosition(mCharacterId, mPosition);
		GL11.glPushMatrix();
		GL11.glTranslatef(mPosition.x, mPosition.y, mPosition.z);
		GL11.glRotatef(mHeading, 0, 1, 0);
		GL11.glRotatef(mPitch, 1, 0, 0);
		Game.collision.drawCube(2, 2, 2);
		GL11.glPopMatrix();

		mEquipment.updatePositionLook(mPosition, mHeading, mPitch);
		mEquipment.render();
	}

	void move() {
		Vec3.HPVector(mForwardDir, 180 - mHeading, 0);
		Vec3.HPVector(mStrafeDir, 90 - mHeading, 0);

		mEquipment.pollInput();

		// set walkDirection for character
		Vector3f walkDirection = new Vector3f(0, 0, 0);
		if (Keyboard.isKeyDown(Keyboard.KEY_W))
			walkDirection.add(mForwardDir);
		if (Keyboard.isKeyDown(Keyboard.KEY_S))
			walkDirection.sub(mForwardDir);
		if (Keyboard.isKeyDown(Keyboard.KEY_A))
			walkDirection.add(mStrafeDir);
		if (Keyboard.isKeyDown(Keyboard.KEY_D))
			walkDirection.sub(mStrafeDir);

		if (walkDirection.length() > 1f)
			walkDirection.normalize();
		walkDirection.scale(0.15f);

		boolean applyIfJumping = Config.PLAYER_MIDAIR_CONTROL;
		Game.collision.physics.controlCharacter(mCharacterId, applyIfJumping,
				Keyboard.isKeyDown(Keyboard.KEY_SPACE), walkDirection.x,
				walkDirection.y, walkDirection.z);
	}

	@Override
	public void setHeadingPitch(float heading, float pitch) {
		mHeading = heading;
		mPitch = pitch;
	}

	@Override
	public void getPos(Vector3f pos) {
		Game.collision.physics.queryCharacterPosition(mCharacterId, pos);
	}
}
