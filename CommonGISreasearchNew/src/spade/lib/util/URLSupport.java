package spade.lib.util;

import java.net.MalformedURLException;
import java.net.URL;

public class URLSupport {
	public static URL makeQueryURL(String hostName, String path, String query) {
		String cmdLine = path;
		if (query != null && cmdLine.indexOf('?') > 0) {
			cmdLine = cmdLine + query;
		}
		System.out.println("cmdline=" + cmdLine);
		URL url = null;
		try {
			url = new URL("http", hostName, cmdLine + "\n");
		} catch (MalformedURLException mfe) {
			System.out.println(mfe);
			return null;
		}
		return url;
	}

	/**
	* When not an applet, docBase should be null.
	*/
	public static URL makeURLbyPath(URL docBase, String path) {
		URL url = null;
		path = path.replace('\\', '/');
		System.out.println("path=" + path);
		try {
			if (path.startsWith("file:") || path.startsWith("http:")) {
				url = new URL(path);
				//System.out.println("make URL by new URL(path)");
			} else {
				if (docBase == null) { //try to get data using the "file:" protocol
					String workPath = path;
					if (!workPath.startsWith("/") && workPath.charAt(1) != ':') {
						System.out.println("Making full path by getting the current directory");
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
					System.out.println("urlString=" + urlString);
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
					url = new URL(s1 + path);
					//System.out.println("make URL by new URL(s1+path), sl=["+s1+"], path="+
					//  path);
				}
			}
			System.out.println("Data url=" + url);
		} catch (MalformedURLException mfe) {
			System.out.println(mfe);
			return null;
		}
		return url;
	}
}