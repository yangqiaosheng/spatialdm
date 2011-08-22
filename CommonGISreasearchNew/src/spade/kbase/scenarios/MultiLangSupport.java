package spade.kbase.scenarios;

import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
* Handles names and instruction texts specified in the knowledge base in several
* languages. Retrieves only the text in the current interface language.
*/
public class MultiLangSupport {
	public static String currLang = null;

	public static String getCurrentLanguage() {
		return currLang;
	}

	public static void setCurrentLanguage(String language) {
		currLang = language;
	}

	/**
	* Retrieves a text in current language from a vector with the following
	* structure:
	* at 0 - default text;
	* at (2*i+1), i>=0 - language name;
	* at (2*i+2), i>=0 - text in the respective language.
	*/
	public static String getCurrentLanguageText(Vector v) {
		if (v == null || v.size() < 1)
			return null;
		if (currLang == null)
			return (String) v.elementAt(0);
		for (int i = 1; i < v.size(); i += 2)
			if (currLang.equalsIgnoreCase((String) v.elementAt(i)))
				return (String) v.elementAt(i + 1);
		return (String) v.elementAt(0);
	}

	protected static String getTextFromNode(Node node) {
		if (node == null || node.getNodeType() != Node.ELEMENT_NODE)
			return null;
		((Element) node).normalize();
		NodeList children = node.getChildNodes();
		if (children == null || children.getLength() < 1)
			return null;
		for (int i = 0; i < children.getLength(); i++)
			if (children.item(i) != null && children.item(i).getNodeType() == Node.TEXT_NODE) {
				Text txt = (Text) children.item(i);
				String str = txt.getData();
				if (str != null)
					return str.trim();
			}
		return null;
	}

	/**
	* Retrieves texts in multiple languages from an element having children
	* DefaultText (one) and OneLanguageText (any number, including 0).
	* The elements of the type OneLanguageText should have an attribute Language.
	* Returns a vector with the following structure:
	* at 0 - default text;
	* at (2*i+1), i>=0 - language name;
	* at (2*i+2), i>=0 - text in the respective language.
	*/
	public static Vector getMultiLanguageTexts(Node node) {
		if (node == null || node.getNodeType() != Node.ELEMENT_NODE)
			return null;
		Element element = (Element) node;
		NodeList defaultList = element.getElementsByTagName("DefaultText");
		NodeList langTextList = element.getElementsByTagName("OneLanguageText");
		if ((defaultList == null || defaultList.getLength() < 1) && (langTextList == null || langTextList.getLength() < 1))
			return null; //no texts in expected format
		Vector v = new Vector(10, 10);
		if (defaultList != null && defaultList.getLength() > 0) {
			v.addElement(getTextFromNode(defaultList.item(0)));
		} else {
			v.addElement(null);
		}
		if (langTextList != null) {
			for (int i = 0; i < langTextList.getLength(); i++) {
				String text = getTextFromNode(langTextList.item(i));
				if (text != null && langTextList.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element tElem = (Element) langTextList.item(i);
					String lang = tElem.getAttribute("Language");
					if (lang != null) {
						v.addElement(lang);
						v.addElement(text);
					}
				}
			}
		}
		return v;
	}

	/**
	* Retrieves the text in the current language from an element having children
	* DefaultText (one) and OneLanguageText (any number, including 0).
	* The elements of the type OneLanguageText should have an attribute Language.
	* Returns the text from the element where the value of the attribute Language
	* is equal to currLang. If there is no such element, or if currLang==0,
	* returns the text from the element DefaultText. May return null if
	* DefaultText is absent.
	*/
	public static String getCurrentLanguageText(Node node) {
		if (node == null || node.getNodeType() != Node.ELEMENT_NODE)
			return null;
		Element element = (Element) node;
		String text = null;
		NodeList defaultList = element.getElementsByTagName("DefaultText");
		if (defaultList != null && defaultList.getLength() > 0) {
			text = getTextFromNode(defaultList.item(0));
		}
		if (currLang == null)
			return text; //return the default text
		NodeList langTextList = element.getElementsByTagName("OneLanguageText");
		if (langTextList == null || langTextList.getLength() < 1)
			return text; //no texts in different languages
		for (int i = 0; i < langTextList.getLength(); i++)
			if (langTextList.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element tElem = (Element) langTextList.item(i);
				String lang = tElem.getAttribute("Language");
				if (lang != null && lang.equalsIgnoreCase(currLang))
					return getTextFromNode(tElem);
			}
		return text;
	}
}