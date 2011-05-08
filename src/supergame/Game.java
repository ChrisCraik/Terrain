package supergame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Vector;

import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;

public class Game {
	private final int MAX_CHUNKS = 40;
	private final int MS_PER_HEARTBEAT = 1000;

	private boolean done = false;
	private boolean fullscreen = false;
	private Camera camera = null;
	private final String windowTitle = "SUPER VOXEL TEST";
	private boolean f1 = false;

	private DisplayMode displayMode;

	private Vector<Chunk> chunks;
	private PriorityQueue<Chunk> dirtyChunks;
	private ArrayList<ChunkBakerThread> bakerThreads;

	/* TIMING */
	public static int delta = 0;
	public static boolean heartbeatFrame = false;
	private static int msSinceLastHeartbeat = 0;

	private long lastFrame = 0;
	private int fps = 0;
	private long lastFPS = 0;

	private void updateDelta() {
		long time = getTime();
		delta = (int) (time - lastFrame);
		lastFrame = time;

		// compute heartbeat
		msSinceLastHeartbeat += delta;
		if (msSinceLastHeartbeat > MS_PER_HEARTBEAT) {
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

	private long getTime() {
		return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}

	public static void main(String args[]) {
		boolean fullscreen = false;
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("fullscreen")) {
				fullscreen = true;
			}
		}

		Game thegame = new Game();
		thegame.run(fullscreen);
	}

	public void run(boolean fullscreen) {
		updateDelta();

		try {
			init();
			this.fullscreen = fullscreen;
			this.camera = new Camera();
			while (!done) {
				for (ChunkBakerThread c : bakerThreads)
					c.run();

				updateDelta();
				pollVideo();
				camera.pollInput();
				render();
				Display.update();
				Display.sync(120);
			}
			cleanup();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
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

	private boolean render() {
		if (Game.heartbeatFrame)
			System.out.println("Clearing buffer, rendering chunks");

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // Clear the screen and the depth buffer
		GL11.glLoadIdentity(); // Reset The Current Modelview Matrix
		camera.apply();

		for (Chunk c : chunks)
			c.render(camera);

		return true;
	}

	private void createWindow() throws Exception {
		Display.setFullscreen(fullscreen);
		for (DisplayMode d : Display.getAvailableDisplayModes()) {
			if (d.getWidth() == 1280 && d.getHeight() == 1024 && d.getBitsPerPixel() == 32) {
				displayMode = d;
				break;
			}
		}
		Display.setDisplayMode(displayMode);
		Display.setTitle(windowTitle);
		Display.create();
	}

	private void init() throws Exception {
		createWindow();

		chunks = new Vector<Chunk>();
		dirtyChunks = new PriorityQueue<Chunk>();
		long x, y, z;
		for (x = 0; x < MAX_CHUNKS; x++)
			for (y = 0; y < Math.min(5, MAX_CHUNKS); y++)
				for (z = 0; z < MAX_CHUNKS; z++)
					dirtyChunks.add(new Chunk(x, y, z));

		bakerThreads = new ArrayList<ChunkBakerThread>();
		for (int i = 0; i < 8; i++)
			bakerThreads.add(new ChunkBakerThread(dirtyChunks, chunks));

		initGL();
	}

	public static final int FLOAT_SIZE = 4;

	private FloatBuffer makeFB(float[] floatarray) {
		FloatBuffer fb = ByteBuffer.allocateDirect(floatarray.length * FLOAT_SIZE).order(ByteOrder.nativeOrder())
				.asFloatBuffer();
		fb.put(floatarray).flip();
		return fb;
	}

	private void makeLight(int light, float[] diffuse, float[] specular, float[] ambient, float[] position) {
		GL11.glLight(light, GL11.GL_DIFFUSE, makeFB(diffuse)); // color of the direct illumination
		GL11.glLight(light, GL11.GL_SPECULAR, makeFB(specular)); // color of the highlight
		GL11.glLight(light, GL11.GL_AMBIENT, makeFB(ambient)); // color of the reflected light
		GL11.glLight(light, GL11.GL_POSITION, makeFB(position));
		GL11.glEnable(light);
	}

	private void initGL() {
		GL11.glEnable(GL11.GL_TEXTURE_2D); // Enable Texture Mapping
		//GL11.glShadeModel(GL11.GL_SMOOTH); // Enable Smooth Shading
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Black Background
		GL11.glClearDepth(1.0); // Depth Buffer Setup
		GL11.glEnable(GL11.GL_DEPTH_TEST); // Enables Depth Testing
		GL11.glDepthFunc(GL11.GL_LEQUAL); // The Type Of Depth Testing To Do
		GL11.glEnable(GL11.GL_CULL_FACE); // Don't render the backs of triangles

		GL11.glMatrixMode(GL11.GL_PROJECTION); // Select The Projection Matrix
		GL11.glLoadIdentity(); // Reset The Projection Matrix

		// Calculate The Aspect Ratio Of The Window
		org.lwjgl.util.glu.GLU.gluPerspective(Camera.angle,
				(float) displayMode.getWidth() / (float) displayMode.getHeight(), 0.1f, 200.0f);
		GL11.glMatrixMode(GL11.GL_MODELVIEW); // Select The Modelview Matrix

		// Really Nice Perspective Calculations
		GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);

		//Fog!
		GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
		GL11.glFogf(GL11.GL_FOG_DENSITY, 0.03f);
		GL11.glFogf(GL11.GL_FOG_START, 80.0f);
		GL11.glFogf(GL11.GL_FOG_END, 150.0f);
		GL11.glEnable(GL11.GL_FOG);

		//Lighting!
		makeLight(GL11.GL_LIGHT0, new float[] { 0, 1, 0, 1 }, new float[] { 0, 1, 0, 1 }, new float[] { 0, 1, 0, 1 },
				new float[] { 0, 1, 0, 0 });
		GL11.glEnable(GL11.GL_NORMALIZE);
		//GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, makeFB(new float[] { 0.8f, 0.8f, 0.8f, 1f }));
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	private static void cleanup() {
		Display.destroy();
	}
}