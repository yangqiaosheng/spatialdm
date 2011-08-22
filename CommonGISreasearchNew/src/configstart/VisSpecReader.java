package configstart;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import spade.lib.util.ProcessListener;
import spade.lib.util.ProcessStateNotifier;
import spade.vis.spec.SpecSaver;

public class VisSpecReader {
	/**
	* The source of data - a path to a file or a URL
	*/
	protected String dataSource = null;
	/**
	* The stream from which the data are read
	*/
	protected InputStream stream = null;
	/**
	* Used to register possible listeners and notify them about the state of
	* data loading
	*/
	protected ProcessStateNotifier notifier = null;

	/**
	* Sets the data source - a path to a file or a URL
	*/
	public void setDataSource(String source) {
		dataSource = source;
	}

	/**
	* Adds a listener of the process of specification loading
	*/
	public void addProcessListener(ProcessListener lst) {
		if (lst == null)
			return;
		if (notifier == null) {
			notifier = new ProcessStateNotifier();
		}
		notifier.addProcessListener(lst);
	}

	/**
	* Notifies the listeners, if any, about the status of the process of data
	* loading. If "trouble" is true, then this is an error message.
	*/
	public void notifyProcessState(String processState, boolean trouble) {
		if (notifier != null) {
			notifier.notifyProcessState(this, "Reading visualization specification", processState, trouble);
		}
		if (trouble) {
			System.err.println("Reading visualization specification: " + processState);
		}
	}

	/**
	* Opens the stream on the earlier specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected void openStream() {
		if (stream != null)
			return;
		if (dataSource == null)
			return;
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
				stream = url.openStream();
			} else {
				stream = new FileInputStream(dataSource);
			}
		} catch (IOException ioe) {
			notifyProcessState("Error accessing " + dataSource + ": " + ioe, true);
			return;
		}
	}

	protected void closeStream() {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
		stream = null;
	}

	/**
	* Reads specifications for data visualization (on a map or/and additional
	* graphs). Returns a vector with the specifications. The elements of the
	* vector are some specifications, in particular, instances of
	* spade.vis.spec.ToolSpec or its descendants.
	*/
	public Vector read() {
		if (stream == null) {
			openStream();
		}
		if (stream == null)
			return null;
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));

		Vector specs = new Vector(20, 10);
		while (true) {
			try {
				String str = br.readLine();
				if (str == null) {
					break;
				}
				str = str.trim();
				if (str.length() < 1) {
					continue;
				}
				if (str.startsWith("<")) {
					str = str.substring(1).trim();
					int idx = str.indexOf('>');
					if (idx > 0) {
						String key = str.substring(0, idx).trim();
						if (key.length() > 0) {
							SpecSaver saver = StateSaverFactory.getSpecSaver(key);
							if (saver != null) {
								Object spec = saver.readSpecification(key, br);
								if (spec != null) {
									specs.addElement(spec);
								}
							}
						}
					}
				}
			} catch (EOFException ioe) {
				notifyProcessState("successfully finished", false);
				break;
			} catch (IOException ioe) {
				notifyProcessState("exception " + ioe.toString(), true);
				break;
			}
		}
		closeStream();
		if (specs.size() < 1) {
			notifyProcessState("no valid specification found!", true);
			return null;
		}
		return specs;
	}
}
