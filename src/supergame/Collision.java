
package supergame;

import org.lwjgl.opengl.GL11;

import supergame.physics.JavaPhysics;
import supergame.physics.NativePhysics;
import supergame.physics.Physics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Collision {

    public static final int START_POS_X = 0, START_POS_Y = 20, START_POS_Z = 0;

    // create 27 (3x3x3) dynamic objects
    private static final int ARRAY_SIZE_X = 3;
    private static final int ARRAY_SIZE_Y = 3;
    private static final int ARRAY_SIZE_Z = 3;

    public Character mCharacter;
    public Physics mPhysics;

    ArrayList<Long> mBodyList = new ArrayList<Long>();

    private static int displayList = -1;

    public Collision() {
        mPhysics = Config.PHYSICS_USE_NATIVE ? new NativePhysics() : new JavaPhysics();
        mPhysics.initialize(Config.PHYSICS_GRAVITY, Config.CHUNK_DIVISION);

        float start_x = START_POS_X - ARRAY_SIZE_X / 2;
        float start_y = START_POS_Y;
        float start_z = START_POS_Z - ARRAY_SIZE_Z / 2;

        mBodyList.add(mPhysics.createCube(15, 0, 0, 0, 0));

        float mass = 1;
        for (int k = 0; k < ARRAY_SIZE_Y; k++) {
            for (int i = 0; i < ARRAY_SIZE_X; i++) {
                for (int j = 0; j < ARRAY_SIZE_Z; j++) {
                    float x = 2f * i + start_x;
                    float y = 10f + 2f * k + start_y;
                    float z = 2f * j + start_z;
                    mBodyList.add(mPhysics.createCube(1, mass, x, y, z));
                }
            }
        }
    }

    public Physics getPhysics() {
        return mPhysics;
    }

    public void drawCube(float x, float y, float z) {
        GL11.glPushMatrix();
        GL11.glScalef(x, y, z);
        if (displayList == -1) {
            displayList = GL11.glGenLists(1);
            GL11.glNewList(displayList, GL11.GL_COMPILE_AND_EXECUTE);
            GL11.glBegin(GL11.GL_QUADS); // Start Drawing Quads
            // Front Face
            GL11.glNormal3f(0f, 0f, 1f); // Normal Facing Forward
            GL11.glVertex3f(-1f, -1f, 1f); // Bottom Left Of The Texture and
                                           // Quad
            GL11.glVertex3f(1f, -1f, 1f); // Bottom Right Of The Texture and
                                          // Quad
            GL11.glVertex3f(1f, 1f, 1f); // Top Right Of The Texture and Quad
            GL11.glVertex3f(-1f, 1f, 1f); // Top Left Of The Texture and Quad
            // Back Face
            GL11.glNormal3f(0f, 0f, -1f); // Normal Facing Away
            GL11.glVertex3f(-1f, -1f, -1f); // Bottom Right Of The Texture and
                                            // Quad
            GL11.glVertex3f(-1f, 1f, -1f); // Top Right Of The Texture and Quad
            GL11.glVertex3f(1f, 1f, -1f); // Top Left Of The Texture and Quad
            GL11.glVertex3f(1f, -1f, -1f); // Bottom Left Of The Texture and
                                           // Quad
            // Top Face
            GL11.glNormal3f(0f, 1f, 0f); // Normal Facing Up
            GL11.glVertex3f(-1f, 1f, -1f); // Top Left Of The Texture and Quad
            GL11.glVertex3f(-1f, 1f, 1f); // Bottom Left Of The Texture and Quad
            GL11.glVertex3f(1f, 1f, 1f); // Bottom Right Of The Texture and Quad
            GL11.glVertex3f(1f, 1f, -1f); // Top Right Of The Texture and Quad
            // Bottom Face
            GL11.glNormal3f(0f, -1f, 0f); // Normal Facing Down
            GL11.glVertex3f(-1f, -1f, -1f); // Top Right Of The Texture and Quad
            GL11.glVertex3f(1f, -1f, -1f); // Top Left Of The Texture and Quad
            GL11.glVertex3f(1f, -1f, 1f); // Bottom Left Of The Texture and Quad
            GL11.glVertex3f(-1f, -1f, 1f); // Bottom Right Of The Texture and
                                           // Quad
            // Right face
            GL11.glNormal3f(1f, 0f, 0f); // Normal Facing Right
            GL11.glVertex3f(1f, -1f, -1f); // Bottom Right Of The Texture and
                                           // Quad
            GL11.glVertex3f(1f, 1f, -1f); // Top Right Of The Texture and Quad
            GL11.glVertex3f(1f, 1f, 1f); // Top Left Of The Texture and Quad
            GL11.glVertex3f(1f, -1f, 1f); // Bottom Left Of The Texture and Quad
            // Left Face
            GL11.glNormal3f(-1f, 0f, 0f); // Normal Facing Left
            GL11.glVertex3f(-1f, -1f, -1f); // Bottom Left Of The Texture and
                                            // Quad
            GL11.glVertex3f(-1f, -1f, 1f); // Bottom Right Of The Texture and
                                           // Quad
            GL11.glVertex3f(-1f, 1f, 1f); // Top Right Of The Texture and Quad
            GL11.glVertex3f(-1f, 1f, -1f); // Top Left Of The Texture and Quad
            GL11.glEnd();// Done Drawing Quads
            GL11.glEndList();
        } else
            GL11.glCallList(displayList);
        GL11.glPopMatrix();
    }

    public void stepSimulation(float duration) {
        if (mCharacter != null) {
            mCharacter.move();
        }
        mPhysics.stepSimulation(duration, 10);
    }

    private static ByteBuffer matrix = ByteBuffer.allocateDirect(16 * 4).order(
            ByteOrder.nativeOrder());

    public void render() {
        if (mCharacter != null) {
            mCharacter.render();
        }
        for (int i = 0; i < mBodyList.size(); i++) {
            int size = (i == 0) ? 15 : 1;
            mPhysics.queryObject(mBodyList.get(i), matrix);

            GL11.glPushMatrix();
            GL11.glMultMatrix(matrix.asFloatBuffer());
            drawCube(size, size, size);
            GL11.glPopMatrix();
        }
    }

    public void createCharacter() {
        mCharacter = new Character(mPhysics.createCharacter(START_POS_X, START_POS_Y + 40,
                START_POS_Z));
    }
}
