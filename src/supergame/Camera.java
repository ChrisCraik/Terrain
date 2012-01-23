
package supergame;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import supergame.character.Controller;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ControlMessage;

import javax.vecmath.Vector3f;

public class Camera extends Controller {
    private final Vector3f mPosition;
    private float mPitchAngle = -64, mHeadingAngle = 56;
    private final Vector3f mForwardDir = new Vector3f();
    private final Vector3f mStrafeDir = new Vector3f();
    private final Plane mFrustumPlanes[];

    /*package*/ Camera() {
        mPosition = new Vector3f(15, 40, 8);
        Mouse.setGrabbed(true);
        mFrustumPlanes = new Plane[6];
        for (int i = 0; i < 6; i++)
            mFrustumPlanes[i] = new Plane();

        // initialize mouse position
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
            b = new Vec3(0, 1, 0).cross(this.mNormal); // relative horizontal
                                                       // vector
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
     * be rendered. TODO: rewrite without the use of object allocations
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

    private int mToolSelection = 0;
    private int updateToolSelection() {
        if (Keyboard.isKeyDown(Keyboard.KEY_0)) {
            mToolSelection = 0;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_1)) {
            mToolSelection = 1;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_2)) {
            mToolSelection = 2;
        }
        return mToolSelection;
    }

    static final float MIN_TARGET = 3;
    static final float MAX_TARGET = 10;
    private float mTargetDistance = 5;
    private float updateTargetDistance() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKey() == Keyboard.KEY_UP
                    && Keyboard.getEventKeyState()) {
                mTargetDistance += 1;
            } else if (Keyboard.getEventKey() == Keyboard.KEY_DOWN
                    && Keyboard.getEventKeyState()) {
                mTargetDistance -= 1;
            }
        }
        mTargetDistance += Mouse.getDWheel() / 1000.0;
        mTargetDistance = Math.min(mTargetDistance, MAX_TARGET);
        mTargetDistance = Math.max(mTargetDistance, MIN_TARGET);
        return mTargetDistance;
    }

    /**
     * Process input from the mouse and keyboard, adjusting the heading and pitch of the
     * camera, and any character under direct control.
     */
    @Override
    public void control(double localTime, ControlMessage control, ChatMessage chat) {
        DisplayMode dm = Display.getDisplayMode();
        int height = dm.getHeight();
        int width = dm.getWidth();

        // System.out.println("x move:"+(Mouse.getX()-width/2)+",y move:"+(Mouse.getY()-height/2));

        mHeadingAngle -= (Mouse.getX() - width / 2) / 10.0;
        mPitchAngle += (Mouse.getY() - height / 2) / 10.0;
        mPitchAngle = Math.min(mPitchAngle, 90);
        mPitchAngle = Math.max(mPitchAngle, -90);

        // calculate absolute forward/strafe
        Vec3.HPVector(mForwardDir, 180 - mHeadingAngle, 0);
        Vec3.HPVector(mStrafeDir, 90 - mHeadingAngle, 0);

        control.heading = mHeadingAngle;
        control.pitch = mPitchAngle;

        if (tryChat(chat)) {
            // still chatting, so ignore keys for movement, tool select
            return;
        }

        // set walkDirection for character
        Vector3f walkDirection = new Vector3f(0, 0, 0);
        if (Keyboard.isKeyDown(Keyboard.KEY_W))
            walkDirection.add(mForwardDir);
        if (Keyboard.isKeyDown(Keyboard.KEY_S))
            walkDirection.sub(mForwardDir);
        if (Keyboard.isKeyDown(Keyboard.KEY_A))
            walkDirection.add(mStrafeDir);
        if (Keyboard.isKeyDown(Keyboard.KEY_D))
            walkDirection.sub(mStrafeDir);

        control.x = walkDirection.x;
        control.z = walkDirection.z;
        control.jump = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
        control.duck = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
        control.sprint = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);

        control.use0 = Mouse.isButtonDown(0);
        control.use1 = Mouse.isButtonDown(1);
        control.toolSelection = updateToolSelection();
        control.targetDistance = updateTargetDistance();
        // TODO: movement when no char attached

        Mouse.setCursorPosition(width / 2, height / 2);

        if (Game.heartbeatFrame) {
            System.out.println("dir:" + walkDirection + ", pitch:" + mPitchAngle + ", heading:"
                    + mHeadingAngle);
        }
    }

    private String mCurrent = null;
    private boolean tryChat(ChatMessage chat) {
        if (mCurrent == null) {
            while (InputProcessor.PollKeyboard()) {
                if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
                    mCurrent = "";
                    break;
                }
            }
        }

        if (mCurrent == null)
            return false;

        // build up the string the player is typing until a 'return'
        while (InputProcessor.PollKeyboard()) {
            if (Keyboard.getEventKey() == Keyboard.KEY_BACK
                    && !mCurrent.isEmpty()) {
                mCurrent = mCurrent.substring(0, mCurrent.length() - 1);
            } else if (Keyboard.getEventKey() == Keyboard.KEY_RETURN) {
                chat.s = mCurrent;
                mCurrent = null;
                ChatDisplay.current = null;
                return true;
            } else {
                mCurrent += Keyboard.getEventCharacter();
            }
        }
        ChatDisplay.current = mCurrent;
        return true;
    }

    @Override
    public void response(Vector3f pos) {
        mPosition.set(pos);
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

        GL11.glRotatef(mPitchAngle, -1, 0, 0);
        GL11.glRotatef(mHeadingAngle, 0, -1, 0);
        GL11.glTranslatef(-mPosition.x, -mPosition.y, -mPosition.z);
    }
}
