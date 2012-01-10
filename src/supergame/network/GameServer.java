
package supergame.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;

import supergame.character.Character;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
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
    public <K extends Entity> void registerEntity(K entity) {
        registerEntity(entity, mNextEntityId++);
    }

    public void sendToAllTCP(Object o) {
        ((Server) mEndPoint).sendToAllTCP(o);
    }

    public void bind(int tcp, int udp) throws IOException {
        ((Server) mEndPoint).start(); // there's probably a reason not to do this here...
        ((Server) mEndPoint).bind(tcp, udp);
    }


    HashMap<Integer, Integer> mCharControlMap = new HashMap<Integer, Integer>(); // maps connection to character

    @Override
    public void stepWorld(double frameTime) {
        // if a connection doesn't remain, delete the char
        for (Integer connectionId : mCharControlMap.keySet()) {
            for (Connection c : ((Server) mEndPoint).getConnections()) {
                if (c.getID() == connectionId)
                    continue;
            }

            Integer charId = mCharControlMap.remove(connectionId);
            // TODO: check null
            mEntityMap.remove(charId);
        }

        // new connection: create a character
        for (Connection c : ((Server) mEndPoint).getConnections()) {
            if (!mCharControlMap.containsKey(c.getID())) {
                Character newChar = new Character();
                // TODO: create new character in entity map (added automatically)
                // TODO: add new character id to mCharControlMap
            }
        }
    }

    @Override
    public void postCollide(double frameTime) {
        StateMessage serverState = new StateMessage();
        serverState.timestamp = frameTime;
        serverState.data = getEntityChanges();
        sendToAllTCP(serverState); // TODO: UDP
    }
}
