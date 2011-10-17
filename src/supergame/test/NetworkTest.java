package supergame.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import supergame.network.GameClient;
import supergame.network.GameServer;
import supergame.network.Structs.Entity;
import supergame.network.Structs.State;
import supergame.test.NetworkTestHelpers.TestInterpData;
import supergame.test.NetworkTestHelpers.TestInterpEntity;
import supergame.test.NetworkTestHelpers.TestSimpleData;
import supergame.test.NetworkTestHelpers.TestSimpleEntity;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

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
		client.getClient().start();
		server.getServer().start();
	}

	@After
	public void tearDown() {
		client.getClient().close();
		server.getServer().close();
	}

	private abstract class Transmit {
		public void init(GameServer serverNetwork, final GameClient clientNetwork) {};
		abstract void transmitServerToClient(double timestamp,
				GameServer serverNetwork,
				GameClient clientNetwork);
	}

	private final Transmit simpleTransmit = new Transmit() {
		@Override
		public void transmitServerToClient(double timestamp,
				GameServer serverNetwork, GameClient clientNetwork) {
			clientNetwork.applyEntityChanges(
					timestamp,
					serverNetwork.getEntityChanges());
		}
	};

	private final Transmit loopbackTransmit = new Transmit() {
		State mClientState = null;

		@Override
		public void init(GameServer serverNetwork,
				final GameClient clientNetwork) {
			try {
				serverNetwork.getServer().bind(12415, 12415);
				clientNetwork.getClient().connect(5000, "localhost", 12415, 12415);
			} catch (IOException e) {
				fail("connection issues");
				e.printStackTrace();
			}

			clientNetwork.getClient().addListener(new Listener() {
				@Override
				public void received(Connection connection, Object object) {
					synchronized (loopbackTransmit) {
						if (object instanceof State) {
							mClientState = (State) object;
							loopbackTransmit.notify();
						} else {
							fail("incorrect data transmission format!");
						}
					}
				}
			});
		}

		@Override
		public void transmitServerToClient(double timestamp,
				GameServer serverNetwork, GameClient clientNetwork) {

			State serverState = new State();
			serverState.timestamp = timestamp;
			serverState.data = serverNetwork.getEntityChanges();

			// TODO: don't assume non-trivial time
			serverNetwork.getServer().sendToAllTCP(serverState);

			try {
				synchronized(loopbackTransmit) {
					if (mClientState == null) {
						loopbackTransmit.wait(1000);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				fail("network wait ");
			}
			if (mClientState == null)
				fail("transmission failed after 5000ms");

			clientNetwork.applyEntityChanges(mClientState.timestamp, mClientState.data);
			mClientState = null;
		}
	};

	public void testSimpleEntityTransmit(Transmit t) {
		// create a server side object, verify it's replicated client side
		TestSimpleEntity serverEnt = new TestSimpleEntity();
		server.registerEntity(serverEnt);

		// send server data to client (and create remote object)
		t.transmitServerToClient(-1, server, client);

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
			t.transmitServerToClient(i, server, client);
		}
	}

	public void testInterpEntityTransmit(Transmit t) {
		// create a server side object, verify it's replicated client side
		TestInterpEntity serverEnt = new TestInterpEntity();
		server.registerEntity(serverEnt);
		serverEnt.update(-1, -1, 1);

		// send server data to client (and create remote object)
		t.transmitServerToClient(-1, server, client);

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
			t.transmitServerToClient(timestamp, server, client);
		}
	}

	@Test
	public void testSimpleEntityWithoutNetwork() {
		testSimpleEntityTransmit(simpleTransmit);
	}

	@Test
	public void testInterpEntityWithoutNetwork() {
		testInterpEntityTransmit(simpleTransmit);
	}

	@Test
	public void testNetworkEntitySerialization() {
		// connect, register callback
		loopbackTransmit.init(server, client);

		testInterpEntityTransmit(loopbackTransmit);
	}

}
