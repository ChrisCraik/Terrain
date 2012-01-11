
package supergame.network;

import java.util.HashMap;

public class Structs {
    public static class StateMessage {
        public double timestamp;
        public HashMap<Integer, EntityData> data;
    }

    public static class StartMessage {
        public int characterEntity;
    }

    public static class ControlMessage {
        public double timestamp;
        public float x;
        public float z;
        public float heading;
        public float pitch;
        public boolean jump;
        // TODO: event list
    }

    public static abstract class EntityData {
    };

    public static abstract class Entity {
        PiecewiseLerp mLerp;

        abstract public void apply(double timestamp, EntityData packet);

        abstract public EntityData getState();
    }
}
