package esda_main;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.Parameters;
import spade.lib.util.URLSupport;
import ui.BasicUI;
import ui.MainWin;
import ui.SnapshotManager;
import configstart.SysConfigReader;
import configstart.VisSpecReader;
import core.Core;

/**
* Root class for basic CommonGIS applet (without a menu bar and, hence, with
* restricted functionality). Creates the UI of the system. The UI may run
* in a standalone frame or in the applet window.
*/
public class BasicCommonGIS extends Applet implements ActionListener, Runnable {
	public static final String[][] paramInfo = { { "DATA_SERVER", "URL", "URL of the Data Server used for loading data" }, { "application", "relative or absolute file name or URL", "path to the application's configuration file" }, };
	/**
	* Name of configuration file.
	*/
	protected String cfgFile = null, sysCfgFile = null;

	/**
	* The panel with the user interface of the CommonGIS system (does not include
	* the menu).
	*/
	protected BasicUI ui = null;
	/**
	* Indicates whether the UI of the system must be in a frame. This depends on
	* the size of the applet window.
	*/
	protected boolean startInFrame = false;
	/**
	* CommonGIS main window (if exists). Alternatively, all the UI may be
	* included in the applet window.
	*/
	protected MainWin mainwin = null;

	/**
	* Initialize the applet.
	* Reads configuration file name ('application') and user level ('mode') from parameters.
	*/
	@Override
	public void init() {
		this.requestFocus();
		Helper.setApplet(this);
		setBackground(Color.white);
		Dimension size = getSize();
		startInFrame = (size == null || size.width < 400 || size.height < 400);
		if (startInFrame) {
			setLayout(new FlowLayout());
			Button b = new Button("restart");
			add(b);
			b.addActionListener(this);
		} else {
			setLayout(new BorderLayout());
		}
		cfgFile = getParameter("Application");
		String sConfigFileName = getParameter("config_file");
		if (sConfigFileName == null || sConfigFileName.length() < 1) {
			sConfigFileName = "system.cnf";
		}
		sysCfgFile = URLSupport.makeURLbyPath(getDocumentBase(), sConfigFileName).toString();
		System.out.println("application cfg file=" + cfgFile);
		System.out.println("system cnf file=" + sConfigFileName);
		if ((cfgFile == null) || (cfgFile.length() < 1)) {
			System.err.println("Cannot find parameter 'application', starting empty.");
		} else {
			cfgFile = URLSupport.makeURLbyPath(getDocumentBase(), cfgFile).toString();
		}
		Thread thr = new Thread(this);
		thr.start();
	}

	/**
	* Start the applet.
	* Creates and displays the main window.
	*/
	@Override
	public void run() {
		mainwin = null;
		constructUI();
	}

	protected void constructUI() {
		//read and set values of system parameters
		Parameters parm = new Parameters();
		AppletContext context = getAppletContext();
		parm.setParameter("isLocalSystem", (context == null) ? "true" : "false");
		parm.setParameter("AppletContext", context);
		parm.setParameter("DocumentBase", getDocumentBase());
		//get applet parameters from the applet tag and store them in system parameters
		parm.setParameter("DATA_SERVER", getParameter("DATA_SERVER"));
		parm.setParameter("TutorialScript", getParameter("TutorialScript"));
		parm.setParameter("TestResultsDir", getParameter("TestResultsDir"));
		parm.setParameter("SaveDataScript", getParameter("SaveDataScript"));
		parm.setParameter("SaveDataDir", getParameter("SaveDataDir"));
		parm.setParameter("Application", getParameter("Application"));
		parm.setParameter("config_file", getParameter("config_file"));
		parm.setParameter("User", getParameter("User"));
		parm.setParameter("VotingServletURL", getParameter("VotingServletURL"));
		parm.setParameter("ImageServlet", getParameter("ImageServlet"));
		//for CHCC must be false
		parm.setParameter("Allow_Background_Visualization", "true");
		// specially for GIMMI Area Selector
		parm.setParameter("soil_attr", getParameter("soil_attr"));
		parm.setParameter("climate_attr", getParameter("climate_attr"));
		parm.setParameter("crop_attr", getParameter("crop_attr"));
		parm.setParameter("Data_From", getParameter("Data_From"));
		parm.setParameter("APPLET", this);

		//parametrized APP file
		parm.setParameter("%1", getParameter("%1"));
		parm.setParameter("%2", getParameter("%2"));
		parm.setParameter("%3", getParameter("%3"));
		parm.setParameter("%4", getParameter("%4"));
		parm.setParameter("%5", getParameter("%5"));
		parm.setParameter("%6", getParameter("%6"));
		parm.setParameter("%7", getParameter("%7"));
		parm.setParameter("%8", getParameter("%8"));
		parm.setParameter("%9", getParameter("%9"));

		SysConfigReader scr = new SysConfigReader(parm, null);
		boolean paramOK = scr.readConfiguration(sysCfgFile);
		String uiLang = getParameter("language");
		if (uiLang != null) {
			parm.setParameter("language", uiLang);
		} else {
			uiLang = parm.getParameterAsString("language");
		}

		if (uiLang != null) {
			Language.currLanguage = uiLang;
		}
		System.out.println("Interface Language: " + Language.currLanguage);
		if (paramOK) {
			String path = parm.getParameterAsString("BROWSER");
			if (path != null) {
				Helper.setPathToBrowser(path);
			}
			path = parm.getParameterAsString("PATH_TO_HELP");
			if (path != null) {
				Helper.setPathToHelpFiles(path);
			}
		}

		//create the UI
		ui = new BasicUI();
		if (startInFrame) {
			mainwin = new MainWin();
			mainwin.add(ui, "Center");
			ui.invalidate();
			mainwin.invalidate();
			mainwin.validate();
		} else {
			mainwin = null;
			add(ui, "Center");
			ui.invalidate();
			invalidate();
			validate();
		}
		// create system core; set values of system parameters
		Core core = new Core();
		core.setSystemSettings(parm);
		core.setUI(ui);
		// for GIMMI Area Selectr
		parm.setParameter("CORE", core);
		//read customization parameters: visualization methods available, etc.
		Parameters mapVis = new Parameters();
		scr = new SysConfigReader(mapVis, null);
		String fileName = getParameter("map_vis_file");
		if (fileName == null) {
			fileName = "map_vis.cnf";
		}
		String paramFile = URLSupport.makeURLbyPath(getDocumentBase(), fileName).toString();
		if (scr.readConfiguration(paramFile)) {
			core.getSystemSettings().setParameter("MapVisMethods", mapVis);
		}
		core.makeOptionalComponents();

		ui.startWork(core, cfgFile);
		if (cfgFile != null && getParameter("InitVisSpec") != null) {
			String vspFileName = URLSupport.makeURLbyPath(getDocumentBase(), getParameter("InitVisSpec")).toString();
			VisSpecReader vspr = new VisSpecReader();
			vspr.setDataSource(vspFileName);
			vspr.addProcessListener(ui.getStatusLine());
			Vector specs = vspr.read();
			if (specs != null && specs.size() > 0) {
				parm.setParameter("DefaultVisualization", specs);
				SnapshotManager ini = core.getSnapshotManager();
				ini.fulfillSpecifications(specs, true);
			}
		}
		ui.legendToFront();
		ui.autostartTools();
	}

	@Override
	public void stop() {
		if (mainwin != null) {
			mainwin.dispose();
			mainwin = null;
		} else if (ui != null) {
			ui.destroy();
		}
	}

	/**
	* Get applet information
	*/
	@Override
	public String getAppletInfo() {
		return "Basic CommonGIS applet";
	}

	/**
	* Get parameter info.
	*/
	@Override
	public String[][] getParameterInfo() {
		return paramInfo;
	}

	/**
	* Processes button click event.
	*
	* @param ae The action event.
	*/
	@Override
	public void actionPerformed(ActionEvent ae) {
		if (mainwin != null) {
			mainwin.dispose();
		}
		mainwin = null;
		constructUI();
	}

}
