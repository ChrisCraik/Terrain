package supergame.modify;

import java.util.Vector;

import javax.vecmath.Vector3f;

import supergame.ChunkIndex;

public class SphereChunkModifier extends ChunkModifier {

	private final Vector3f mPosition;
	private final float mRadius;

	public SphereChunkModifier(Vector3f position, float radius) {
		mPosition = position;
		mRadius = radius;
	}

	@Override
	protected Vector<ChunkIndex> getIndexList() {
		return getBoundingIndexList(
				mPosition.x, mPosition.y, mPosition.z,
				mRadius, mRadius, mRadius);
	}

	@Override
	public float getModification(Vector3f p) {
		Vector3f origin = new Vector3f(mPosition);

		origin.sub(p);
		if (origin.length() <= mRadius) {
			// returning some positive constant here will give a vaguely round
			// shape, but giving a variant density over the surface smoothes it
			
			return 2*(mRadius - origin.length());
		}

		return 0;
	}
}
