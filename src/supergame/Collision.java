package supergame;

/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 * HelloWorld port by: Clark Dorman
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.dispatch.GhostPairCallback;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import javax.vecmath.Vector3f;

import org.lwjgl.opengl.GL11;

/**
 * This is a Hello World program for running a basic Bullet physics simulation.
 * it is a direct translation of the C++ HelloWorld app.
 *
 * @author cdorman
 */
public class Collision {
	
	// create 125 (5x5x5) dynamic object
	private static final int ARRAY_SIZE_X = 4;
	private static final int ARRAY_SIZE_Y = 4;
	private static final int ARRAY_SIZE_Z = 4;

	// maximum number of objects (and allow user to shoot additional boxes)
	private static final int MAX_PROXIES = (ARRAY_SIZE_X * ARRAY_SIZE_Y * ARRAY_SIZE_Z + 1024);

	private static final int START_POS_X = 30;
	private static final int START_POS_Y = 80;
	private static final int START_POS_Z = 30;

	// keep track of the shapes, we release memory at exit.
	// make sure to re-use collision shapes among rigid bodies whenever
	// possible!
	public ObjectArrayList<CollisionShape> collisionShapes = new ObjectArrayList<CollisionShape>();
	public DiscreteDynamicsWorld dynamicsWorld;
	
	public Character character;
	
	Collision() {
		// collision configuration contains default setup for memory, collision
		// setup. Advanced users can create their own configuration.
		CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();

		// use the default collision dispatcher. For parallel processing you
		// can use a diffent dispatcher (see Extras/BulletMultiThreaded)
		CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);

		// the maximum size of the collision world. Make sure objects stay
		// within these boundaries
		// Don't make the world AABB size too large, it will harm simulation
		// quality and performance
		Vector3f worldAabbMin = new Vector3f(-10000, -10000, -10000);
		Vector3f worldAabbMax = new Vector3f(10000, 10000, 10000);
		AxisSweep3 overlappingPairCache = new AxisSweep3(worldAabbMin, worldAabbMax, MAX_PROXIES);
		overlappingPairCache.getOverlappingPairCache().setInternalGhostPairCallback(new GhostPairCallback());
		//BroadphaseInterface overlappingPairCache = new SimpleBroadphase(
		//		maxProxies);

		// the default constraint solver. For parallel processing you can use a
		// different solver (see Extras/BulletMultiThreaded)
		SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache, solver, collisionConfiguration);

		dynamicsWorld.setGravity(new Vector3f(0, -Config.PHYSICS_GRAVITY, 0));

		// create a few basic rigid bodies
		CollisionShape groundShape = new BoxShape(new Vector3f(50.f, 50.f, 50.f));

		collisionShapes.add(groundShape);

		Transform groundTransform = new Transform();
		groundTransform.setIdentity();
		groundTransform.origin.set(new Vector3f(0.f, -56.f, 0.f));

		{
			float mass = 0f;
			Vector3f localInertia = new Vector3f(0, 0, 0);

			// using motionstate is recommended, it provides interpolation
			// capabilities, and only synchronizes 'active' objects
			DefaultMotionState myMotionState = new DefaultMotionState(groundTransform);
			RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, groundShape,
					localInertia);
			RigidBody body = new RigidBody(rbInfo);

			// add the body to the dynamics world
			dynamicsWorld.addRigidBody(body);
		}
		{
			// create a few dynamic rigidbodies
			// Re-using the same collision is better for memory usage and performance
			CollisionShape colShape = new BoxShape(new Vector3f(1, 1, 1));
			//CollisionShape colShape = new SphereShape(1f);
			collisionShapes.add(colShape);

			// Create Dynamic Objects
			Transform startTransform = new Transform();
			startTransform.setIdentity();

			float mass = 1f;
			Vector3f localInertia = new Vector3f(0, 0, 0);
			colShape.calculateLocalInertia(mass, localInertia);

			float start_x = START_POS_X - ARRAY_SIZE_X / 2;
			float start_y = START_POS_Y;
			float start_z = START_POS_Z - ARRAY_SIZE_Z / 2;

			for (int k = 0; k < ARRAY_SIZE_Y; k++) {
				for (int i = 0; i < ARRAY_SIZE_X; i++) {
					for (int j = 0; j < ARRAY_SIZE_Z; j++) {
						startTransform.origin.set(2f * i + start_x, 10f + 2f * k + start_y, 2f * j + start_z);

						// using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
						DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
						RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, colShape,
								localInertia);
						RigidBody body = new RigidBody(rbInfo);
						//body.setActivationState(RigidBody.ISLAND_SLEEPING);

						dynamicsWorld.addRigidBody(body);
						//body.setActivationState(RigidBody.ISLAND_SLEEPING);
					}
				}
			}
			character = new Character(dynamicsWorld, new Vector3f(start_x, start_y, start_z));
		}

	}

	public void drawCube(Vector3f size) {
		GL11.glScalef(size.x, size.y, size.z);

		GL11.glBegin(GL11.GL_QUADS); // Start Drawing Quads
		// Front Face
		GL11.glNormal3f(0f, 0f, 1f); // Normal Facing Forward
		GL11.glVertex3f(-1f, -1f, 1f); // Bottom Left Of The Texture and Quad
		GL11.glVertex3f(1f, -1f, 1f); // Bottom Right Of The Texture and Quad
		GL11.glVertex3f(1f, 1f, 1f); // Top Right Of The Texture and Quad
		GL11.glVertex3f(-1f, 1f, 1f); // Top Left Of The Texture and Quad
		// Back Face
		GL11.glNormal3f(0f, 0f, -1f); // Normal Facing Away
		GL11.glVertex3f(-1f, -1f, -1f); // Bottom Right Of The Texture and Quad
		GL11.glVertex3f(-1f, 1f, -1f); // Top Right Of The Texture and Quad
		GL11.glVertex3f(1f, 1f, -1f); // Top Left Of The Texture and Quad
		GL11.glVertex3f(1f, -1f, -1f); // Bottom Left Of The Texture and Quad
		// Top Face
		GL11.glNormal3f(0f, 1f, 0f); // Normal Facing Up
		GL11.glVertex3f(-1f, 1f, -1f); // Top Left Of The Texture and Quad
		GL11.glVertex3f(-1f, 1f, 1f); // Bottom Left Of The Texture and Quad
		GL11.glVertex3f(1f, 1f, 1f); // Bottom Right Of The Texture and Quad
		GL11.glVertex3f(1f, 1f, -1f); // Top Right Of The Texture and Quad
		// Bottom Face
		GL11.glNormal3f(0f, -1f, 0f); // Normal Facing Down
		GL11.glVertex3f(-1f, -1f, -1f); // Top Right Of The Texture and Quad
		GL11.glVertex3f(1f, -1f, -1f); // Top Left Of The Texture and Quad
		GL11.glVertex3f(1f, -1f, 1f); // Bottom Left Of The Texture and Quad
		GL11.glVertex3f(-1f, -1f, 1f); // Bottom Right Of The Texture and Quad
		// Right face
		GL11.glNormal3f(1f, 0f, 0f); // Normal Facing Right
		GL11.glVertex3f(1f, -1f, -1f); // Bottom Right Of The Texture and Quad
		GL11.glVertex3f(1f, 1f, -1f); // Top Right Of The Texture and Quad
		GL11.glVertex3f(1f, 1f, 1f); // Top Left Of The Texture and Quad
		GL11.glVertex3f(1f, -1f, 1f); // Bottom Left Of The Texture and Quad
		// Left Face
		GL11.glNormal3f(-1f, 0f, 0f); // Normal Facing Left
		GL11.glVertex3f(-1f, -1f, -1f); // Bottom Left Of The Texture and Quad
		GL11.glVertex3f(-1f, -1f, 1f); // Bottom Right Of The Texture and Quad
		GL11.glVertex3f(-1f, 1f, 1f); // Top Right Of The Texture and Quad
		GL11.glVertex3f(-1f, 1f, -1f); // Top Left Of The Texture and Quad
		GL11.glEnd();// Done Drawing Quads
	}

	public void stepSimulation(float duration) {
		character.move();
		dynamicsWorld.stepSimulation(duration, 10);
	}

	private static float[] glMat = new float[16];
	public void render() {
		// render all collision objects
		character.render();
		for (int j = dynamicsWorld.getNumCollisionObjects() - 1; j >= 0; j--) {
			CollisionObject obj = dynamicsWorld.getCollisionObjectArray().getQuick(j);
			RigidBody body = RigidBody.upcast(obj);
			if (body != null && body.getMotionState() != null) {
				Transform trans = new Transform();
				body.getMotionState().getWorldTransform(trans);
				CollisionShape shape = body.getCollisionShape();

				GL11.glPushMatrix();
				trans.getOpenGLMatrix(glMat);
				GL11.glMultMatrix(Game.makeFB(glMat));
				switch (shape.getShapeType()) {
				case BOX_SHAPE_PROXYTYPE:
					BoxShape boxShape = (BoxShape) shape;
					Vector3f halfExtent = boxShape.getHalfExtentsWithMargin(new Vector3f());
					drawCube(halfExtent);
					break;

				case SPHERE_SHAPE_PROXYTYPE:
					SphereShape sphereShape = (SphereShape) shape;
					Vector3f sphereSize = new Vector3f(sphereShape.getMargin(), sphereShape.getMargin(),
							sphereShape.getMargin());
					drawCube(sphereSize);
				}
				/*
				if (Game.heartbeatFrame)
					System.out.printf("%s: world pos = %f,%f,%f\n", body.getCollisionShape().getShapeType(),
							trans.origin.x, trans.origin.y, trans.origin.z);
				*/
				GL11.glPopMatrix();
			}
		}
	}
}
