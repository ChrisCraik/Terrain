package supergame;

import javax.vecmath.Vector3f;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.bulletphysics.collision.broadphase.CollisionFilterGroups;
import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.character.KinematicCharacterController;
import com.bulletphysics.linearmath.Transform;

public class Character {

	public KinematicCharacterController character;
	public PairCachingGhostObject ghostObject;

	Character(DiscreteDynamicsWorld dynamicsWorld, Vector3f startPos) {
		Transform startTransform = new Transform();
		startTransform.setIdentity();
		startTransform.origin.set(startPos);

		ghostObject = new PairCachingGhostObject();
		ghostObject.setWorldTransform(startTransform);
		//ConvexShape ghostShape = new CapsuleShape(1.5f, 1.5f);
		//ConvexShape ghostShape = new BoxShape(new Vector3f(1f, 2f, 1f));
		ConvexShape ghostShape = new SphereShape(1f);
		ghostObject.setCollisionShape(ghostShape);
		ghostObject.setCollisionFlags(CollisionFlags.CHARACTER_OBJECT);

		float stepHeight = 0.5f;
		character = new KinematicCharacterController(ghostObject, ghostShape, stepHeight);
		character.setGravity(100);
		character.setJumpSpeed(25);

		dynamicsWorld.addCollisionObject(ghostObject);
		//CollisionFilterGroups.CHARACTER_FILTER,
		//		(short) (CollisionFilterGroups.STATIC_FILTER | CollisionFilterGroups.DEFAULT_FILTER));
		dynamicsWorld.addAction(character);
	}

	private static float[] glMat = new float[16];

	void move() {
		if (Game.collision.dynamicsWorld == null)
			return;

		// set walkDirection for our character
		Transform xform = ghostObject.getWorldTransform(new Transform());
		if (Game.heartbeatFrame)
			System.out.println("char is at " + xform.origin);

		GL11.glPushMatrix();
		xform.getOpenGLMatrix(glMat);
		GL11.glMultMatrix(Game.makeFB(glMat));

		Game.collision.drawCube(new Vector3f(1f, 1f, 1f));

		GL11.glPopMatrix();

		if (character.onGround() || Config.MIDAIR_CONTROL) {
			Vector3f forwardDir = new Vector3f();
			xform.basis.getRow(2, forwardDir);
			Vector3f upDir = new Vector3f();
			xform.basis.getRow(1, upDir);
			Vector3f strafeDir = new Vector3f();
			xform.basis.getRow(0, strafeDir);
			forwardDir.normalize();
			upDir.normalize();
			strafeDir.normalize();

			Vector3f walkDirection = new Vector3f(0f, 0f, 0f);
			if (Keyboard.isKeyDown(Keyboard.KEY_UP))
				walkDirection.add(forwardDir);
			if (Keyboard.isKeyDown(Keyboard.KEY_DOWN))
				walkDirection.sub(forwardDir);
			if (Keyboard.isKeyDown(Keyboard.KEY_LEFT))
				walkDirection.add(strafeDir);
			if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
				walkDirection.sub(strafeDir);

			if (walkDirection.length() > 1f)
				walkDirection.normalize();
			walkDirection.scale(0.5f);

			character.setWalkDirection(walkDirection);
			
			if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
				character.jump();
		}
	}
}
