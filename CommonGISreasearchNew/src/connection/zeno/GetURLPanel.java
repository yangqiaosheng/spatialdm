package connection.zeno;

import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import spade.lib.basicwin.DialogContent;
import spade.lib.lang.Language;

/**
* Asks the user to enter a URL. After entering, checks the correctness of the
* URL by trying to open it.
*/
public class GetURLPanel extends Panel implements DialogContent {
	static ResourceBundle res = Language.getTextResource("connection.zeno.Res");
	/**
	* This string contains an error message if an attempt to open the specified
	* URL fails.
	*/
	protected String err = null;
	/**
	* In this text field the user enters the URL
	*/
	protected TextField tf = null;
	/**
	* The correct URL
	*/
	protected URL url = null;
	/**
	* The document base (if the component is used in an applet). May be null.
	*/
	protected URL docBase = null;

	/**
	* Constructs the panel. The argument is the document base (if the component is
	* used in an applet). May be null.
	*/
	public GetURLPanel(URL documentBase, URL currentURL, String prompt) {
		docBase = documentBase;
		setLayout(new GridLayout(2, 1, 5, 0));
		add(new Label(prompt));
		tf = new TextField("", 50);
		if (currentURL != null) {
			tf.setText(currentURL.toString());
		} else if (docBase == null) {
			tf.setText("file://");
		} else {
			tf.setText("http://");
		}
		add(tf);
	}

	/**
	* Checks the correctness of the URL specified by the user by trying to open it.
	*/
	@Override
	public boolean canClose() {
		String str = tf.getText();
		if (str != null) {
			str = str.trim();
		}
		if (str == null || str.length() < 1) {
			err = res.getString("No_URL_entered_");
			return false;
		}
		url = tryMakeURL(str);
		if (url == null)
			return false;
		return true;
	}

	protected URL tryMakeURL(String path) {
		URL url = null;
		path = path.replace('\\', '/');
		try {
			if (path.startsWith("file:") || path.startsWith("http:")) {
				url = new URL(path);
			} else {
				if (docBase == null) { //try to get data using the "file:" protocol
					String workPath = path;
					if (!workPath.startsWith("/") && workPath.charAt(1) != ':') {
						//System.out.println("Making full path by getting the current directory");
						workPath = java.lang.System.getProperty("user.dir");
						if (workPath == null)
							return null;
						workPath = workPath.replace('\\', '/');
						if (workPath.endsWith("/")) {
							workPath = workPath + path;
						} else {
							workPath = workPath + "/" + path;
						}
					}
					String urlString = ((workPath.charAt(0) == '/') ? "file:" : "file:/") + workPath;
					//System.out.println("urlString="+urlString);
					tf.setText(urlString);
					url = new URL(urlString);
				} else if (path.startsWith("/") || path.charAt(1) == ':') {
					url = new URL(docBase, path);
					//System.out.println("make URL by new URL(docBase,path)");
				} else {
					String s1 = docBase.toString().replace('\\', '/');
					int ix = s1.lastIndexOf('/');
					if (ix > 0) {
						s1 = s1.substring(0, ix + 1);
					}
					String urlString = s1 + path;
					tf.setText(urlString);
					url = new URL(urlString);
					//System.out.println("make URL by new URL(s1+path), sl=["+s1+"], path="+
					//  path);
				}
			}
			//System.out.println("Data url="+url);
		} catch (MalformedURLException mfe) {
			err = res.getString("Failed_to_open_the") + mfe.toString();
			System.out.println(mfe);
			return null;
		}
		return url;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}

	public URL getURL() {
		return url;
	}
}
