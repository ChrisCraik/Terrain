package supergame.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;

import com.esotericsoftware.kryonet.Client;

public class GameClient extends GameEndPoint {
	Client mClient = new Client();

	public GameClient() {
		super(new Client());
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
				Class entityClass = mPacketToClassMap.get(data.getClass());
				try {
					Entity newEntity = (Entity) entityClass.newInstance();
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

	public Client getClient() {
		return (Client)getEndPoint();
	}
}
