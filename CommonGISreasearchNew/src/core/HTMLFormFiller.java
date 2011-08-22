package core;

import java.applet.Applet;

import netscape.javascript.JSObject;

/**
* Puts values into an HTML form by means of communicating with JavaScript.
*/
public class HTMLFormFiller implements FormFiller {
	/**
	*  Elements for communication with javascript objects
	*/
	protected Applet a = null;
	protected JSObject win = null;
	protected JSObject document = null;
	protected JSObject form = null;

	/**
	* Sets a reference to the applet that needs to put values in a form.
	*/
	@Override
	public void setApplet(Applet app) {
		a = app;
	}

	/**
	* Setups a link to the form. Returns true on success.
	*/
	@Override
	public boolean initLinkToForm(String docName, String formName) {
		if (a == null)
			return false;
		/* build the form object to access fields in the web page */
		try {
			win = JSObject.getWindow(a);
			if (win == null)
				return false;
			document = (JSObject) win.getMember(docName);
			if (document == null)
				return false;
			form = (JSObject) document.getMember(formName);
			if (form != null)
				return true;
		} catch (Throwable ex) {
			System.out.println("ERROR: Cannot connect to the HTML page; document name=<" + docName + ">, form name=<" + formName + ">");
		}
		return false;
	}

	/**
	* Puts the specified value in the form field with the given name.
	*/
	@Override
	public void setField(String fieldName, String value) {
		if (form == null) {
			//System.out.println("form = null");
			System.out.println("===> FORM: field " + fieldName);
			System.out.println(value);
			return;
		}
		try {
			JSObject jfield = (JSObject) form.getMember(fieldName);
			if (jfield == null) {
				System.out.println("===> FORM: field " + fieldName + " is null!!!");
				return;
			}
			jfield.setMember("value", value);
		} catch (Exception ex) {
		}
	}
}
