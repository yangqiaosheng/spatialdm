package de.fraunhofer.iais.spatial.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class XmlUtil {

	public static String xml2String(Document document, boolean compact) {
		XMLOutputter xmlOutputter = new XMLOutputter();
		if (compact == true) {
			xmlOutputter.setFormat(Format.getCompactFormat());
		} else {
			xmlOutputter.setFormat(Format.getPrettyFormat());
		}
		return xmlOutputter.outputString(document);
	}

	public static void xml2File(Document document, String url, boolean compact) {
		XMLOutputter xmlOutputter = new XMLOutputter();
		if (compact == true) {
			xmlOutputter.setFormat(Format.getCompactFormat());
		} else {
			xmlOutputter.setFormat(Format.getPrettyFormat());
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(url);

			xmlOutputter.output(document, fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void xml2Kmz(Document document, String filenamePrefix, boolean compact) {
		XMLOutputter xmlOutputter = new XMLOutputter();
		if (compact == true) {
			xmlOutputter.setFormat(Format.getCompactFormat());
		} else {
			xmlOutputter.setFormat(Format.getPrettyFormat());
		}

		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(filenamePrefix + ".kml"));
			zos.putNextEntry(new ZipEntry(filenamePrefix.substring(filenamePrefix.indexOf('/') + 1) + ".kml"));

			xmlOutputter.output(document, zos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (zos != null) {
					zos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
