package supergame.network;

import java.util.Collection;
import java.util.HashMap;

import supergame.network.Structs.Entity;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.compress.DeflateCompressor;
import com.esotericsoftware.kryo.serialize.MapSerializer;
import com.esotericsoftware.kryonet.EndPoint;

public abstract class GameEndPoint {
	protected final HashMap<Integer, Entity> mEntityMap = new HashMap<Integer, Entity>();
	private final EndPoint mEndPoint;

	public EndPoint getEndPoint() {
		return mEndPoint;
	}

	/**
	 * Maps incoming network data types to the associated network entity
	 * classes. If a new object update of type K shows up, the client should
	 * create an object of type mPacketToClassMap.get(K)
	 */
	@SuppressWarnings("rawtypes")
	protected final HashMap<Class, Class> mPacketToClassMap = new HashMap<Class, Class>();

	// This registers objects that are going to be sent over the network.
	public GameEndPoint(EndPoint endPoint) {
		mEndPoint = endPoint;
		Kryo kryo = endPoint.getKryo();

		// Register the hashmap as compressed, so the final hashmap we send over
		// the network is compressed
		kryo.register(HashMap.class, new DeflateCompressor(new MapSerializer(
				kryo)));

		kryo.register(Structs.EntityData.class);
		kryo.register(Structs.PositionData.class);
		endPoint.start();
	}

	public void registerEntityPacket(Class dataClass, Class entityClass) {
		mEndPoint.getKryo().register(dataClass);
		mPacketToClassMap.put(dataClass, entityClass);
	}

	public Collection<Entity> getEntities() {
		return mEntityMap.values();
	}

	/**
	 * Client/Server-side registration of network entities
	 *
	 * @param entity
	 * @param id
	 */
	public <K extends Entity> void registerEntity(K entity, int id) {
		assert (!mEntityMap.containsKey(id));
		mEntityMap.put(id, entity);
	}
}
