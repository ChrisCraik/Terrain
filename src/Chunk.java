import java.util.ArrayList;
import org.lwjgl.opengl.GL11;

public class Chunk implements Comparable<Chunk> {
	private static final int size = 8;
	private static final float metersPerCube = 1;
	public static final float MPC = metersPerCube;

	private boolean initialized = false;
	private ArrayList<Vec3> triangles;
	private float[][][] weights;

	private long xid, yid, zid;
	public Vec3 pos;

	private float getDensity(float x, float y, float z) {
		return (float) Perlin.noise(x / 10, y / 10, z / 10);
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
					MarchingCubes.makeMesh(blockPos, x, y, z, weights, 0.25f, triangles);// (float) noise);
				}

		if (triangles.size() == 0) {
			triangles = null;
			weights = null; //save memory on 'empty' chunks?
		} else {
			System.out.println(xid + " " + yid + " " + zid + " has " + triangles.size() / 3 + " polys");
		}
		initialized = true;
	}

	private static final float colors[][][] = { { { 0, 1, 0 }, { 1, 0, 0 } }, { { 1, 0.5f, 0 }, { 0.5f, 0, 1 } }, { { 0.9f, 0.9f, 0.9f }, { 0.5f, 0.5f, 0.5f } } };

	public void render(Camera cam) {
		if (triangles == null)
			return;

		if ((cam.pos.add(pos)).length() > 100) // NOTE: CAMERA POSITIONS ARE NEGATIVE
			return;

		int chunkColorIndex = (int) ((xid + yid + zid) % 2);
		
		for (int i = 0; i < triangles.size(); i += 3) {
			int subChunkColorIndex = i % 2;
			GL11.glColor3f(colors[chunkColorIndex][subChunkColorIndex][0],
					colors[chunkColorIndex][subChunkColorIndex][1], colors[chunkColorIndex][subChunkColorIndex][2]);
			triangles.get(i).GLdraw();
			triangles.get(i + 1).GLdraw();
			triangles.get(i + 2).GLdraw();
		}
	}

	@Override
	public int compareTo(Chunk b) {
		return (int) ((xid * xid + yid * yid + zid * zid) - (b.xid * b.xid + b.yid * b.yid + b.zid * b.zid));
	}
}
