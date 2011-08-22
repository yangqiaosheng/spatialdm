package spade.lib.util;

// no texts

import java.io.BufferedReader;
import java.io.IOException;

public class MyStream {
	BufferedReader DIS = null;
	static String lastLine = null;

	public MyStream(BufferedReader dis) {
		DIS = dis;
		lastLine = null;
	}

	public String receiveString() {
		String str = null;
		try {
			str = DIS.readLine();
		} catch (IOException e) {
		}
		return str;
	}

	public String getNewLine() {
		if (lastLine == null) {
			do {
				try {
					lastLine = DIS.readLine();
				} catch (IOException e) {
					return null;
				}
				if (lastLine != null) {
					lastLine.trim();
				}
			} while (lastLine != null && lastLine.equals(""));
		}
		return lastLine;
	}

	public String readLine() {
		String str = null;
		if (lastLine != null) {
			str = lastLine;
			lastLine = null;
			return str;
		}
		do {
			try {
				str = DIS.readLine();
			} catch (IOException e) {
				System.out.println("IOException in readLine, e=" + e);
			}
			if (str != null) {
				str.trim();
			}
		} while (str != null && str.equals(""));
		return str;
	}

	public void clearLine() {
		lastLine = null;
	}
}