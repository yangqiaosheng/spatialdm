package esda_main;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.analysis.system.ToolReCreator;
import spade.lib.lang.Language;
import spade.lib.util.Parameters;
import spade.lib.util.URLSupport;
import spade.vis.mapvis.SimpleDataMapper;
import ui.MainWin;
import ui.ShowMapUI;
import configstart.SysConfigReader;
import configstart.ToolRestoreFactory;
import configstart.VisSpecReader;
import core.SupervisorImplement;

/**
* Conceived as a very small CommonGIS subset that only loads a map description
* and then constructs and displays the corresponding map.
*/
public class ShowMap extends Applet implements Runnable {
	/**
	* Name of configuration file.
	*/
	protected String cfgFile = null, sysCfgFile = null;
	/**
	* The panel with the user interface of the CommonGIS system (does not include
	* the menu).
	*/
	protected ShowMapUI ui = null;
	/**
	* The system's supervisor
	*/
	protected Supervisor supervisor = null;
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
	* Reads configuration file name ('application') from the applet parameters
	*/
	@Override
	public void init() {
		this.requestFocus();
		setBackground(Color.white);
		Dimension size = getSize();
		startInFrame = (size == null || size.width < 400 || size.height < 400);
		setLayout(new BorderLayout());
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

	/**
	* Reads system's parameters from the file system.cnf
	*/
	protected void readSystemParamsFromFile(Parameters parm) {
		if (parm == null || sysCfgFile == null)
			return;
		SysConfigReader scr = new SysConfigReader(parm, null);
		scr.readConfiguration(sysCfgFile);
	}

	/**
	* Read and set values of system parameters
	*/
	protected void setSystemParameters(Parameters parm) {
		AppletContext context = getAppletContext();
		parm.setParameter("isLocalSystem", (context == null) ? "true" : "false");
		parm.setParameter("AppletContext", context);
		parm.setParameter("DocumentBase", getDocumentBase());
		//Reads system's parameters from the file system.cnf and store them in system parameters
		readSystemParamsFromFile(parm);
		//get applet parameters from the applet tag and store them in system parameters
		parm.setParameter("DATA_SERVER", getParameter("DATA_SERVER"));
		parm.setParameter("Application", getParameter("Application"));
		parm.setParameter("config_file", getParameter("config_file"));
		parm.setParameter("User", getParameter("User"));
		parm.setParameter("ImageServlet", getParameter("ImageServlet"));
		parm.setParameter("Allow_Background_Visualization", "true");
		parm.setParameter("APPLET", this);
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

		//parametrized APP file
		String curPar = "";
		int parIdx = 1;
		do {
			curPar = getParameter("%" + Integer.toString(parIdx));
			if (curPar != null) {
				parm.setParameter("%" + Integer.toString(parIdx), curPar);
				parIdx++;
			}
		} while (curPar != null && curPar.length() > 0);
	}

	protected void constructUI() {
		//read and set values of system parameters
		Parameters parm = new Parameters();
		setSystemParameters(parm);

		SupervisorImplement supervisor = new SupervisorImplement();
		supervisor.setSystemSettings(parm);

		//create the UI
		ui = new ShowMapUI();
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
		supervisor.setUI(ui);
		this.supervisor = supervisor;

		ui.startWork(supervisor, cfgFile);

		if (cfgFile != null && getParameter("InitVisSpec") != null) {
			String vspFileName = URLSupport.makeURLbyPath(getDocumentBase(), getParameter("InitVisSpec")).toString();
			VisSpecReader vspr = new VisSpecReader();
			vspr.setDataSource(vspFileName);
			vspr.addProcessListener(ui.getStatusLine());
			Vector specs = vspr.read();
			if (specs != null && specs.size() > 0) {
				parm.setParameter("DefaultVisualization", specs);
				ToolReCreator restorer = ToolRestoreFactory.getToolRestorer("configstart.MapVisInitiator");
				if (restorer != null) {
					SimpleDataMapper mapper = new SimpleDataMapper();
					for (int i = 0; i < specs.size(); i++) {
						restorer.fulfillSpecification(specs.elementAt(i), ui.getDataKeeper(), supervisor, mapper, true);
					}
				}
			}
		}
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
}