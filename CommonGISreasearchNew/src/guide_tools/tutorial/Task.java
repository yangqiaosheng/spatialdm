package guide_tools.tutorial;

import java.io.PrintStream;
import java.util.Vector;

public class Task {
	/**
	* Numbers of the task and the unit it belongs to
	*/
	public int tN = 0, uN = 0;
	public String taskText = null;
	/**
	* If this is a form rather than a task, it may have a name
	*/
	public String name = null;
	/**
	* There may be a picture (illustration) to the task. This variable contains
	* the path to the picture.
	*/
	public String pathToPicture = null;
	/**
	* A task includes one or more questions. This vector contains the
	* questions - instances of the class Question
	*/
	public Vector questions = null;

	public void setTaskNumber(int n) {
		tN = n;
		for (int i = 0; i < getQuestionCount(); i++) {
			getQuestion(i).setTaskNumber(tN);
		}
	}

	public void setUnitNumber(int n) {
		uN = n;
		for (int i = 0; i < getQuestionCount(); i++) {
			getQuestion(i).setUnitNumber(uN);
		}
	}

	public void addTextString(String str) {
		if (str == null)
			return;
		if (taskText == null) {
			taskText = new String(str);
		} else {
			taskText += " " + str;
		}
	}

	public String getTaskText() {
		return taskText;
	}

	public void addQuestion(Question quest) {
		if (quest == null)
			return;
		if (questions == null) {
			questions = new Vector(5, 5);
		}
		questions.addElement(quest);
		quest.setQuestionNumber(questions.size());
		quest.setTaskNumber(tN);
		quest.setUnitNumber(uN);
	}

	public int getQuestionCount() {
		if (questions == null)
			return 0;
		return questions.size();
	}

	public Question getQuestion(int idx) {
		if (idx < 0 || idx >= getQuestionCount())
			return null;
		return (Question) questions.elementAt(idx);
	}

	public void printToStream(PrintStream ps) {
		if (name != null) {
			ps.println("Form \"" + name + "\"");
		} else {
			ps.print("Task ");
			if (uN > 0) {
				ps.print(uN + ".");
			}
			if (tN > 0) {
				ps.print(tN);
			}
			ps.println();
		}
		if (taskText != null) {
			ps.println(taskText);
		}
		if (pathToPicture != null) {
			ps.println("Illustration: " + pathToPicture);
		}
		for (int i = 0; i < getQuestionCount(); i++) {
			getQuestion(i).printToStream(ps);
		}
		ps.println();
	}
}