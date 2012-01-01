
package supergame.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import supergame.network.PiecewiseLerp;
import supergame.network.Structs.Entity;
import supergame.network.Structs.EntityData;
import supergame.test.NetworkTest.Transmit;

import java.util.Collection;

public class NetworkTestHelpers {

    public static void testSimpleEntityTransmit(Transmit t) {
        // create a server side object, verify it's replicated client side
        TestSimpleEntity serverEnt = new TestSimpleEntity();
        t.server.registerEntity(serverEnt);

        // send server data to client (and create remote object)
        t.transmitServerToClient(-1);

        // verify entity on client
        Collection<Entity> entities = t.client.getEntities();
        assertEquals(entities.size(), 1);
        TestSimpleEntity clientEnt = (TestSimpleEntity) entities.iterator()
                .next();

        for (int i = 0; i < 5; i++) {
            // up to date....
            assertEquals(clientEnt.mX, serverEnt.mX, 0.001);
            assertEquals(clientEnt.mY, serverEnt.mY, 0.001);

            // modify source object
            serverEnt.update();

            // inconsistent!
            assertEquals(clientEnt.mX + 1, serverEnt.mX, 0.001);
            assertEquals(clientEnt.mY - 1, serverEnt.mY, 0.001);

            // send server data to client
            t.transmitServerToClient(i);
        }
        t.finish();
    }

    public static void testInterpEntityTransmit(Transmit t) {
        // create a server side object, verify it's replicated client side
        TestInterpEntity serverEnt = new TestInterpEntity();
        t.server.registerEntity(serverEnt);
        serverEnt.update(-1, -1, 1);

        // send server data to client (and create remote object)
        t.transmitServerToClient(-1);

        // verify entity on client
        Collection<Entity> entities = t.client.getEntities();
        assertEquals(entities.size(), 1);
        TestInterpEntity clientEnt = (TestInterpEntity) entities.iterator().next();

        float cPos[] = new float[2];
        float sPos[] = new float[2];
        for (int i = 0; i < 5; i++) {
            double timestamp = i;

            // up to date....
            assertTrue(serverEnt.sample(timestamp - 1, sPos));
            assertTrue(clientEnt.sample(timestamp - 1, cPos));
            assertArrayEquals(cPos, sPos, 0.001f);

            // modify source object
            serverEnt.update(timestamp, i, -i);

            // surprise! still up to date!
            assertTrue(serverEnt.sample(timestamp, sPos));
            // false here - client is extrapolating
            assertFalse(clientEnt.sample(timestamp, cPos));

            if (i > 0) {
                // after 2 samples on client, it can extrapolate
                assertArrayEquals(cPos, sPos, 0.001f);
            }

            // send server data to client
            t.transmitServerToClient(timestamp);
        }
        t.finish();
    }

    public static class TestSimpleData extends EntityData {
        float pos[] = new float[2];
    }

    public static class TestSimpleEntity extends Entity {
        public float mX = 0, mY = 0;

        // class has NO CONSTRUCTOR or SINGLE CONSTRUCTOR WITHOUT ARGUMENTS

        @Override
        public void apply(double timestamp, EntityData packet) {
            assert (packet instanceof TestSimpleData);
            TestSimpleData data = (TestSimpleData) packet;
            mX = data.pos[0];
            mY = data.pos[1];
        }

        @Override
        public EntityData getState() {
            TestSimpleData t = new TestSimpleData();
            t.pos[0] = mX;
            t.pos[1] = mY;
            return t;
        }

        public void update() {
            mX += 1;
            mY -= 1;
        }
    }

    // unique data class solely for differentiation
    public static class TestInterpData extends TestSimpleData {
    };

    public static class TestInterpEntity extends Entity {
        PiecewiseLerp mPosition = new PiecewiseLerp(4);

        // class has NO CONSTRUCTOR or SINGLE CONSTRUCTOR WITHOUT ARGUMENTS

        @Override
        public void apply(double timestamp, EntityData packet) {
            assert (packet instanceof TestInterpData);
            TestInterpData data = (TestInterpData) packet;
            mPosition.addSample(timestamp, data.pos);
        }

        @Override
        public EntityData getState() {
            // TODO: avoid allocation, pass in object to populate
            TestInterpData t = new TestInterpData();
            mPosition.sampleLatest(t.pos);
            return t;
        }

        public boolean sample(double timestamp, float[] pos) {
            return mPosition.sample(timestamp, pos);
        }

        public void update(double timestamp, float x, float y) {
            // TODO: avoid the allocation, but make more clear that it's just
            // doing a store underneath, not a copy of the array
            mPosition.addSample(timestamp, new float[] {
                    x, y
            });
        }
    }
}
