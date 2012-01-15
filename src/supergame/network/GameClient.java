
package supergame.network;

import com.esotericsoftware.kryonet.Client;

import org.newdawn.slick.Color;

import supergame.Game;
import supergame.character.Character;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
import supergame.network.Structs.StartMessage;
import supergame.network.Structs.StateMessage;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
     * @param w The client stores received packets from server in this.
     */
    public GameClient(WritableByteChannel w) {
        super(new Client(), w, null);
    }

    /**
     * 'Virtual' Game client that connects to a BufferedReader instead of a
     * server, for playing back saved network transcripts.
     *
     * @param r Read by the client as though a stream of packets from the
     *            server.
     */
    public GameClient(ReadableByteChannel r) {
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

    public void connect(int timeout, String address, int tcp, int udp) throws IOException {
        if (mEndPoint != null) {
            mEndPoint.start(); // there's probably a reason not to do this
                               // here...
            ((Client) mEndPoint).connect(timeout, address, tcp, udp);
        }
    }

    @Override
    public void setupMove(double frameTime) {
        // send control info to server
        if (mEntityMap.containsKey(mLocalCharId)) {
            Character localChar = (Character)mEntityMap.get(mLocalCharId);
            localChar.setController(Game.mCamera);
            ((Client)mEndPoint).sendUDP(localChar.getControl());

            ChatMessage chat = localChar.getChat();
            if (chat != null && chat.s != null) {
                System.err.println("Sending chat to server:" + chat.s);
                ((Client)mEndPoint).sendTCP(chat);
                chat.s = null;
            }
        }

        // receive world state from server
        TransmitPair pair;
        for (;;) {
            pair = pollHard(frameTime, 0);
            if (pair == null)
                break;

            if (pair.object instanceof StateMessage) {
                // Server updates client with state of all entities
                StateMessage state = (StateMessage) pair.object;
                applyEntityChanges(state.timestamp, state.data);
            } else if (pair.object instanceof StartMessage) {
                // Server tells client which character the player controls
                mLocalCharId = ((StartMessage) pair.object).characterEntity;
                System.err.println("Client sees localid, " + mLocalCharId);
            } else if (pair.object instanceof ChatMessage) {
                ChatMessage chat = (ChatMessage) pair.object;
                mChatDisplay.addChat(frameTime, chat.s, Color.white);
            }
        }

        // move local char
        if (mEntityMap.containsKey(mLocalCharId)) {
            Character localChar = (Character)mEntityMap.get(mLocalCharId);
            localChar.setupMove(frameTime);
        }
    }

    @Override
    public void postMove(double frameTime) {
        // for each character, sample interpolation window
        for (Integer key : mEntityMap.keySet()) {
            Entity value = mEntityMap.get(key);
            if (value instanceof Character) {
                float bias = (key == mLocalCharId) ? 1.0f : 0.5f;
                // TODO: time is now for local, past for remote?
                ((Character)value).sample(frameTime, bias);
            }
        }

        // update controller with local position
        if (mEntityMap.containsKey(mLocalCharId)) {
            Character localChar = (Character)mEntityMap.get(mLocalCharId);
            localChar.postMove();
        }
    }
}
