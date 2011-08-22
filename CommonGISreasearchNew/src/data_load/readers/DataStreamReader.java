package data_load.readers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;

import spade.lib.lang.Language;

/**
* A base class for reading data from streams.
*/

public abstract class DataStreamReader extends BaseDataReader {
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	/**
	* The stream from which the data are read
	*/
	protected InputStream stream = null;

	/**
	* Opens the stream on the earlier specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected void openStream() {
		if (stream != null)
			return;
		if (spec == null || spec.source == null)
			return;
		//System.out.println("openStream: path="+spec.source);
		int idx = spec.source.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = spec.source.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		try {
			if (isURL) { //try to access the source as a URL
				//following text:"Trying to open the URL "
				showMessage(res.getString("Trying_to_open_the") + spec.source, false);
				System.out.println("Trying to open the URL " + spec.source);
				URL url = new URL(spec.source);
				System.out.println("URL=" + url);
				URLConnection urlc = url.openConnection();
				urlc.setUseCaches(mayUseCache);
				System.out.println(">>> Use caches is set to " + mayUseCache);
				stream = urlc.getInputStream();
			} else {
				stream = new FileInputStream(spec.source);
			}
		} catch (IOException ioe) {
			//following text:"Error accessing "
			showMessage(res.getString("Error_accessing") + spec.source + ": " + ioe, true);
			dataError = true;
			return;
		} catch (Throwable thr) {
			//following text:"Error accessing "
			showMessage(res.getString("Error_accessing") + spec.source + ": " + thr.toString(), true);
			dataError = true;
			return;
		}
		if (stream == null) {
			//following text:"Could not open "
			showMessage(res.getString("Could_not_open") + spec.source, true);
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
}
