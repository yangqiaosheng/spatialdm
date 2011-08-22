package spade.lib.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class IconUtil {
	protected static byte buf[] = null;

	public static Image loadImage(Class master, String name, int length) {
		int nbytes = 0;
		if (buf == null || buf.length < length) {
			buf = new byte[length];
		}
		for (int i = 0; i < buf.length; i++) {
			buf[i] = 0;
		}
		try {
			InputStream is = master.getResourceAsStream(name);
			if (is == null) {
				System.out.println("Could not get the resource " + name + " for " + master.getName());
				return null;
			}
			DataInputStream stream = new DataInputStream(is);
			/*
			* the following line does not work well in JRE 1.2.2 and later
			* nbytes=stream.read(buf);
			*/
			nbytes = length;
			stream.readFully(buf);
		} catch (EOFException eofe) {
			for (int i = buf.length - 1; i > 0; i--) {
				if (buf[i] == 0) {
					continue;
				}
				nbytes = i + 1;
				break;
			}
			System.out.println("read " + nbytes + " bytes of the image  " + name);
		} catch (IOException e) {
			System.out.println(e.toString());
		} catch (Throwable th) {
			System.out.println("Could not read the image as a stream: " + th);
		}
		if (nbytes > 0) {
			byte imgbuf[] = new byte[nbytes];
			for (int i = 0; i < nbytes; i++) {
				imgbuf[i] = buf[i];
			}
			Image icon = Toolkit.getDefaultToolkit().createImage(imgbuf);
			return icon;
		}
		return null;
	}
}