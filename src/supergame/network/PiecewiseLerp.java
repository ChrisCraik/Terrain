
package supergame.network;

import java.util.Iterator;
import java.util.LinkedList;

public class PiecewiseLerp {
    private class Sample {
        public Sample(double timestamp, float[] value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Sample at " + timestamp + ", values " + value[0] + "...";
        }

        double timestamp;
        float[] value;
    }

    private final int mSampleCount;
    private final LinkedList<Sample> mSamples;

    public PiecewiseLerp(int sampleCount) {
        mSampleCount = sampleCount;
        mSamples = new LinkedList<Sample>();
    }

    /**
     * @param timestamp Time of sample
     * @param array Array to store in sample
     * @return true if sample inserted successfully
     */
    public boolean addSample(double timestamp, float[] array) {
        int index = mSamples.size();
        Iterator<Sample> iter = mSamples.descendingIterator();
        while (iter.hasNext()) {
            Sample s = iter.next();
            if (timestamp > s.timestamp)
                break;
            index--;
        }

        if (index == 0 && mSamples.size() == mSampleCount) {
            // if sample list is full, don't throw away newer samples
            return false;
        }

        mSamples.add(index, new Sample(timestamp, array));

        if (mSamples.size() > mSampleCount)
            mSamples.removeFirst();

        return true;
    }

    /**
     * @param timestamp Time of sample
     * @param array Array in which (interpolated) sample is stored/copied
     * @return true if sample was interpolated, false if extrapolated
     */
    public boolean sample(double timestamp, float[] array) {
        Sample older = null;
        Sample newer = null;

        // note: failure occurs if there are fewer than 2 samples
        Iterator<Sample> iter = mSamples.descendingIterator();
        while (iter.hasNext()) {
            Sample s = iter.next();
            newer = older;
            older = s;
            if (timestamp >= s.timestamp && newer != null) {
                break;
            }
        }

        assert (older != null);

        if (newer == null) {
            // only one sample...
            for (int i = 0; i < array.length; i++) {
                array[i] = older.value[i];
            }
            return timestamp == older.timestamp;
        }

        // do the linear interpolation
        double delta = newer.timestamp - older.timestamp;
        float oldWeight = (float) ((newer.timestamp - timestamp) / delta);

        for (int i = 0; i < array.length; i++) {
            array[i] = oldWeight * older.value[i] + (1 - oldWeight)
                    * newer.value[i];
        }

        // if oldweight is between 0 and 1 inclusive, both weights are positive
        return oldWeight >= 0 && oldWeight <= 1;
    }

    /**
     * Copy the most recent sample into 'array'
     *
     * @param array
     */
    public void sampleLatest(float[] array) {
        Sample s = mSamples.getLast();
        for (int i = 0; i < array.length; i++) {
            array[i] = s.value[i];
        }
    }
}
