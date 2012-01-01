
package supergame;

import org.lwjgl.opengl.GL11;

import javax.vecmath.Vector3f;

public class Vec3 {
    public static void HPVector(Vector3f vec, float heading, float pitch) {
        float x,y,z;
        x = -(float) (Math.sin(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));
        y = (float) (Math.sin(pitch * Math.PI / 180.0));
        z = (float) (Math.cos(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));

        vec.set(x, y, z);
    }

    float mData[] = new float[3];

    public Vec3(float x, float y, float z) {
        mData[0] = x;
        mData[1] = y;
        mData[2] = z;
    }

    public Vec3(float heading, float pitch) {
        mData[0] = -(float) (Math.sin(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));
        mData[2] = (float) (Math.cos(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));
        mData[1] = (float) (Math.sin(pitch * Math.PI / 180.0));
    }

    public Vec3(Vec3 pos) {
        mData[0] = pos.getX();
        mData[1] = pos.getY();
        mData[2] = pos.getZ();
    }

    public void actuallyAdd(Vec3 other) {
        mData[0] += other.getX();
        mData[1] += other.getY();
        mData[2] += other.getZ();
    }

    public void actuallySubtract(Vec3 other) {
        mData[0] -= other.getX();
        mData[1] -= other.getY();
        mData[2] -= other.getZ();
    }

    public void set(float x, float y, float z) {
        mData[0] = x;
        mData[1] = y;
        mData[2] = z;
    }

    public void GLdraw() {
        GL11.glVertex3f(mData[0], mData[1], mData[2]);
    }
    public void GLnormal() {
        GL11.glNormal3f(mData[0], mData[1], mData[2]);
    }

    public Vec3 multiply(float f) {
        return new Vec3(mData[0] * f, mData[1] * f, mData[2] * f);
    }

    public Vec3 add(Vec3 b) {
        return new Vec3(mData[0] + b.getX(), mData[1] + b.getY(), mData[2] + b.getZ());
    }

    public Vec3 subtract(Vec3 b) {
        return new Vec3(mData[0] - b.getX(), mData[1] - b.getY(), mData[2] - b.getZ());
    }

    public Vec3 cross(Vec3 b) {
        return new Vec3(getY() * b.getZ() - b.getY() * getZ(), getZ() * b.getX() - b.getZ() * getX(), getX() * b.getY()
                - b.getX() * getY());
    }

    public Vec3 normalize() {
        float l = length();
        return new Vec3(mData[0] / l, mData[1] / l, mData[2] / l);
    }

    public float length() {
        return (float) Math.sqrt(mData[0] * mData[0] + mData[1] * mData[1] + mData[2] * mData[2]);
    }

    public float getX() {
        return mData[0];
    }

    public float getY() {
        return mData[1];
    }

    public float getZ() {
        return mData[2];
    }

    @Override
    public String toString() {
        return String.format("Vec3: (%3f %3f %3f)", mData[0], mData[1], mData[2]);
    }

    public float innerProduct(Vec3 point) {
        return point.getX() * getX() + point.getY() * getY() + point.getZ() * getZ();
    }

    //Into functions - change internal vector state
    public void addInto(int index, float offset) {
        mData[index] += offset;
    }

    public void scaleInto(float scale) {
        mData[0] *= scale;
        mData[1] *= scale;
        mData[2] *= scale;
    }
}
