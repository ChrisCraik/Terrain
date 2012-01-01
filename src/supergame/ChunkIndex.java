
package supergame;

public class ChunkIndex {
    private final long mX, mY, mZ;

    public ChunkIndex(long x, long y, long z) {
        mX = x;
        mY = y;
        mZ = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ChunkIndex))
            return false;

        ChunkIndex ci = (ChunkIndex) o;

        return (ci.mX == mX && ci.mY == mY && ci.mZ == mZ);
    }

    @Override
    public int hashCode() {
        int hash;
        hash = Long.valueOf(mX).hashCode();
        hash ^= Long.valueOf(mY).hashCode() << 1;
        hash ^= Long.valueOf(mZ).hashCode() << 2;
        return hash;
    }

    public Vec3 getVec3() {
        return new Vec3(mX, mY, mZ);
    }

    @Override
    public String toString() {
        return String.format("ID:%d %d %d", mX, mY, mZ);
    }
}
