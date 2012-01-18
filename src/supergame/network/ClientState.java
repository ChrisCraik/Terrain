package supergame.network;

import com.esotericsoftware.kryonet.Server;

import supergame.Collision;
import supergame.character.Character;
import supergame.network.Structs.StartMessage;

public class ClientState {
    public final Character mCharacter;
    public final int mCharacterId;
    private String mName = null;

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

    public void setName(String name) {
        // TODO: set char name as well
        mName = name;
    }

    public String getName() {
        return mName;
    }
}
