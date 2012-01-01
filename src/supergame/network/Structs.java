package supergame.network;

import java.util.HashMap;


public class Structs {
	public static class State {
		public double timestamp;
		public HashMap<Integer, EntityData> data;
	}

	public static abstract class EntityData {};

	public static abstract class Entity {
		PiecewiseLerp mLerp;
		abstract public void apply(double timestamp, EntityData packet);
		abstract public EntityData getState();
	}
}
