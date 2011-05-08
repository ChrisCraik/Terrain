package supergame;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;

import supergame.Camera.Frustrumable;
import supergame.Camera.Inclusion;

public class Chunk implements Comparable<Chunk>, Frustrumable {
	public static final boolean USE_DEBUG_COLORS = false;
	public static final int size = 8;
	public static final float metersPerCube = 1;
	//public static final float MPC = metersPerCube;

	private boolean initialized = false;
	private boolean empty = true;
	private ArrayList<Vec3> triangles;
	private float[][][] weights;
	private int displayList;

	private long xid, yid, zid;
	private Vec3 pos;

	public Vec3 getVertexP(Vec3 n) {
		Vec3 res = new Vec3(pos);

		if (n.getX() > 0)
			res.addInto(0, size * metersPerCube);
		if (n.getY() > 0)
			res.addInto(1, size * metersPerCube);
		if (n.getZ() > 0)
			res.addInto(2, size * metersPerCube);

		return res;
	}

	public Vec3 getVertexN(Vec3 n) {
		Vec3 res = new Vec3(pos);

		if (n.getX() < 0)
			res.addInto(0, size * metersPerCube);
		if (n.getY() < 0)
			res.addInto(1, size * metersPerCube);
		if (n.getZ() < 0)
			res.addInto(2, size * metersPerCube);

		return res;
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
	}

	public void initialize() { //todo: split into initialization-done once and baking-may need to be redone
		assert initialized == false;

		initialized = true;

		weights = new float[size + 1][size + 1][size + 1];
		triangles = new ArrayList<Vec3>();

		// pos is the cube's origin
		pos = new Vec3(this.xid, this.yid, this.zid);
		pos = pos.multiply(size * metersPerCube);

		// cache weights 
		int x, y, z;
		for (x = 0; x < size + 1; x++)
			for (y = 0; y < size + 1; y++)
				for (z = 0; z < size + 1; z++) {
					weights[x][y][z] = getDensity(pos.getX() + x * metersPerCube, pos.getY() + y * metersPerCube,
							pos.getZ() + z * metersPerCube);
				}

		// create polys
		for (x = 0; x < size; x++)
			for (y = 0; y < size; y++)
				for (z = 0; z < size; z++) {
					Vec3 blockPos = new Vec3(pos.getX() + x * metersPerCube, pos.getY() + y * metersPerCube, pos.getZ()
							+ z * metersPerCube);
					MarchingCubes.makeMesh(blockPos, x, y, z, weights, 0.0f, triangles);// (float) noise);
				}

		if (triangles.size() == 0) {
			triangles = null;
			weights = null; //save memory on 'empty' chunks?
			empty = true;
		} else {
			//System.out.println(xid + " " + yid + " " + zid + " has " + triangles.size() / 3 + " polys");
			displayList = -1;
			empty = false;
		}
		initialized = true;
	}

	public static final float colors[][][] = { { { 0, 1, 0 }, { 1, 0, 0 } }, { { 1, 0.5f, 0 }, { 0.5f, 0, 1 } },
			{ { 0.9f, 0.9f, 0.9f }, { 0.4f, 0.4f, 0.4f } } };

	public void render(Camera cam) {
		if (empty)
			return;

		Inclusion FrustumInclusion = cam.frustrumTest(this);

		if (FrustumInclusion == Inclusion.OUTSIDE)
			return;

		if (displayList == -1) {
			int chunkColorIndex = (int) ((xid + yid + zid) % 2);

			displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE_AND_EXECUTE);
			GL11.glBegin(GL11.GL_TRIANGLES); // Draw some triangles
			if (!USE_DEBUG_COLORS)
				GL11.glColor3f(0.2f, 0.5f, 0.1f);
			for (int i = 0; i < triangles.size(); i += 3) {
				if (USE_DEBUG_COLORS) {
					int subChunkColorIndex = i % 2;
					GL11.glColor3f(colors[chunkColorIndex][subChunkColorIndex][0],
							colors[chunkColorIndex][subChunkColorIndex][1],
							colors[chunkColorIndex][subChunkColorIndex][2]);
				}
				Vec3[] points = new Vec3[] { triangles.get(i), triangles.get(i + 1), triangles.get(i + 2) };

				//register the normal
				Vec3 a = points[2].subtract(points[0]);
				Vec3 b = points[1].subtract(points[0]);
				Vec3 norm = (b.cross(a)).normalize();
				norm.GLnormal();

				for (Vec3 p : points)
					p.GLdraw();
			}
			GL11.glEnd();
			GL11.glEndList();
		} else
			GL11.glCallList(displayList);
	}

	@Override
	public int compareTo(Chunk b) {
		//TODO: prioritize near camera (or better: in frustum too)
		return (int) ((xid * xid + yid * yid + zid * zid) - (b.xid * b.xid + b.yid * b.yid + b.zid * b.zid));
	}
}
