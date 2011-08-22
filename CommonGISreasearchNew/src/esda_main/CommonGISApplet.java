package esda_main;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Menu;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import spade.lib.basicwin.MenuConstructor;
import spade.lib.basicwin.MyMenuBar;
import spade.lib.basicwin.MyMenuItem;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.Parameters;
import spade.lib.util.URLSupport;
import ui.DescartesUI;
import ui.MainWin;
import ui.SnapshotManager;
import configstart.SysConfigReader;
import core.Core;

/**
* Root class for CommonGIS applet. Created the UI of the system. The UI may run
* in a standalone frame or in the applet window. In the latter case the applet
* constructs the system menu (an instance of MyMenuBar)
*/
public class CommonGISApplet extends Applet implements ActionListener, MenuConstructor, Runnable {
	public static final String[][] paramInfo = { { "DATA_SERVER", "URL", "URL of the Data Server used for loading data" }, { "application", "relative or absolute file name or URL", "path to the application's configuration file" },
			{ "TutorialScript", "URL", "the CGI script used for storing user\'s answers " + "to the tutorial/test questions (optional)" },
			{ "TestResultsDir", "relative or absolute directory name", "the directory in which tutorial/test results are saved (optional)" }, { "SaveDataScript", "URL", "the CGI script used for storing results " + "of data analysis" },
			{ "SaveDataDir", "relative or absolute directory name", "the directory in which analysis results are saved (optional)" }, { "User", "Identifier of user of the system for voting purposes", "received from Zeno" },
			{ "VotingServletURL", "URL of servlet for storing results of voting", "relative or absolute path received from Zeno" } };
	/**
	* Name of configuration file.
	*/
	protected String cfgFile = null, sysCfgFile = null;

	/**
	* The panel with the user interface of the CommonGIS system (does not include
	* the menu).
	*/
	protected DescartesUI ui = null;
	/**
	* The "menu" of the system that is created only in a case when the whole
	* UI is included in the applet window.
	*/
	protected MyMenuBar menuBar = null;
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
	* Replies if simple items (not menus) are allowed in the topmost menu (menu
	* bar). Since not a standard menu bar is used, simple items are allowed.
	*/
	@Override
	public boolean allowSimpleItems() {
		return true;
	}

	/**
	* Replies if shortcuts are allowed. Since MyMenuBar is used rather than a
	* standard menu bar, shortcuts are NOT allowed in this case.
	*/
	@Override
	public boolean allowShortcuts() {
		return false;
	}

	/**
	* Returns the number of items in the menu
	*/
	@Override
	public int getMenuItemCount() {
		if (menuBar == null)
			return 0;
		return menuBar.getItemCount();
	}

	/**
	* If the item with the given index is a menu, returns this item.
	* If this is MyMenuItem, returns its popup menu (if exists).
	* May return null;
	*/
	@Override
	public Menu getMenu(int idx) {
		if (idx < 0 || idx >= getMenuItemCount())
			return null;
		return menuBar.getItem(idx).getMenu();
	}

	/**
	* Adds a new menu item with the given label, command (optional), and
	* ActionListener. Returns the index of this item.
	* The argument hasSubMenu indicates whether this item must be a simple
	* item or a menu.
	*/
	@Override
	public int addMenuItem(String label, String command, ActionListener list, boolean hasSubMenu) {
		if (menuBar == null) {
			menuBar = new MyMenuBar();
			add(menuBar, "North");
		}
		MyMenuItem mit = new MyMenuItem(label);
		if (command != null) {
			mit.setActionCommand(command);
		}
		if (list != null) {
			mit.addActionListener(list);
		}
		if (hasSubMenu) {
			mit.add(new PopupMenu());
		}
		menuBar.addItem(mit);
		return menuBar.getItemCount() - 1;
	}

	/**
	* Removes the menu item with the given index
	*/
	@Override
	public void removeMenuItem(int idx) {
		if (menuBar != null) {
			menuBar.remove(idx);
		}
	}

	@Override
	public boolean isFocusTraversable() {
		return true;
	}

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
		if (cfgFile == null || cfgFile.length() < 1) {
			System.err.println("Cannot find parameter 'application', starting empty.");
		} else if (getParameter("DATA_SERVER") == null && getDocumentBase() != null) {
			URL url = URLSupport.makeURLbyPath(getDocumentBase(), cfgFile);
			if (url != null) {
				cfgFile = url.toString();
			}
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
		menuBar = null;
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
		parm.setParameter("User", getParameter("User"));
		parm.setParameter("VotingServletURL", getParameter("VotingServletURL"));
		parm.setParameter("ImageServlet", getParameter("ImageServlet"));
		//for CHCC must be false
		parm.setParameter("Allow_Background_Visualization", "true");
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
		if (startInFrame) {
			mainwin = new MainWin();
			ui = new DescartesUI(mainwin);
			mainwin.add(ui, "Center");
			ui.invalidate();
			mainwin.invalidate();
			mainwin.validate();
		} else {
			mainwin = null;
			ui = new DescartesUI(this);
			add(ui, "Center");
			ui.invalidate();
			invalidate();
			validate();
		}
		// create system core; set values of system parameters
		Core core = new Core();
		core.setSystemSettings(parm);
		core.setUI(ui);
		// for GIMMI Area Selector
		parm.setParameter("CORE", core);
		//read customization parameters: menu, visualization methods available, etc.
		Parameters menuParm = new Parameters();
		scr = new SysConfigReader(menuParm, null);
		String fileName = getParameter("menu_param_file");
		if (fileName == null) {
			fileName = "menu.cnf";
		}
		String paramFile = URLSupport.makeURLbyPath(getDocumentBase(), fileName).toString();
		if (scr.readConfiguration(paramFile)) {
			core.getSystemSettings().setParameter("MainMenu", menuParm);
		}
		Parameters mapVis = new Parameters();
		scr = new SysConfigReader(mapVis, null);
		fileName = getParameter("map_vis_file");
		if (fileName == null) {
			fileName = "map_vis.cnf";
		}
		paramFile = URLSupport.makeURLbyPath(getDocumentBase(), fileName).toString();
		if (scr.readConfiguration(paramFile)) {
			core.getSystemSettings().setParameter("MapVisMethods", mapVis);
		}
		core.makeOptionalComponents();

		ui.startWork(core, cfgFile);
		if (cfgFile != null && getParameter("InitVisSpec") != null) {
			String vspFileName = URLSupport.makeURLbyPath(getDocumentBase(), getParameter("InitVisSpec")).toString();
			if (vspFileName != null && vspFileName.length() > 0) {
				SnapshotManager sman = core.getSnapshotManager();
				sman.loadSnapshot(vspFileName, true);
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
		return "CommonGIS applet";
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
