package spade.lib.help;

import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author not attributable
 * @version 1.0
 * Changes: hdz, 2004.03,
 *          new static Variable maxNumValuesForQualitativeVis and access Methods
 */
public class Helper {
	protected static Applet applet = null;
	protected static String pathToBrowser = null, pathToHelpFiles = null;
	protected static HelpIndex index = null;
	protected static int maxNumValuesForQualitativeVis = 20; //hdz

	public static void setApplet(Applet anApplet) {
		applet = anApplet;
	}

	public static boolean isApplet() {
		return applet != null;
	}

	public static void setPathToBrowser(String path) {
		pathToBrowser = path;
	}

	public static String getPathToBrowser() {
		return pathToBrowser;
	}

	public static void setPathToHelpFiles(String path) {
		pathToHelpFiles = path;
	}

	public static void setHelpIndex(HelpIndex ind) {
		index = ind;
	}

	//hdz, 2004.03.25 for applying quali. Methods to integer attributes
	public static int getMaxNumValuesForQualitativeVis() {
		return maxNumValuesForQualitativeVis;
	}

	public static void setMaxNumValuesForQualitativeVis(int maxValue) {
		maxNumValuesForQualitativeVis = maxValue;
	}

	public static void help(String topic) {
		if (index == null || (applet == null && pathToBrowser == null))
			return;
		String fname = index.getHelpFileName(topic);
		if (pathToHelpFiles != null) {
			fname = pathToHelpFiles + fname;
		}
		//System.out.println("File with help on the topic "+topic+": "+fname);
		showPage(fname, "Help");
	}

	public static void showPage(String fname, String title) {
		if (fname == null)
			return;
		if (applet == null) {
			try {
				System.out.println("Trying to run browser in " + pathToBrowser);
				String cmd[] = new String[2];
				cmd[0] = pathToBrowser;
				cmd[1] = "file://" + (new File(fname)).getAbsolutePath();
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				System.out.println(e.toString());
			}
		} else {
			fname = fname.replace('\\', '/');
			try {
				System.out.println("Applet is now trying to open a page in the browser...");
				applet.getAppletContext().showDocument(new URL(applet.getCodeBase(), fname), title);
			} catch (MalformedURLException e) {
				System.out.println(e.toString());
			}
		}
	}

	/**
	* Returns true if the specified help topic is found in the index and there
	* is a corresponding path to the file with the help, and either the applet or
	* the path to the browser is specified
	*/
	public static boolean canHelp(String topic) {
		return index != null && index.getHelpFileName(topic) != null && (applet != null || pathToBrowser != null);
	}
}
