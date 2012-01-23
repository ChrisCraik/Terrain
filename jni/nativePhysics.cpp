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

#define CHAR_GRAVITY 10.0f
#define CHAR_JUMP_SPEED 10
#define CHAR_MAX_SLOPE 2 //Radians

class NativePhysics
{
public:
	NativePhysics();
	btDefaultCollisionConfiguration m_collisionConfiguration;
	btCollisionDispatcher m_dispatcher;
	btAxisSweep3 m_broadphase;
	btSequentialImpulseConstraintSolver m_solver;
	btGhostPairCallback m_ghostPairCallback;
	btDiscreteDynamicsWorld m_dynamicsWorld;
	float m_chunkSize;
};

NativePhysics::NativePhysics() :
		m_collisionConfiguration(),
		m_dispatcher(&m_collisionConfiguration),
		m_broadphase(btVector3(-1000,-1000,-1000), btVector3(1000,1000,1000), 4096),
		m_solver(),
		m_ghostPairCallback(),
		m_dynamicsWorld(
				&m_dispatcher, &m_broadphase, &m_solver,
				&m_collisionConfiguration)
{
	m_broadphase.getOverlappingPairCache()->setInternalGhostPairCallback(&m_ghostPairCallback);
}

static NativePhysics physics;

class NativeChunk
{
public:
	NativeChunk(int totalTriangles, void* indices, int indexStride,
			int totalVertices, void* vertices, int vertexStride,
			btVector3 &inertia, btVector3 &aabbMin,
			btVector3 &aabbMax);

	btTriangleIndexVertexArray m_indexVertexArrays;
	btBvhTriangleMeshShape m_shape;
	btRigidBody::btRigidBodyConstructionInfo m_info;
	btRigidBody m_body;
};

NativeChunk::NativeChunk(int totalTriangles, void* indices, int indexStride,
		int totalVertices, void* vertices, int vertexStride,
		btVector3 &inertia, btVector3 &aabbMin, btVector3 &aabbMax) :
		m_indexVertexArrays(totalTriangles, (int*) indices, indexStride, totalVertices, (btScalar*) vertices, vertexStride),
		m_shape(&m_indexVertexArrays, true, aabbMin, aabbMax),
		m_info((btScalar) 0, 0, &m_shape, inertia),
		m_body(m_info)
{
}

class NativeCharacter
{
public:
	NativeCharacter(btTransform &transform) :
			m_shape(1, 1.5),
			m_character(&m_ghostObject, &m_shape, btScalar(0.5))
	{
		m_ghostObject.setWorldTransform(transform);
		//physics.m_broadphase.getOverlappingPairCache()->setInternalGhostPairCallback(new btGhostPairCallback());

		m_ghostObject.setCollisionShape(&m_shape);
		m_ghostObject.setCollisionFlags(btCollisionObject::CF_CHARACTER_OBJECT);
		m_character.setGravity(CHAR_GRAVITY);
		m_character.setJumpSpeed(CHAR_JUMP_SPEED);
                m_character.setMaxSlope(CHAR_MAX_SLOPE);
		//physics.m_dynamicsWorld.addCollisionObject(&m_ghostObject);
		physics.m_dynamicsWorld.addCollisionObject(&m_ghostObject,
				btBroadphaseProxy::CharacterFilter,
				btBroadphaseProxy::StaticFilter|btBroadphaseProxy::DefaultFilter);
		physics.m_dynamicsWorld.addAction(&m_character);
	}
	btPairCachingGhostObject m_ghostObject;
	btCapsuleShape m_shape;
	btKinematicCharacterController m_character;

	bool m_onGroundLastFrame;
};

class NativeCube
{
public:
	NativeCube(float size, float mass, btTransform &transform,
			btMotionState &motion, btVector3 &inertia) :
				m_shape(btVector3(size,size,size)),
				m_state(transform),
				m_info(mass, mass == 0.f ? 0 : &m_state, &m_shape, inertia),
				m_body(m_info)
	{
#ifdef DEBUG
		fprintf(stderr, "mass is %f\n", mass);
#endif
		physics.m_dynamicsWorld.addRigidBody(&m_body);
	}
	btBoxShape m_shape;
	btDefaultMotionState m_state;
	btRigidBody::btRigidBodyConstructionInfo m_info;
	btRigidBody m_body;
};

static btTransform startTransform;
static btDefaultMotionState motion(startTransform);
static btVector3 inertia(0, 0, 0), aabbMin(-1000, -1000, -1000), aabbMax(1000, 1000, 1000);

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeInitialize(
		JNIEnv* env, jobject obj, jfloat gravity, jfloat chunkSize)
{
	fprintf(stderr, "Hooray, physics! Woo! Gravity = %f\n", gravity);
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

#ifdef DEBUG
	fprintf(stderr, "Creating native mesh! %d tris, %d verts\n", tris, verts);
	fprintf(stderr, "coordBytes %d, numTriangles %d, numVert %d, triangleIndexStride %d, vertexStride %d\n",
			indexScalarSize, tris, verts,
			3*indexScalarSize, 3*4);
#endif

	return (jlong) new NativeChunk(tris, indices, 3 * indexScalarSize, verts,
			vertices, 3 * 4, inertia, aabbMin, aabbMax);
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeRegisterMesh(
		JNIEnv* env, jobject obj, jlong meshPtr)
{
#ifdef DEBUG
	fprintf(stderr, "Registering chunk...\n");
#endif
	NativeChunk* chunk = (NativeChunk*) meshPtr;
	if (NULL == chunk)
	{
		fprintf(stderr,
				"WARNING: native layer passed null ptr for registry.\n");
		return;
	}
	physics.m_dynamicsWorld.addRigidBody(&(chunk->m_body));
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeUnregisterMesh(
		JNIEnv* env, jobject obj, jlong meshPtr)
{
	NativeChunk* chunk = (NativeChunk*) meshPtr;
	if (NULL == chunk)
	{
		fprintf(stderr, "WARNING: native layer passed null ptr to unregister.\n");
		return;
	}
	physics.m_dynamicsWorld.removeRigidBody(&(chunk->m_body));
	delete (chunk);
}

JNIEXPORT jlong JNICALL Java_supergame_physics_NativePhysics_nativeCreateCharacter(
		JNIEnv* env, jobject obj, jfloat x, jfloat y, jfloat z)
{
#ifdef DEBUG
	fprintf(stderr,"creating character\n");
#endif
	btTransform startTransform;
	startTransform.setIdentity();
	startTransform.setOrigin(btVector3(x,y,z));
	return (jlong) new NativeCharacter(startTransform);
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeControlCharacter
  (JNIEnv *env, jobject obj, jlong charPtr, jfloat strengthIfJumping, jboolean jump, jfloat x, jfloat y, jfloat z)
{
	NativeCharacter* character = (NativeCharacter*) charPtr;

	bool onGround = character->m_character.onGround();
	btVector3 walkDir(x, y, z);
	if (!onGround)
		walkDir *= strengthIfJumping;
	character->m_character.setWalkDirection(walkDir);
	if (onGround && jump) {
		character->m_character.setJumpSpeed(CHAR_JUMP_SPEED);
		character->m_character.jump();
	}

/*
        // NOTE: the below is a workaround for characters falling immediatly at max speed
        if (!onGround && character->m_onGroundLastFrame) {
            character->m_character.setJumpSpeed(CHAR_JUMP_SPEED);
            character->m_character.jump();
        }
	character->m_onGroundLastFrame = character->m_character.onGround() && !jump;
*/
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeQueryCharacterPosition
  (JNIEnv *env, jobject obj, jlong charPtr, jobject position)
{
	NativeCharacter* character = (NativeCharacter*) charPtr;
	btTransform world = character->m_ghostObject.getWorldTransform();

	btVector3 origin(world.getOrigin());

#ifdef DEBUG
	fprintf(stderr,"querying character at %f %f %f\n",
			world.getOrigin().getX(), world.getOrigin().getY(), world.getOrigin().getZ());
#endif

	static jclass vectorclass = env->FindClass("javax/vecmath/Vector3f");
	static jfieldID xfid = env->GetFieldID(vectorclass, "x", "F");
	static jfieldID yfid = env->GetFieldID(vectorclass, "y", "F");
	static jfieldID zfid = env->GetFieldID(vectorclass, "z", "F");

	env->SetFloatField(position, xfid, origin.getX());
	env->SetFloatField(position, yfid, origin.getY());
	env->SetFloatField(position, zfid, origin.getZ());

	//float* matrix = (float*) env->GetDirectBufferAddress(matrixBuffer);
	//world.getOpenGLMatrix(matrix);
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeSetCharacterPosition
  (JNIEnv *env, jobject obj, jlong charPtr, jobject position, jfloat bias)
{
	// TODO: set char position
	NativeCharacter* character = (NativeCharacter*) charPtr;
	btTransform world = character->m_ghostObject.getWorldTransform();

	static jclass vectorclass = env->FindClass("javax/vecmath/Vector3f");
	static jfieldID xfid = env->GetFieldID(vectorclass, "x", "F");
	static jfieldID yfid = env->GetFieldID(vectorclass, "y", "F");
	static jfieldID zfid = env->GetFieldID(vectorclass, "z", "F");

	btVector3 newOrigin(
		env->GetFloatField(position, xfid),
		env->GetFloatField(position, yfid),
		env->GetFloatField(position, zfid));

	if (bias != 1.0f) {
		btVector3 oldOrigin(world.getOrigin());
		newOrigin *= bias;
		newOrigin += oldOrigin * (1.0f - bias);
	}
	world.setOrigin(newOrigin);
	character->m_ghostObject.setWorldTransform(world);
}

JNIEXPORT jlong JNICALL Java_supergame_physics_NativePhysics_nativeCreateCube(
		JNIEnv* env, jobject obj, jfloat size, jfloat mass, jfloat x, jfloat y, jfloat z)
{
#ifdef DEBUG
	fprintf(stderr,"creating cube of size %f, mass %f, at %f %f %f!\n", size, mass, x, y, z);
#endif
	btTransform transform;
	transform.setIdentity();
	transform.setOrigin(btVector3(x,y,z));
	return (jlong) new NativeCube(size, mass, transform, motion, inertia);
}

JNIEXPORT void JNICALL Java_supergame_physics_NativePhysics_nativeQueryObject
  (JNIEnv *env, jobject obj, jlong cubePtr, jobject matrixBuffer)
{
	NativeCube* cube = (NativeCube*)cubePtr;
	btTransform world = cube->m_body.getWorldTransform();

#ifdef DEBUG
	fprintf(stderr,"querying cube at %f %f %f\n",
			world.getOrigin().getX(), world.getOrigin().getY(), world.getOrigin().getZ());
#endif

	float* matrix = (float*) env->GetDirectBufferAddress(matrixBuffer);
	world.getOpenGLMatrix(matrix);
}
