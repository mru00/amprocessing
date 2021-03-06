/*
 * Runner.java
 * contains the main method, handles the input parameters, writes the results to files and evaluates the results
 * if possible, do not change anything in this file
 * 
 */
package at.cp.jku.teaching.amprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * @author andreas arzt
 */
public class Runner {

    /*
     * Options:
     * -i WAVFILENAME (the file you want to analyze)
     * -o DIR (the directory in which the 2 resultfiles (WAVFILENAME.onsets and WAVEFILENAME.tempo) are written to
     * -g ONSETGROUNDTRUTHFILE (the file including the onset groundtruth, optional!)
     * -t TEMPOGROUNDTRUTHFILE (the file including the tempo groundtruth, optional!)
     *
     */
    public static void main(String[] args) {
        String wavFileName = new String();
        String shortWavFileName = new String();
        String outputDirectory = new String();
        String outputOnsetsFileName = new String();
        String outputTempoFileName = new String();
        String onsetGroundTruthFileName = new String();
        String tempoGroundTruthFileName = new String();
        String odf_plotFileName = null;
        String acf_plotFileName = null;
        String ioi_plotFileName = null;
        boolean hasOnsetGroundTruth = false;
        boolean hasTempoGroundTruth = false;

        OptionParser parser = new OptionParser("qi:o:g:t:p:r:s:");
        OptionSet options = parser.parse(args);

        if (options.has("q")) {
            Log.doLog = false;
        }


        if (!options.has("i")) {
            Log.log("Inputfile required! (-i INPUTFILE)");
            System.exit(1);
        }

        if (!options.has("o")) {
            Log.log("Output Directory required! (-o OUTPUTDIR)");
            System.exit(1);
        }


        wavFileName = options.valueOf("i").toString();
        outputDirectory = options.valueOf("o").toString();

        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            Log.log("Output directory does not exist!");
            System.exit(1);
        }

        if (!outputDirectory.endsWith("/")) {
            outputDirectory = outputDirectory + "/";
        }

        shortWavFileName = wavFileName.substring(wavFileName.lastIndexOf("/") + 1, wavFileName.lastIndexOf("."));
        outputOnsetsFileName = outputDirectory + shortWavFileName + ".onsets";
        outputTempoFileName = outputDirectory + shortWavFileName + ".tempo";

        if (options.has("g")) {
            onsetGroundTruthFileName = options.valueOf("g").toString();
            hasOnsetGroundTruth = true;
        }

        if (options.has("p")) {
            odf_plotFileName = options.valueOf("p").toString();
        }

        if (options.has("r")) {
            acf_plotFileName = options.valueOf("r").toString();
        }
        if (options.has("s")) {
            ioi_plotFileName = options.valueOf("s").toString();
        }


        if (options.has("t")) {
            tempoGroundTruthFileName = options.valueOf("t").toString();
            hasTempoGroundTruth = true;
        }

        Processor p = new Processor(wavFileName, 2);
        p.analyze();

        Log.log();
        Log.log("Outputting Onset Times to " + outputOnsetsFileName + "...");
        writeDataToFile(p.getOnsets(), outputOnsetsFileName);
        Log.log("Outputting Tempo to " + outputTempoFileName + "...");
        writeDataToFile(p.getTempo(), outputTempoFileName);

        if (hasOnsetGroundTruth) {
            String onsetEvalOut = outputDirectory + shortWavFileName + ".onsets.eval";
            evaluateOnsets(p.getOnsets(), onsetGroundTruthFileName, onsetEvalOut);
        }
        if (hasTempoGroundTruth) {
            String tempoEvalOut = outputDirectory + shortWavFileName + ".tempo.eval";
            evaluateTempo(p.getTempo(), tempoGroundTruthFileName, tempoEvalOut);
        }

        if (odf_plotFileName != null) {
            writeDataToFile(p.onsetDetectionFunction, p.m_audiofile.hopTime, odf_plotFileName);
        }

        if (acf_plotFileName != null) {
            writeDataToFile(p.acf, p.m_audiofile.hopTime, acf_plotFileName);
        }

        if (ioi_plotFileName != null) {
        //    writeDataToFile(p.ioi.toArray(new int[0]), p.m_audiofile.hopTime, ioi_plotFileName);
        }

    }

    /*
     * Simple Fileout Method for LinkedList<Double>
     */
    private static void writeDataToFile(List<Double> data, String filename) {
        try {
            FileWriter outputwriter = new FileWriter(filename);
            for (Double d : data) {
                outputwriter.write(d + "\n");
            }
            outputwriter.flush();
            outputwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    private static void writeDataToFile(double[] data, double hoptime, String filename) {
        try {
            FileWriter outputwriter = new FileWriter(filename);
            int i = 0;
            for (Double d : data) {
                outputwriter.write(i * hoptime + " ");
                outputwriter.write(d + "\n");
                i++;
            }
            outputwriter.flush();
            outputwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private static void writeDataToFile(int[] data, double hoptime, String filename) {
        try {
            FileWriter outputwriter = new FileWriter(filename);
            int i = 0;
            for (int d : data) {
                outputwriter.write(i * hoptime + " ");
                outputwriter.write(d + "\n");
                i++;
            }
            outputwriter.flush();
            outputwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /*
     * Simple Fileout Method for a single double (the tempo...)
     */
    private static void writeDataToFile(double data, String filename) {
        try {
            FileWriter outputwriter = new FileWriter(filename);
            outputwriter.write(data + "\n");
            outputwriter.flush();
            outputwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    // Evaluate the Onset Estimations
    private static void evaluateOnsets(LinkedList<Double> onsets, String onsetGroundTruthFileName, String onsetEvalOut) {

        int TP = 0;
        int FP = 0;
        int FN = 0;
        double precision = 0;
        double recall = 0;
        double fmeasure = 0;

        LinkedList<Double> groundtruthOnsets = new LinkedList<Double>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(onsetGroundTruthFileName));
            String line;

            while ((line = reader.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                groundtruthOnsets.add(Double.parseDouble(st.nextToken()));
            }
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }

//        Log.log(onsets.size());
//        Log.log(groundtruthOnsets.size());

        Iterator<Double> it = groundtruthOnsets.iterator();
        while (it.hasNext()) {
            double d = it.next();
            double tmp = findNearest(d, onsets);
            if (Math.abs(d - tmp) <= 0.05) {
                onsets.remove(tmp);
                it.remove();
                TP++;
            }

        }
        FN = groundtruthOnsets.size();
        FP = onsets.size();

//        Log.log(onsets.size());
//        Log.log(groundtruthOnsets.size());

        precision = (double) TP / (TP + FP);
        recall = (double) TP / (TP + FN);
        fmeasure = (2 * precision * recall) / (precision + recall);

        StringBuilder sb = new StringBuilder();
        sb.append("TP: ");
        sb.append(TP);
        sb.append("\n");
        sb.append("FP: ");
        sb.append(FP);
        sb.append("\n");
        sb.append("FN: ");
        sb.append(FN);
        sb.append("\n");
        sb.append("Precision: ");
        sb.append(precision);
        sb.append("\n");
        sb.append("Recall: ");
        sb.append(recall);
        sb.append("\n");
        sb.append("F-Measure: ");
        sb.append(fmeasure);

        Log.log("\nOnset Evaluation: \n" + sb.toString());
        Log.log("Outputting Onset Evaluation to " + onsetEvalOut);
        try {
            FileWriter outputwriter = new FileWriter(onsetEvalOut);
            outputwriter.append(sb.toString());
            outputwriter.flush();
            outputwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static double findNearest(double val, LinkedList<Double> list) {
        double minDist = Double.MAX_VALUE;
        double retval = 0;

        for (Double d : list) {
            if (Math.abs(val - d) < minDist) {
                minDist = Math.abs(val - d);
                retval = d;
            }
        }
        return retval;

    }

    // Evaluate the Tempo Estimation
    private static void evaluateTempo(double tempo, String tempoGroundTruthFileName, String tempoEvalOut) {
        double gtempo = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(tempoGroundTruthFileName));
            String line;

            line = reader.readLine();
            StringTokenizer st = new StringTokenizer(line);

            double tempo1 = Double.parseDouble(st.nextToken());
            double tempo2 = Double.parseDouble(st.nextToken());
            double perc = Double.parseDouble(st.nextToken());

            if (perc >= 0.5) {
                gtempo = tempo1;
            } else {
                gtempo = tempo2;
            }
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }

        boolean correctTempo = false;
        boolean multipleTempo = false;

        if (gtempo * 1.04 > tempo && gtempo * 0.96 < tempo) {
            correctTempo = true;
            multipleTempo = true;
        } else {
            LinkedList<Double> multTempi = new LinkedList<Double>();
            multTempi.add(gtempo * 2);
            multTempi.add(gtempo / 2);
            multTempi.add(gtempo * 3);
            multTempi.add(gtempo / 2);

            for (Double d : multTempi) {
                if (d * 1.04 > tempo && d * 0.96 < tempo) {
                    multipleTempo = true;
                }
            }
        }

        Log.log("\nTempo Evaluation:");
        Log.log("Correct Tempo:" + gtempo + " Estimated Tempo: " + tempo);
        Log.log("Correct Tempo found: " + correctTempo);
        Log.log("Multiple of Correct Tempo found: " + multipleTempo);

        Log.log("Outputting Tempo Evaluation to " + tempoEvalOut);
        try {
            FileWriter outputwriter = new FileWriter(tempoEvalOut);
            if (correctTempo) {
                outputwriter.append("1 ");
            } else {
                outputwriter.append("0 ");
            }

            if (multipleTempo) {
                outputwriter.append("1");
            } else {
                outputwriter.append("0");
            }

            outputwriter.flush();
            outputwriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
