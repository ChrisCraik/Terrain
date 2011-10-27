package supergame.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Collection;
import java.util.HashMap;

import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
import supergame.network.Structs.State;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;

public abstract class GameEndPoint {
	protected final HashMap<Integer, Entity> mEntityMap = new HashMap<Integer, Entity>();
	protected final EndPoint mEndPoint;
	protected final BufferedWriter mWriter;
	protected final BufferedReader mReader;
	protected final Kryo mKryo;

	/**
	 * Maps incoming network data types to the associated network entity
	 * classes. If a new object update of type K shows up, the client should
	 * create an object of type mPacketToClassMap.get(K)
	 */
	protected final HashMap<Class<? extends EntityData>, Class<? extends Entity>> mPacketToClassMap = new HashMap<Class<? extends EntityData>, Class<? extends Entity>>();

	public GameEndPoint(EndPoint endPoint, BufferedWriter writer, BufferedReader reader) {
		mEndPoint = endPoint;
		mWriter = writer;
		mReader = reader;

		if (endPoint == null) {
			mKryo = new Kryo();
		} else {
			mKryo = endPoint.getKryo();
		}

		mKryo.register(float[].class);
		mKryo.register(HashMap.class);
		mKryo.register(Structs.EntityData.class);
		mKryo.register(Structs.PositionData.class);

		// TODO: compress the State class
		mKryo.register(State.class);
	}

	public void registerEntityPacket(Class<? extends EntityData> dataClass,
			Class<? extends Entity> entityClass) {
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

	public void start() {
		mEndPoint.start();
	}

	public void close() {
		mEndPoint.close();
	}

	public void addListener(Listener l) {
		// TODO: virtualize this so that there's a simpler interface
		mEndPoint.addListener(l);
	}
}
