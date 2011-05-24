/*
 * SpectralData.java
 *
 * The container for the data of one frame of the audio file
 */
package at.cp.jku.teaching.amprocessing;

/**
 *
 * @author andreas arzt
 */
public class SpectralData {

    // contains the magnitudes of the sinusoids
    public double[] magnitudes;
    // contains the phases of the sinusoids
    public double[] phases;
    //contains the unwrapped phases of the sinusoids
    public double[] unwrappedPhases;
    //contains the total energy in this frame
    public double totalEnergy;
    // the size of each of the above arrays (= fftSize/2 + 1)
    public int size;

    SpectralData(double[] reBuffer, double[] imBuffer, int fftSize) {
        size = fftSize / 2 + 1;
        phases = new double[size];
        unwrappedPhases = new double[size];
        magnitudes = new double[size];

        for (int i = 0; i < size; i++) {
            magnitudes[i] = reBuffer[i];
            totalEnergy += reBuffer[i];
            phases[i] = imBuffer[i];
        }
    }

    public void computeUnwrappedPhases(double[] uphases) {
        double cutoff = Math.PI;


        for (int i = 0; i < size; i++) {
            unwrappedPhases[i] = phases[i];

            double dp = phases[i] - uphases[i];
            double dps = normphase(dp);

            if (dps == -Math.PI && dp > 0) {
                dps = Math.PI;
            }
            if (Math.abs(dp) >= cutoff) {
                unwrappedPhases[i] += (dps - dp);
            }
        }
    }

    public static double normphase(final double ph) {
        return myfmod(ph + Math.PI, 2 * Math.PI) - Math.PI;
    }

    public static double myfmod(final double x, final double y) {
        return x - y * Math.floor(x / y);
    }



    /**
     * the following code is from:
     * http://www.java2s.com/Tutorial/Java/0120__Development/Normalizeanangleina2piwideintervalaroundacentervalue.htm
     * 
     * and much faster!
     */

    private static final double TWO_PI = 2 * Math.PI;

    public static double normalizeAngle(final double a, final double center) {
        return a - TWO_PI * Math.floor((a + Math.PI - center) / TWO_PI);
    }
}
