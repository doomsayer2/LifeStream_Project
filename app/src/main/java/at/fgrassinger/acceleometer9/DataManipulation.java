package at.fgrassinger.acceleometer9;

/**
 * This class is mainly used for data manipulation. It provides methods for filtering the data
 * with various filters.
 * Created by admin on 14.12.2015.
 */
public final class DataManipulation {

    private DataManipulation() {}                   //Prevent instantiate for anyone

    /**
     * This function is used in order to smooth the accelerometer values or other values which are
     * passed ot it.
     * @see //http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
     * @see //http://en.wikipedia.org/wiki/Low-pass_filter#Simple_infinite_impulse_response_filter
     *
     * @param input The array to process
     * @param output The result array
     * @param alpha The cut-off value from 0.0 - 1.0
     * @return The filtered results
     */
    public static float[] lowPass(float[] input, float[] output, double alpha) {
        if (output == null) return input;

        for ( int i = 0; i < input.length; i++ ) {
            output[i] = (float) (output[i] + alpha * (input[i] - output[i]));
        }
        return output;
    }

    /**
     * This function is used to smooth the accelerometer values again by filtering out low frequencies.
     * @see //http://developer.android.com/reference/android/hardware/SensorEvent.html
     * @param x The x Axis values
     * @param y The y Axis values
     * @param z The z Axis values
     * @return filtered or smoothed values
     */
    public static float[] highPass(float x, float y, float z, double alpha) {
        float[] gravity = {0,0,0};                 //Store the gravity for the high pass filter
        float[] filteredValues = new float[3];

        gravity[0] = (float) (alpha * gravity[0] + (1 - alpha) * x);
        gravity[1] = (float) (alpha * gravity[1] + (1 - alpha) * y);
        gravity[2] = (float) (alpha * gravity[2] + (1 - alpha) * z);

        filteredValues[0] = x - gravity[0];
        filteredValues[1] = y - gravity[1];
        filteredValues[2] = z - gravity[2];

        return filteredValues;
    }

}
