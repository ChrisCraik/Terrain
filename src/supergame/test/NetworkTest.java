package supergame.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.junit.Test;
import org.lwjgl.Sys;

import supergame.network.GameClient;
import supergame.network.GameEndPoint.TransmitPair;
import supergame.network.GameServer;
import supergame.network.Structs.State;
import supergame.test.NetworkTestHelpers.TestInterpData;
import supergame.test.NetworkTestHelpers.TestInterpEntity;
import supergame.test.NetworkTestHelpers.TestSimpleData;
import supergame.test.NetworkTestHelpers.TestSimpleEntity;

public class NetworkTest {
	final static int MAX_TIME = 1000;
	final static int WAIT_TIME = 1000;

	public static abstract class Transmit {
		public GameClient client;
		public GameServer server;
		private final long mStart = Sys.getTime();

		public Transmit(GameClient client, GameServer server) {
			this.client = client;
			this.server = server;

			// TODO: better way to do this - in constructor?
			client.registerEntityPacket(TestSimpleData.class, TestSimpleEntity.class);
			client.registerEntityPacket(TestInterpData.class, TestInterpEntity.class);
			server.registerEntityPacket(TestSimpleData.class, TestSimpleEntity.class);
			server.registerEntityPacket(TestInterpData.class, TestInterpEntity.class);

			init();
		}

		public void finish() {
			client.close();
			server.close();

			long timeTaken = Sys.getTime() - mStart;
			if (timeTaken > MAX_TIME)
				fail("Took " + timeTaken + "ms, should be near-instant.");
		}
		protected void init() {};
		public abstract void transmitServerToClient(double timestamp);
	};

	public static class SimpleTransmit extends Transmit {
		public SimpleTransmit() {
			super(new GameClient(), new GameServer());
		}

		@Override
		public void transmitServerToClient(double timestamp) {
			client.applyEntityChanges(
					timestamp,
					server.getEntityChanges());
		}
	};

	public static class LoopbackTransmit extends Transmit {
		public LoopbackTransmit() {
			super(new GameClient(), new GameServer());
		}

		public LoopbackTransmit(WritableByteChannel writer) {
			super(new GameClient(writer), new GameServer());
		}

		public LoopbackTransmit(ReadableByteChannel reader) {
			super(new GameClient(reader), new GameServer());
		}

		@Override
		protected void init() {
			try {
				server.bind(12415, 12415);
				client.connect(5000, "localhost", 12415, 12415);
			} catch (IOException e) {
				fail("connection issues");
				e.printStackTrace();
			}
		}

		@Override
		public void transmitServerToClient(double timestamp) {
			State serverState = new State();
			serverState.timestamp = timestamp;
			serverState.data = server.getEntityChanges();
			server.sendToAllTCP(serverState);

			TransmitPair p = client.pollHard(timestamp, WAIT_TIME);
			if (p == null || !(p.object instanceof State))
				fail("no data/wrong data type!");

			State state = ((State)p.object);
			client.applyEntityChanges(state.timestamp, state.data);
		}
	};

	@Test
	public void testSimpleEntityWithoutNetwork() {
		NetworkTestHelpers.testSimpleEntityTransmit(new SimpleTransmit());
	}

	@Test
	public void testInterpEntityWithoutNetwork() {
		NetworkTestHelpers.testInterpEntityTransmit(new SimpleTransmit());
	}

	@Test
	public void testNetworkEntitySerialization() {
		NetworkTestHelpers.testInterpEntityTransmit(new LoopbackTransmit());
	}

	@Test
	public void testNetworkDiskEntitySerialization() throws FileNotFoundException {

		// first time through, client saves a copy of everything it receives
		File file = new File("filename");
		boolean append = false;
		WritableByteChannel wChannel = new FileOutputStream(file, append).getChannel();
		NetworkTestHelpers.testInterpEntityTransmit(new LoopbackTransmit(wChannel));

		// second time through, the client doesn't connect to a server at all,
		// but instead reads old network transmission data from the buffered
		// reader
		ReadableByteChannel rChannel = new FileInputStream(file).getChannel();
		NetworkTestHelpers.testInterpEntityTransmit(new LoopbackTransmit(rChannel));
	}
}
