package supergame;

import javax.vecmath.Vector3f;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import supergame.Camera.CameraControllable;

public class Character implements CameraControllable {

	public long mCharacterId;
	private float mHeading, mPitch;
	private Vector3f mLookPos;
	private final Equipment mEquipment = new Equipment();

	Character(long characterId) {
		mCharacterId = characterId;
		mHeading = 0;
		mPitch = 0;
	}

	private static Vector3f position = new Vector3f();

	void render() {
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE,
				Game.makeFB(new float[] { 0.5f, 0.5f, 0.5f, 0.5f }));

		Game.collision.physics.queryCharacterPosition(mCharacterId, position);
		GL11.glPushMatrix();
		GL11.glTranslatef(position.x, position.y, position.z);
		GL11.glRotatef(mHeading, 0, 1, 0);
		GL11.glRotatef(mPitch, 1, 0, 0);
		Game.collision.drawCube(new Vector3f(2, 2, 2));
		GL11.glPopMatrix();

		mEquipment.updatePositionLook(position, mHeading, mPitch);
		mEquipment.render();
	}

	void move() {
		Vector3f forwardDir = Vec3.HPVector(180 - mHeading, 0);
		Vector3f strafeDir = Vec3.HPVector(-mHeading + 90, 0);

		mEquipment.pollInput();

		// set walkDirection for character
		Vector3f walkDirection = new Vector3f(0, 0, 0);
		if (Keyboard.isKeyDown(Keyboard.KEY_W))
			walkDirection.add(forwardDir);
		if (Keyboard.isKeyDown(Keyboard.KEY_S))
			walkDirection.sub(forwardDir);
		if (Keyboard.isKeyDown(Keyboard.KEY_A))
			walkDirection.add(strafeDir);
		if (Keyboard.isKeyDown(Keyboard.KEY_D))
			walkDirection.sub(strafeDir);

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
