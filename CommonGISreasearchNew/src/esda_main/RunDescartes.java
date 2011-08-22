package esda_main;

import java.io.PrintStream;

import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.Parameters;
import ui.DescartesUI;
import ui.MainWin;
import ui.SnapshotManager;
import configstart.SysConfigReader;
import core.Core;

/** Changes
 * hdz, 2004.03.25 main ():
 *      Read Parameter MAX_NUM_VALUES_FOR_QUALITATIVE_VIS from .app and store it
 *      in Helper
 */

public class RunDescartes {
	/**
	* Three optional arguments may be given:
	* 1) the path to the file with the project (application) description (*.app);
	*    this application will be immediately loaded;
	* 2) the path to the file with the specification what data (attributes) must
	*    be visualized at the startup moment and in what way.
	* 3) the path to the file with the system configuration.
	*    Default name is 'system.cnf'. If the first and the second
	*    arguments exist, then this parameter is on the 3rd place.
	*    (added after CommonGIS.com meeting 23.07.2003)
	*/
	public static void main(String args[]) {

		redirectSystemOutputForLogFile(args);

		boolean hasCnf = false;
		boolean hasApp = false;
		boolean hasVisSpec = false;

		//read and set values of system parameters
		Parameters parm = new Parameters();
		parm.setParameter("isLocalSystem", "true");
		parm.setParameter("SaveDataDir", "analysis_results");
		//must be "false" for CHCC
		parm.setParameter("Allow_Background_Visualization", "true");

		String cfgFile = null, paramFile = "system.cnf", visSpecFile = null;
		// analyse command line parameters
		if (args != null && args.length > 0) {
			for (String arg : args)
				if (arg != null)
					if (arg.indexOf(".cnf") > 0) {
						hasCnf = true;
						paramFile = arg;
					} else if (arg.indexOf(".app") > 0) {
						hasApp = true;
						cfgFile = arg;
					} else if (arg.indexOf(".str") > 0) {
						hasVisSpec = true;
						visSpecFile = arg;
					}
		}
//ID
		int parStart = 0;
		if (hasCnf) {
			parStart++;
		}
		if (hasApp) {
			parStart++;
		}
		if (hasVisSpec) {
			parStart++;
		}
		if (args.length > parStart) {
			for (int i = parStart; i < args.length; i++) {
				parm.setParameter("%" + Integer.toString(i - parStart + 1), args[i]);
			}
		}
//~ID
		SysConfigReader scr = new SysConfigReader(parm, null);
		if (scr.readConfiguration(paramFile)) {
			String uiLang = parm.getParameterAsString("LANGUAGE");
			if (uiLang != null) {
				Language.currLanguage = uiLang;
			}
			System.out.println("Interface Language: " + Language.currLanguage);
			String path = parm.getParameterAsString("BROWSER");
			if (path == null) { // no such parameter in system.cnf or no such file at all
				// P.G.: CommonGIS Help reading: solution of "path to browser problem"
				String sIExplore = null;
				sIExplore = System.getProperty("commongis.browser");
				System.out.println("Default browser = " + sIExplore);
				path = sIExplore;
				parm.setParameter("BROWSER", path);
				//~ P.G.
			}
			if (path != null) {
				Helper.setPathToBrowser(path);
			}
			path = parm.getParameterAsString("PATH_TO_HELP");
			if (path != null) {
				Helper.setPathToHelpFiles(path);
			}
			// hdz added 2004.03.25
			String maxValue = parm.getParameterAsString("MAX_NUM_VALUES_FOR_QUALITATIVE_VIS");
			try {
				if (maxValue != null) {
					Helper.setMaxNumValuesForQualitativeVis(Integer.parseInt(maxValue));
				}
			} catch (Exception ex) {
			}
		}

		Core core = new Core();
		core.setSystemSettings(parm);

		Parameters menuParm = new Parameters();
		scr = new SysConfigReader(menuParm, null);
		// P.G. Support of menu configuration in application
		String sMenuConfigFileName = parm.getParameterAsString("menu_param_file");
		if (sMenuConfigFileName == null) {
			sMenuConfigFileName = "menu.cnf";
		}

		if (scr.readConfiguration(sMenuConfigFileName)) {
			core.getSystemSettings().setParameter("MainMenu", menuParm);
		}
		Parameters mapVis = new Parameters();
		scr = new SysConfigReader(mapVis, null);
		if (scr.readConfiguration("map_vis.cnf")) {
			core.getSystemSettings().setParameter("MapVisMethods", mapVis);
		}
		core.makeOptionalComponents();
		MainWin mw = new MainWin();
		DescartesUI ui = new DescartesUI(mw);
		mw.add(ui, "Center");
		ui.startWork(core, cfgFile);
		if (cfgFile != null && hasVisSpec) { // there is a file with visualization specification
			String path = (visSpecFile != null) ? visSpecFile : args[1];
			if (path != null && path.length() > 0) {
				SnapshotManager sman = core.getSnapshotManager();
				sman.loadSnapshot(path, true);
			}
		}
		ui.legendToFront();
		ui.autostartTools();
	}

	/**
	 * ~MO~ redirect system outpu for log file (commongis.log, max length 500K)
	 *
	 * @param args String[]
	 */
	public static void redirectSystemOutputForLogFile(String args[]) {
		boolean hasLog = false;
		for (String arg : args) {
			if (arg.equals("-LOG")) { // -LOG command option
				hasLog = true;
				break;
			}
		}
		if (hasLog) {
			PrintStream pw = null;
			try {
				pw = new java.io.PrintStream(new LogStream());
			} catch (java.io.IOException ioe) {
			}

			if (pw != null) {
				System.setOut(pw);
			}
		}

	}
}
