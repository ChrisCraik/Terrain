package supergame.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;

import com.esotericsoftware.kryonet.Server;

public class GameServer extends GameEndPoint {
	private int mNextEntityId = 1;

	// TODO: 2 constructors, with and without buffer

	public GameServer() {
		super(new Server(), null, null);
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

	public Server getServer() {
		return (Server)getEndPoint();
	}
}
