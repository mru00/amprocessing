/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package at.cp.jku.teaching.amprocessing;

/**
 *
 * @author mru
 */
public class Log {
    public static boolean doLog = false;
    public static void log(String message) {
        if (doLog) System.out.println(message);
    }
    public static void log() {
        if (doLog) System.out.println();
    }

}
