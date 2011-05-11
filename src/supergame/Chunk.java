package supergame;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;

import supergame.Camera.Frustrumable;
import supergame.Camera.Inclusion;

public class Chunk implements Frustrumable {
	public static Vec3[] rayDistribution;

	private boolean initialized = false;
	private boolean empty = true;
	private ArrayList<Vec3> triangles;
	private ArrayList<Vec3> normals;
	private ArrayList<Float> occlusion;
	private float[][][] weights;
	private int displayList;

	private long xid, yid, zid;
	private Vec3 pos;

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

	private Vec3 getNormal(float x, float y, float z) {
		//first attempt: brute force
		float delta = 0.01f, origin, deltaX, deltaY, deltaZ;
		origin = getDensity(x, y, z);
		deltaX = origin - getDensity(x + delta, y, z);
		deltaY = origin - getDensity(x, y + delta, z);
		deltaZ = origin - getDensity(x, y, z + delta);

		return new Vec3(deltaX, deltaY, deltaZ).normalize();
	}

	private float getDensity(float x, float y, float z) {
		return (float) Perlin.noise(x / 10, y / 10, z / 10) + 1.0f - y * 0.06f;
		/*
		float caves, center_falloff, plateau_falloff, density;
		if (y <= 0.8)
			plateau_falloff = 1.0f;
		else if (0.8 < y && y < 0.9)
			plateau_falloff = 1.0f - (y - 0.8f) * 10;
		else
			plateau_falloff = 0.0f;

		center_falloff = (float) (0.1 / (Math.pow((x - 0.5) * 1.5, 2) + Math.pow((y - 1.0) * 0.8, 2) + Math.pow(
				(z - 0.5) * 1.5, 2)));
		caves = (float) Math.pow(Perlin.octave_noise(1, x * 5, y * 5, z * 5), 3);
		density = (float) (Perlin.octave_noise(5, x, y * 0.5, z) * center_falloff * plateau_falloff);
		density *= Math.pow(Perlin.noise((x + 1) * 3.0, (y + 1) * 3.0, (z + 1) * 3.0) + 0.4, 1.8);
		if (caves < 0.5)density = 0;
		return density;
		**/
	}

	Chunk(long xid, long yid, long zid) {
		this.xid = xid;
		this.yid = yid;
		this.zid = zid;
		displayList = -1;
	}

	public void initialize() { //todo: split into initialization-done once and baking-may need to be redone
		assert initialized == false;

		initialized = true;

		weights = new float[Config.CHUNK_DIVISION + 1][Config.CHUNK_DIVISION + 1][Config.CHUNK_DIVISION + 1];
		triangles = new ArrayList<Vec3>();

		// pos is the cube's origin
		pos = new Vec3(this.xid, this.yid, this.zid);
		pos = pos.multiply(Config.CHUNK_DIVISION * Config.METERS_PER_SUBCHUNK);

		// cache weights 
		int x, y, z;
		for (x = 0; x < Config.CHUNK_DIVISION + 1; x++)
			for (y = 0; y < Config.CHUNK_DIVISION + 1; y++)
				for (z = 0; z < Config.CHUNK_DIVISION + 1; z++) {
					weights[x][y][z] = getDensity(pos.getX() + x * Config.METERS_PER_SUBCHUNK, pos.getY() + y
							* Config.METERS_PER_SUBCHUNK, pos.getZ() + z * Config.METERS_PER_SUBCHUNK);
				}

		// create polys
		for (x = 0; x < Config.CHUNK_DIVISION; x++)
			for (y = 0; y < Config.CHUNK_DIVISION; y++)
				for (z = 0; z < Config.CHUNK_DIVISION; z++) {
					Vec3 blockPos = new Vec3(pos.getX() + x * Config.METERS_PER_SUBCHUNK, pos.getY() + y
							* Config.METERS_PER_SUBCHUNK, pos.getZ() + z * Config.METERS_PER_SUBCHUNK);
					MarchingCubes.makeMesh(blockPos, x, y, z, weights, 0.0f, triangles);// (float) noise);
				}

		if (triangles.size() == 0) {
			triangles = null;
			weights = null; //save memory on 'empty' chunks?
			empty = true;
		} else {
			//System.out.println(xid + " " + yid + " " + zid + " has " + triangles.size() / 3 + " polys");
			empty = false;
			normals = new ArrayList<Vec3>(triangles.size());
			occlusion = new ArrayList<Float>(triangles.size());

			for (int i = 0; i < triangles.size(); i++) {
				if (Config.USE_SMOOTH_SHADE || (i % 3 == 0)) {
					Vec3 p = triangles.get(i);
					//for (Vec3 p : triangles) {
					if (Config.USE_AMBIENT_OCCLUSION) {
						float visibility = 0;
						for (Vec3 ray : rayDistribution) {
							boolean isOccluded = false;
							for (int step = 1; step < Config.AMB_OCC_BIGRAY_STEPS && !isOccluded; step++) {
								Vec3 rp = p.add(ray.multiply(Config.AMB_OCC_BIGRAY_STEP_SIZE * step));
								if (getDensity(rp.getX(), rp.getY(), rp.getZ()) > 0)
									isOccluded = true;
							}
							if (!isOccluded)
								visibility += 1.0f / Config.AMB_OCC_RAY_COUNT;
						}
						occlusion.add(Math.min(0.1f, 0.4f * visibility - 0.1f));
					}
					normals.add(getNormal(p.getX(), p.getY(), p.getZ()));
				} else {
					occlusion.add(null);
					normals.add(null);
				}
			}

		}
		initialized = true;
	}

	public static final float colors[][][] = { { { 0, 1, 0 }, { 1, 0, 0 } }, { { 1, 0.5f, 0 }, { 0.5f, 0, 1 } },
			{ { 0.9f, 0.9f, 0.9f }, { 0.4f, 0.4f, 0.4f } } };

	public boolean render(Camera cam, boolean allowBruteForceRender) {
		if (empty)
			return false;

		Inclusion FrustumInclusion = cam.frustrumTest(this);

		if (FrustumInclusion == Inclusion.OUTSIDE)
			return false;

		if (displayList >= 0) {
			GL11.glCallList(displayList);
		} else if (allowBruteForceRender) {
			int chunkColorIndex = (int) ((xid + yid + zid) % 2);

			displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE_AND_EXECUTE);
			GL11.glBegin(GL11.GL_TRIANGLES); // Draw some triangles
			if (!Config.USE_DEBUG_COLORS)
				GL11.glColor3f(0.2f, 0.5f, 0.1f);

			for (int i = 0; i < triangles.size(); i++) {
				if (Config.USE_DEBUG_COLORS && (i % 3 == 0)) {
					int subChunkColorIndex = i % 2;
					GL11.glColor3f(colors[chunkColorIndex][subChunkColorIndex][0],
							colors[chunkColorIndex][subChunkColorIndex][1],
							colors[chunkColorIndex][subChunkColorIndex][2]);
				}

				if (Config.USE_SMOOTH_SHADE || (i % 3 == 0)) {
					if (Config.USE_AMBIENT_OCCLUSION) {
						float ambOcc = occlusion.get(i);
						GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT,
								Game.makeFB(new float[] { ambOcc, ambOcc, ambOcc, 1 }));
					}
					normals.get(i).GLnormal();
				}
				triangles.get(i).GLdraw();
			}
			GL11.glEnd();
			GL11.glEndList();
			return true;
		}
		return false;
	}
}
