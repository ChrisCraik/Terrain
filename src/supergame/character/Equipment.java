
package supergame.character;

import org.lwjgl.opengl.GL11;

import supergame.Game;
import supergame.Vec3;
import supergame.modify.BlockChunkModifier;
import supergame.modify.SphereChunkModifier;
import supergame.network.Structs.ControlMessage;

import javax.vecmath.Vector3f;

public class Equipment {
    static final int NOTHING = 0;
    static final int TROWEL = 1;
    static final int SHOVEL = 2;
    static final float MIN_TARGET = 3;
    static final float MAX_TARGET = 10;

    private final Vector3f mTargetPos = new Vector3f();
    private final Vector3f mTargetVoxelPos = new Vector3f();

    private static int mMsSinceShoot = 0;

    public void render(Vector3f position, ControlMessage message, boolean localToolsAllowed) {
        Vec3.HPVector(mTargetPos, 180 - message.heading, message.pitch);
        mTargetPos.scale(message.targetDistance);
        mTargetPos.add(position);

        // align the target to a voxel
        mTargetVoxelPos.set(
                (float) Math.floor(mTargetPos.x + 0.5),
                (float) Math.floor(mTargetPos.y + 0.5),
                (float) Math.floor(mTargetPos.z + 0.5));

        GL11.glPushMatrix();
        GL11.glTranslatef(mTargetPos.x, mTargetPos.y, mTargetPos.z);
        Game.collision.drawCube(0.1f, 0.1f, 0.1f);
        GL11.glPopMatrix();

        if (message.toolSelection == NOTHING) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(mTargetVoxelPos.x, mTargetVoxelPos.y, mTargetVoxelPos.z);
        Game.collision.drawCube(1, 0.02f, 0.02f);
        Game.collision.drawCube(0.02f, 1, 0.02f);
        Game.collision.drawCube(0.02f, 0.02f, 1);
        GL11.glPopMatrix();

        if (localToolsAllowed) {
            useTool(message);
        }
    }

    private void useTool(ControlMessage message) {
        mMsSinceShoot += Game.delta;
        if (mMsSinceShoot > 300 && (message.use0 || message.use1)) {
            mMsSinceShoot = 0;

            float increment = 0.5f;
            if (message.use1) {
                increment *= -1;
            }

            switch (message.toolSelection) {
                case (TROWEL):
                    new BlockChunkModifier(mTargetVoxelPos, new Vector3f(0.5f, 0.5f, 0.5f), increment);
                    break;

                case (SHOVEL):
                    new SphereChunkModifier(mTargetVoxelPos, 2, message.use0);
                    break;
            }
        }
    }
}
