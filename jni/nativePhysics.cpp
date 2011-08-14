#include <jni.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include "btBulletDynamicsCommon.h"
#include "BulletDynamics/Character/btKinematicCharacterController.h"
#include "BulletCollision/CollisionDispatch/btGhostObject.h"


#include <stdio.h>
#include "supergame_physics_NativePhysics.h"

class NativePhysics
{
public:
	NativePhysics();
	//btRigidBody* addRigidBody(btScalar mass, const btTransform& startTransform, btCollisionShape* shape);
	btDefaultCollisionConfiguration m_collisionConfiguration;
	btCollisionDispatcher m_dispatcher;
	btDbvtBroadphase m_broadphase;
	btSequentialImpulseConstraintSolver m_solver;
	btGhostPairCallback m_ghostPairCallback;
	btDiscreteDynamicsWorld m_dynamicsWorld;
	float m_chunkSize;
};

NativePhysics::NativePhysics() :
		m_collisionConfiguration(), m_dispatcher(&m_collisionConfiguration), m_broadphase(), m_solver(), m_dynamicsWorld(
				&m_dispatcher, &m_broadphase, &m_solver,
				&m_collisionConfiguration)
{
	m_broadphase.getOverlappingPairCache()->setInternalGhostPairCallback(&m_ghostPairCallback);
}

class NativeChunk
{
public:
	NativeChunk(int totalTriangles, void* indices, int indexStride,
			int totalVertices, void* vertices, int vertexStride,
			btMotionState &motion, btVector3 &inertia, btVector3 &aabbMin,
			btVector3 &aabbMax);

	btTriangleIndexVertexArray m_indexVertexArrays;
	btBvhTriangleMeshShape m_shape;
	btRigidBody::btRigidBodyConstructionInfo m_info;
	btRigidBody m_body;
};

NativeChunk::NativeChunk(int totalTriangles, void* indices, int indexStride,
		int totalVertices, void* vertices, int vertexStride,
		btMotionState &motion, btVector3 &inertia, btVector3 &aabbMin,
		btVector3 &aabbMax) :
		m_indexVertexArrays(totalTriangles, (int*) indices, indexStride,
				totalVertices, (btScalar*) vertices, vertexStride), m_shape(
				&m_indexVertexArrays, true, aabbMin, aabbMax), m_info(
				(btScalar) 0, &motion, &m_shape, inertia), m_body(m_info)
{
}

class NativeCharacter
{
public:
	NativeCharacter(btTransform &transform);
	btPairCachingGhostObject m_ghostObject;
	btCapsuleShape m_shape;
	btKinematicCharacterController m_character;
};

NativeCharacter::NativeCharacter(btTransform &transform) :
		m_shape(1.0, 1.5),
		m_character(&m_ghostObject, &m_shape, btScalar(0.35))
{
	m_ghostObject.setWorldTransform(transform);
	m_ghostObject.setCollisionShape(&m_shape);
}

static NativePhysics physics;

static btTransform startTransform;
static btDefaultMotionState motion(startTransform);
static btVector3 inertia(0, 0, 0), aabbMin(-1000, -1000, -1000), aabbMax(1000,
		1000, 1000);

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeInitialize(
		JNIEnv* env, jobject obj, jfloat gravity, jfloat chunkSize)
{
	printf("Hooray, physics! Woo!\n");
	printf("Vector size is %d", (int) sizeof(btVector3));
	printf("Scalar size is %d", (int) sizeof(btScalar));
	physics.m_dynamicsWorld.setGravity(btVector3(0, gravity, 0));
	physics.m_chunkSize = chunkSize;
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeStepSimulation(
		JNIEnv* env, jobject obj, jfloat timeStep, jint maxSubSteps)
{
	physics.m_dynamicsWorld.stepSimulation(timeStep, maxSubSteps);
}

JNIEXPORT jlong JNICALL Java_supergame_physics_NativePhysics_nativeCreateMesh(
		JNIEnv* env, jobject obj, jobject verticesBuffer, jint indexScalarSize,
		jobject indicesBuffer)
{
	static jclass bbclass = env->FindClass("java/nio/ByteBuffer");
	static jmethodID capMethod = env->GetMethodID(bbclass, "capacity", "()I");
	int verticesCap = env->CallIntMethod(verticesBuffer, capMethod, 0);
	int indicesCap = env->CallIntMethod(indicesBuffer, capMethod, 0);

	void* vertices = (void*) env->GetDirectBufferAddress(verticesBuffer);
	void* indices = (void*) env->GetDirectBufferAddress(indicesBuffer);

	int tris = indicesCap / (3 * indexScalarSize);
	int verts = verticesCap / (3 * 4);

	fprintf(stderr, "Creating native mesh! %d tris, %d verts\n", tris, verts);
	//NativeChunk* chunk = new NativeChunk();

	return (jlong) new NativeChunk(tris, indices, 3 * indexScalarSize, verts,
			vertices, 3 * 4, motion, inertia, aabbMin, aabbMax);
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeRegisterMesh(
		JNIEnv* env, jobject obj, jlong meshPtr)
{
	NativeChunk* chunk = (NativeChunk*) meshPtr;
	if (NULL == chunk)
	{
		fprintf(stderr,
				"WARNING: native layer passed null ptr for registry.\n");
		return;
	}
	//collisionShapes.add(body.getCollisionShape());
	physics.m_dynamicsWorld.addRigidBody(&(chunk->m_body));
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeUnregisterMesh(
		JNIEnv* env, jobject obj, jlong meshPtr)
{
	NativeChunk* chunk = (NativeChunk*) meshPtr;
	if (NULL == chunk)
	{
		fprintf(stderr,
				"WARNING: native layer passed null ptr to unregister.\n");
		return;
	}
	physics.m_dynamicsWorld.removeRigidBody(&(chunk->m_body));
	delete (chunk);
}

JNIEXPORT jlong JNICALL Java_supergame_physics_NativePhysics_nativeCreateCharacter(
		JNIEnv* env, jobject obj, jfloat x, jfloat y, jfloat z)
{
	btTransform startTransform;
	startTransform.setIdentity();
	startTransform.setOrigin(btVector3(x,y,z));
	return (jlong) new NativeCharacter(startTransform);
}

JNIEXPORT jlong JNICALL Java_supergame_physics_NativePhysics_nativeCreateCube(
		JNIEnv* env, jobject obj, jfloat, jfloat, jfloat, jfloat, jfloat)
{
}
