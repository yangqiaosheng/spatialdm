package guide_tools.tutorial;

import java.io.PrintStream;
import java.util.Calendar;
import java.util.Vector;

public class TutorialUnit {
	/*
	* Number of the unit
	*/
	public int uN = 0;
	public String name = null;
	public String pathToExample = null;
	/**
	* Description of the unit (to be shown to the user). The description is
	* not necessarily present.
	*/
	public String description = null;
	/**
	* Specification of the map required for the unit
	*/
	public MapSpec msp = null;
	/**
	* Besides the map, a unit may involve supplementary tools like dynamic query,
	* plots etc. This vector contais instances of the class ToolSpec
	*/
	public Vector tools = null;
	/**
	* There may be a recommendation (advise) to the user concerning how to
	* prepare the map (which layers to switch on/off, what colours to choose
	* etc.) This data element contains such recommendations.
	*/
	public Advise advise = null;
	/**
	* The tasks comprising the unit
	*/
	public Vector tasks = null;
	/**
	* The form for unit evaluation by the user
	*/
	public Task evalForm = null;
	/**
	* The times when the unit was started and finished
	*/
	public Calendar start = null, finish = null;

	public void setUnitNumber(int n) {
		uN = n;
		for (int i = 0; i < getTaskCount(); i++) {
			getTask(i).setUnitNumber(uN);
		}
	}

	public void addDescriptionLine(String str) {
		if (str == null)
			return;
		if (description == null) {
			description = new String(str);
		} else {
			description += " " + str;
		}
	}

	public String getDescription() {
		return description;
	}

	public void addTask(Task tsk) {
		if (tsk == null)
			return;
		if (tasks == null) {
			tasks = new Vector(10, 5);
		}
		tasks.addElement(tsk);
		tsk.setTaskNumber(tasks.size());
		tsk.setUnitNumber(uN);
	}

	public int getTaskCount() {
		if (tasks == null)
			return 0;
		return tasks.size();
	}

	public Task getTask(int idx) {
		if (idx < 0 || idx >= getTaskCount())
			return null;
		return (Task) tasks.elementAt(idx);
	}

	public void addToolSpecification(ToolSpec tsp) {
		if (tsp == null)
			return;
		if (tools == null) {
			tools = new Vector(2, 2);
		}
		tools.addElement(tsp);
	}

	public int getToolCount() {
		if (tools == null)
			return 0;
		return tools.size();
	}

	public ToolSpec getToolSpecification(int idx) {
		if (idx < 0 || idx >= getToolCount())
			return null;
		return (ToolSpec) tools.elementAt(idx);
	}

	public void printToStream(PrintStream ps) {
		if (ps == null)
			return;
		ps.print("Unit");
		if (uN > 0) {
			ps.print(" " + uN);
		}
		ps.println(": " + name);
		if (description != null) {
			ps.println(description);
		}
		if (pathToExample != null) {
			ps.println("See example at " + pathToExample);
		}
		if (msp != null) {
			msp.printToStream(ps);
		}
		for (int i = 0; i < getToolCount(); i++) {
			getToolSpecification(i).printToStream(ps);
		}
		if (advise != null) {
			advise.printToStream(ps);
		}
		for (int i = 0; i < getTaskCount(); i++) {
			getTask(i).printToStream(ps);
		}
		if (evalForm != null) {
			ps.println("--- Unit evaluation form ---");
			evalForm.printToStream(ps);
		}
		ps.println("-------------------------------------------");
	}
}