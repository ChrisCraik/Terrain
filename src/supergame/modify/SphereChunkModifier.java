
package supergame.modify;

import supergame.ChunkIndex;

import java.util.Vector;

import javax.vecmath.Vector3f;

public class SphereChunkModifier extends ChunkModifier {

    private final Vector3f mPosition;
    private final float mRadius;
    private final boolean mPositive;

    public SphereChunkModifier(Vector3f position, float radius, boolean positive) {
        mPosition = position;
        mRadius = radius;
        mPositive = positive;
    }

    @Override
    protected Vector<ChunkIndex> getIndexList() {
        return getBoundingIndexList(
                mPosition.x, mPosition.y, mPosition.z,
                mRadius, mRadius, mRadius);
    }

    @Override
    public float getModification(Vector3f p, float current) {
        Vector3f origin = new Vector3f(mPosition);

        origin.sub(p);
        if (origin.length() <= mRadius) {
            // returning some positive constant here will give a vaguely round
            // shape, but giving a variant density over the surface smoothes it
            float newVal = 2 * (mRadius - origin.length());
            if (!mPositive)
                newVal *= -1;
            return Math.max(-1, Math.min(1, current + newVal));
        }

        return current;
    }
}
