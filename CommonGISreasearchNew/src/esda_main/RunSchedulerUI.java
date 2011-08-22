package esda_main;

import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.Parameters;
import ui.EvacSchedulingUI;
import ui.MainWin;
import configstart.SysConfigReader;
import core.Core;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 17-Oct-2007
 * Time: 17:50:12
 * To change this template use File | Settings | File Templates.
 */
public class RunSchedulerUI {
	/**
	* Optional arguments:
	* 1) the path to the file with the project (application) description (*.app);
	*    this application will be immediately loaded;
	* 2) the path to the file with the system configuration.
	*    Default name is 'system.cnf'.
	*/
	public static void main(String args[]) {
		//read and set values of system parameters
		Parameters parm = new Parameters();
		parm.setParameter("isLocalSystem", "true");
		parm.setParameter("Allow_Background_Visualization", "true");
		String cfgFile = null, paramFile = "system.cnf";

		// analyse command line parameters
		boolean hasCnf = false;
		boolean hasApp = false;
		if (args != null && args.length > 0) {
			for (String arg : args)
				if (arg != null)
					if (arg.indexOf(".cnf") > 0) {
						hasCnf = true;
						paramFile = arg;
					} else if (arg.indexOf(".app") > 0) {
						hasApp = true;
						cfgFile = arg;
					}
		}
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
		}

		Core core = new Core();
		core.setSystemSettings(parm);
		core.makeOptionalComponents();
		MainWin mw = new MainWin();
		EvacSchedulingUI ui = new EvacSchedulingUI(mw);
		mw.add(ui, "Center");
		ui.startWork(core, cfgFile);
	}
}
