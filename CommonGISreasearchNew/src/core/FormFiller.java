package core;

import java.applet.Applet;

/**
* An interface for classes puting values into an HTML form by means of
* communicating with JavaScript. Used in order to avoid direct use of these
* classes and inclusion of the library for JavaScript communitation when
* it is not needed.
*/
public interface FormFiller {
	/**
	* Sets a reference to the applet that needs to put values in a form.
	*/
	public void setApplet(Applet app);

	/**
	* Setups a link to the form. Returns true on success.
	*/
	public boolean initLinkToForm(String docName, String formName);

	/**
	* Puts the specified value in the form field with the given name.
	*/
	public void setField(String fieldName, String value);
}
