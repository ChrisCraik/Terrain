
package supergame.character;

import org.lwjgl.opengl.GL11;

import supergame.Collision;
import supergame.Config;
import supergame.Game;
import supergame.network.PiecewiseLerp;
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
    private float mHeading, mPitch;
    private final Equipment mEquipment = new Equipment();

    private Controller mController = null;
    private ControlMessage mControlMessage = new ControlMessage();
    private final PiecewiseLerp mStateLerp = new PiecewiseLerp(5);

    // temporary vectors used for intermediate calculations. should not be
    // queried outside of the functions that set them.
    private final Vector3f mPosition = new Vector3f();

    public Character() {
        mCharacterId = Game.collision.mPhysics.createCharacter(Collision.START_POS_X,
                Collision.START_POS_Y + 40,
                Collision.START_POS_Z);
        mHeading = 0;
        mPitch = 0;
    }

    public void render() {
        Game.collision.getPhysics().queryCharacterPosition(mCharacterId, mPosition);
        GL11.glPushMatrix();
        GL11.glTranslatef(mPosition.x, mPosition.y, mPosition.z);
        GL11.glRotatef(mHeading, 0, 1, 0);
        GL11.glRotatef(mPitch, 1, 0, 0);
        Game.collision.drawCube(2, 2, 2);
        GL11.glPopMatrix();

        mEquipment.updatePositionLook(mPosition, mHeading, mPitch);
        mEquipment.render();
    }

    public void setController(Controller c) {
        mController = c;
    }

    public void setControlMessage(ControlMessage message) {
        // FIXME: only allow on server side, for remote players
        mControlMessage = message;
    }

    public void setupMove(double frameTime) {
        if (mController != null) {
            // local controller
            mController.control(frameTime, mControlMessage);
            mEquipment.pollInput();
        }
        mHeading = mControlMessage.heading;
        mPitch = mControlMessage.pitch;

        Vector3f walkDirection = new Vector3f(mControlMessage.x, 0, mControlMessage.z);
        if (walkDirection.length() > 1f)
            walkDirection.normalize();
        walkDirection.scale(0.15f);

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
    public void apply(double timestamp, EntityData packet) {
        // store position / look angles into interpolation window
        assert (packet instanceof CharacterData);
        CharacterData data = (CharacterData) packet;
        mStateLerp.addSample(timestamp, data.array);
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
    public void sample(double frameTime, float bias) {
        // sample interpolation window
        mStateLerp.sample(frameTime, lerpFloats);

        Vector3f pos = new Vector3f(lerpFloats[0], lerpFloats[1], lerpFloats[2]);
        mHeading = lerpFloats[3];
        mPitch = lerpFloats[4];

        Game.collision.getPhysics().setCharacterPosition(mCharacterId, pos, bias);
    }

    public ControlMessage getControl() {
        return mControlMessage;
    }
}
