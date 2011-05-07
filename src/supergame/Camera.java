package supergame;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

public class Camera {
	Vec3 pos;
	private float pitch, heading;
	public static Vec3 forward, right, up;
	
	long msSinceHeartbeat = 0;

	Camera() {
		pos = new Vec3(0, 0, 0);
		pitch = 0;//-40;
		heading = 0;//-222;
		cameraSetup();
		Mouse.setGrabbed(true);
		pl = new Plane[6];
		for (int i = 0; i < 6; i++)
			pl[i] = new Plane();

		//initialize mouse position
		DisplayMode dm = Display.getDisplayMode();
		int height = dm.getHeight();
		int width = dm.getWidth();
		Mouse.setCursorPosition(width / 2, height / 2);
	}

	static enum inclusion {
		OUTSIDE, INTERSECT, INSIDE
	};

	private final int TOP = 0;
	private final int BOTTOM = 1;
	private final int LEFT = 2;
	private final int RIGHT = 3;
	private final int NEARP = 4;
	private final int FARP = 5;
	Plane pl[];
	Vec3 ntl, ntr, nbl, nbr, ftl, ftr, fbl, fbr;
	private final static float angle = 40.0f;
	private final static float ratio = 1;
	private final static float nearD = 2;
	private final static float farD = 60;
	private float tang, nh, nw, fh, fw;

	public void cameraSetup() {
		tang = (float) Math.tan(Math.PI / 180.0 * angle * 0.5);
		nh = nearD * tang;
		nw = nh * ratio;
		fh = farD * tang;
		fw = fh * ratio;
	}

	private class Plane {
		Vec3 normal, point;

		public void setNormalAndPoint(Vec3 normal, Vec3 point) {
			this.normal = normal;
			this.point = point;
		}

		public String toString() {
			return "Normal:" + normal + ", point:" + point;
		}
		
		public void draw() {

			GL11.glBegin(GL11.GL_TRIANGLES); // Draw some triangles

			Vec3 a,b,c;
			a = this.point;
			b = Camera.up;
			c = a.cross(b).normalize();
			
			b.multiply(10);
			c.multiply(10);
			
			b = b.add(a);
			c = c.add(a);
			
			a.GLdraw();
			c.GLdraw();
			b.GLdraw();

			if (Game.heartbeatFrame) {
				System.out.println(a);
				System.out.println(b);
				System.out.println(c);
			}
			GL11.glEnd();

		}
	}

	public void updateFrustum() {
		Vec3 dir, nc, fc;

		Vec3 Z = forward = new Vec3(heading, pitch);
		Vec3 negZ = new Vec3(-Z.getX(), -Z.getY(), -Z.getZ());
		Vec3 X = right = new Vec3(heading + 90, pitch);
		Vec3 Y = up = new Vec3(heading, pitch - 90);

		// compute the centers of the near and far planes
		nc = pos.subtract(Z.multiply(nearD));
		fc = pos.subtract(Z.multiply(farD));

		pl[NEARP].setNormalAndPoint(negZ, nc);
		pl[FARP].setNormalAndPoint(Z, fc);

		Vec3 aux, normal;
		aux = (nc.add(Y.multiply(nh)).subtract(pos)).normalize();
		pl[TOP].setNormalAndPoint(aux.cross(X), nc.add(Y.multiply(nh)));
		aux = (nc.subtract(Y.multiply(nh)).subtract(pos)).normalize();
		pl[BOTTOM].setNormalAndPoint(X.cross(aux), nc.subtract(Y.multiply(nh)));

		aux = (nc.subtract(X.multiply(nw)).subtract(pos)).normalize();
		pl[LEFT].setNormalAndPoint(aux.cross(Y), nc.add(X.multiply(nw)));
		aux = (nc.add(X.multiply(nw)).subtract(pos)).normalize();
		pl[RIGHT].setNormalAndPoint(Y.cross(aux), nc.add(X.multiply(nw)));

		if (Game.heartbeatFrame) {
			System.out.println(Y.multiply(nh));
			System.out.println(pl[NEARP]);
			System.out.println(pl[FARP]);
			
			/*
			System.out.println(pl[TOP]);
			System.out.println(pl[BOTTOM]);
			System.out.println(pl[LEFT]);
			System.out.println(pl[RIGHT]);
			*/
		}
		/*
		aux = (nc + Y*nh) - p;
		aux.normalize();
		normal = aux * X;
		pl[TOP].setNormalAndPoint(normal,nc+Y*nh);

		aux = (nc - Y*nh) - p;
		aux.normalize();
		normal = X * aux;
		pl[BOTTOM].setNormalAndPoint(normal,nc-Y*nh);
				
		aux = (nc - X*nw) - p;
		aux.normalize();
		normal = aux * Y;
		pl[LEFT].setNormalAndPoint(normal,nc-X*nw);

		aux = (nc + X*nw) - p;
		aux.normalize();
		normal = Y * aux;
		pl[RIGHT].setNormalAndPoint(normal,nc+X*nw);
		*/
	}

	public void pollInput() {
		float speed = (float) (0.01 * Game.delta);

		Vec3 deltaPos = new Vec3(heading, pitch).multiply(speed);

		if (Mouse.isButtonDown(0))
			pos = pos.add(deltaPos);
		if (Mouse.isButtonDown(1))
			pos = pos.subtract(deltaPos);

		DisplayMode dm = Display.getDisplayMode();
		int height = dm.getHeight();
		int width = dm.getWidth();

		// System.out.println("x move:"+(Mouse.getX()-width/2)+",y move:"+(Mouse.getY()-height/2));

		if (msSinceHeartbeat != 0) {
			heading += (Mouse.getX() - width / 2) / 10.0;
			pitch -= (Mouse.getY() - height / 2) / 10.0;
		}

		pitch = Math.min(pitch, 90);
		pitch = Math.max(pitch, -90);

		// System.out.println("pitch:"+Math.sin(pitch*Math.PI/180.0)+", heading:"+Math.sin(heading*Math.PI/180.0));
		Mouse.setCursorPosition(width / 2, height / 2);

		msSinceHeartbeat += Game.delta;
		if (msSinceHeartbeat > 1000) {
			System.out.println(pos.toString());
			System.out.println("pitch:" + pitch + ", heading:" + heading);
			msSinceHeartbeat = 0;
		}

		updateFrustum();
	}

	public boolean frustrumTest(Vec3 point) {
		/*
		for (int i = 0; i<6 ; i++) {
			if (pl[i].distance(point) < 0)
				return false;
		}*/
		return true;
	}

	public void apply() {
		GL11.glRotatef(pitch, 1, 0, 0);
		GL11.glRotatef(heading, 0, 1, 0);
		GL11.glTranslatef(pos.getX(), pos.getY(), pos.getZ());
		

		pl[FARP].draw();
		//pl[NEARP].draw();
		pl[LEFT].draw();
		pl[RIGHT].draw();
	}
}
