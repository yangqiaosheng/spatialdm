package guide_tools.tutorial;

import java.io.PrintStream;
import java.util.Vector;

public class TutorialContent {
	/**
	* A tutorial consists of units. Elements of the Vector are instances of
	* TutorialUnit.
	*/
	public Vector units = null;
	/**
	* A tutorial may include not only units but also various forms such as
	* user profile, subjective satisfaction questionnaire etc. This vector
	* contains all parts of the tutorial (i.e. objects of various types).
	* Some of them are units.
	*/
	public Vector contents = null;
	/**
	* This is the path to the directory with the tutorial. This directory
	* may contain examples, illustrations etc. This is the same  directory
	* where the tutorial description is stored.
	*/
	public String pathToTutorial = "";

	public void addUnit(TutorialUnit unit) {
		if (unit == null)
			return;
		if (units == null) {
			units = new Vector(10, 5);
		}
		units.addElement(unit);
		unit.setUnitNumber(units.size());
		if (contents == null) {
			contents = new Vector(10, 5);
		}
		contents.addElement(unit);
	}

	public void addForm(Task form) {
		if (form == null)
			return;
		if (contents == null) {
			contents = new Vector(10, 5);
		}
		contents.addElement(form);
	}

	public int getUnitCount() {
		if (units == null)
			return 0;
		return units.size();
	}

	public TutorialUnit getUnit(int idx) {
		if (idx < 0 || idx >= getUnitCount())
			return null;
		return (TutorialUnit) units.elementAt(idx);
	}

	public int getContentItemCount() {
		if (contents == null)
			return 0;
		return contents.size();
	}

	public int getIndexInContents(Object item) {
		if (item == null || contents == null)
			return -1;
		return contents.indexOf(item);
	}

	public Object getContentItem(int idx) {
		if (idx < 0 || idx >= getContentItemCount())
			return null;
		return contents.elementAt(idx);
	}

	public void printToStream(PrintStream ps) {
		if (ps == null)
			return;
		if (contents == null || contents.size() < 1) {
			ps.println("No contents in the tutorial!");
		} else {
			for (int i = 0; i < contents.size(); i++)
				if (contents.elementAt(i) instanceof TutorialUnit) {
					((TutorialUnit) contents.elementAt(i)).printToStream(ps);
				} else if (contents.elementAt(i) instanceof Task) {
					((Task) contents.elementAt(i)).printToStream(ps);
				}
		}
	}
}