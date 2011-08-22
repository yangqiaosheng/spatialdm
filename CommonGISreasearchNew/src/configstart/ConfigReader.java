package configstart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import spade.lib.basicwin.NotificationLine;
import spade.lib.util.CopyFile;
import spade.lib.util.URLSupport;

public abstract class ConfigReader {
	protected NotificationLine lStatus = null;
	/**
	* The path to the directory with the configuration file
	*/
	protected String pathToDir = null;

	public ConfigReader(NotificationLine statusLabel) {
		lStatus = statusLabel;
	}

	/**
	* Notifies about the status of the process of data
	* loading. If "trouble" is true, then this is an error message.
	*/
	public void notifyProcessState(String processState, boolean trouble) {
		if (lStatus != null) {
			lStatus.showMessage(processState, trouble);
		}
		if (trouble) {
			System.out.println("ERROR: " + processState);
		} else {
			System.out.println(processState);
		}
	}

	/**
	* Notifies about the status of the process of data
	* loading. Assumes that this is a normal message, not an error.
	*/
	public void notifyProcessState(String processState) {
		notifyProcessState(processState, false);
	}

	/**
	* Reads the configuration from the specified BufferedReader.
	*/
	public abstract boolean readConfiguration(BufferedReader reader);

	/**
	* Reads the configuration from the specified file or URL. Created the
	* appropriate BufferedReader (using the method openStream) and calls the
	* method readConfiguration(BufferedReader) that should be implemented in
	* the descendants.
	*/
	public boolean readConfiguration(String cfgFile) {
		InputStream stream = openStream(cfgFile);
		if (stream == null)
			return false;
		pathToDir = CopyFile.getDir(cfgFile);
		if (pathToDir != null && pathToDir.length() < 1) {
			pathToDir = null;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		if (br == null)
			return false;
		boolean value = readConfiguration(br);
		try {
			stream.close();
		} catch (IOException ioe) {
		}
		return value;
	}

	/**
	* Opens the stream on the specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	public InputStream openStream(String dataSource) {
		if (dataSource == null)
			return null;
		int idx = dataSource.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = dataSource.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(dataSource);
				return url.openStream();
			} else {
				File file = new File(dataSource);
				if (file.exists())
					return new FileInputStream(file);
				URL url = URLSupport.makeURLbyPath(null, dataSource);
				if (url == null) {
					notifyProcessState("Cannot find " + dataSource);
				} else
					return url.openStream();
			}
		} catch (IOException ioe) {
			notifyProcessState("Error accessing " + dataSource + ": " + ioe);
		}
		return null;
	}
}
