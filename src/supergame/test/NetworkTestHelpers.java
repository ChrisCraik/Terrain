package supergame.test;

import supergame.network.PiecewiseLerp;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;

public class NetworkTestHelpers {

	public static class TestSimpleData extends EntityData {
		float pos[] = new float[2];
	}

	public static class TestSimpleEntity extends Entity {
		public float mX = 0, mY = 0;

		// class has NO CONSTRUCTOR or SINGLE CONSTRUCTOR WITHOUT ARGUMENTS

		@Override
		public void apply(double timestamp, EntityData packet) {
			assert (packet instanceof TestSimpleData);
			TestSimpleData data = (TestSimpleData) packet;
			mX = data.pos[0];
			mY = data.pos[1];
		}

		@Override
		public EntityData getState() {
			TestSimpleData t = new TestSimpleData();
			t.pos[0] = mX;
			t.pos[1] = mY;
			return t;
		}

		public void update() {
			mX += 1;
			mY -= 1;
		}
	}

	// unique data class solely for differentiation
	public static class TestInterpData extends TestSimpleData {};

	public static class TestInterpEntity extends Entity {
		PiecewiseLerp mPosition = new PiecewiseLerp(4);

		// class has NO CONSTRUCTOR or SINGLE CONSTRUCTOR WITHOUT ARGUMENTS

		@Override
		public void apply(double timestamp, EntityData packet) {
			assert (packet instanceof TestInterpData);
			TestInterpData data = (TestInterpData) packet;
			mPosition.addSample(timestamp, data.pos);
		}

		@Override
		public EntityData getState() {
			// TODO: avoid allocation, pass in object to populate
			TestInterpData t = new TestInterpData();
			mPosition.sampleLatest(t.pos);
			return t;
		}

		public boolean sample(double timestamp, float[] pos) {
			return mPosition.sample(timestamp, pos);
		}

		public void update(double timestamp, float x, float y) {
			// TODO: avoid the allocation, but make more clear that it's just
			// doing a store underneath, not a copy of the array
			mPosition.addSample(timestamp, new float[] { x, y });
		}
	}

}
