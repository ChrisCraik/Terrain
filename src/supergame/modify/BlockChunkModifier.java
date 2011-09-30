package supergame.modify;

import java.util.Vector;

import javax.vecmath.Vector3f;

import supergame.ChunkIndex;

public class BlockChunkModifier extends ChunkModifier {
	private Vector3f mPosition;
	private Vector3f mSize;

	public BlockChunkModifier(Vector3f position, Vector3f size) {
		mPosition = position;
		mSize = size;
	}

	@Override
	public float getModification(Vector3f p) {
		if (p.x < mPosition.x - mSize.x || p.x > mPosition.x + mSize.x)
			return 0;
		if (p.y < mPosition.y - mSize.y || p.y > mPosition.y + mSize.y)
			return 0;
		if (p.z < mPosition.z - mSize.z || p.z > mPosition.z + mSize.z)
			return 0;
		return 5;
	}

	@Override
	protected Vector<ChunkIndex> getIndexList() {
		return getBoundingIndexList(
				mPosition.x, mPosition.y, mPosition.z,
				mSize.x, mSize.y, mSize.z);
	}
}
