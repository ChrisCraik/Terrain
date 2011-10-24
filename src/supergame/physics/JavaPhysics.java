package supergame.physics;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.dispatch.GhostPairCallback;
import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.collision.shapes.ScalarType;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.character.KinematicCharacterController;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

public class JavaPhysics implements Physics {

	// create 125 (5x5x5) dynamic object
	private static final int ARRAY_SIZE_X = 3;
	private static final int ARRAY_SIZE_Y = 3;
	private static final int ARRAY_SIZE_Z = 3;

	private static final int PLAYER_GRAVITY = 10;
	private static final int PLAYER_JUMP_SPEED = 10;

	// maximum number of objects (and allow user to shoot additional boxes)
	private static final int MAX_PROXIES = (ARRAY_SIZE_X * ARRAY_SIZE_Y * ARRAY_SIZE_Z + 1024 * 4);

	private DiscreteDynamicsWorld dynamicsWorld;

	private class CharPhysics {
		public CharPhysics(KinematicCharacterController controller, PairCachingGhostObject ghostObject) {
			mController = controller;
			mGhostObject = ghostObject;
			mMoveDirection = new Vector3f(); // TODO: use this for smoother air control
		}
		public KinematicCharacterController mController;
		public PairCachingGhostObject mGhostObject;
		public Vector3f mMoveDirection;
	}

	private final Map<Long, RigidBody> rigidBodyMap = new HashMap<Long, RigidBody>();
	private long nextRigidBodyId = 1;
	private final Map<Long, CharPhysics> characterMap = new HashMap<Long, CharPhysics>();
	private long nextCharacterId = 1;

	private final float matrixArray[] = new float[16];

	@Override
	public void initialize(float gravity, float chunkSize) {
		// collision configuration contains default setup for memory, collision
		// setup. Advanced users can create their own configuration.
		CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();

		// use the default collision dispatcher. For parallel processing you
		// can use a different dispatcher (see Extras/BulletMultiThreaded)
		CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);

		// the maximum size of the collision world. Make sure objects stay
		// within these boundaries
		// Don't make the world AABB size too large, it will harm simulation
		// quality and performance
		Vector3f worldAabbMin = new Vector3f(-10000, -10000, -10000);
		Vector3f worldAabbMax = new Vector3f(10000, 10000, 10000);
		AxisSweep3 overlappingPairCache = new AxisSweep3(worldAabbMin, worldAabbMax, MAX_PROXIES);
		overlappingPairCache.getOverlappingPairCache().setInternalGhostPairCallback(new GhostPairCallback());

		// the default constraint solver. For parallel processing you can use a
		// different solver (see Extras/BulletMultiThreaded)
		SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache, solver, collisionConfiguration);

		dynamicsWorld.setGravity(new Vector3f(0, gravity, 0));

	}

	@Override
	public void stepSimulation(float timeStep, int maxSubSteps) {
		dynamicsWorld.stepSimulation(timeStep, maxSubSteps);
	}

	@Override
	public long createMesh(ByteBuffer vertices, int indexBytes, ByteBuffer indices) {
		vertices.rewind();
		indices.rewind();

		TriangleIndexVertexArray indexVertexArrays;
		indexVertexArrays = new TriangleIndexVertexArray();

		IndexedMesh mesh = new IndexedMesh();
		mesh.indexType = (indexBytes == 2) ? ScalarType.SHORT : ScalarType.INTEGER;
		mesh.numTriangles = indices.capacity() / (indexBytes * 3);
		mesh.numVertices = vertices.capacity() / (4 * 3);
		mesh.triangleIndexBase = indices;
		mesh.triangleIndexStride = indexBytes * 3;
		mesh.vertexBase = vertices;
		mesh.vertexStride = 4 * 3;

/*
		System.out.printf("coordBytes %d, numTriangles %d, numVert %d, triangleIndexStride %d, vertexStride %d\n",
				indexBytes, mesh.numTriangles, mesh.numVertices,
				mesh.triangleIndexStride, mesh.vertexStride);

		System.out.printf("indices cap %d, indices lim %d,    vert cap %d, vert lim %d\n",
				indices.capacity(), indices.limit(),
				vertices.capacity(), vertices.limit());
*/
		indexVertexArrays.addIndexedMesh(mesh, (indexBytes == 2) ? ScalarType.SHORT : ScalarType.INTEGER);

		boolean useQuantizedAabbCompression = true;
		BvhTriangleMeshShape trimeshShape = new BvhTriangleMeshShape(indexVertexArrays, useQuantizedAabbCompression);

		Transform groundTransform = new Transform();
		groundTransform.setIdentity();
		groundTransform.origin.set(new Vector3f(0, 0, 0));

		float mass = 0f;
		Vector3f localInertia = new Vector3f(0, 0, 0);

		// using motionstate is recommended, it provides interpolation
		// capabilities, and only synchronizes 'active' objects
		DefaultMotionState myMotionState = new DefaultMotionState(groundTransform);
		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, trimeshShape,
				localInertia);

		// store the new rigid body in a map so that it can be accessed with a unique long id
		long newRigidBodyId = nextRigidBodyId++;
		rigidBodyMap.put(newRigidBodyId, new RigidBody(rbInfo));
		return newRigidBodyId;
	}

	@Override
	public void registerMesh(long meshId) {
		if (0 == meshId)
			return;
		RigidBody body = rigidBodyMap.get(meshId);
		dynamicsWorld.addRigidBody(body);
	}

	@Override
	public void unregisterMesh(long meshId) {
		if (0 == meshId)
			return;
		RigidBody body = rigidBodyMap.remove(meshId);
		dynamicsWorld.removeRigidBody(body);
	}

	@Override
	public long createCharacter(float x, float y, float z) {

		Transform startTransform = new Transform();
		startTransform.setIdentity();
		startTransform.origin.set(x,y,z);

		PairCachingGhostObject ghostObject = new PairCachingGhostObject();
		ghostObject.setWorldTransform(startTransform);
		ConvexShape ghostShape = new CapsuleShape(1f, 1.5f);
		ghostObject.setCollisionShape(ghostShape);
		//ghostObject.setCollisionFlags(CollisionFlags.CHARACTER_OBJECT);

		float stepHeight = 0.5f;
		KinematicCharacterController character = new KinematicCharacterController(ghostObject, ghostShape,
				stepHeight);
		character.setGravity(PLAYER_GRAVITY);
		character.setJumpSpeed(PLAYER_JUMP_SPEED);

		dynamicsWorld.addCollisionObject(ghostObject);
		dynamicsWorld.addAction(character);

		long newCharacterId = nextCharacterId++;

		characterMap.put(newCharacterId, new CharPhysics(character, ghostObject));
		return newCharacterId;
	}

	@Override
	public void controlCharacter(long characterId, float strengthIfJumping, boolean jump,
			float x, float y, float z) {
		KinematicCharacterController character = characterMap.get(characterId).mController;

		Vector3f direction = new Vector3f(x,y,z);
		if (character.onGround()) {

		}
			direction.scale(strengthIfJumping);
		character.setWalkDirection(direction);
		if (character.onGround() && jump)
			character.jump();
	}

	@Override
	public void queryCharacterPosition(long characterId, Vector3f position) {
		PairCachingGhostObject ghostObject = characterMap.get(characterId).mGhostObject;
		Transform world = new Transform();
		ghostObject.getWorldTransform(world);

		//System.out.println("Querying character" + characterId + world.origin);

		position.x = world.origin.x;
		position.y = world.origin.y;
		position.z = world.origin.z;
		//world.getOpenGLMatrix(matrixArray);
		//matrix.asFloatBuffer().put(matrixArray).flip();
	}

	private static final Vector3f inertia = new Vector3f(0,0,0);
	private final Map<Float, CollisionShape> boxShapes = new HashMap<Float, CollisionShape>();

	@Override
	public long createCube(float size, float mass, float x, float y, float z) {
		//System.out.println("Creating cube:" + x + " " + y  + " " + z);
		Transform startTransform = new Transform();
		startTransform.setIdentity();
		startTransform.origin.set(x,y,z);

		//re-use the same collision shape when possible for performance
		if (!boxShapes.containsKey(size))
			boxShapes.put(size, new BoxShape(new Vector3f(size, size, size)));

		// using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
		DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, boxShapes.get(size), inertia);
		RigidBody body = new RigidBody(rbInfo);
		//body.setActivationState(RigidBody.ISLAND_SLEEPING);

		dynamicsWorld.addRigidBody(body);
		long newObjectId = nextRigidBodyId++;
		rigidBodyMap.put(newObjectId, body);
		return newObjectId;
	}

	@Override
	public void queryObject(long objectId, ByteBuffer matrix) {
		RigidBody body = rigidBodyMap.get(objectId);

		if (body != null && body.getMotionState() != null) {
			Transform world = new Transform();
			body.getWorldTransform(world);

			//System.out.println("Querying object" + objectId + world.origin);

			world.getOpenGLMatrix(matrixArray);
			matrix.asFloatBuffer().put(matrixArray).flip();
		} else {
			System.err.println("Warning: failed to get matrix for obj " + objectId);
		}
	}
}
