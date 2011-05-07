package supergame;

import org.lwjgl.opengl.GL11;

public class Vec3 {
	float data[];

	public Vec3(float x, float y, float z) {
		data = new float[3];
		data[0] = x;
		data[1] = y;
		data[2] = z;
	}

	public Vec3(float heading, float pitch) {
		data = new float[3];
		data[0] = -(float) (Math.sin(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));
		data[2] = (float) (Math.cos(heading * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0));
		data[1] = (float) (Math.sin(pitch * Math.PI / 180.0));
	}

	public Vec3(Vec3 pos) {
		data = new float[3];
		data[0] = pos.getX();
		data[1] = pos.getY();
		data[2] = pos.getZ();
	}

	public void GLdraw() {
		GL11.glVertex3f(data[0], data[1], data[2]);
	}

	public Vec3 multiply(float f) {
		return new Vec3(data[0] * f, data[1] * f, data[2] * f);
	}

	public Vec3 add(Vec3 b) {
		return new Vec3(data[0] + b.getX(), data[1] + b.getY(), data[2] + b.getZ());
	}

	public Vec3 subtract(Vec3 b) {
		return new Vec3(data[0] - b.getX(), data[1] - b.getY(), data[2] - b.getZ());
	}

	public Vec3 cross(Vec3 b) {
		return new Vec3(
				getY() * b.getZ() - b.getY() * getZ(),
				getZ() * b.getX() - b.getZ() * getX(),
				getX() * b.getY() - b.getX() * getY());
	}

	public Vec3 normalize() {
		float l = length();
		return new Vec3(data[0] / l, data[1] / l, data[2] / l);
	}

	public float length() {
		return (float) Math.sqrt(data[0] * data[0] + data[1] * data[1] + data[2] * data[2]);
	}

	public float getX() {
		return data[0];
	}

	public float getY() {
		return data[1];
	}

	public float getZ() {
		return data[2];
	}

	public String toString() {
		return String.format("Vec3: (%3f %3f %3f)", data[0], data[1], data[2]);
	}

	public float innerProduct(Vec3 point) {
		return point.getX()*getX() + point.getY()*getY() + point.getZ()*getZ();
	}

	//Into functions - change internal vector state
	public void addInto(int index, float offset) {
		data[index] += offset;
	}
}
