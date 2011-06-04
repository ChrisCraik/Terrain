package supergame;

import java.lang.reflect.*;

public class Config {
	public static int MS_PER_HEARTBEAT = 1000;
	public static int WORKER_THREADS = 3;
	public static int RESOLUTION_X = 1024;
	public static int RESOLUTION_Y = 768;
	
	// CHUNKS
	public static float METERS_PER_SUBCHUNK = 1;
	public static int CHUNK_DIVISION = 32;
	public static int CHUNK_LOAD_DISTANCE = 5;
	public static int CHUNK_CACHE_SIZE = 4096;
	
	// GRAPHICS
	public static boolean USE_DEBUG_COLORS = false;
	public static boolean USE_SMOOTH_SHADE = true;
	
	// AMBIENT OCCLUSION
	public static boolean USE_AMBIENT_OCCLUSION = false;
	public static int AMB_OCC_RAY_COUNT = 16;
	public static int AMB_OCC_BIGRAY_STEPS = 6;
	public static float AMB_OCC_BIGRAY_STEP_SIZE = 1f;
	
	//FRUSTUM CULLING
	public static boolean FRUSTUM_CULLING = true; //enable frustum culling
	public static boolean FROZEN_FRUSTUM_POS = false; //used to test view frustum culling
	public static boolean FROZEN_FRUSTUM_HEAD = false;
	public static boolean FROZEN_FRUSTUM_PITCH = false;
	
	//CONTROL
	public static boolean PLAYER_MIDAIR_CONTROL = false;
	public static float PLAYER_JUMP_SPEED = 25;
	
	//PHYSICS
	public static float PHYSICS_GRAVITY = 50;
	
	public static void setOption(String fieldname, String value) {
		try {
			Field f = Config.class.getField(fieldname);
			if (int.class.getClass().equals(f.getClass()))
				f.set(null, Integer.parseInt(value));
			else if (float.class.getClass().equals(f.getClass()))
				f.set(null, Float.parseFloat(value));
			else if (boolean.class.getClass().equals(f.getClass()))
				f.set(null, Boolean.parseBoolean(value));
			else if (String.class.getClass().equals(f.getClass()))
				f.set(null, value);
		} catch (SecurityException e) {
			System.err.println("Security Exception!");
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			System.err.println("Attempted to set a non-existant option"+fieldname);
			e.printStackTrace();
		} catch (NumberFormatException e) {
			System.err.println(fieldname+" could not be parsed from "+value);
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void parseConfig(String [][] settings) {		
		for (String[] optionValue : settings)
			setOption(optionValue[0], optionValue[1]);

	}
}
