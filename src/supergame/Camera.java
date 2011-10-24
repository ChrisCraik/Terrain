package supergame;

import javax.vecmath.Vector3f;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

public class Camera {
	private final Vector3f mPosition;
	private float mPitchAngle = -64, mHeadingAngle = 56;
	private CameraControllable mControllable;
	private final Plane mFrustumPlanes[];

	public interface CameraControllable {
		void setHeadingPitch(float heading, float pitch);
		void getPos(Vector3f pos);
	}

	Camera() {
		mPosition = new Vector3f(15, 40, 8);
		Mouse.setGrabbed(true);
		mFrustumPlanes = new Plane[6];
		for (int i = 0; i < 6; i++)
			mFrustumPlanes[i] = new Plane();

		//initialize mouse position
		DisplayMode dm = Display.getDisplayMode();
		int height = dm.getHeight();
		int width = dm.getWidth();
		Mouse.setCursorPosition(width / 2, height / 2);
	}

	public void setControllable(CameraControllable controllable) {
		mControllable = controllable;
	}

	private final int TOP = 0;
	private final int BOTTOM = 1;
	private final int LEFT = 2;
	private final int RIGHT = 3;
	private final int NEARP = 4;
	private final int FARP = 5;

	public final static float FOV = 60.0f;
	private final static float ratio = 1;
	private final static float nearD = 0.1f;
	public final static float farD = 150;
	private final float tang = (float) Math.tan(Math.PI / 180.0 * FOV * 0.7);
	private final float nh = nearD * tang;
	private final float nw = nh * ratio;

	private class Plane {
		Vec3 mNormal, mPoint;
		float mOffset;

		public void setNormalAndPoint(Vec3 normal, Vec3 point) {
			this.mNormal = normal.normalize();
			this.mPoint = point;
			this.mOffset = -this.mNormal.innerProduct(this.mPoint);
		}

		@Override
		public String toString() {
			return "Normal:" + mNormal + ", point:" + mPoint + ", d=" + mOffset;
		}

		public float distance(Vec3 p) {
			return this.mOffset + this.mNormal.innerProduct(p);
		}

		@SuppressWarnings("unused")
		public void draw(float[] color) {
			GL11.glBegin(GL11.GL_TRIANGLES); // Draw some triangles

			Vec3 a, b, c, d;

			a = new Vec3(0, 0, 0);
			b = new Vec3(0, 1, 0).cross(this.mNormal); // relative horizontal vector
			c = b.cross(this.mNormal);
			d = b.add(c);

			a = a.add(this.mPoint);
			b = b.multiply(5).add(this.mPoint);
			c = c.multiply(5).add(this.mPoint);
			d = d.multiply(5).add(this.mPoint);

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

	/**
	 * This function updates the 6 planes that define the limits of the camera's
	 * view frustum. If a chunk (or anything else, for that matter) lies in the
	 * region boxed off by these 6 planes, it is potentially visible and should
	 * be rendered.
	 *
	 * TODO: rewrite without the use of object allocations
	 */
	public void calculateViewFrustum() {
		Vec3 nc, fc;

		float heading = 0, pitch = 0;
		Vec3 offset = new Vec3(0, 0, 0);
		if (!Config.FROZEN_FRUSTUM_HEAD)
			heading = this.mHeadingAngle;
		if (!Config.FROZEN_FRUSTUM_PITCH)
			pitch = this.mPitchAngle;
		if (!Config.FROZEN_FRUSTUM_POS)
			offset.set(this.mPosition.x, this.mPosition.y, this.mPosition.z);

		Vec3 Z = new Vec3(-heading, -pitch);
		Vec3 negZ = Z.multiply(-1);
		Vec3 X = new Vec3(-heading + 90, 0);
		Vec3 Y = new Vec3(-heading, -pitch - 90);

		// compute the centers of the near and far planes
		nc = Z.multiply(-nearD).add(offset);
		fc = Z.multiply(-farD).add(offset);

		mFrustumPlanes[NEARP].setNormalAndPoint(negZ, nc);
		mFrustumPlanes[FARP].setNormalAndPoint(Z, fc);

		Vec3 aux;
		aux = (nc.add(Y.multiply(nh)).subtract(offset)).normalize();
		mFrustumPlanes[TOP].setNormalAndPoint(aux.cross(X), nc.add(Y.multiply(nh)));
		aux = (nc.subtract(Y.multiply(nh)).subtract(offset)).normalize();
		mFrustumPlanes[BOTTOM].setNormalAndPoint(X.cross(aux), nc.subtract(Y.multiply(nh)));

		aux = (nc.subtract(X.multiply(nw)).subtract(offset)).normalize();
		mFrustumPlanes[LEFT].setNormalAndPoint(aux.cross(Y), nc.subtract(X.multiply(nw)));
		aux = (nc.add(X.multiply(nw)).subtract(offset)).normalize();
		mFrustumPlanes[RIGHT].setNormalAndPoint(Y.cross(aux), nc.add(X.multiply(nw)));
	}

	/**
	 * Process input from the mouse, adjusting the heading and pitch of the camera.
	 */
	public void pollInput() {
		float speed = (float) (0.01 * Game.delta);

		Vec3 deltaPos = new Vec3(-mHeadingAngle, -mPitchAngle).multiply(speed);
		Vector3f d = new Vector3f(deltaPos.getX(), deltaPos.getY(), deltaPos.getZ());

		if(Game.heartbeatFrame)
			System.out.println("Pos = " + mPosition);

		DisplayMode dm = Display.getDisplayMode();
		int height = dm.getHeight();
		int width = dm.getWidth();

		// System.out.println("x move:"+(Mouse.getX()-width/2)+",y move:"+(Mouse.getY()-height/2));

		mHeadingAngle -= (Mouse.getX() - width / 2) / 10.0;
		mPitchAngle += (Mouse.getY() - height / 2) / 10.0;

		mPitchAngle = Math.min(mPitchAngle, 90);
		mPitchAngle = Math.max(mPitchAngle, -90);

		if (Config.PLAYER_CONTROLS_CAMERA) {
			if (null != mControllable) {
				mControllable.setHeadingPitch(mHeadingAngle, mPitchAngle);
				mControllable.getPos(mPosition);
			}
		} else {
			if (Mouse.isButtonDown(0)) mPosition.sub(d);
			if (Mouse.isButtonDown(1)) mPosition.add(d);
		}

		Mouse.setCursorPosition(width / 2, height / 2);

		if (Game.heartbeatFrame) {
			System.out.println("pos:" + mPosition + ", pitch:" + mPitchAngle + ", heading:" + mHeadingAngle);
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
		if (!Config.FRUSTUM_CULLING) {
			return Inclusion.INSIDE;
		}

		Inclusion incl = Inclusion.INSIDE;
		for (int i = 0; i < 6; i++) {
			Vec3 p = f.getVertexP(mFrustumPlanes[i].mNormal);
			float dist = mFrustumPlanes[i].distance(p);

			if (dist < 0) {
				return Inclusion.OUTSIDE;
			}

			p = f.getVertexN(mFrustumPlanes[i].mNormal);
			dist = mFrustumPlanes[i].distance(p);

			if (dist < 0) {
				incl = Inclusion.INTERSECT;
			}
		}
		return incl;
	}

	public void apply() {
		calculateViewFrustum();

		/*
		GL11.glRotatef(mPitchAngle, -1, 0, 0);
		GL11.glRotatef(mHeadingAngle, 0, -1, 0);
		GL11.glTranslatef(-mPosition.x, -mPosition.y, -mPosition.z);
		*/
	}
}
