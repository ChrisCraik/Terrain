package supergame.network;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;

import com.esotericsoftware.kryonet.Server;

public class GameServer extends GameEndPoint {
	private int mNextEntityId = 1;

	/**
	 * Normal game server constructor.
	 */
	public GameServer() {
		super(new Server(), null, null);
	}

	/**
	 * Game server that also saves sent packets for future playback to a virtual
	 * client.
	 *
	 * @param w
	 *            The server stores packets it sends in this.
	 */
	public GameServer(BufferedWriter w) {
		super(new Server(), w, null);
	}


	/**
	 * Get local changes server side, to be sent remotely
	 *
	 * @param changeMap
	 */
	public HashMap<Integer, EntityData> getEntityChanges() {
		HashMap<Integer, EntityData> changeMap = new HashMap<Integer, EntityData>();

		Iterator<Map.Entry<Integer, Entity>> it = mEntityMap.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Entity> entry = it.next();
			changeMap.put(entry.getKey(), entry.getValue().getState());
		}
		return changeMap;
	}

	/**
	 * Server-side registration of network entities
	 *
	 * @param entity
	 */
	public <K extends Entity> void registerEntity(K entity) {
		registerEntity(entity, mNextEntityId++);
	}

	public void sendToAllTCP(Object o) {
		((Server)mEndPoint).sendToAllTCP(o);
	}

	public void bind(int udp, int tcp) throws IOException {
		((Server)mEndPoint).bind(udp, tcp);
	}
}
