package supergame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Vector3f;

import org.lwjgl.opengl.GL11;

import supergame.Camera.Frustrumable;
import supergame.Camera.Inclusion;
import supergame.modify.ChunkModifierInterface;

public class Chunk implements Frustrumable {
	public static Vec3[] rayDistribution;

	private boolean empty = true;
	private int displayList = -1;

	private ArrayList<Vec3> triangles;

	private ChunkIndex index;
	private Vec3 pos; //cube's origin (not center)

	private float modifiedWeights[][][];
	private Chunk modifiedParent;
	private final ChunkModifierInterface modifyComplete;

	private static final int SERIAL_INITIAL = 0; // Chunk allocated, not yet processed
	private static final int PARALLEL_PROCESSING = 1; // being processed by worker thread
	private static final int PARALLEL_COMPLETE = 2; // Processing complete, render-able
	private static final int PARALLEL_GARBAGE = 3; // Processing interrupted by chunk garbage collection
	private AtomicInteger state;

	private long meshId = 0;

	public static final float colors[][][] = { { { 0, 1, 0, 1 }, { 1, 0, 0, 1 } },
			{ { 1, 0.5f, 0, 1 }, { 0.5f, 0, 1, 1 } }, { { 0.9f, 0.9f, 0.9f, 1 }, { 0.4f, 0.4f, 0.4f, 1 } } };

	private ByteBuffer chunkShortIndices, chunkIntIndices, chunkVertices, chunkNormals;


	//Serial Methods - called by main loop

	public Chunk(ChunkIndex index) {
		this.index = index;
		this.state = new AtomicInteger(SERIAL_INITIAL);
		this.modifyComplete = null;
	}

	public Chunk(ChunkIndex index, Chunk other, ChunkModifierInterface cm) {
		// TODO: copy weights from other to here, so that modifications can be
		// further modified. Two ways of doing this - always save weights so
		// that modification is faster, or only save weights when a chunk is
		// first modified.
		this.index = index;
		this.state = new AtomicInteger(SERIAL_INITIAL);
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
						buffers.weights[x][y][z] += modifyComplete.getModification(localPos);

					if (buffers.weights[x][y][z] < 0)
						negCount++;
					else
						posCount++;
				}
		return (negCount == 0) || (posCount == 0); // return true if empty
	}

	private void parallel_processCalcGeometry(WorkerBuffers buffers) {
		int x, y, z;
		buffers.indicesIntCount = 0;
		buffers.verticesFloatCount = 0;
		if (Config.CHUNK_REUSE_VERTS) {
			for (x = 0; x < Config.CHUNK_DIVISION + 1; x++)
				for (y = 0; y < Config.CHUNK_DIVISION + 1; y++)
					for (z = 0; z < Config.CHUNK_DIVISION + 1; z++)
						if (MarchingCubes.cubeOccupied(x, y, z, buffers.weights)) {
							//calculate vertices, populate vert buffer, vertIndexVolume buffer (NOTE some of these vertices wasted: reside in neighbor chunks)
							Vec3 blockPos = new Vec3(pos.getX() + x * Config.METERS_PER_SUBCHUNK, pos.getY() + y
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
	}

	private boolean parallel_processSaveGeometry(WorkerBuffers buffers) {
		chunkVertices = ByteBuffer.allocateDirect(buffers.verticesFloatCount * 4).order(ByteOrder.nativeOrder());
		chunkNormals = ByteBuffer.allocateDirect(buffers.verticesFloatCount * 4).order(ByteOrder.nativeOrder());
		chunkShortIndices = ByteBuffer.allocateDirect(buffers.indicesIntCount * 2).order(ByteOrder.nativeOrder());
		chunkIntIndices = ByteBuffer.allocateDirect(buffers.indicesIntCount * 4).order(ByteOrder.nativeOrder());

		if (Config.CHUNK_REUSE_VERTS) {
			for (int i = 0; i < buffers.verticesFloatCount; i += 3) {
				float vx = buffers.vertices[i + 0];
				float vy = buffers.vertices[i + 1];
				float vz = buffers.vertices[i + 2];

				chunkVertices.putFloat(vx);
				chunkVertices.putFloat(vy);
				chunkVertices.putFloat(vz);

				Vec3 normal = TerrainGenerator.getNormal(vx, vy, vz);

				chunkNormals.putFloat(normal.getX());
				chunkNormals.putFloat(normal.getY());
				chunkNormals.putFloat(normal.getZ());
			}
			for (int i = 0; i < buffers.indicesIntCount; i++) {
				chunkShortIndices.putShort((short) (buffers.indices[i] / 3));
				chunkIntIndices.putInt(buffers.indices[i] / 3);
			}
		} else {
			for (int i = 0; i < buffers.indicesIntCount; i++) {
				Vec3 vert = triangles.get(i);
				float vx = vert.getX();
				float vy = vert.getY();
				float vz = vert.getZ();

				chunkVertices.putFloat(vx);
				chunkVertices.putFloat(vy);
				chunkVertices.putFloat(vz);

				Vec3 normal = TerrainGenerator.getNormal(vx, vy, vz);

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

	public void parallel_process(Object workerBuffers) {
		if (!state.compareAndSet(SERIAL_INITIAL, PARALLEL_PROCESSING))
			return;

		WorkerBuffers buffers = (WorkerBuffers) workerBuffers;

		pos = this.index.getVec3();
		pos = pos.multiply(Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);

		buffers.verticesFloatCount = 0;
		buffers.indicesIntCount = 0;

		// cache weights
		boolean isEmpty = parallel_processCalcWeights(buffers);

		if (!isEmpty) {
			// create polys
			parallel_processCalcGeometry(buffers);

			// save polys in bytebuffers for rendering/physics
			parallel_processSaveGeometry(buffers);

			parallel_processPhysics();
			empty = false; // flag tells main loop that chunk can be used
		}

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
