
package supergame.modify;

import javax.vecmath.Vector3f;

public interface ChunkModifierInterface {
    public float getModification(Vector3f p, float current);

    public void chunkCompletion();
}
