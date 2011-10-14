package supergame.network;


public class Structs {
	public static abstract class EntityData {};

	public static abstract class Entity {
		PiecewiseLerp mLerp;
		abstract public void apply(double timestamp, EntityData packet);
		abstract public EntityData getState();
	}

	public static class PositionData extends EntityData {
		public float x, y, z;
		public float heading, pitch;
	}

	public static class Character extends Entity {
		@Override
		public void apply(double timestamp, EntityData packet) {
			PositionData data = (PositionData)packet;
		}

		@Override
		public EntityData getState() {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
