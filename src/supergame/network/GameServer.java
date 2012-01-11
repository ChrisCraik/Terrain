
package supergame.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

import supergame.Collision;
import supergame.Game;
import supergame.character.Character;
import supergame.character.NPCController;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
import supergame.network.Structs.StartMessage;
import supergame.network.Structs.StateMessage;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
     * @param w The server stores packets it sends in this.
     */
    public GameServer(WritableByteChannel w) {
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
    public void registerEntity(Entity entity) {
        registerEntity(entity, mNextEntityId++);
    }

    public void sendToAllTCP(Object o) {
        ((Server) mEndPoint).sendToAllTCP(o);
    }

    public void bind(int tcp, int udp) throws IOException {
        ((Server) mEndPoint).start(); // there's probably a reason not to do this here...
        ((Server) mEndPoint).bind(tcp, udp);
    }

    // map connection id to character's entity id
    HashMap<Integer, Integer> mCharControlMap = new HashMap<Integer, Integer>();

    private int getEntityId(Entity e) {
        for (Integer id : mEntityMap.keySet()) {
            if (mEntityMap.get(id) == e)
                return id;
        }
        // FIXME: proper error handling
        return -1;
    }

    @Override
    public void setupMove(double frameTime) {
        // if a connection doesn't remain, delete the char
        for (Integer connectionId : mCharControlMap.keySet()) {
            if (connectionId == 0) {
                continue;
            }

            // TODO: skip this loop iteration if connection exists
            /*
            for (Connection c : ((Server) mEndPoint).getConnections()) {
                if (c.getID() == connectionId) {
                    break;
                }
            }
            */

            int charId = mCharControlMap.remove(connectionId);
            // FIXME: check null
            mEntityMap.remove(charId);
        }

        // create a local char, if it hasn't been done yet
        if (!mCharControlMap.containsKey(0)) {
            System.err.println("creating char for local connection " + 0);
            // FIXME: this assumes 0 isn't a valid connection. is that guaranteed?
            Character local = new Character(Collision.START_POS_X,
                    Collision.START_POS_Y + 40,
                    Collision.START_POS_Z);
            registerEntity(local);
            local.setController(Game.mCamera);
            mCharControlMap.put(0, getEntityId(local));

            // Create NPC
            Character npc = new Character(Collision.START_POS_X,
                    Collision.START_POS_Y + 40,
                    Collision.START_POS_Z + 10);
            registerEntity(npc);
            npc.setController(new NPCController());
        }

        // new connection: create a character
        for (Connection c : ((Server) mEndPoint).getConnections()) {
            if (!mCharControlMap.containsKey(c.getID())) {
                System.err.println("creating char for connection " + c.getID());
                // create new character in entity map (TODO: added automatically)
                Character newChar = new Character(Collision.START_POS_X,
                        Collision.START_POS_Y + 40,
                        Collision.START_POS_Z);
                registerEntity(newChar);

                // add new character id to mCharControlMap
                int charId = getEntityId(newChar);
                mCharControlMap.put(c.getID(), charId);

                // tell client their character ID
                StartMessage m = new StartMessage();
                m.characterEntity = charId;
                ((Server)mEndPoint).sendToTCP(c.getID(), m);
            }
        }

        for (Entity e : mEntityMap.values()) {
            if (e instanceof Character) {
                ((Character)e).setupMove(frameTime);
            }
        }
    }

    @Override
    public void postMove(double frameTime) {
        for (Entity e : mEntityMap.values()) {
            if (e instanceof Character) {
                ((Character)e).postMove();
            }
        }

        StateMessage serverState = new StateMessage();
        serverState.timestamp = frameTime;
        serverState.data = getEntityChanges();
        sendToAllTCP(serverState); // TODO: UDP
    }
}
