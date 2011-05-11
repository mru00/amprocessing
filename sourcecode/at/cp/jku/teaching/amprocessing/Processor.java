/*
 * Processor.java
 *
 * This is the class where you can implement your onset detection / tempo extraction methods
 * Of course you may also define additional classes.
 */
package at.cp.jku.teaching.amprocessing;

import java.util.LinkedList;

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

        //This is a very simple kind of Onset Detector... You have to implement at least 2 more different onset detection functions
        // have a look at the SpectralData Class - there you can also access the magnitude and the phase in each FFT bin...
        for (int i = 0; i < m_audiofile.spectralDataContainer.size(); i++) {
            if (i == 0) {
                continue;
            }
            if (m_audiofile.spectralDataContainer.get(i).totalEnergy - m_audiofile.spectralDataContainer.get(i - 1).totalEnergy > 10) {
                m_onsetList.add(i * m_audiofile.hopTime);
            }
        }
    }

    public LinkedList<Double> getOnsets() {
        return m_onsetList;
    }

    public double getTempo() {
        return m_tempo;
    }
}
