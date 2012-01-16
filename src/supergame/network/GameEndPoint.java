
package supergame.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.Listener;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import supergame.ChatDisplay;
import supergame.Game;
import supergame.character.Character;
import supergame.character.Character.CharacterData;
import supergame.graphics.Graphics;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ControlMessage;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
import supergame.network.Structs.StartMessage;
import supergame.network.Structs.StateMessage;

import java.io.IOException;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class GameEndPoint {
    public static class TransmitPair {
        public TransmitPair(Connection connection, Object object) {
            this.connection = connection;
            this.object = object;
        }

        public Connection connection;
        public Object object;
    }

    protected final HashMap<Integer, Entity> mEntityMap = new HashMap<Integer, Entity>();
    protected final EndPoint mEndPoint;
    protected final WritableByteChannel mWriter;
    protected final ReadableByteChannel mReader;
    protected final Kryo mKryo;
    private final LinkedBlockingQueue<TransmitPair> mQueue = new LinkedBlockingQueue<TransmitPair>();
    private double mMostRecentFrameTime = 0;

    // TODO: consider non-native ordering, so files can be shared
    private final ByteBuffer mBuffer = BufferUtils.createByteBuffer(4096);

    /**
     * Maps incoming network data types to the associated network entity
     * classes. If a new object update of type K shows up, the client should
     * create an object of type mPacketToClassMap.get(K)
     */
    protected final HashMap<Class<? extends EntityData>, Class<? extends Entity>> mPacketToClassMap = new HashMap<Class<? extends EntityData>, Class<? extends Entity>>();

    public GameEndPoint(EndPoint endPoint, WritableByteChannel writer, ReadableByteChannel reader) {
        mEndPoint = endPoint;
        mWriter = writer;
        mReader = reader;

        if (mEndPoint == null) {
            mKryo = new Kryo();
        } else {
            mKryo = endPoint.getKryo();
            mEndPoint.addListener(new Listener() {
                @Override
                public void received(Connection connection, Object object) {
                    TransmitPair pair = new TransmitPair(connection, object);
                    mQueue.add(pair);

                    if (mWriter != null) {
                        mBuffer.clear();

                        // write contents of packet to buffer
                        mBuffer.position(12);
                        mKryo.writeObject(mBuffer, object);

                        // write position to head of buffer
                        int objectSize = mBuffer.position() - 12;
                        mBuffer.asDoubleBuffer().put(0, mMostRecentFrameTime);
                        mBuffer.asIntBuffer().put(2, objectSize);
                        try {
                            mWriter.write(mBuffer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        mKryo.register(float[].class);
        mKryo.register(double[].class);
        mKryo.register(short[].class);
        mKryo.register(HashMap.class);
        mKryo.register(Structs.EntityData.class);

        // used for saving
        mKryo.register(TransmitPair.class);
        mKryo.register(Connection.class);
        mKryo.register(Client.class);
        mKryo.register(Inet4Address.class);

        System.out.println("");

        // TODO: compress the State class
        mKryo.register(StateMessage.class);

        mKryo.register(StartMessage.class);
        mKryo.register(ChatMessage.class);
        mKryo.register(ControlMessage.class);
        registerEntityPacket(CharacterData.class, Character.class);
    }

    public void registerEntityPacket(Class<? extends EntityData> dataClass,
            Class<? extends Entity> entityClass) {
        mKryo.register(dataClass);
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

    public void close() {
        if (mEndPoint != null) {
            mEndPoint.stop();
            mEndPoint.close();
        }
        try {
            if (mReader != null) {
                mReader.close();
            }
            if (mWriter != null) {
                mWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TransmitPair pollHard(double localTime, int timeInMS) {
        mMostRecentFrameTime = localTime;

        // TODO: read objects from file! hooray!

        try {
            return mQueue.poll(timeInMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected int mLocalCharId = -1;
    protected final ChatDisplay mChatDisplay = new ChatDisplay();

    public abstract void setupMove(double localTime);
    public abstract void postMove(double localTime);

    public void render(double localTime) {
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE,
                Game.makeFB(new float[] {
                        0.5f, 0, 0.5f, 0.5f
                }));
        for (Entity e : mEntityMap.values()) {
            if (e instanceof Character) {
                ((Character)e).render();
            }
        }

        Graphics.instance.switchTo2d();
        mChatDisplay.render(localTime);
    }
}
