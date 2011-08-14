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
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.collision.shapes.ScalarType;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;

public class JavaPhysics implements Physics {

	// create 125 (5x5x5) dynamic object
	private static final int ARRAY_SIZE_X = 3;
	private static final int ARRAY_SIZE_Y = 3;
	private static final int ARRAY_SIZE_Z = 3;

	// maximum number of objects (and allow user to shoot additional boxes)
	private static final int MAX_PROXIES = (ARRAY_SIZE_X * ARRAY_SIZE_Y * ARRAY_SIZE_Z + 1024 * 4);

	// keep track of the shapes, we release memory at exit.
	// make sure to re-use collision shapes among rigid bodies whenever
	// possible!
	private ObjectArrayList<CollisionShape> collisionShapes = new ObjectArrayList<CollisionShape>();
	private DiscreteDynamicsWorld dynamicsWorld;
	
	private Map<Long, RigidBody> rigidBodyMap = new HashMap<Long, RigidBody>();
	private long nextRigidBodyId = 1;
	
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
		//BroadphaseInterface overlappingPairCache = new SimpleBroadphase(
		//		maxProxies);

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

		System.out.printf("coordBytes %d, numTriangles %d, numVert %d, triangleIndexStride %d, vertexStride %d\n",
				indexBytes, mesh.numTriangles, mesh.numVertices,
				mesh.triangleIndexStride, mesh.vertexStride);
		
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
		collisionShapes.add(body.getCollisionShape());
		dynamicsWorld.addRigidBody(body);
	}

	@Override
	public void unregisterMesh(long meshId) {
		if (0 == meshId)
			return;
		RigidBody body = rigidBodyMap.remove(meshId);
		collisionShapes.remove(body.getCollisionShape());
		dynamicsWorld.removeRigidBody(body);
		
	}

	@Override
	public long createCharacter(float x, float y, float z) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private static final Vector3f inertia = new Vector3f(0,0,0);
	private Map<Float, CollisionShape> boxShapes = new HashMap<Float, CollisionShape>();
	
	@Override
	public long createCube(float size, float mass, float x, float y, float z) {
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
		return 0;
	}

}