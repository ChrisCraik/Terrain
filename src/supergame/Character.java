package supergame;

import javax.vecmath.Vector3f;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import supergame.Camera.CameraControllable;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.character.KinematicCharacterController;
import com.bulletphysics.linearmath.Transform;

public class Character implements CameraControllable {

	public KinematicCharacterController character;
	public PairCachingGhostObject ghostObject;
	private float heading, pitch;

	Character(DiscreteDynamicsWorld dynamicsWorld, Vector3f startPos) {
		Transform startTransform = new Transform();
		startTransform.setIdentity();
		startTransform.origin.set(startPos);

		ghostObject = new PairCachingGhostObject();
		ghostObject.setWorldTransform(startTransform);
		ConvexShape ghostShape = new CapsuleShape(1f, 1.5f);
		ghostObject.setCollisionShape(ghostShape);
		ghostObject.setCollisionFlags(CollisionFlags.CHARACTER_OBJECT);

		float stepHeight = 0.5f;
		character = new KinematicCharacterController(ghostObject, ghostShape, stepHeight);
		character.setGravity(Config.PHYSICS_GRAVITY);
		character.setJumpSpeed(Config.PLAYER_JUMP_SPEED);

		dynamicsWorld.addCollisionObject(ghostObject);
		//CollisionFilterGroups.CHARACTER_FILTER,
		//		(short) (CollisionFilterGroups.STATIC_FILTER | CollisionFilterGroups.DEFAULT_FILTER));
		dynamicsWorld.addAction(character);
		heading = 0;
		pitch = 0;
	}

	private static float[] glMat = new float[16];

	void render() {	
		Transform xform = ghostObject.getWorldTransform(new Transform());
		if (Game.heartbeatFrame)
			System.out.println("char is at " + xform.origin);

		GL11.glPushMatrix();
		xform.getOpenGLMatrix(glMat);
		GL11.glMultMatrix(Game.makeFB(glMat));

		GL11.glRotatef(heading, 0, 1, 0);
		GL11.glRotatef(pitch, 1, 0, 0);
		Game.collision.drawCube(new Vector3f(1f, 1.5f, 1f));

		GL11.glPopMatrix();
	}
	
	private static BoxShape boxShape = new BoxShape(new Vector3f(0.5f,0.5f,0.5f));
	private static Transform startTransform = new Transform();
	private static Vector3f localInertia = new Vector3f(0, 0, 0);
	private static int msSinceShoot = 0;
	
	void move() {
		if (Game.collision.dynamicsWorld == null)
			return;

		msSinceShoot += Game.delta;
		if (msSinceShoot > 500 && Mouse.isButtonDown(0)) {
			msSinceShoot = 0;
			float mass = 0.5f;
			Vector3f lookDir = Vec3.HPVector(180-heading, pitch);
			
			Transform xform = ghostObject.getWorldTransform(new Transform());
			
			startTransform.setIdentity();
			startTransform.origin.set(lookDir);
			startTransform.origin.scale(2);
			startTransform.origin.add(xform.origin);
			
			boxShape.calculateLocalInertia(mass, localInertia);
			RigidBody cube = Game.collision.spawnCube(mass, boxShape, startTransform, localInertia);
			lookDir.scale(20);
			cube.applyCentralImpulse(lookDir);
		}
			

		if (character.onGround() || Config.PLAYER_MIDAIR_CONTROL) {
			Vector3f forwardDir = Vec3.HPVector(180-heading, 0);
			Vector3f strafeDir = Vec3.HPVector(-heading + 90, 0);
			
			// set walkDirection for character
			Vector3f walkDirection = new Vector3f(0, 0, 0);
			if (Keyboard.isKeyDown(Keyboard.KEY_COMMA))
				walkDirection.add(forwardDir);
			if (Keyboard.isKeyDown(Keyboard.KEY_O))
				walkDirection.sub(forwardDir);
			if (Keyboard.isKeyDown(Keyboard.KEY_A))
				walkDirection.add(strafeDir);
			if (Keyboard.isKeyDown(Keyboard.KEY_E))
				walkDirection.sub(strafeDir);

			if (walkDirection.length() > 1f)
				walkDirection.normalize();
			walkDirection.scale(0.25f);

			character.setWalkDirection(walkDirection);
			
			if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
				character.jump();
		}
	}

	@Override
	public void setHeadingPitch(float heading, float pitch) {
		this.heading = heading;
		this.pitch = pitch;
	}

	@Override
	public Vector3f getPos() {
		Transform xform = ghostObject.getWorldTransform(new Transform());
		return xform.origin;
	}
}
