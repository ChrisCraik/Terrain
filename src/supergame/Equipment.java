
package supergame;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import supergame.modify.BlockChunkModifier;
import supergame.modify.SphereChunkModifier;

import javax.vecmath.Vector3f;

public class Equipment {
    static final int NOTHING = 0;
    static final int TROWEL = 1;
    static final int SHOVEL = 2;
    static final int BLOCK_SHOVEL = 3;
    static final int BIG_BLOCK_SHOVEL = 4;
    static final int ROCKET = 5;
    static final float MIN_TARGET = 3;
    static final float MAX_TARGET = 10;

    private int mTool = 0;
    private float mTargetDistance = 5;

    private final Vector3f mTargetPos = new Vector3f();
    private final Vector3f mTargetVoxelPos = new Vector3f();

    private static int mMsSinceShoot = 0;

    public void updatePositionLook(Vector3f position, float heading, float pitch) {
        Vec3.HPVector(mTargetPos, 180 - heading, pitch);
        mTargetPos.scale(mTargetDistance);
        mTargetPos.add(position);

        // align the target to a voxel
        mTargetVoxelPos.set(
                (float) Math.floor(mTargetPos.x + 0.5),
                (float) Math.floor(mTargetPos.y + 0.5),
                (float) Math.floor(mTargetPos.z + 0.5));
    }

    public void render() {
        GL11.glPushMatrix();
        GL11.glTranslatef(mTargetPos.x, mTargetPos.y, mTargetPos.z);
        Game.collision.drawCube(0.1f, 0.1f, 0.1f);
        GL11.glPopMatrix();

        if (mTool == NOTHING)
            return;

        GL11.glPushMatrix();
        GL11.glTranslatef(mTargetVoxelPos.x, mTargetVoxelPos.y, mTargetVoxelPos.z);
        Game.collision.drawCube(1, 0.02f, 0.02f);
        Game.collision.drawCube(0.02f, 1, 0.02f);
        Game.collision.drawCube(0.02f, 0.02f, 1);
        GL11.glPopMatrix();
    }

    public void pollInput() {
        // TODO: make this not dumb
        if (Keyboard.isKeyDown(Keyboard.KEY_0)) {
            mTool = 0;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_1)) {
            mTool = 1;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_2)) {
            mTool = 2;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_3)) {
            mTool = 3;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_4)) {
            mTool = 4;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_5)) {
            mTool = 5;
        }

        while (Keyboard.next()) {
            if (Keyboard.getEventKey() == Keyboard.KEY_UP
                    && Keyboard.getEventKeyState()) {
                mTargetDistance += 1;
            } else if (Keyboard.getEventKey() == Keyboard.KEY_DOWN
                    && Keyboard.getEventKeyState()) {
                mTargetDistance -= 1;
            }
        }
        mTargetDistance += Mouse.getDWheel() / 1000.0;

        mTargetDistance = Math.min(mTargetDistance, MAX_TARGET);
        mTargetDistance = Math.max(mTargetDistance, MIN_TARGET);

        mMsSinceShoot += Game.delta;
        if (mMsSinceShoot > 200 && (Mouse.isButtonDown(0) || Mouse.isButtonDown(1))) {
            mMsSinceShoot = 0;

            float increment = 0.5f;
            if (Mouse.isButtonDown(1))
                increment *= -1;

            switch (mTool) {
                case (TROWEL):
                    new BlockChunkModifier(mTargetVoxelPos, new Vector3f(0.5f, 0.5f, 0.5f), increment);
                    break;

                case (SHOVEL):
                    new SphereChunkModifier(mTargetVoxelPos, 2, Mouse.isButtonDown(0));
                    break;

                case (BLOCK_SHOVEL):
                    new BlockChunkModifier(mTargetVoxelPos, new Vector3f(1, 1, 1), increment);
                    break;

                case (BIG_BLOCK_SHOVEL):
                    new BlockChunkModifier(mTargetVoxelPos, new Vector3f(2, 2, 2), increment * 2);
                    break;

                case (ROCKET):
                    // TODO: fire cube!
                    new SphereChunkModifier(mTargetVoxelPos, 3, false);
                    break;
            }
        }
    }
}
