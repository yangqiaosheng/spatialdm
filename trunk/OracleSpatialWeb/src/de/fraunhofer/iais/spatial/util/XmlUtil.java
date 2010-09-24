package de.fraunhofer.iais.spatial.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
		FileOutputStream o = null;
		try {
			o = new FileOutputStream(url);
			xmlOutputter.output(document, o);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (o != null) {
					o.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
