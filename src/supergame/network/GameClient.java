package supergame.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;

import com.esotericsoftware.kryonet.Client;

public class GameClient extends GameEndPoint {
	/**
	 * Normal game client constructor.
	 */
	public GameClient() {
		super(new Client(), null, null);
	}

	/**
	 * Game client that also saves received packets for future playback.
	 *
	 * @param w
	 *            The client stores received packets from server in this.
	 */
	public GameClient(BufferedWriter w) {
		super(new Client(), w, null);
	}

	/**
	 * 'Virtual' Game client that connects to a BufferedReader instead of a server, for
	 * playing back saved network transcripts.
	 *
	 * @param r
	 *            Read by the client as though a stream of packets from the
	 *            server.
	 */
	public GameClient(BufferedReader r) {
		super(null, null, r);
	}

	/**
	 * Apply remote changes client side
	 *
	 * @param timestamp
	 * @param changeMap
	 */
	public void applyEntityChanges(double timestamp,
			HashMap<Integer, EntityData> changeMap) {
		Iterator<Map.Entry<Integer, EntityData>> it = changeMap.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, EntityData> entry = it.next();
			int id = entry.getKey();
			EntityData data = entry.getValue();

			Entity localEntity = mEntityMap.get(id);
			if (localEntity != null) {
				localEntity.apply(timestamp, data);
			} else {
				// NEW ENTITY
				assert (mPacketToClassMap.containsKey(data.getClass()));
				Class<? extends Entity> entityClass = mPacketToClassMap.get(data.getClass());
				try {
					Entity newEntity = entityClass.newInstance();
					registerEntity(newEntity, id);
					newEntity.apply(timestamp, data);
				} catch (InstantiationException e) {
					System.err.println("ERROR: class lacks no-arg constructor");
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					System.err.println("ERROR: class lacks public constructor");
					e.printStackTrace();
				}
			}
		}
	}

	public void connect(int timeout, String address, int udp, int tcp) throws IOException {
		((Client)mEndPoint).start(); // there's probably a reason not to do this here...
		((Client)mEndPoint).connect(timeout, address, udp, tcp);
	}
}
