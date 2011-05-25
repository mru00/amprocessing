/*
 * Processor.java
 *
 * This is the class where you can implement your onset detection / tempo extraction methods
 * Of course you may also define additional classes.
 *
 *
 * most of the detection functions are taken from [1] 
 *
 * also interesting, but nothing implemented: [3]
 *
 * [1] http://www.dafx.ca/proceedings/papers/p_133.pdf
 * [2] http://java-gaming.org/index.php?topic=24191.0
 * [3] http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.60.9607&rep=rep1&type=pdf
 */
package at.cp.jku.teaching.amprocessing;

import java.util.LinkedList;
import java.util.List;
import static java.lang.Math.*;
import static at.cp.jku.teaching.amprocessing.SpectralData.normalizeAngle;

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
    double[] acf = new double[0];
    List<Integer> ioi = new LinkedList<Integer>();
    private final int bpmMin = 50;
    private final int bpmMax = 200;
    private int odf_algorithm;
    private Integer setup_m = null;
    private Integer setup_w = null;
    private Double setup_alpha = null;
    private Double setup_delta = null;

    public Processor(String filename, int algorithm) {
        this(filename);
        this.odf_algorithm = algorithm;
    }

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

        //m_audiofile = new AudioFile(m_filename, 0.002322, 0.005);
        // this starts the extraction of the basis features (the STFT)
        m_audiofile.processFile();
    }

    /* 
     * for parameter study, i need to override the peakpicker parameters. 
     * 
     * when this.{m,w,alpha,delta} is not equal null, peakpicker will use 
     * the member's values instead of the values supplied by the algorithms.
     */
    public void setup(Integer algorithm, Integer w, Integer m, Double alpha, Double delta) {
        if (algorithm != null) {
            this.odf_algorithm = algorithm;
        }
        this.setup_w = w;
        this.setup_m = m;
        this.setup_delta = delta;
        this.setup_alpha = alpha;
    }

    // This method is called from the Runner and is the starting point of your onset detection / tempo extraction code
    public void analyze() {
        Log.log("Running Analysis...");


        final int numSamples = m_audiofile.spectralDataContainer.size();
        onsetDetectionFunction = new double[numSamples];
        m_onsetList.clear();

        List<Integer> peaks;
        switch (odf_algorithm) {
            case 1:
                peaks = odf_phase_deviation();
                break;
            case 2:
                peaks = odf_spectral_flux();
                break;
            case 3:
                peaks = odf_complex_domain();
                break;
            case 4:
                peaks = odf_weighted_phase_deviation();
                break;
            case 5:
                peaks = odf_normalized_weighted_phase_deviation();
                break;
            case 6:
                peaks = odf_rectified_complex_domain();
                break;
            case 7:
                peaks = odf_frame_distance();
                break;
            default:
                peaks = null;
        }


        for (int p : peaks) {
            m_onsetList.add(p * m_audiofile.hopTime);
        }

        m_tempo = bdf_acf();

    }

    /**
     * beat detection function: autocorrelation
     * @return
     */
    private double bdf_acf() {
        final int numSamples = m_audiofile.spectralDataContainer.size();

        final double[] rect_odf = new double[numSamples];
        for (int i = 0; i < numSamples; i++) {
            rect_odf[i] = halfRect(onsetDetectionFunction[i]);
        }

        final int from = bpmToIndex(bpmMax);
        final int to = bpmToIndex(bpmMin);

        acf = new double[to];

        for (int tau = from; tau < to; tau++) {
            double r_tau = 0.0;
            for (int k = 0; k < rect_odf.length - tau; k++) {
                r_tau += rect_odf[k + tau] * rect_odf[k];
            }
            acf[tau] = r_tau;
        }

        int peakIdx = bdf_acf_pick_peak_max();

        System.out.println("peak at: " + peakIdx * m_audiofile.hopTime);
        return 60.0 / (peakIdx * m_audiofile.hopTime);
    }

    private int bdf_acf_pick_peak_max() {
        return findMax(acf);
    }

    private int bdf_acf_pick_peak_dixon() {
        List<Integer> peaks = pickPeaksDixon(acf, 5, 5, 0.99, 0.5);
        if (peaks.isEmpty()) {
            return 0;
        } else {
            return peaks.get(0);
        }
    }

    private int bpmToIndex(double bpm) {
        return (int) (60.0 / (m_audiofile.hopTime * bpm));
    }

    /**
     * tempo extraction using inter onset intervals
     * @return
     */
    private double bdf_ioi() {


        final int from = bpmToIndex(bpmMax);
        final int to = bpmToIndex(bpmMin);

        ioi.clear();

        int[] hist = new int[to];

        for (double d1 : m_onsetList) {
            for (double d2 : m_onsetList) {

                int distance = (int) (abs(d2 - d1) / m_audiofile.hopTime);
                if (distance > from && distance < to) {
                    hist[distance]++;
                }
            }
        }

        for (int h : hist) {
            ioi.add(h);
        }

        int peakIdx = 0;
        peakIdx = findMax(ioi);
        return 60 / (peakIdx * m_audiofile.hopTime);
    }

    private int findMax(final double[] d) {
        double max = Double.MIN_VALUE;
        int idx = -1;
        for (int i = 0; i < d.length; i++) {
            if (d[i] > max) {
                max = d[i];
                idx = i;
            }
        }
        return idx;
    }

    private int findMax(final List<Integer> d) {
        int max = Integer.MIN_VALUE;
        int idx = -1;
        for (int i = 0; i < d.size(); i++) {
            if (d.get(i) > max) {
                max = d.get(i);
                idx = i;
            }
        }
        return idx;
    }

    // alg 1
    private List<Integer> odf_phase_deviation() {
        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int n = 2; n < numSamples; n++) {

            final SpectralData f_n_2 = m_audiofile.spectralDataContainer.get(n - 2);
            final SpectralData f_n_1 = m_audiofile.spectralDataContainer.get(n - 1);
            final SpectralData f_n = m_audiofile.spectralDataContainer.get(n);

            final double[] phi_n_0 = f_n.phases;
            final double[] phi_n_1 = f_n_1.phases;
            final double[] phi_n_2 = f_n_2.phases;

            double dphi_acc = 0.0;
            for (int k = 0; k < f_n.size; k++) {

                final double dphi_n_0 = normalizeAngle(phi_n_1[k], phi_n_0[k]) - phi_n_1[k];
                final double dphi_n_1 = normalizeAngle(phi_n_2[k], phi_n_1[k]) - phi_n_2[k];
                final double d2phi_n = normalizeAngle(dphi_n_1, dphi_n_0) - dphi_n_1;

                dphi_acc += abs(d2phi_n);
            }
            onsetDetectionFunction[n] = dphi_acc / f_n.size;
        }

        return pickPeaksDixon(onsetDetectionFunction, 50, 20, 1, 0.9);
    }

    // alg 2
    private List<Integer> odf_spectral_flux() {


        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int n = 1; n < numSamples; n++) {

            final SpectralData f_n = m_audiofile.spectralDataContainer.get(n);
            final SpectralData f_n_1 = m_audiofile.spectralDataContainer.get(n - 1);
            final double[] mag_n = f_n.magnitudes;
            final double[] mag_n_1 = f_n_1.magnitudes;

            double acc = 0.0;
            for (int k = 0; k < f_n.size; k++) {
                acc += halfRect(abs(mag_n[k]) - abs(mag_n_1[k]));
            }
            onsetDetectionFunction[n] = acc;
        }

        return pickPeaksDixon(onsetDetectionFunction, 5, 5, 0.9, 0.9);
    }

    // alg 3
    private List<Integer> odf_complex_domain() {
        // from dixon, implementation: confident
        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int n = 2; n < numSamples; n++) {

            final SpectralData f_n_0 = m_audiofile.spectralDataContainer.get(n);
            final SpectralData f_n_1 = m_audiofile.spectralDataContainer.get(n - 1);
            final SpectralData f_n_2 = m_audiofile.spectralDataContainer.get(n - 2);
            final double[] phi_n_0 = f_n_0.phases;
            final double[] phi_n_1 = f_n_1.phases;
            final double[] phi_n_2 = f_n_2.phases;
            final double[] mag_n_0 = f_n_0.magnitudes;
            final double[] mag_n_1 = f_n_1.magnitudes;

            double deviation_acc = 0.0;
            for (int k = 0; k < f_n_0.size; k++) {

                final double dphi_n_1 = normalizeAngle(phi_n_2[k], phi_n_1[k]) - phi_n_2[k];

                deviation_acc += radialDistance(mag_n_0[k], phi_n_0[k], mag_n_1[k], normalizeAngle(phi_n_1[k] + dphi_n_1, 0.0));
            }
            onsetDetectionFunction[n] = deviation_acc;
        }

        return pickPeaksDixon(onsetDetectionFunction, 3, 4, 0.3, 0.5);
    }
    // alg 4

    private List<Integer> odf_weighted_phase_deviation() {
        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int n = 2; n < numSamples; n++) {
            final SpectralData f_n_0 = m_audiofile.spectralDataContainer.get(n);
            final SpectralData f_n_1 = m_audiofile.spectralDataContainer.get(n - 1);
            final SpectralData f_n_2 = m_audiofile.spectralDataContainer.get(n - 2);
            final double[] mag_n_0 = f_n_0.magnitudes;
            final double[] phi_n_0 = f_n_0.phases;
            final double[] phi_n_1 = f_n_1.phases;
            final double[] phi_n_2 = f_n_2.phases;

            double dphi_acc = 0.0;
            for (int k = 0; k < f_n_0.size; k++) {

                final double dphi_n_0 = normalizeAngle(phi_n_1[k], phi_n_0[k]) - phi_n_1[k];
                final double dphi_n_1 = normalizeAngle(phi_n_2[k], phi_n_1[k]) - phi_n_2[k];
                final double d2phi_n = normalizeAngle(dphi_n_1, dphi_n_0) - dphi_n_1;

                dphi_acc += abs(mag_n_0[k] * d2phi_n);
            }
            onsetDetectionFunction[n] = dphi_acc / f_n_0.size;
        }

        return pickPeaksDixon(onsetDetectionFunction, 4, 4, 0.7, 0.9);
    }
    // alg 5

    private List<Integer> odf_normalized_weighted_phase_deviation() {
        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int n = 2; n < numSamples; n++) {

            final SpectralData f_n_0 = m_audiofile.spectralDataContainer.get(n);
            final SpectralData f_n_1 = m_audiofile.spectralDataContainer.get(n - 1);
            final SpectralData f_n_2 = m_audiofile.spectralDataContainer.get(n - 2);
            final double[] mag_n_0 = f_n_0.magnitudes;
            final double[] phi_n_0 = f_n_0.phases;
            final double[] phi_n_1 = f_n_1.phases;
            final double[] phi_n_2 = f_n_2.phases;

            double dphi_acc = 0.0;
            double mag_acc = 0.0;
            for (int k = 0; k < f_n_0.size; k++) {

                final double dphi_n_0 = normalizeAngle(phi_n_1[k], phi_n_0[k]) - phi_n_1[k];
                final double dphi_n_1 = normalizeAngle(phi_n_2[k], phi_n_1[k]) - phi_n_2[k];
                final double d2phi_n = normalizeAngle(dphi_n_1, dphi_n_0) - dphi_n_1;

                double X_n_k = mag_n_0[k];
                dphi_acc += abs(X_n_k * d2phi_n);
                mag_acc += abs(X_n_k);
            }
            onsetDetectionFunction[n] = dphi_acc / mag_acc;
        }

        return pickPeaksDixon(onsetDetectionFunction, 5, 2, 0.9, 0.9);
    }

    // alg 6
    private List<Integer> odf_rectified_complex_domain() {
        // from dixon, implementation: confident
        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int n = 2; n < numSamples; n++) {

            final SpectralData f_n_0 = m_audiofile.spectralDataContainer.get(n);
            final SpectralData f_n_1 = m_audiofile.spectralDataContainer.get(n - 1);
            final SpectralData f_n_2 = m_audiofile.spectralDataContainer.get(n - 2);
            final double[] mag_n_0 = f_n_0.magnitudes;
            final double[] mag_n_1 = f_n_1.magnitudes;
            final double[] phi_n_0 = f_n_0.phases;
            final double[] phi_n_1 = f_n_1.phases;
            final double[] phi_n_2 = f_n_2.phases;

            double deviation_acc = 0.0;
            for (int k = 0; k < f_n_0.size; k++) {
                if (mag_n_0[k] >= mag_n_1[k]) {

                    final double dphi_n_1 = normalizeAngle(phi_n_2[k], phi_n_1[k]) - phi_n_2[k];

                    deviation_acc += radialDistance(mag_n_0[k], phi_n_0[k], mag_n_1[k], normalizeAngle(phi_n_1[k] + dphi_n_1, 0.0));
                }
            }
            onsetDetectionFunction[n] = deviation_acc;
        }

        return pickPeaksDixon(onsetDetectionFunction, 4, 4, 0.85, 0.9);
    }

    // alg 7
    private List<Integer> odf_frame_distance() {

        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int n = 1; n < numSamples; n++) {

            final SpectralData f_n_0 = m_audiofile.spectralDataContainer.get(n);
            final SpectralData f_n_1 = m_audiofile.spectralDataContainer.get(n - 1);
            final double[] mag_n_0 = f_n_0.magnitudes;
            final double[] mag_n_1 = f_n_1.magnitudes;
            final double[] phi_n_0 = f_n_0.phases;
            final double[] phi_n_1 = f_n_1.phases;

            double sum = 0.0;
            for (int k = 0; k < f_n_0.size; k++) {
                // i assume currentFrame.size == lastFrame.size

                double diff = radialDistance(mag_n_0[k], phi_n_0[k], mag_n_1[k], phi_n_1[k]);
                sum += abs(diff);
            }

            //sum /= currentFrame.size;

            onsetDetectionFunction[n] = sum;
        }

        return pickPeaksDixon(onsetDetectionFunction, 3, 5, 0.5, 0.9);
    }

    // alg 8
    private List<Integer> odf_frame_distance_2() {

        final int numSamples = m_audiofile.spectralDataContainer.size();

        for (int n = 1; n < numSamples; n++) {

            final SpectralData f_n_0 = m_audiofile.spectralDataContainer.get(n);
            final SpectralData f_n_1 = m_audiofile.spectralDataContainer.get(n - 1);
            final double[] mag_n_0 = f_n_0.magnitudes;
            final double[] mag_n_1 = f_n_1.magnitudes;
            final double[] phi_n_0 = f_n_0.phases;

            double sum = 0.0;
            for (int k = 0; k < f_n_0.size; k++) {
                // i assume currentFrame.size == lastFrame.size

                double diff = radialDistance(mag_n_0[k], phi_n_0[k], mag_n_1[k], phi_n_0[k]);
                sum += abs(diff);
            }

            onsetDetectionFunction[n] = sum;
        }

        return pickPeaksDixon(onsetDetectionFunction, 3, 5, 0.5, 0.9);
    }

    /**
     * normalize the array values such that:
     * mean(data) = 0
     * stddev(data) = 1
     * @param data
     */
    private void normalizeArray(final double[] data) {
        double mean = mean(data);
        double stddev = stddev(data, mean);
        for (int n = 0; n < data.length; n++) {
            data[n] = (data[n] - mean) / stddev;
        }
    }

    /**
     * peak picking as described in [1]
     * @param data
     * @param m
     * @param w
     * @param alpha
     * @param delta
     * @return
     */
    private List<Integer> pickPeaksDixon(double[] data, int m, int w, double alpha, double delta) {

        // this is needed for the parameter study
        // if the member "delta" is set, override the given parameter
        // @see: setup()

        if (this.setup_delta != null) {
            delta = this.setup_delta;
        }
        if (this.setup_alpha != null) {
            alpha = this.setup_alpha;
        }
        if (this.setup_w != null) {
            w = this.setup_w;
        }
        if (this.setup_m != null) {
            m = this.setup_m;
        }

        normalizeArray(data);

        double ga;
        double ga_next = Double.MIN_VALUE;

        List<Integer> peaks = new LinkedList<Integer>();

        // simple peak picking
        outer:
        for (int n = w * m; n < data.length - (w + 1); n++) {

            if (alpha != Double.NaN) {

                ga = ga_next;
                ga_next = max(data[n], alpha * ga + (1.0 - alpha) * data[n]);

                if (data[n] < ga) {
                    continue outer;
                }
            }

            inner:
            for (int k = n - w; k <= n + w; k++) {
                if (k != n) {
                    if (data[n] < data[k]) {
                        continue outer;
                    }
                }
            }

            if (delta != Double.NaN) {

                double sum1 = 0.0;
                for (int k = n - m * w; k <= n + w; k++) {
                    sum1 += data[k];
                }

                sum1 = delta + (sum1 / (m * w + w + 1));

                if (data[n] < sum1) {
                    continue outer;
                }
            }

            peaks.add(n);
        }
        return peaks;
    }

    /**
     * mean of array
     * @param d
     * @return
     */
    private double mean(final double[] d) {
        double sum = 0.0;
        for (double e : d) {
            sum += e;
        }
        return sum / d.length;
    }

    /**
     * standard deviation
     * @param d
     * @param mean
     * @return
     */
    private double stddev(final double[] d, final double mean) {
        double sum = 0.0;
        for (double e : d) {
            sum += pow(e - mean, 2);
        }
        return sqrt(sum / d.length);
    }

    private double halfRect(final double d) {
        return (d + abs(d)) / 2;
    }

    private double radialDistance(final double m1, final double phi1, final double m2, final double phi2) {
        //  http://en.wikipedia.org/wiki/Radial_distance_(geometry)
        return sqrt(m1 * m1 + m2 * m2 - 2 * m1 * m2 * cos(phi1 - phi2));
    }

    public LinkedList<Double> getOnsets() {
        return m_onsetList;
    }

    public double getTempo() {
        return m_tempo;
    }
}
