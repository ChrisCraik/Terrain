package supergame.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import supergame.network.PiecewiseLerp;

public class PiecewiseLerpTest {
	private PiecewiseLerp mInterp;
	private static float[][] samples;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		samples = new float[][] { { 4, 1 }, { 3, 2 }, { 2, 3 }, { 1, 4 } };
	}

	@Before
	public void setUp() throws Exception {
		mInterp = new PiecewiseLerp(samples.length);
	}

	/**
	 * Test method for {@link com.wholesomeindustries.networking.PiecewiseLinearInterpolator#addSample(double, float[])}.
	 */
	@Test
	public void testAddSample() {

		for (int i = 0; i < samples.length; i++)
			assertTrue(mInterp.addSample(i, samples[i]));
		assertFalse(mInterp.addSample(-100, new float[] { 0, 0 }));
	}

	/**
	 * Test method for {@link com.wholesomeindustries.networking.PiecewiseLinearInterpolator#sample(double, float[])}.
	 */
	@Test
	public void testSample() {
		float[] read = new float[] { 0, 0 };
		float[] expected = new float[] { 0, 0 };

		for (int i = 0; i < samples.length; i++)
			mInterp.addSample(i, samples[i]);

		for (int i = 0; i < samples.length; i++) {
			mInterp.sample(i, read);
			assertArrayEquals(samples[i], read, 0.01f);
		}

		// test INTERpolation
		for (int i = 0; i < samples.length - 1; i++) {
			expected[0] = (samples[i][0] + samples[i + 1][0]) / 2f;
			expected[1] = (samples[i][1] + samples[i + 1][1]) / 2f;
			assertTrue(mInterp.sample(i + 0.5f, read));
			assertArrayEquals(expected, read, 0.01f);
		}

		// test EXTRApolation
		expected[0] = 2 * samples[samples.length - 1][0] - samples[samples.length - 2][0];
		expected[1] = 2 * samples[samples.length - 1][1] - samples[samples.length - 2][1];
		assertFalse(mInterp.sample(samples.length, read));
		assertArrayEquals(expected, read, 0.01f);

		expected[0] = 2 * samples[0][0] - samples[1][0];
		expected[1] = 2 * samples[0][1] - samples[1][1];
		assertFalse(mInterp.sample(-1, read));
		assertArrayEquals(expected, read, 0.01f);
	}

}