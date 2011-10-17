package supergame.network;

import java.util.Collection;
import java.util.HashMap;

import supergame.network.Structs.Entity;
import supergame.network.Structs.State;

import com.esotericsoftware.kryo.Kryo;
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

		kryo.register(float[].class);
		kryo.register(HashMap.class);
		kryo.register(Structs.EntityData.class);
		kryo.register(Structs.PositionData.class);

		// TODO: compress the State class
		kryo.register(State.class);
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
