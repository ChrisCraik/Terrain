package supergame.physics;

import java.nio.ByteBuffer;

import javax.vecmath.Vector3f;

public class NativePhysics implements Physics {
	public native void nativeInitialize(float gravity, float chunkSize);

	public native void nativeStepSimulation(float timeStep, int maxSubSteps);

	public native long nativeCreateMesh(ByteBuffer vertices, int vertBytes,
			ByteBuffer indices);

	public native void nativeRegisterMesh(long meshId);

	public native void nativeUnregisterMesh(long meshId);

	public native long nativeCreateCharacter(float x, float y, float z);

	public native void nativeQueryCharacterPosition(long character, Vector3f position);

	public native void nativeControlCharacter(long character,
			boolean applyIfJumping, boolean jump, float x, float y, float z);

	public native long nativeCreateCube(float size, float mass, float x,
			float y, float z);

	public native void nativeQueryObject(long object, ByteBuffer matrix);
	
	static {
		System.err.println(System.getProperty("java.library.path"));
		try {
			System.loadLibrary("NativePhysics");
			System.err.println("Hooray! loaded successfully");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize(float gravity, float chunkSize) {
		nativeInitialize(gravity, chunkSize);
	}

	@Override
	public void stepSimulation(float timeStep, int maxSubSteps) {
		nativeStepSimulation(timeStep, maxSubSteps);
	}

	@Override
	public long createMesh(ByteBuffer vertices, int vertBytes,
			ByteBuffer indices) {
		return nativeCreateMesh(vertices, vertBytes, indices);
	}

	@Override
	public void registerMesh(long meshId) {
		nativeRegisterMesh(meshId);
	}

	@Override
	public void unregisterMesh(long meshId) {
		nativeUnregisterMesh(meshId);
	}

	@Override
	public long createCharacter(float x, float y, float z) {
		return nativeCreateCharacter(x, y, z);
	}

	@Override
	public void controlCharacter(long character, boolean applyIfJumping, boolean jump,
			float x, float y, float z) {
		nativeControlCharacter(character, applyIfJumping, jump, x, y, z);
	}

	@Override
	public void queryCharacterPosition(long character, Vector3f position) {
		nativeQueryCharacterPosition(character, position);
	}

	@Override
	public long createCube(float size, float mass, float x, float y, float z) {
		return nativeCreateCube(size, mass, x, y, z);
	}

	@Override
	public void queryObject(long object, ByteBuffer matrix) {
		nativeQueryObject(object, matrix);
	}
}
