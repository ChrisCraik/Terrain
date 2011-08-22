package supergame.physics;

import java.nio.ByteBuffer;

import javax.vecmath.Vector3f;

public interface Physics {
	/**
	 * Create an empty physics environment
	 */
	void initialize(float gravity, float chunkSize);
	
	void stepSimulation(float timeStep, int maxSubSteps);

	/**
	 * Create a static collision mesh (such as terrain) that can be collided
	 * against but that is immobile. This function is intended to simply take a mesh as defined for OpenGL rendering as input.
	 * @param coords List of points in the mesh, with each three floating point(float or double) numbers representing a x,y,z coordinate
	 * @param coordBytes Number of bytes used to represent a single floating point coordinate. 4 for 'float' or 8 for 'double'
	 * @param indices List of points that make up the triangles of the mesh. Each point in 3d space is represented by its index in the coords list
	 * @return unique identifier of the mesh created, used in registerMesh() to add the mesh to the environment
	 */
	long createMesh(ByteBuffer coords, int indexBytes, ByteBuffer indices);

	/**
	 * Add a static mesh to the current dynamics simulation
	 */
	void registerMesh(long meshId);

	void unregisterMesh(long meshId);

	long createCharacter(float x, float y, float z);

	void controlCharacter(long character, boolean applyIfJumping, boolean jump, float x, float y, float z);

	public void queryCharacterPosition(long characterId, Vector3f position);

	long createCube(float size, float mass, float x, float y, float z);

	void queryObject(long object, ByteBuffer matrix);
}
