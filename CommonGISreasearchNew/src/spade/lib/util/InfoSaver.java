package spade.lib.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Calendar;

public class InfoSaver {
	protected String fname = null;
	protected PrintStream ps = null;
	protected OutputStream out = null;
	protected ObjectOutputStream objOut = null;
	protected static String pathToScript = null;
	protected static String votingServletURL = null;
	protected static boolean isApplet = false;
	protected boolean newFile = true;

	protected URL urlDocBase = null;
	private URLConnection urlcVotingServlet = null;

	private boolean newConnection = true;

	public void setFileName(String fileName) {
		fname = fileName;
		newFile = true;
	}

	public void setPathToScript(String path) {
		pathToScript = path;
	}

	public void setVotingServletURL(String path) {
		votingServletURL = path;
	}

	public void setDocBaseURL(URL docBase) {
		urlDocBase = docBase;
	}

	public void setIsApplet(boolean value) {
		isApplet = value;
	}

	public void saveString(String str) {
		if (isApplet) {
			if (pathToScript != null) {
				saveStringUsingScript(str);
			}
		} else {
			saveStringToFile(str);
		}
	}

	public String encodeStringForScript(String str) {
		if (str == null)
			return null;
		StringBuffer sb = new StringBuffer(str.length() * 3);
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
				sb.append(c);
			} else if (c == ' ') {
				sb.append("%" + Integer.toHexString('_')); //for Apache
			} else if (c >= 16) {
				sb.append("%" + Integer.toHexString(c));
			}
		}
		return sb.toString();
	}

	protected void saveStringUsingScript(String str) {
		if (pathToScript == null || fname == null)
			return;
		// now convert the string: remove all "wrong" characters...
		str = encodeStringForScript(str);
		// save
		String urlStr = pathToScript + "?" + fname + "+" + str;
		BufferedReader reader = null;
		// Starting the script
		try {
			URL url = new URL(urlStr + "\n");
			if (url != null) {
				URLConnection urlc = url.openConnection();
				urlc.setUseCaches(false);
				InputStreamReader isr = new InputStreamReader(urlc.getInputStream());
				reader = new BufferedReader(isr);
			}
		} catch (IOException e) {
			System.out.println(e.toString());
			return;
		}
		if (reader == null) {
			System.out.println("Failed to start the script " + urlStr);
			return;
		}
		//System.out.println("*** Script's stdout:");
		String inStr;
		boolean NoMoreLines = false;
		while (!NoMoreLines) {
			try {
				inStr = reader.readLine();
			} catch (IOException e) {
				NoMoreLines = true;
				break;
			}
			if (inStr == null) {
				NoMoreLines = true;
				//else
				//System.out.println(inStr);
			}
		}
		try {
			reader.close();
		} catch (IOException ioe) {
		}
	}

	protected void saveStringToFile(String str) {
		if (ps == null)
			if (!openFile())
				return;
		ps.println(str);
		ps.flush();
	}

	public void saveStringThroughServlet(String str) throws Exception {
		if (objOut == null && newConnection) {
			if (!initServlet())
				throw (new Exception("Voting servlet was not initialized"));
		}
		if (str != null && objOut != null) {
			if (!str.endsWith("\n")) {
				str += '\n';
			}
			try {
				objOut.writeObject(str);
				if (str.indexOf("EOF") > 0) {
					objOut.flush();
				}
			} catch (IOException ioe) {
				throw (new Exception("Cannot send object to servlet"));
			}
		}
	}

	protected boolean openFile() {
		if (fname == null)
			return false;
		File f = new File(fname);
		if (newFile && f.exists()) {
			f.delete();
		}
		try {
			ps = new PrintStream(new FileOutputStream(fname, true));
		} catch (IOException ioe) {
			System.out.println("Cannot open the output file: " + fname + "\n" + ioe.toString());
			return false;
		}
		newFile = false;
		return true;
	}

	protected boolean initServlet() {
		if (votingServletURL == null) {
			System.out.println("Cannot initialize: servlet's URL not specified" + "\n");
			return false;
		}
		URL storeURL = null;
		newConnection = false;
		try {
			if (votingServletURL.indexOf("http") < 0 && urlDocBase != null) {
				storeURL = URLSupport.makeURLbyPath(urlDocBase, votingServletURL);
			} else {
				storeURL = new URL(votingServletURL);
			}
		} catch (MalformedURLException mfe) {
			System.out.println(mfe);
			return false;
		}
		out = null;
		try {
			urlcVotingServlet = storeURL.openConnection();
			urlcVotingServlet.setDoOutput(true);
			out = urlcVotingServlet.getOutputStream();
			if (out == null) {
				System.out.println("Cannot access output stream of servlet's connection");
			}
			objOut = new ObjectOutputStream(out);
		} catch (Exception e) {
			System.out.println("Cannot initialize the servlet as output: " + votingServletURL + "\n" + e.toString());
			return false;
		}
		System.out.println("Servlet initialized: " + votingServletURL + "\n");
		return true;
	}

	protected String getString(int n, int length) {
		String str = String.valueOf(n);
		while (str.length() < length) {
			str = "0" + str;
		}
		return str;
	}

	public String generateFileName() {
		Calendar currTime = Calendar.getInstance();
		String name = currTime.get(Calendar.YEAR) + "_" + getString(1 + currTime.get(Calendar.MONTH), 2) + "_" + getString(currTime.get(Calendar.DAY_OF_MONTH), 2) + "_" + getString(currTime.get(Calendar.HOUR_OF_DAY), 2) + "_"
				+ getString(currTime.get(Calendar.MINUTE), 2) + "_" + getString(currTime.get(Calendar.SECOND), 2);
		String host = null;
		System.out.println("InfoSaver: isApplet=" + isApplet);
		if (!isApplet) {
			try {
				host = InetAddress.getLocalHost().getHostAddress().replace('.', '-');
			} catch (UnknownHostException uhe) {
			} catch (Exception ex) {
			}
		}
		if (host != null) {
			name += "_" + host;
		}
		return name;
	}

	public void finish() {
		if (out != null) {
			try {
				out.close();
			} catch (IOException ioe) {
			}
		}
		out = null;
		if (ps != null) {
			ps.close();
		}
		ps = null;
		if (urlcVotingServlet != null) {
			collectServletFeedback();
		}
	}

	private void collectServletFeedback() {
		// servlet specific things: establishing feedback
		InputStream in = null;
		BufferedReader servletFeedback = null;
		try {
			in = urlcVotingServlet.getInputStream();
			servletFeedback = new BufferedReader(new InputStreamReader(in));
		} catch (Exception e) {
			System.out.println("ERROR: Cannot establish servlet's feedback: \n" + e.toString());
		}
		try {
			if (servletFeedback != null) {
				String line = null;
				while (true) {
					line = servletFeedback.readLine();
					if (line == null) {
						break;
					}
					line = line.trim();
					System.out.println("Servlet's output: [" + line + "]");
				}
			}
		} catch (Exception e) {
			System.out.println("ERROR: cannot read servlet's feedback: \n" + e.toString());
		}
		if (servletFeedback != null) {
			try {
				servletFeedback.close();
			} catch (IOException ioe) {
			}
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException ioe) {
				System.out.println("ERROR: cannot close servlet's feedback: \n" + ioe.toString());
			}
		}
	}
}
