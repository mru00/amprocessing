/*
 * Processor.java
 *
 * This is the class where you can implement your onset detection / tempo extraction methods
 * Of course you may also define additional classes.
 */
package at.cp.jku.teaching.amprocessing;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author andreas arzt
 */
public class Processor {

    private String m_filename;
    AudioFile m_audiofile;
    // this List should contain your results of the onset detection step (onset times in seconds)
    private LinkedList<Double> m_onsetList;
    // this may contain your intermediate results (in frames, before conversion to time in seconds)
    private LinkedList<Integer> m_onsetListFrames;
    // this variable should contain your result of the tempo estimation algorithm
    private double m_tempo;
    double[] onsetDetectionFunction;

    public Processor(String filename) {
        Log.log("Initializing Processor...");
        m_filename = filename;
        m_onsetList = new LinkedList<Double>();
        m_onsetListFrames = new LinkedList<Integer>();

        Log.log("Reading Audio-File " + filename);
        Log.log("Performing FFT...");
        // an AudioFile object is created with the following Paramters: AudioFile(WAVFILENAME, FFTLENGTH in seconds, HOPLENGTH in seconds)
        // if you would like to work with multiple resolutions you simple create multiple AudioFile objects with different parameters
        // given an audio file with 44.100 Hz the parameters below translate to an FFT with size 2048 points
        // Note that the value is not taken to be precise; it is adjusted so that the FFT Size is always power of 2.
        m_audiofile = new AudioFile(m_filename, 0.02322, 0.005);
        // this starts the extraction of the basis features (the STFT)
        m_audiofile.processFile();
    }

    // This method is called from the Runner and is the starting point of your onset detection / tempo extraction code
    public void analyze() {
        Log.log("Running Analysis...");


        final int numSamples = m_audiofile.spectralDataContainer.size();
        onsetDetectionFunction = new double[numSamples];


        analyze_spectral_flux();


        System.out.println("found peaks: " + m_onsetList.size());
    }

    private void analyze_spectral_flux() {
        // cmp http://www.dafx.ca/proceedings/papers/p_133.pdf

        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int i = 0; i < numSamples; i++) {
            final SpectralData currentFrame = m_audiofile.spectralDataContainer.get(i);
            final double[] phi = currentFrame.unwrappedPhases;

            double acc = 0.0;
            for (int j = 1; j < currentFrame.size; j++) {
                acc += halfRect(Math.abs(currentFrame.magnitudes[j]) - Math.abs(currentFrame.magnitudes[j - 1]));
            }
            onsetDetectionFunction[i] = acc;
        }

        Integer[] peaks = pickPeaksDixon(onsetDetectionFunction, -10000.0, 0.9);
        for (int p : peaks) {
            m_onsetList.add((p - 1) * m_audiofile.hopTime);
        }
    }

    private void analyze_phase_deviation() {
        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int i = 0; i < numSamples; i++) {
            final SpectralData currentFrame = m_audiofile.spectralDataContainer.get(i);
            final double[] phi = currentFrame.unwrappedPhases;

            double dphi_acc = 0.0;
            for (int j = 2; j < currentFrame.size; j++) {
                double dphi = phi[j] + phi[j - 2] - 2 * phi[j - 1];
                dphi_acc += Math.abs(dphi);
            }
            onsetDetectionFunction[i] = dphi_acc / currentFrame.size;
        }

        Integer[] peaks = pickPeaksSimple(onsetDetectionFunction, 0);
        for (int p : peaks) {
            m_onsetList.add((p - 1) * m_audiofile.hopTime);
        }
    }

    private void analyze_complex_domain() {
        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int i = 0; i < numSamples; i++) {
            final SpectralData currentFrame = m_audiofile.spectralDataContainer.get(i);
            final double[] phi = currentFrame.unwrappedPhases;

            double dphi_acc = 0.0;
            for (int j = 2; j < currentFrame.size; j++) {
                double dphi = phi[j] + phi[j - 2] - 2 * phi[j - 1];
                dphi_acc += Math.abs(dphi);
            }
            onsetDetectionFunction[i] = dphi_acc / currentFrame.size;
        }

        Integer[] peaks = pickPeaksDixon(onsetDetectionFunction, 10, 0.1);
        for (int p : peaks) {
            m_onsetList.add((p - 1) * m_audiofile.hopTime);
        }
    }

    private void analyze_fftshift() {

        final int numSamples = m_audiofile.spectralDataContainer.size();

        double[] fftshift = new double[numSamples];
        double maxshift = Double.MIN_VALUE;

        // analyze

        //This is a very simple kind of Onset Detector... You have to implement at least 2 more different onset detection functions
        // have a look at the SpectralData Class - there you can also access the magnitude and the phase in each FFT bin...
        for (int i = 1; i < numSamples; i++) {
            //if (i == 0) {
//                continue;
//            }

            SpectralData currentFrame = m_audiofile.spectralDataContainer.get(i);
            SpectralData lastFrame = m_audiofile.spectralDataContainer.get(i - 1);

            double sum = 0.0;
            for (int j = 0; j < currentFrame.size; j++) {
                // i assume currentFrame.size == lastFrame.size

                double diff = vectDiff(currentFrame.magnitudes[j], currentFrame.unwrappedPhases[j], lastFrame.magnitudes[j], lastFrame.unwrappedPhases[j]);
                sum += Math.abs(diff);
            }

            sum /= currentFrame.size;


            if (sum > maxshift) {
                maxshift = sum;
            }

            //System.out.println((i - 1) * m_audiofile.hopTime + "," + sum);
            fftshift[i] = sum;
        }

//        for (int i = 1; i < numSamples; i++) {
//            fftshift[i] /= maxshift;
//        }

        // simple peak picking
        Integer[] peaks = pickPeaksSimple(fftshift, 0.1);
        for (int p : peaks) {
            m_onsetList.add((p - 1) * m_audiofile.hopTime);
        }

    }

    private Integer[] pickPeaksSimple(double[] data, double threshold) {
        List<Integer> peaks = new LinkedList<Integer>();

        // simple peak picking
        for (int i = 1; i < data.length - 1; i++) {

            // possible peak
            if (data[i] > threshold && data[i - 1] < data[i] && data[i] > data[i + 1]) {

                // TODO: find a way to remove the constant
                // either filter the fftshift (matlab: filtfilt)
                // or calculate something reasonable from hopTime

                peaks.add(i);

            }
        }
        return peaks.toArray(new Integer[peaks.size()]);
    }

    private Integer[] pickPeaksDixon(double[] data, final double delta, final double alpha) {

        final int w = 10;
        final int m = 10;

        double mean = mean(data);
        double stddev = stddev(data, mean);
        for (int i = 0; i < data.length; i++) {
            data[i] = (data[i] - mean) / stddev;
        }

        {
            double mean1 = mean(data);
            double stddev1 = stddev(data, mean1);
            System.out.println("mean1:" + mean1);
            System.out.println("stddev1:" + stddev1);
        }

        double ga;
        double ga_next = Double.MIN_VALUE;

        List<Integer> peaks = new LinkedList<Integer>();

        // simple peak picking
        outer:
        for (int n = w * m; n < data.length - w; n++) {

            ga = ga_next;
            ga_next = Math.max(data[n], alpha * ga + (1.0 - alpha) * data[n]);

            if (data[n] < ga) {
                continue outer;
            }

            inner:
            for (int k = n - w; k <= n + w; k++) {
                if (k != n) {
                    if (data[n] < data[k]) {
                        continue outer;
                    }
                }
            }

            double sum1 = 0.0;
            for (int k = n - m * w; k <= n + w; k++) {
                sum1 += data[k];
            }

            sum1 = delta + (sum1 / m * w + w + 1);

            if (data[n] <= sum1) {
                continue outer;
            }

            peaks.add(n);
        }
        return peaks.toArray(new Integer[peaks.size()]);
    }

    private double mean(double[] d) {
        double sum = 0.0;
        for (double e : d) {
            sum += e;
        }
        return sum / d.length;
    }

    private double stddev(double[] d, double mean) {
        double sum = 0.0;
        for (double e : d) {
            sum += Math.pow(e - mean, 2);
        }
        return Math.sqrt(sum / d.length);
    }

    private double halfRect(double d) {
        return (d + Math.abs(d)) / 2;
    }

    private double vectDiff(double m1, double phi1, double m2, double phi2) {
        double x1, y1, x2, y2;
        x1 = m1 * Math.cos(phi1);
        y1 = m1 * Math.sin(phi1);
        x2 = m2 * Math.cos(phi2);
        y2 = m2 * Math.sin(phi2);

        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public LinkedList<Double> getOnsets() {
        return m_onsetList;
    }

    public double getTempo() {
        return m_tempo;
    }
}
