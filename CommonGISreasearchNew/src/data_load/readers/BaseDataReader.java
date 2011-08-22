package data_load.readers;

import java.awt.Frame;

import spade.analysis.system.DataReader;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.GetPathDlg;
import spade.vis.spec.DataSourceSpec;

/**
* Data readers from various sources may extend this class that provides
* useful functions for notification about the status of data loading and
* for browsing for files.
* For convenience, all methods declared in the interface DataReader are listed
* here.
*/
public abstract class BaseDataReader implements DataReader {
	/**
	* Remembers the last directory from which some file have been loaded in
	* order to propose the user next time to select a file in the same directory
	*/
	protected static String dir = null;
	/**
	* The UI used, in particular, for displaying notification messages
	*/
	protected SystemUI ui = null;
	/**
	* Indication of data error - to avoid repeated attempts to read data
	* when the first attempt failed
	*/
	protected boolean dataError = false;
	/**
	* The specification of the data source
	*/
	protected DataSourceSpec spec = null;
	/**
	* Indicates whether the process of reading data is in progress.
	*/
	protected boolean dataReadingInProgress = false;
	/**
	* Indicates whether the use of the cache is allowed when data are loaded
	* over the Internet. By default, using of caches is allowed.
	*/
	protected boolean mayUseCache = true;

	/**
	* Specifies the source of the data to be loaded.
	*/
	@Override
	public void setDataSource(DataSourceSpec spec) {
		this.spec = spec;
	}

	/**
	* Returns its specification of the data source
	*/
	@Override
	public DataSourceSpec getDataSourceSpecification() {
		return spec;
	}

	/**
	* Allows or prohibits using the cache while loading data over the Internet.
	*/
	@Override
	public void setMayUseCache(boolean value) {
		mayUseCache = value;
	}

	/**
	* Sets the system UI that can be used, in particular, for displaying
	* messages about the status of data loading. Through the system UI the reader
	* may get access to the main frame of the system (needed for dialogs). When
	* necessary, the reader may also get the map view and the map canvas from it.
	* In this way the current territory extent may be found out.
	*/
	@Override
	public void setUI(SystemUI ui) {
		this.ui = ui;
	}

	/**
	* Displays the notification message using the system UI. The second argument
	* indicates whether this is an error message.
	*/
	protected void showMessage(String msg, boolean error) {
		if (ui != null) {
			ui.showMessage(msg, error);
		}
		if (msg != null && error) {
			System.err.println("ERROR: " + msg);
			//System.out.println(msg);
		}
	}

	/**
	* Gets a reference to a frame (needed for construction of dialogs).
	* First tries to get the main frame from the system UI. If this fails,
	* uses an invisible "dummy" frame
	*/
	protected Frame getFrame() {
		if (ui != null && ui.getMainFrame() != null)
			return ui.getMainFrame();
		return CManager.getAnyFrame();
	}

	/**
	* Runs a dialog in which the user can specify the file or URL with data
	* to be loaded
	*/
	protected String browseForFile(String dialogTitle, String fileMask) {
		GetPathDlg fd = new GetPathDlg(getFrame(), dialogTitle);
		if (fileMask != null) {
			fd.setFileMask(fileMask);
		}
		if (dir != null) {
			fd.setDirectory(dir);
		}
		fd.show();
		dir = fd.getDirectory();
		return fd.getPath();
	}

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
	@Override
	public abstract boolean loadData(boolean mayAskUser);

	/**
	* Sets the flag that the process of reading data is currently in progress
	* (true) or not (false).
	*/
	protected synchronized void setDataReadingInProgress(boolean flag) {
		synchronized (this) {
			dataReadingInProgress = flag;
			if (!dataReadingInProgress) {
				try {
					notifyAll();
				} catch (Exception e) {
				}
			}
		}
	};

	/**
	* Waits until the process of reading data finishes.
	*/
	protected void waitDataReadingFinish() {
		if (!dataReadingInProgress)
			return;
		while (dataReadingInProgress) {
			synchronized (this) {
				try {
					wait(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};
}
