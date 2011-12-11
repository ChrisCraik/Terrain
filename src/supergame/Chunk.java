package supergame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import supergame.Camera.Frustrumable;
import supergame.Camera.Inclusion;
import supergame.modify.ChunkModifierInterface;

/**
 * A chunk is an NxN volumetric surface that is collide-able. It's 'processed'
 * by one of N worker threads in parallel, which generates the necessary
 * geometry for display and physics. parallel_ methods are those called
 * (indirectly) by those processing threads. Serial methods (such as drawing
 * the chunks) are for the UI thread.
 *
 * In order to be modified (for building, and destruction) chunks are replaced
 * with copies that are modified. See the second constructor.
 *
 * First time processing:
 *     1) Weights are calculated
 *     2) Create geometry
 *     3) Create index and vertex lists are created (the format used by OpenGL
 *        & Bullet)
 *     4) Generate point normals (for OpenGL) from TerrainGenerator
 *
 * Then the chunk is ready for rendering and collisions. But there's more:
 *
 * First modification:
 *     1) Weights are calculated from the TerrainGenerator, combined with the
 *        modification
 *     2) (same as above)
 *     3) (same as above)
 *     4) Generate point normals by brute force, by averaging triangle normals
 *     5) finally, the chunk's weights are saved so that later
 *        modifications can use it as a starting point
 *
 * Further modifications:
 *     1) Weights are taken from the previous chunk, and combined with the new
 *        modification
 *     2...5) same as with first modification
 */
public class Chunk implements Frustrumable {
	public static Vec3[] rayDistribution;

	private boolean empty = true;
	private int displayList = -1;

	private ArrayList<Vec3> triangles;

	private final ChunkIndex index;
	private Vec3 pos; //cube's origin (not center)

	private float modifiedWeights[][][] = null;
	HashMap<Integer, Vec3> modifyNormals = null; //TODO: turn this into a workers buffer array
	private Chunk modifiedParent = null;
	private final ChunkModifierInterface modifyComplete;

	private static final int SERIAL_INITIAL = 0; // Chunk allocated, not yet processed
	private static final int PARALLEL_PROCESSING = 1; // being processed by worker thread
	private static final int PARALLEL_COMPLETE = 2; // Processing complete, render-able
	private static final int PARALLEL_GARBAGE = 3; // Processing interrupted by chunk garbage collection
	private final AtomicInteger state = new AtomicInteger(SERIAL_INITIAL);

	private long meshId = 0;

	public static final float colors[][][] = { { { 0, 1, 0, 1 }, { 1, 0, 0, 1 } },
			{ { 1, 0.5f, 0, 1 }, { 0.5f, 0, 1, 1 } }, { { 0.9f, 0.9f, 0.9f, 1 }, { 0.4f, 0.4f, 0.4f, 1 } } };

	// physics engine needs ByteBuffers, so we don't use others for simplicity
	private ByteBuffer chunkShortIndices, chunkIntIndices, chunkVertices, chunkNormals;


	//Serial Methods - called by main loop

	public Chunk(ChunkIndex index) {
		this.index = index;
		this.modifyComplete = null;
	}

	public Chunk(ChunkIndex index, Chunk other, ChunkModifierInterface cm) {
		this.index = index;
		this.modifyComplete  = cm;
		this.modifiedParent = other;
	}

	public boolean serial_render(Camera cam, boolean allowBruteForceRender, boolean display) {
		if (empty)
			return false;

		if (display && cam.frustrumTest(this) == Inclusion.OUTSIDE)
			return false;

		if (displayList >= 0) {
			if (display) {
				GL11.glCallList(displayList);
			}
		} else if (allowBruteForceRender) {
			if (Config.CHUNK_PHYSICS) {
				// register the terrain chunk with the physics engine
				Game.collision.physics.registerMesh(meshId);
			}

			displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, display ? GL11.GL_COMPILE_AND_EXECUTE : GL11.GL_COMPILE);
			GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
			GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
			GL11.glNormalPointer(0, chunkNormals.asFloatBuffer());
			GL11.glVertexPointer(3, 0, chunkVertices.asFloatBuffer());
			GL11.glDrawElements(GL11.GL_TRIANGLES, chunkShortIndices.asShortBuffer());
			GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
			GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
			/*
			GL11.glBegin(GL11.GL_TRIANGLES); // Draw some triangles

			if (!Config.USE_DEBUG_COLORS)
				GL11.glColor3f(0.2f, 0.5f, 0.1f);

			for (int i = 0; i < triangles.size(); i++) {
				if (Config.USE_SMOOTH_SHADE || (i % 3 == 0)) {
					if (Config.USE_AMBIENT_OCCLUSION) {
						float ambOcc = occlusion.get(i);
						GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT,
								Game.makeFB(new float[] { ambOcc, ambOcc, ambOcc, 1 }));
					}
					normals.get(i).GLnormal();
				}
				if (Config.USE_DEBUG_COLORS && (i % 3 == 0)) {
					int subChunkColorIndex = i % 2;
					GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE,
							Game.makeFB(colors[chunkColorIndex][subChunkColorIndex]));
				}
				triangles.get(i).GLdraw();
			}
			GL11.glEnd();*/
			GL11.glEndList();

			return true;

			//if (!state.compareAndSet(PARALLEL_COMPLETE, SERIAL_COMPLETE)) {
			//	System.err.println("Error: in");
			//	System.exit(1);
			//}
		}
		return false;
	}

	public void serial_clean() {
		if (displayList >= 0) {
			GL11.glDeleteLists(displayList, 1);
			displayList = -1;
			if (Config.CHUNK_PHYSICS) {
				if (0 == meshId) {
					System.err.println("improperly initialized body/collision shape");
					System.exit(1);
				}
				Game.collision.physics.unregisterMesh(meshId);
				meshId = 0;
			}
		}

		if (!empty) {
			chunkVertices = null;
			chunkNormals = null;
			chunkIntIndices = null;
			chunkShortIndices = null;
		}

		pos = null;
	}

	//Parallel Methods - called by worker threads

	//per worker temporary buffer data for chunk processing
	private static class WorkerBuffers {
		public float[][][] weights = new float[Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2];
		public float[] vertices = new float[9 * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION];
		public int verticesFloatCount;
		public int[] indices = new int[15 * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION * Config.CHUNK_DIVISION];
		public int indicesIntCount;
		public int[][][][] vertIndexVolume = new int[Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][3];
	}

	public static Object parallel_workerBuffersInit() {
		return new WorkerBuffers();
	}

	private boolean parallel_processCalcWeights(WorkerBuffers buffers) {
		// initialize weights from modifiedParent, if parent was modified
		boolean skipGeneration = false;
		if (modifiedParent != null) {
			float oldWeights[][][] = modifiedParent.getModifiedWeights();
			if (oldWeights != null) {
				buffers.weights = oldWeights;
				skipGeneration = true;
			}
		}

		int posCount = 0, negCount = 0;
		for (int x = 0; x < Config.CHUNK_DIVISION + 1; x++)
			for (int y = 0; y < Config.CHUNK_DIVISION + 1; y++)
				for (int z = 0; z < Config.CHUNK_DIVISION + 1; z++) {
					Vector3f localPos = new Vector3f(pos.getX() + x * Config.METERS_PER_SUBCHUNK, pos.getY() + y
							* Config.METERS_PER_SUBCHUNK, pos.getZ() + z * Config.METERS_PER_SUBCHUNK);

					if (!skipGeneration) {
						// haven't already initialized the weights from parent,
						// so need to call TerrainGenerator
						buffers.weights[x][y][z] = TerrainGenerator.getDensity(localPos);
					}

					if (modifyComplete != null)
						buffers.weights[x][y][z] = modifyComplete.getModification(
								localPos,
								buffers.weights[x][y][z]);

					if (buffers.weights[x][y][z] < 0)
						negCount++;
					else
						posCount++;
				}
		return (negCount != 0) && (posCount != 0); // return false if empty
	}

	private boolean parallel_processCalcGeometry(WorkerBuffers buffers) {
		int x, y, z;
		buffers.indicesIntCount = 0;
		buffers.verticesFloatCount = 0;
		if (Config.CHUNK_REUSE_VERTS) {
			Vec3 blockPos = new Vec3(0, 0, 0);
			for (x = 0; x < Config.CHUNK_DIVISION + 1; x++)
				for (y = 0; y < Config.CHUNK_DIVISION + 1; y++)
					for (z = 0; z < Config.CHUNK_DIVISION + 1; z++)
						if (MarchingCubes.cubeOccupied(x, y, z, buffers.weights)) {
							//calculate vertices, populate vert buffer, vertIndexVolume buffer (NOTE some of these vertices wasted: reside in neighbor chunks)
							blockPos.set(pos.getX() + x * Config.METERS_PER_SUBCHUNK, pos.getY() + y
									* Config.METERS_PER_SUBCHUNK, pos.getZ() + z * Config.METERS_PER_SUBCHUNK);
							buffers.verticesFloatCount = MarchingCubes.writeLocalVertices(blockPos, x, y, z,
									buffers.weights, buffers.vertices, buffers.verticesFloatCount,
									buffers.vertIndexVolume);
						}
			for (x = 0; x < Config.CHUNK_DIVISION; x++)
				for (y = 0; y < Config.CHUNK_DIVISION; y++)
					for (z = 0; z < Config.CHUNK_DIVISION; z++)
						if (MarchingCubes.cubeOccupied(x, y, z, buffers.weights)) {
							//calculate indices
							buffers.indicesIntCount = MarchingCubes.writeLocalIndices(x, y, z, buffers.weights,
									buffers.indices, buffers.indicesIntCount, buffers.vertIndexVolume);
						}
		} else {
			triangles = new ArrayList<Vec3>();
			for (x = 0; x < Config.CHUNK_DIVISION; x++)
				for (y = 0; y < Config.CHUNK_DIVISION; y++)
					for (z = 0; z < Config.CHUNK_DIVISION; z++) {
						if (MarchingCubes.cubeOccupied(x, y, z, buffers.weights)) {
							Vec3 blockPos = new Vec3(pos.getX() + x * Config.METERS_PER_SUBCHUNK, pos.getY() + y
									* Config.METERS_PER_SUBCHUNK, pos.getZ() + z * Config.METERS_PER_SUBCHUNK);
							MarchingCubes.makeMesh(blockPos, x, y, z, buffers.weights, 0.0, triangles);
						}
					}
			buffers.verticesFloatCount = triangles.size() * 3;
			buffers.indicesIntCount = triangles.size();
		}

		if (buffers.verticesFloatCount == 0 || buffers.indicesIntCount == 0)
			return false;

		if (modifyComplete != null) {
			modifyNormals = new HashMap<Integer, Vec3>();
			// manually calculate normals, because we have been modified since generation
			for (int i=0; i<buffers.indicesIntCount; i+=3) {
				Vec3 vectors[] = new Vec3[3];
				for (int j=0; j<3; j++) {
					vectors[j] = new Vec3(
							buffers.vertices[buffers.indices[i+j]+0],
							buffers.vertices[buffers.indices[i+j]+1],
							buffers.vertices[buffers.indices[i+j]+2]);
				}

				Vec3 a = vectors[0].subtract(vectors[1]);
				Vec3 b = vectors[0].subtract(vectors[2]);

				// Note: don't normalize so that bigger polys have more weight
				Vec3 normal = a.cross(b); //.normalize();

				for (int j=0; j<3; j++) {
					int index = buffers.indices[i+j];
					Vec3 normalSumForIndex = modifyNormals.get(index);
					if (normalSumForIndex == null) {
						// COPY normal into hash (since the value in hash will be modified)
						modifyNormals.put(index, new Vec3(normal));
					} else {
						// add into normal (since they're all normalized, we
						// can normalize the sum later to get the average)
						normalSumForIndex.actuallyAdd(normal);
					}
				}
			}
		}
		return true;
	}

	private boolean parallel_processSaveGeometry(WorkerBuffers buffers) {
		chunkVertices = BufferUtils.createByteBuffer(buffers.verticesFloatCount * 4);
		chunkNormals = BufferUtils.createByteBuffer(buffers.verticesFloatCount * 4);
		chunkShortIndices = BufferUtils.createByteBuffer(buffers.indicesIntCount * 2);
		chunkIntIndices = BufferUtils.createByteBuffer(buffers.indicesIntCount * 4);

		if (Config.CHUNK_REUSE_VERTS) {
			Vec3 normal = new Vec3(0,0,0);
			for (int i = 0; i < buffers.verticesFloatCount; i += 3) {
				float vx = buffers.vertices[i + 0];
				float vy = buffers.vertices[i + 1];
				float vz = buffers.vertices[i + 2];

				chunkVertices.putFloat(vx);
				chunkVertices.putFloat(vy);
				chunkVertices.putFloat(vz);

				if (modifyComplete != null && modifyNormals.containsKey(i)) {
					normal = modifyNormals.get(i).normalize();
				} else {
					TerrainGenerator.getNormal(vx, vy, vz, normal);
				}

				chunkNormals.putFloat(normal.getX());
				chunkNormals.putFloat(normal.getY());
				chunkNormals.putFloat(normal.getZ());
			}
			for (int i = 0; i < buffers.indicesIntCount; i++) {
				chunkShortIndices.putShort((short) (buffers.indices[i] / 3));
				chunkIntIndices.putInt(buffers.indices[i] / 3);
			}
		} else {
			Vec3 normal = new Vec3(0,0,0);
			for (int i = 0; i < buffers.indicesIntCount; i++) {
				Vec3 vert = triangles.get(i);
				float vx = vert.getX();
				float vy = vert.getY();
				float vz = vert.getZ();

				chunkVertices.putFloat(vx);
				chunkVertices.putFloat(vy);
				chunkVertices.putFloat(vz);

				TerrainGenerator.getNormal(vx, vy, vz, normal);

				chunkNormals.putFloat(normal.getX());
				chunkNormals.putFloat(normal.getY());
				chunkNormals.putFloat(normal.getZ());

				chunkShortIndices.putShort((short) i);
				chunkIntIndices.putInt(i);
			}
		}

		chunkVertices.flip();
		chunkNormals.flip();
		chunkShortIndices.flip();
		chunkIntIndices.flip();
		return false;
	}

	private void parallel_processSetEmpty(WorkerBuffers buffers) {
		if (!parallel_processCalcWeights(buffers)) {
			// if weights all negative or positive, no geometry
			return;
		}

		if (!parallel_processCalcGeometry(buffers)) {
			// no geometry generated (probably due to rounding issues)
			return;
		}

		// save polys in bytebuffers for rendering/physics
		parallel_processSaveGeometry(buffers);

		parallel_processPhysics();
		empty = false; // flag tells main loop that chunk can be used
	}

	public void parallel_process(Object workerBuffers) {
		if (!state.compareAndSet(SERIAL_INITIAL, PARALLEL_PROCESSING))
			return;

		WorkerBuffers buffers = (WorkerBuffers) workerBuffers;

		pos = this.index.getVec3();
		pos = pos.multiply(Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);

		buffers.verticesFloatCount = 0;
		buffers.indicesIntCount = 0;

		parallel_processSetEmpty(buffers);

		if (!state.compareAndSet(PARALLEL_PROCESSING, PARALLEL_COMPLETE)) {
			System.err.println("Error: Chunk parallel processing interrupted");
			System.exit(1);
		}

		if (modifyComplete == null) {
			// Weights aren't modified, so don't save them since they can be regenerated
			this.modifiedWeights = null;
		} else {
			// Because the weights have been modified, they can't be regenerated
			// from the TerrainGenerator, so we save them.
			this.modifiedWeights = buffers.weights;
			buffers.weights = new float[Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2][Config.CHUNK_DIVISION + 2];

			modifyComplete.chunkCompletion();
		}
	}

	/*
	if (triangles.size() > 0) {
		System.out.println("-- START Small Chunk Splat---");
		for (int i = 0; i < triangles.size(); i++) {
			System.out.printf("ORIG %f,%f,%f    NEW %f,%f,%f\n", triangles.get(i).getX(), triangles.get(i).getY(),
					triangles.get(i).getZ(), buffers.vertices[buffers.indices[i] + 0],
					buffers.vertices[buffers.indices[i] + 1], buffers.vertices[buffers.indices[i] + 2]);
		}
		System.out.println("-- END   Small Chunk Splat---");
	}
	*/
	////

	/*
	normals = new ArrayList<Vec3>(triangles.size());
	if (Config.USE_AMBIENT_OCCLUSION)
		occlusion = new ArrayList<Float>(triangles.size());

	for (int i = 0; i < triangles.size(); i++) {
		if (Config.USE_SMOOTH_SHADE || (i % 3 == 0)) {
			Vec3 p = triangles.get(i);
			if (Config.USE_AMBIENT_OCCLUSION) {
				float visibility = 0;
				for (Vec3 ray : rayDistribution) {
					boolean isOccluded = false;
					for (int step = 1; step < Config.AMB_OCC_BIGRAY_STEPS && !isOccluded; step++) {

						Vec3 rp = p.add(ray.multiply(Config.AMB_OCC_BIGRAY_STEP_SIZE * step));
						if (TerrainGenerator.getDensity(rp.getX(), rp.getY(), rp.getZ()) > 0)
							isOccluded = true;
					}
					if (!isOccluded)
						visibility += 1.0f / Config.AMB_OCC_RAY_COUNT;
				}
				occlusion.add(Math.min(0.1f, 0.4f * visibility - 0.1f));
			}
		} else {
			if (Config.USE_AMBIENT_OCCLUSION)
				occlusion.add(null);
		}
	}
	*/

	private void parallel_processPhysics() {
		if (!Config.CHUNK_PHYSICS)
			return;

		meshId = Game.collision.physics.createMesh(chunkVertices, 4, chunkIntIndices);
		//meshId = Game.collision.physics.createMesh(chunkVertices, 2, chunkShortIndices);
	}

	// parallel OR serial
	public ChunkIndex getChunkIndex() {
		return index;
	}

	@Override
	public Vec3 getVertexP(Vec3 n) {
		Vec3 res = new Vec3(pos);

		if (n.getX() > 0)
			res.addInto(0, Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);
		if (n.getY() > 0)
			res.addInto(1, Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);
		if (n.getZ() > 0)
			res.addInto(2, Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);

		return res;
	}

	@Override
	public Vec3 getVertexN(Vec3 n) {
		Vec3 res = new Vec3(pos);

		if (n.getX() < 0)
			res.addInto(0, Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);
		if (n.getY() < 0)
			res.addInto(1, Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);
		if (n.getZ() < 0)
			res.addInto(2, Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);

		return res;
	}

	public boolean processingIsComplete() {
		return state.get() == PARALLEL_COMPLETE;
	}

	public void cancelParallelProcessing() {
		if (state.compareAndSet(SERIAL_INITIAL, PARALLEL_GARBAGE))
			return;
		while (state.get() != PARALLEL_COMPLETE);
	}

	public float[][][] getModifiedWeights() {
		float ret[][][] = this.modifiedWeights;
		this.modifiedWeights = null;
		return ret;
	}
}
