package supergame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.vecmath.Vector3f;

import org.lwjgl.opengl.GL11;

import supergame.physics.JavaPhysics;
import supergame.physics.NativePhysics;
import supergame.physics.Physics;

public class Collision {

	public static final int START_POS_X = 0, START_POS_Y = 20, START_POS_Z = 0;

	// create 27 (3x3x3) dynamic objects
	private static final int ARRAY_SIZE_X = 3;
	private static final int ARRAY_SIZE_Y = 3;
	private static final int ARRAY_SIZE_Z = 3;
/*

	// maximum number of objects (and allow user to shoot additional boxes)
	private static final int MAX_PROXIES = (ARRAY_SIZE_X * ARRAY_SIZE_Y * ARRAY_SIZE_Z + 1024 * 4);

	// keep track of the shapes, we release memory at exit.
	// make sure to re-use collision shapes among rigid bodies whenever
	// possible!
	public ObjectArrayList<CollisionShape> collisionShapes = new ObjectArrayList<CollisionShape>();
	public DiscreteDynamicsWorld dynamicsWorld;

*/
	public Character character;
	public Physics physics;
	
	ArrayList<Long> bodyList = new ArrayList<Long>();
	
	Collision() {
		physics = Config.PHYSICS_USE_NATIVE ? new NativePhysics() : new JavaPhysics();
		physics.initialize(Config.PHYSICS_GRAVITY, Config.CHUNK_DIVISION);
		character = new Character(physics.createCharacter(START_POS_X, START_POS_Y + 40, START_POS_Z));

		float start_x = START_POS_X - ARRAY_SIZE_X / 2;
		float start_y = START_POS_Y;
		float start_z = START_POS_Z - ARRAY_SIZE_Z / 2;
		
		bodyList.add(physics.createCube(15, 0, 0, 0, 0));
		
		float mass = 1;
		for (int k = 0; k < ARRAY_SIZE_Y; k++) {
			for (int i = 0; i < ARRAY_SIZE_X; i++) {
				for (int j = 0; j < ARRAY_SIZE_Z; j++) {
					float x = 2f * i + start_x;
					float y = 10f + 2f * k + start_y;
					float z = 2f * j + start_z;
					bodyList.add(physics.createCube(1, mass, x, y, z));
				}
			}
		}
		/*

		// create a few basic rigid bodies
		CollisionShape groundShape = new BoxShape(new Vector3f(15.f, 15.f, 15.f));

		collisionShapes.add(groundShape);

		Transform groundTransform = new Transform();
		groundTransform.setIdentity();
		groundTransform.origin.set(new Vector3f(0.f, 0.f, 0.f));

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
						spawnCube(mass, colShape, startTransform, localInertia);
					}
				}
			}
			character = new Character(dynamicsWorld, new Vector3f(start_x, start_y, start_z));
		}*/
	}

	/*
	public RigidBody spawnCube(float mass, CollisionShape colShape, Transform startTransform, Vector3f localInertia) {
		// using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
		DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, colShape, localInertia);
		RigidBody body = new RigidBody(rbInfo);
		//body.setActivationState(RigidBody.ISLAND_SLEEPING);

		dynamicsWorld.addRigidBody(body);
		return body;
	}
	*/

	private static int displayList = -1;
	public void drawCube(Vector3f size) {
		GL11.glScalef(size.x, size.y, size.z);

		if (displayList == -1) {
			displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);
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
			GL11.glEndList();
		} else
			GL11.glCallList(displayList);
	}

	public void stepSimulation(float duration) {
		character.move();
		physics.stepSimulation(duration, 10);
	}

	private static ByteBuffer matrix = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder());
	
	public void render() {
		character.render();
		
		for (int i=0; i<bodyList.size(); i++) {
		//for (int i=1; i>=0; i--) {
			int size = 0==i ? 15 : 1;
			physics.queryObject(bodyList.get(i), matrix);
			
			GL11.glPushMatrix();
			GL11.glMultMatrix(matrix.asFloatBuffer());
			drawCube(new Vector3f(size, size, size));
			GL11.glPopMatrix();
		}
		
		/*
		int size;
		
		physics.queryObject(bodyList.get(0), matrix);
		size = 10;
		GL11.glPushMatrix();
		FloatBuffer f = matrix.asFloatBuffer();
		GL11.glMultMatrix(f);
		//matrix.flip();

		for (int i=0; i<4; i++) {
			for (int k=0; k<4; k++) {
				System.err.print("  " + f.get());
			}
			System.err.println();
		}
		System.err.println();
		//GL11.glRotatef(45, 0, 1, 1);
		drawCube(new Vector3f(size, size, size));
		GL11.glPopMatrix();
		
		physics.queryObject(bodyList.get(1), matrix);
		size = 3;
		GL11.glPushMatrix();
		GL11.glLoadMatrix(matrix.asFloatBuffer());
		//GL11.glRotatef(45, 0, 1, 0);
		drawCube(new Vector3f(size, size, size));
		GL11.glPopMatrix();
		/ *
		// render all collision objects
		character.render();
		Game.PROFILE("char render");
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
				/ *
				if (Game.heartbeatFrame)
					System.out.printf("%s: world pos = %f,%f,%f\n", body.getCollisionShape().getShapeType(),
							trans.origin.x, trans.origin.y, trans.origin.z);
				* /
				GL11.glPopMatrix();
			}
		}
		*/
	}
}
