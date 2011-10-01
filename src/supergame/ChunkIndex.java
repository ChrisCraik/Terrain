package supergame;

public class ChunkIndex {
	private final long x, y, z;

	public ChunkIndex(long x, long y, long z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ChunkIndex))
			return false;

		ChunkIndex ci = (ChunkIndex) o;

		return (ci.x == this.x && ci.y == this.y && ci.z == this.z);
	}

	@Override
	public int hashCode() {
		int hash;
		hash = Long.valueOf(x).hashCode();
		hash ^= Long.valueOf(y).hashCode()<<1;
		hash ^= Long.valueOf(z).hashCode()<<2;
		return hash;
	}

	public Vec3 getVec3() {
		return new Vec3(x, y, z);
	}

	@Override
	public String toString() {
		return String.format("ID:%d %d %d", x, y, z);
	}
}