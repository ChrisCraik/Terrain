package supergame.graphics;

public abstract class Graphics {
	public static Graphics instance = null;

	public abstract void clear();
	public abstract void switchTo3d(float displayRatio);
	public abstract void switchTo2d();

	//TODO: make the below better.
	public abstract void loadIdentity();
	public abstract void createLight();
}
