package supergame.network;

import com.esotericsoftware.kryonet.Server;

import supergame.Chunk;
import supergame.ChunkIndex;
import supergame.Collision;
import supergame.character.Character;
import supergame.network.Structs.StartMessage;

import java.util.HashMap;

public class ClientState {
    public final Character mCharacter;


    private final int mCharacterId;
    private String mName = null;
    private final HashMap<ChunkIndex, Chunk> mChunksToSync = new HashMap<ChunkIndex, Chunk>();

    public ClientState(GameServer server, int connectionId) {
        mCharacter = new Character(Collision.START_POS_X,
                Collision.START_POS_Y + 40,
                Collision.START_POS_Z);
        mCharacterId = server.registerEntity(mCharacter);

        if (connectionId < 1) {
            return;
        }

        // tell client their character ID
        StartMessage m = new StartMessage();
        m.characterEntity = mCharacterId;
        ((Server)server.mEndPoint).sendToTCP(connectionId, m);
    }

    public void disconnect(GameServer server) {
        // Client has disconnected.
        server.unregisterEntity(mCharacterId);
    }

    public void setName(String name) {
        // TODO: set char name as well
        mName = name;
    }

    public String getName() {
        return mName;
    }
}
