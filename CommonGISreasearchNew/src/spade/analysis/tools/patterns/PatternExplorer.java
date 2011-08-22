package spade.analysis.tools.patterns;

import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.StringTokenizer;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jan 30, 2009
 * Time: 10:33:21 AM
 * Allows the user to load and explore "patterns" (results of analysis).
 * Currently works with T-patterns.
 */
public class PatternExplorer implements DataAnalyser {
	protected ESDACore core = null;

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * Here, always returns true.
	 */
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* Remembers the last directory from where patterns were loaded
	*/
	protected static String lastDir = null;

	@Override
	public void run(ESDACore core) {
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		FileDialog fd = new FileDialog(core.getUI().getMainFrame(), "File with processor description?");
		if (lastDir != null) {
			fd.setDirectory(lastDir);
		}
		fd.setFile("*.dat;*.pat");
		fd.setMode(FileDialog.LOAD);
		fd.show();
		if (fd.getDirectory() == null)
			return;
		lastDir = fd.getDirectory();
		String fname = fd.getFile();
		if (lastDir != null && lastDir.length() > 0) {
			File file = new File(lastDir);
			if (file.exists()) {
				lastDir = file.getAbsolutePath().replace('\\', '/');
				if (!lastDir.endsWith("/")) {
					lastDir += "/";
				}
				fname = lastDir + fname;
			}
		}
		InputStream stream = openStream(fname);
		if (stream == null)
			return;
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		boolean openingTagFound = false;
		while (!openingTagFound) {
			try {
				String s = reader.readLine();
				if (s == null) {
					break;
				}
				s = s.trim();
				if (s.length() < 1) {
					continue;
				}
				if (s.equalsIgnoreCase("<PATTERNS>")) {
					openingTagFound = true;
					break;
				}
				StringTokenizer st = new StringTokenizer(s, " \n\r");
				if (st.hasMoreTokens()) {
					String tok = st.nextToken().toLowerCase();
					if (tok.startsWith("http") || tok.startsWith("accept-") || tok.startsWith("server") || tok.startsWith("date:") || tok.startsWith("last-modified:") || tok.startsWith("etag:") || tok.startsWith("content-")
							|| tok.startsWith("connection")) {
						continue;
					} else {
						break;
					}
				}
			} catch (IOException ioe) {
				showMessage(ioe.toString(), true);
				break;
			}
		}
		closeStream(stream);
		if (!openingTagFound) {
			showMessage("The opening tag <PATTERNS> has not been found!", true);
			return;
		}
		stream = openStream(fname); //to be at the beginning
		if (stream == null)
			return;
		reader = new BufferedReader(new InputStreamReader(stream));
		PatternReader patternReader = new PatternReader();
		patternReader.setCore(core);
		patternReader.setDirectory(lastDir);
		boolean ok = patternReader.loadPatterns(reader);
		closeStream(stream);
		if (ok) {
			showMessage("Successfully loaded!", false);
		}
	}

	protected InputStream openStream(String path) {
		if (path == null)
			return null;
		int idx = path.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = path.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		InputStream istream = null;
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(path);
				istream = url.openStream();
			} else {
				istream = new FileInputStream(path);
			}
		} catch (IOException ioe) {
			showMessage(ioe.toString(), true);
			return null;
		}
		return istream;
	}

	protected void closeStream(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		}
		if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
