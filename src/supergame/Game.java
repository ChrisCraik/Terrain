package supergame;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import supergame.modify.ChunkCastle;
import supergame.network.GameEndPoint;

public class Game {
	private static final String WINDOW_TITLE = "SUPER VOXEL TEST";
	public static final int FLOAT_SIZE = 4;

	public static boolean isRunning() {
		return !done;
	}

	public static Config mConfig;

	private static boolean done = false;
	private boolean fullscreen = false;
	private Camera camera = null;
	private boolean f1 = false;

	private DisplayMode displayMode;
	private GameEndPoint mGameEndPoint;

	private ChunkManager chunkManager;
	private InputProcessor inputProcessor;

	public static Collision collision;
	/* TIMING */
	public static int delta = 0;
	public static boolean heartbeatFrame = false;
	private static int msSinceLastHeartbeat = 0;

	private long lastFrame = getTime();
	private int fps = 0;
	private long lastFPS = 0;

	private void updateDelta() {
		long time = getTime();
		delta = (int) (time - lastFrame);
		lastFrame = time;

		// compute heartbeat
		msSinceLastHeartbeat += delta;
		if (msSinceLastHeartbeat > Config.MS_PER_HEARTBEAT) {
			heartbeatFrame = true;
			msSinceLastHeartbeat = 0;
		} else
			heartbeatFrame = false;

		// Update FPS display
		lastFPS += Game.delta;
		if (lastFPS > 1000) {
			Display.setTitle("FPS: " + fps);
			lastFPS = 0;
			fps = 0;
		}
		fps++;
	}

	private static long getTime() {
		return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}

	public void run(boolean fullscreen) {
		updateDelta();

		Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
		try {
			init();
			this.fullscreen = fullscreen;
			collision = new Collision();
			chunkManager = new ChunkManager(Collision.START_POS_X/Config.CHUNK_DIVISION,
					Collision.START_POS_Y/Config.CHUNK_DIVISION,
					Collision.START_POS_Z/Config.CHUNK_DIVISION,
					Config.CHUNK_LOAD_DISTANCE);
			camera = new Camera();
			inputProcessor = new InputProcessor();

			while (!done) {
				//System.out.println("----------");
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

				PROFILE("GL draw");
				updateDelta();
				PROFILE("updateDelta");
				pollVideo();
				PROFILE("pollVideo");
				camera.pollInput();
				PROFILE("pollInput");

				switchGL3d();
				render();


				if (inputProcessor != null) {
					switchGL2d();
					mGameEndPoint = inputProcessor.processInput();

					if (mGameEndPoint != null) {
						// TODO: connect or host. game is starting.
						collision.createCharacter();
						camera.setControllable(collision.character);
						inputProcessor = null;
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
		if (Keyboard.isKeyDown(Keyboard.KEY_F1) && !f1) { // Is F1 Being Pressed?
			f1 = true; // Tell Program F1 Is Being Held
			switchMode(); // Toggle Fullscreen / Windowed Mode
		}
		if (!Keyboard.isKeyDown(Keyboard.KEY_F1)) { // Is F1 Being Pressed?
			f1 = false;
		}
	}

	private void switchMode() {
		fullscreen = !fullscreen;
		try {
			Display.setFullscreen(fullscreen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static long endTime;
	public static void PROFILE(String label) {
		long tempTime = getTime();
		if (tempTime-endTime > 20)
			System.out.printf("%20s: %d\n", label+" took", (tempTime-endTime));
		endTime = tempTime;
	}
	private boolean render() {
		collision.stepSimulation(delta / 1000f);
		PROFILE("Collision");

		/*
		Vector3f center = collision.character.getPos();
		chunkManager.updatePosition((long)Math.floor(center.x/Config.CHUNK_DIVISION),
				(long)Math.floor(center.y/Config.CHUNK_DIVISION),
				(long)Math.floor(center.z/Config.CHUNK_DIVISION));
				*/
		chunkManager.updateWithPosition(0, 0, 0);
		PROFILE("Update pos");

		GL11.glLoadIdentity(); // Reset The Current Modelview Matrix
		camera.apply();
		PROFILE("Cam stuff");

		// re-apply the light each frame, after the camera transformations
		GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION,
				Game.makeFB(new float[] { 0, 1, 0.5f, 0 })); // from above!

		collision.render();
		PROFILE("Col rendr");

		chunkManager.renderChunks(camera);
		PROFILE("chunk rdr");

		return true;
	}

	private void createWindow() throws Exception {
		Display.setFullscreen(fullscreen);
		for (DisplayMode d : Display.getAvailableDisplayModes()) {
			if (d.getWidth() == Config.RESOLUTION_X && d.getHeight() == Config.RESOLUTION_Y && d.getBitsPerPixel() == 32) {
				displayMode = d;
				break;
			}
		}
		Display.setDisplayMode(displayMode);
		Display.setTitle(WINDOW_TITLE);
		Display.create();
	}

	private void init() throws Exception {
		createWindow();

		Chunk.rayDistribution = new Vec3[Config.AMB_OCC_RAY_COUNT];
		Random gen = new Random(0);
		for (int i = 0; i < Config.AMB_OCC_RAY_COUNT; i++)
			Chunk.rayDistribution[i] = new Vec3(gen.nextFloat() - 0.5f, gen.nextFloat() - 0.5f, gen.nextFloat() - 0.5f)
					.normalize();

		System.out.println("have the threads!");
		initGL3d();
		initGL2d();
	}

	public static FloatBuffer makeFB(float[] floatarray) {
		FloatBuffer fb = BufferUtils.createFloatBuffer(floatarray.length);
		fb.put(floatarray).flip();
		return fb;
	}

	private void switchGL2d() {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);

		// projection
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, Config.RESOLUTION_X, Config.RESOLUTION_Y, 0, 1, -1);

		// modelview
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
	}

	private void initGL2d() {
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	private void switchGL3d() {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glDisable(GL11.GL_BLEND);

		// projection
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		org.lwjgl.util.glu.GLU.gluPerspective(Camera.FOV,
				(float) displayMode.getWidth() / (float) displayMode.getHeight(), 0.1f, Camera.farD);

		// modelview
		GL11.glMatrixMode(GL11.GL_MODELVIEW); // Select The Modelview Matrix
		GL11.glLoadIdentity();
	}

	private void initGL3d() {
		GL11.glEnable(GL11.GL_TEXTURE_2D); // Enable Texture Mapping
		GL11.glShadeModel(GL11.GL_SMOOTH); // Enable Smooth Shading
		GL11.glClearColor(0.6f, 0.6f, 0.9f, 0.0f); // Sky
		GL11.glClearDepth(1.0); // Depth Buffer Setup
		GL11.glEnable(GL11.GL_DEPTH_TEST); // Enables Depth Testing
		GL11.glDepthFunc(GL11.GL_LEQUAL); // The Type Of Depth Testing To Do
		GL11.glEnable(GL11.GL_CULL_FACE); // Don't render the backs of triangles


		// Really Nice Perspective Calculations
		GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);

		//Fog!
		GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
		GL11.glFog(GL11.GL_FOG_COLOR, makeFB(new float[] { 0.6f, 0.6f, 0.9f, 1 }));
		GL11.glFogf(GL11.GL_FOG_DENSITY, 0.1f);
		GL11.glFogf(GL11.GL_FOG_START, 0.75f * Camera.farD);
		GL11.glFogf(GL11.GL_FOG_END, Camera.farD);
		GL11.glEnable(GL11.GL_FOG);

		//Lighting!
		int light = GL11.GL_LIGHT0;
		GL11.glLight(light, GL11.GL_DIFFUSE, makeFB(new float[] { 1, 1, 1, 1 })); // color of the direct illumination
		GL11.glLight(light, GL11.GL_SPECULAR, makeFB(new float[] { 1, 1, 1, 1 })); // color of the highlight
		GL11.glLight(light, GL11.GL_AMBIENT, makeFB(new float[] { 1, 1, 1, 1 })); // color of the reflected light
		GL11.glLight(light, GL11.GL_POSITION, makeFB(new float[] { 0, 1, 0, 0 })); // from above!
		GL11.glEnable(light);
		GL11.glEnable(GL11.GL_NORMALIZE);
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, makeFB(new float[] { 0.4f, 0.3f, 0.0f, 1 }));
		//GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, makeFB(new float[] { 0, 0, 0, 1 }));
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, makeFB(new float[] { 0, 0, 0, 1 }));
		GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, makeFB(new float[] { 0, 0, 0, 1 }));
		GL11.glEnable(GL11.GL_LIGHTING);
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