
package supergame.character;

import org.lwjgl.opengl.GL11;

import supergame.Config;
import supergame.Game;
import supergame.network.PiecewiseLerp;
import supergame.network.Structs.ChatMessage;
import supergame.network.Structs.ControlMessage;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;

import javax.vecmath.Vector3f;

public class Character extends Entity {

    private final static int LERP_FIELDS = 5;

    public static class CharacterData extends EntityData {
        // state for any given character that the server sends to the client
        float array[] = new float[LERP_FIELDS]; // x,y,z, heading, pitch
    }

    public final long mCharacterId;
    private float mHeading = 0;
    private float mPitch = 0;
    private final Equipment mEquipment = new Equipment();

    private Controller mController = null;
    private ControlMessage mControlMessage = new ControlMessage();
    private final ChatMessage mChatMessage = new ChatMessage();
    private final PiecewiseLerp mStateLerp = new PiecewiseLerp(Config.CHAR_STATE_SAMPLES);

    // temporary vectors used for intermediate calculations. should not be
    // queried outside of the functions that set them.
    private final Vector3f mPosition = new Vector3f();

    public Character() {
        mCharacterId = Game.collision.mPhysics.createCharacter(0, 0, 0);
    }

    public Character(float x, float y, float z) {
        mCharacterId = Game.collision.mPhysics.createCharacter(x, y, z);
    }

    public void render() {
        Game.collision.getPhysics().queryCharacterPosition(mCharacterId, mPosition);
        GL11.glPushMatrix();
        GL11.glTranslatef(mPosition.x, mPosition.y, mPosition.z);
        GL11.glRotatef(mHeading, 0, 1, 0);
        GL11.glRotatef(mPitch, 1, 0, 0);
        Game.collision.drawCube(1, 1.5f, 1);
        GL11.glPopMatrix();

        mEquipment.updatePositionLook(mPosition, mHeading, mPitch);
        mEquipment.render();
    }

    public void setController(Controller c) {
        mController = c;
    }

    public void setControlMessage(ControlMessage message) {
        // FIXME: handle out of order messages

        // FIXME: only allow on server side, for remote players
        mControlMessage = message;
    }

    public void setChatMessage(ChatMessage chat) {
        mChatMessage.s = chat.s;
    }

    public void setupMove(double localTime) {
        if (mController != null) {
            // local controller
            mController.control(localTime, mControlMessage, mChatMessage);
            mEquipment.pollInput();
        }
        mHeading = mControlMessage.heading;
        mPitch = mControlMessage.pitch;

        Vector3f walkDirection = new Vector3f(mControlMessage.x, 0, mControlMessage.z);
        if (walkDirection.length() > 1f)
            walkDirection.normalize();

        // scale by frame time, since btKinematicCharacterController doesn't
        walkDirection.scale(Game.delta / 100f);

        float strengthIfJumping = Config.PLAYER_MIDAIR_CONTROL;
        Game.collision.getPhysics().controlCharacter(mCharacterId, strengthIfJumping,
                mControlMessage.jump, walkDirection.x, 0, walkDirection.z);
    }

    public void postMove() {
        Game.collision.getPhysics().queryCharacterPosition(mCharacterId, mPosition);

        if (mController != null) {
            mController.response(mPosition);
        }
    }

    @Override
    public void apply(double serverTime, EntityData packet) {
        // store position / look angles into interpolation window
        assert (packet instanceof CharacterData);
        CharacterData data = (CharacterData) packet;
        mStateLerp.addSample(serverTime, data.array);
    }

    @Override
    public EntityData getState() {
        CharacterData d = new CharacterData();
        // FIXME: use final ints to index into array
        d.array[0] = mPosition.x;
        d.array[1] = mPosition.y;
        d.array[2] = mPosition.z;
        d.array[3] = mHeading;
        d.array[4] = mPitch;
        return d;
    }

    private final float lerpFloats[] = new float[LERP_FIELDS];
    public void sample(double serverTime, float bias) {
        // sample interpolation window
        mStateLerp.sample(serverTime, lerpFloats);

        Vector3f pos = new Vector3f(lerpFloats[0], lerpFloats[1], lerpFloats[2]);
        mHeading = lerpFloats[3];
        mPitch = lerpFloats[4];

        Game.collision.getPhysics().setCharacterPosition(mCharacterId, pos, bias);
    }

    public ControlMessage getControl() {
        return mControlMessage;
    }

    public ChatMessage getChat() {
        return mChatMessage;
    }
}
