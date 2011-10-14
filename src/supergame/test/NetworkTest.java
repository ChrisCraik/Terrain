package supergame.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import supergame.network.GameClient;
import supergame.network.GameServer;
import supergame.network.PiecewiseLerp;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;

public class NetworkTest {
	GameClient client;
	GameServer server;

	@Before
	public void setUp() {
		client = new GameClient();
		server = new GameServer();

		// TODO: better way to do this
		client.registerEntityPacket(TestSimpleData.class, TestSimpleEntity.class);
		client.registerEntityPacket(TestInterpData.class, TestInterpEntity.class);
		server.registerEntityPacket(TestSimpleData.class, TestSimpleEntity.class);
		server.registerEntityPacket(TestInterpData.class, TestInterpEntity.class);
	}

	@After
	public void tearDown() {
		client.getEndPoint().close();
		server.getEndPoint().close();
	}

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

	private void transmitServerToClient(double timestamp,
			GameServer serverNetwork, GameClient clientNetwork) {
		clientNetwork.applyEntityChanges(timestamp,
				serverNetwork.getEntityChanges());
	}

	@Test
	public void testSimpleEntityTransmit() {
		// create a server side object, verify it's replicated client side
		TestSimpleEntity serverEnt = new TestSimpleEntity();
		server.registerEntity(serverEnt);

		// send server data to client (and create remote object)
		transmitServerToClient(-1, server, client);

		// verify entity on client
		Collection<Entity> entities = client.getEntities();
		assertEquals(entities.size(), 1);
		TestSimpleEntity clientEnt = (TestSimpleEntity) entities.iterator()
				.next();

		for (int i = 0; i < 5; i++) {
			// up to date....
			assertEquals(clientEnt.mX, serverEnt.mX, 0.001);
			assertEquals(clientEnt.mY, serverEnt.mY, 0.001);

			// modify source object
			serverEnt.update();

			// inconsistent!
			assertEquals(clientEnt.mX + 1, serverEnt.mX, 0.001);
			assertEquals(clientEnt.mY - 1, serverEnt.mY, 0.001);

			// send server data to client
			transmitServerToClient(i, server, client);
		}
	}

	// unique class solely for differentiation
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

	@Test
	public void testInterpEntityTransmit() {
		// create a server side object, verify it's replicated client side
		TestInterpEntity serverEnt = new TestInterpEntity();
		server.registerEntity(serverEnt);
		serverEnt.update(-1, -1, 1);

		// send server data to client (and create remote object)
		transmitServerToClient(-1, server, client);

		// verify entity on client
		Collection<Entity> entities = client.getEntities();
		assertEquals(entities.size(), 1);
		TestInterpEntity clientEnt = (TestInterpEntity) entities.iterator().next();

		float cPos[] = new float[2];
		float sPos[] = new float[2];
		for (int i = 0; i < 5; i++) {
			double timestamp = i;

			// up to date....
			assertTrue(serverEnt.sample(timestamp - 1, sPos));
			assertTrue(clientEnt.sample(timestamp - 1, cPos));
			assertArrayEquals(cPos, sPos, 0.001f);

			// modify source object
			serverEnt.update(timestamp, i, -i);

			// surprise! still up to date!
			assertTrue(serverEnt.sample(timestamp, sPos));
			assertFalse(clientEnt.sample(timestamp, cPos)); // false here - client is extrapolating

			if (i > 0) {
				// after 2 samples on client, it can extrapolate
				assertArrayEquals(cPos, sPos, 0.001f);
			}

			// send server data to client
			transmitServerToClient(timestamp, server, client);
		}
	}
}
