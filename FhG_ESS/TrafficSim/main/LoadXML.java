package main;

import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: natascha
 * Date: Dec 2, 2011
 * Time: 3:34:19 PM
 * Used to read an arbitrary XML document.
 */
public class LoadXML {

	public static Document loadXML(String fname) {
		if (fname == null) {
			System.out.println("File not found: " + fname);
			return null;
		}
		File f = new File(fname);
		if (!f.exists()) {
			System.out.println("File not found: " + fname);
			return null;
		}
		Document doc = null;
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			doc = docBuilder.parse(new File(fname));
			// normalize text representation
			doc.getDocumentElement().normalize();
		} catch (SAXParseException err) {
			System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
			System.out.println("   " + err.getMessage());
			// print stack trace as below
		} catch (SAXException e) {
			Exception x = e.getException();
			((x == null) ? e : x).printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return doc;
	}
}
