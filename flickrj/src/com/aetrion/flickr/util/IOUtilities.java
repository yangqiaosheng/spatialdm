/*
 * Copyright (c) 2005 Aetrion LLC.
 */
package com.aetrion.flickr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Common IO utilities.
 *
 * @author Anthony Eden
 */
public class IOUtilities {

	private IOUtilities() {

	}

	public static void close(HttpURLConnection conn) {
		if (conn != null) {
			try {
				conn.disconnect();
			} finally {
				conn = null;
			}
		}
	}

	public static void close(InputStream s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {

			} finally {
				s = null;
			}
		}
	}

	public static void close(OutputStream s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {

			} finally {
				s = null;
			}
		}
	}

	public static void close(Reader s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {

			}
		}
	}

	public static void close(Writer s) {
		if (s != null) {
			try {
				s.close();
			} catch (IOException e) {

			} finally {
				s = null;
			}
		}
	}

}
