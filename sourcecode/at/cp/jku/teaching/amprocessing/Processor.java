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
    private AudioFile m_audiofile;
    // this List should contain your results of the onset detection step (onset times in seconds)
    private LinkedList<Double> m_onsetList;
    // this may contain your intermediate results (in frames, before conversion to time in seconds)
    private LinkedList<Integer> m_onsetListFrames;
    // this variable should contain your result of the tempo estimation algorithm
    private double m_tempo;

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

        analyze_fftshift();
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
        Integer[] peaks = pickPeaks(fftshift, 0.1);
        for (int p: peaks)
            m_onsetList.add((p - 1) * m_audiofile.hopTime);
        
    }

    private Integer[] pickPeaks(double[] data, double threshold) {
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
