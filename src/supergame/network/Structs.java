
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

    static final short OP_USE_PRIMARY = 1;
    static final short OP_USE_SECONDARY = 2;
    static final short OP_TOOL1 = 101;
    static final short OP_TOOL2 = 102;
    static final short OP_TOOL3 = 103;
    static final short OP_TOOL4 = 104;
    static final short OP_TOOL5 = 105;
    static final short OP_TOOL6 = 106;
    static final short OP_TOOL7 = 107;
    static final short OP_TOOL8 = 108;

    public static class ChatMessage {
        public String s = null;
    }

    public static class ControlMessage {
        public double timestamp;
        public float x;
        public float z;
        public float heading;
        public float pitch;
        public boolean jump;
        public boolean sprint;
        public boolean duck;
        public double controlTimes[];
        public short controlOps[];
    }

    public static abstract class EntityData {
    };

    public static abstract class Entity {
        PiecewiseLerp mLerp;

        abstract public void apply(double timestamp, EntityData packet);

        abstract public EntityData getState();
    }
}
