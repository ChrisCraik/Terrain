package supergame;

import javax.vecmath.Vector3f;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

public class Camera {
	private Vector3f pos;
	private float pitch, heading;
	public Vec3 forward, right, up;

	long msSinceHeartbeat = 0;

	public interface CameraControllable {
		void setHeadingPitch(float heading, float pitch);
		Vector3f getPos();
	}
	
	private CameraControllable controllable;
	Camera(CameraControllable controllable) {
		pos = new Vector3f(15, 40, 8);
		pitch = -20;
		heading = 200;
		cameraSetup();
		Mouse.setGrabbed(true);
		pl = new Plane[6];
		for (int i = 0; i < 6; i++)
			pl[i] = new Plane();
		
		this.controllable = controllable;

		//initialize mouse position
		DisplayMode dm = Display.getDisplayMode();
		int height = dm.getHeight();
		int width = dm.getWidth();
		Mouse.setCursorPosition(width / 2, height / 2);
	}

	private final int TOP = 0;
	private final int BOTTOM = 1;
	private final int LEFT = 2;
	private final int RIGHT = 3;
	private final int NEARP = 4;
	private final int FARP = 5;
	static Plane pl[];
	Vec3 ntl, ntr, nbl, nbr, ftl, ftr, fbl, fbr;
	public final static float angle = 60.0f;
	private final static float ratio = 1;
	private final static float nearD = 0.1f;
	public final static float farD = 150;
	private float tang, nh, nw;

	public void cameraSetup() {
		tang = (float) Math.tan(Math.PI / 180.0 * angle * 0.7);
		nh = nearD * tang;
		nw = nh * ratio;
	}

	private class Plane {
		Vec3 normal, point;
		float offset;

		public void setNormalAndPoint(Vec3 normal, Vec3 point) {
			this.normal = normal.normalize();
			this.point = point;
			this.offset = -this.normal.innerProduct(this.point);
		}

		public String toString() {
			return "Normal:" + normal + ", point:" + point + ", d=" + offset;
		}

		public float distance(Vec3 p) {
			return this.offset + this.normal.innerProduct(p);
		}

		public void draw(float[] color) {
			GL11.glBegin(GL11.GL_TRIANGLES); // Draw some triangles

			Vec3 a, b, c, d;

			a = new Vec3(0, 0, 0);
			b = new Vec3(0,1,0).cross(this.normal); // relative horizontal vector
			c = b.cross(this.normal);
			d = b.add(c);

			a = a.add(this.point);
			b = b.multiply(5).add(this.point);
			c = c.multiply(5).add(this.point);
			d = d.multiply(5).add(this.point);

			GL11.glColor3f(color[0], color[1], color[2]);

			a.GLdraw();
			GL11.glColor3f(0.1f, 0.1f, 0.1f);
			c.GLdraw();
			GL11.glColor3f(color[0], color[1], color[2]);
			b.GLdraw();

			d.GLdraw();
			c.GLdraw();
			b.GLdraw();

			if (Game.heartbeatFrame) {
				System.out.println(a);
				System.out.println(b);
				System.out.println(c);
				System.out.println(d);
			}
			GL11.glEnd();
		}
	}

	public void updateFrustum() {
		Vec3 nc, fc;

		float heading = 0, pitch = 0;
		Vec3 offset = new Vec3(0, 0, 0);
		if (!Config.FROZEN_FRUSTUM_HEAD)
			heading = this.heading;
		if (!Config.FROZEN_FRUSTUM_PITCH)
			pitch = this.pitch;
		if (!Config.FROZEN_FRUSTUM_POS)
			offset = new Vec3(this.pos.x,this.pos.y,this.pos.z);

		Vec3 Z = forward = new Vec3(-heading, -pitch);
		Vec3 negZ = forward.multiply(-1);
		Vec3 X = right = new Vec3(-heading + 90, 0);
		Vec3 Y = up = new Vec3(-heading, -pitch - 90);

		// compute the centers of the near and far planes
		nc = Z.multiply(-nearD).add(offset);
		fc = Z.multiply(-farD).add(offset);

		pl[NEARP].setNormalAndPoint(negZ, nc);
		pl[FARP].setNormalAndPoint(Z, fc);

		Vec3 aux;
		aux = (nc.add(Y.multiply(nh)).subtract(offset)).normalize();
		pl[TOP].setNormalAndPoint(aux.cross(X), nc.add(Y.multiply(nh)));
		aux = (nc.subtract(Y.multiply(nh)).subtract(offset)).normalize();
		pl[BOTTOM].setNormalAndPoint(X.cross(aux), nc.subtract(Y.multiply(nh)));

		aux = (nc.subtract(X.multiply(nw)).subtract(offset)).normalize();
		pl[LEFT].setNormalAndPoint(aux.cross(Y), nc.subtract(X.multiply(nw)));
		aux = (nc.add(X.multiply(nw)).subtract(offset)).normalize();
		pl[RIGHT].setNormalAndPoint(Y.cross(aux), nc.add(X.multiply(nw)));
	}

	public void pollInput() {
		float speed = (float) (0.01 * Game.delta);

		Vec3 deltaPos = new Vec3(-heading, -pitch).multiply(speed);
		Vector3f d = new Vector3f(deltaPos.getX(), deltaPos.getY(), deltaPos.getZ());
		if (Mouse.isButtonDown(0))
			pos.sub(d);
		if (Mouse.isButtonDown(1))
			pos.add(d);
		
		DisplayMode dm = Display.getDisplayMode();
		int height = dm.getHeight();
		int width = dm.getWidth();

		// System.out.println("x move:"+(Mouse.getX()-width/2)+",y move:"+(Mouse.getY()-height/2));

		if (msSinceHeartbeat != 0) {
			heading -= (Mouse.getX() - width / 2) / 10.0;
			pitch += (Mouse.getY() - height / 2) / 10.0;
		}

		pitch = Math.min(pitch, 90);
		pitch = Math.max(pitch, -90);

		controllable.setHeadingPitch(heading, pitch);
		
		// System.out.println("pitch:"+Math.sin(pitch*Math.PI/180.0)+", heading:"+Math.sin(heading*Math.PI/180.0));
		Mouse.setCursorPosition(width / 2, height / 2);

		msSinceHeartbeat += Game.delta;
		if (msSinceHeartbeat > 1000) {
			System.out.println("pos:" + pos + ", pitch:" + pitch + ", heading:" + heading);
			msSinceHeartbeat = 0;
		}
	}

	public static enum Inclusion {
		OUTSIDE, INTERSECT, INSIDE
	};

	interface Frustrumable {
		Vec3 getVertexP(Vec3 n);
		Vec3 getVertexN(Vec3 n);
	}

	public Inclusion frustrumTest(Frustrumable f) {
		Inclusion incl = Inclusion.INSIDE;

		if (!Config.FRUSTUM_CULLING)
			return incl;

		for (int i = 0; i < 6; i++) {
			Vec3 p = f.getVertexP(pl[i].normal);
			float dist = pl[i].distance(p);

			if (dist < 0)
				return Inclusion.OUTSIDE;

			p = f.getVertexN(pl[i].normal);
			dist = pl[i].distance(p);

			if (dist < 0)
				incl = Inclusion.INTERSECT;
		}
		return incl;
	}

	public void apply() {
		pos = controllable.getPos();
		updateFrustum();
		
		GL11.glRotatef(pitch, -1, 0, 0);
		GL11.glRotatef(heading, 0, -1, 0);
		GL11.glTranslatef(-pos.x, -pos.y, -pos.z);
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, Game.makeFB(new float[] { 0, 1, 0, 0 })); // from above!
	}
}
