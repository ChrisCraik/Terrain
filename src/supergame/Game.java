
package supergame;

import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import supergame.graphics.GLGraphics;
import supergame.graphics.Graphics;
import supergame.modify.ChunkCastle;
import supergame.network.GameEndPoint;

import java.nio.FloatBuffer;
import java.util.Random;

public class Game {
    private static final String WINDOW_TITLE = "SUPER VOXEL TEST";
    public static final int FLOAT_SIZE = 4;

    public static boolean isRunning() {
        return !done;
    }

    public static Config mConfig;

    private static boolean done = false;

    public static Collision collision;
    /* TIMING */
    public static int delta = 0;
    public static boolean heartbeatFrame = false;
    private static int msSinceLastHeartbeat = 0;

    private boolean mIsFullscreen = false;
    private Camera mCamera = null;
    private boolean mF1 = false;

    private DisplayMode mDisplayMode;
    private GameEndPoint mGameEndPoint;

    private ChunkManager mChunkManager;
    private InputProcessor mInputProcessor;

    private long mLastFrame = getTime();
    private int mFps = 0;
    private long mLastFPS = 0;

    private void updateDelta() {
        long time = getTime();
        delta = (int) (time - mLastFrame);
        mLastFrame = time;

        // compute heartbeat
        msSinceLastHeartbeat += delta;
        if (msSinceLastHeartbeat > Config.MS_PER_HEARTBEAT) {
            heartbeatFrame = true;
            msSinceLastHeartbeat = 0;
        } else
            heartbeatFrame = false;

        // Update FPS display
        mLastFPS += Game.delta;
        if (mLastFPS > 1000) {
            Display.setTitle("FPS: " + mFps);
            mLastFPS = 0;
            mFps = 0;
        }
        mFps++;
    }

    private static long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    public void run(boolean fullscreen) {
        updateDelta();

        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        try {
            init();
            this.mIsFullscreen = fullscreen;
            collision = new Collision();
            mChunkManager = new ChunkManager(Collision.START_POS_X / Config.CHUNK_DIVISION,
                    Collision.START_POS_Y / Config.CHUNK_DIVISION,
                    Collision.START_POS_Z / Config.CHUNK_DIVISION,
                    Config.CHUNK_LOAD_DISTANCE);
            mCamera = new Camera();
            mInputProcessor = new InputProcessor();

            while (!done) {
                // System.out.println("----------");
                Graphics.instance.clear();

                PROFILE("GL draw");
                updateDelta();
                PROFILE("updateDelta");
                pollVideo();
                PROFILE("pollVideo");
                mCamera.pollInput();
                PROFILE("pollInput");

                Graphics.instance.switchTo3d((float) mDisplayMode.getWidth()
                        / mDisplayMode.getHeight());
                render();

                if (mInputProcessor != null) {
                    Graphics.instance.switchTo2d();
                    mGameEndPoint = mInputProcessor.processInput();

                    if (mGameEndPoint != null) {
                        // TODO: connect or host. game is starting.
                        collision.createCharacter();
                        mCamera.setControllable(collision.mCharacter);
                        mInputProcessor = null;
                    }
                }
                Display.update();
                PROFILE("Display update");
                Display.sync(Config.FRAME_CAP);
                PROFILE("Display sync");
            }
            Display.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void pollVideo() {
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) { // Exit if Escape is pressed
            done = true;
        }
        if (Display.isCloseRequested()) { // Exit if window is closed
            done = true;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F1) && !mF1) { // Is F1 Being Pressed?
            mF1 = true; // Tell Program F1 Is Being Held
            switchMode(); // Toggle Fullscreen / Windowed Mode
        }
        if (!Keyboard.isKeyDown(Keyboard.KEY_F1)) { // Is F1 Being Pressed?
            mF1 = false;
        }
    }

    private void switchMode() {
        mIsFullscreen = !mIsFullscreen;
        try {
            Display.setFullscreen(mIsFullscreen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long endTime;

    public static void PROFILE(String label) {
        long tempTime = getTime();
        if (tempTime - endTime > 20)
            System.out.printf("%20s: %d\n", label + " took", (tempTime - endTime));
        endTime = tempTime;
    }

    private boolean render() {
        collision.stepSimulation(delta / 1000f);
        PROFILE("Collision");

        /*
        Vector3f center = collision.character.getPos();
        chunkManager.updatePosition((long)Math.floor(center.x/Config.CHUNK_DIVISION),
                (long)Math.floor(center.y/Config.CH    NK_DIVISION),
                (long)Math.floor(center.z/Config.CHUNK_DIVISION));
                */
        mChunkManager.updateWithPosition(0, 0, 0);
        PROFILE("Update pos");

        Graphics.instance.loadIdentity();
        mCamera.apply();
        PROFILE("Cam stuff");

        // re-apply the light each frame, after the camera transformations
        Graphics.instance.createLight();

        collision.render();
        PROFILE("Col rendr");

        mChunkManager.renderChunks(mCamera);
        PROFILE("chunk rdr");

        return true;
    }

    private void createWindow() throws Exception {
        Display.setFullscreen(mIsFullscreen);
        for (DisplayMode d : Display.getAvailableDisplayModes()) {
            if (d.getWidth() == Config.RESOLUTION_X && d.getHeight() == Config.RESOLUTION_Y
                    && d.getBitsPerPixel() == 32) {
                mDisplayMode = d;
                break;
            }
        }
        Display.setDisplayMode(mDisplayMode);
        Display.setTitle(WINDOW_TITLE);
        Display.create();
    }

    private void init() throws Exception {
        createWindow();

        Chunk.rayDistribution = new Vec3[Config.AMB_OCC_RAY_COUNT];
        Random gen = new Random(0);
        for (int i = 0; i < Config.AMB_OCC_RAY_COUNT; i++)
            Chunk.rayDistribution[i] = new Vec3(
                    gen.nextFloat() - 0.5f,
                    gen.nextFloat() - 0.5f,
                    gen.nextFloat() - 0.5f).normalize();

        System.out.println("have the threads!");
        Graphics.instance = new GLGraphics();
    }

    public static FloatBuffer makeFB(float[] floatarray) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(floatarray.length);
        fb.put(floatarray).flip();
        return fb;
    }

    public static void main(String args[]) {
        boolean fullscreen = false;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("fullscreen")) {
                fullscreen = true;
            }
        }

        Game thegame = new Game();
        ChunkCastle.create();
        thegame.run(fullscreen);
    }
}
