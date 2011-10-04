package supergame.modify;

import java.util.Vector;

import javax.vecmath.Vector3f;

import supergame.ChunkIndex;

public class BlockChunkModifier extends ChunkModifier {
	private final Vector3f mPosition;
	private final Vector3f mSize;
	private final float mIncrement;

	public BlockChunkModifier(Vector3f position, Vector3f size, float increment) {
		mPosition = position;
		mSize = size;
		mIncrement = increment;
	}

	@Override
	public float getModification(Vector3f p, float current) {
		if (p.x < mPosition.x - mSize.x || p.x > mPosition.x + mSize.x)
			return current;
		if (p.y < mPosition.y - mSize.y || p.y > mPosition.y + mSize.y)
			return current;
		if (p.z < mPosition.z - mSize.z || p.z > mPosition.z + mSize.z)
			return current;

		return Math.min(1.0f, mIncrement + current);
	}

	@Override
	protected Vector<ChunkIndex> getIndexList() {
		return getBoundingIndexList(
				mPosition.x, mPosition.y, mPosition.z,
				mSize.x, mSize.y, mSize.z);
	}
}
