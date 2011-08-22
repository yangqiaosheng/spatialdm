package ui;

import java.awt.Component;

/**
* Finds what About class is present in the current configuration and creates
* its instance. Each About class must be a descendant of java.awt.Component.
*/
public class AboutFinder {
	/**
	* Existing "About" classes
	*/
	protected static String aboutClasses[] = { "ui.AboutBetaVersion", "ui.AboutSystem", "ui.AboutCHCC" };

	/**
	* Finds what About class is present in the current configuration and creates
	* its instance.
	*/
	static public Component getAbout() {
		for (String aboutClasse : aboutClasses) {
			try {
				Component about = (Component) Class.forName(aboutClasse).newInstance();
				return about;
			} catch (Exception e) {
			}
		}
		return null;
	}
}