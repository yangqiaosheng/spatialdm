package guide_tools.tutorial;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.ImageCanvas;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.help.Helper;
import spade.lib.util.InfoSaver;
import spade.lib.util.StringUtil;

/**
* A Tutor is a wizard that runs a tutorial specified as an instance of
* TutorialContent: prepares maps and additional tools, gives instructions,
* puts tasks, asks questions, and stores the answers.
*/
public class Tutor implements ActionListener {
	protected static Color bkgWinColor = new Color(210, 255, 210), bkgInstructionColor = new Color(192, 255, 192), bkgTaskColor = new Color(192, 192, 255), bkgQuestionColor = new Color(255, 255, 192);
	/**
	* The tutorial to follow. A tutorial consists of one or more units.
	*/
	protected TutorialContent content = null;
	/**
	* The TutorSupport provides access to system tools and thereby allows
	* to prepare maps and other tools for fulfilling a unit.
	*/
	protected TutorSupport tsup = null;
	/**
	* The frame in which everything is shown
	*/
	protected Frame win = null;
	/**
	* Current unit number
	*/
	protected int currUnitN = -1;
	/**
	* Current task number (within the current unit)
	*/
	protected int currTaskN = -1;
	/**
	* Current question number (within the current task)
	*/
	protected int currQN = -1;
	/**
	* The number of the last unit completed
	*/
	protected int lastUnitN = -1;
	/**
	* The panel that includes a task and a question
	*/
	protected Panel mainPanel = null;
	/**
	* Text canvas with task text
	*/
	protected TextCanvas taskTC = null;
	/**
	* References to these components are needed to simplify their removal
	* from the main panel
	*/
	protected Component imageView = null, questionPanel = null;
	/**
	* The time when the tutor started
	*/
	protected Calendar startTime = null;
	/**
	* The time when the current question was asked
	*/
	protected long qTime = 0L, exampleStartTime = 0L;
	/**
	* Used to save results of testing
	*/
	protected InfoSaver saver = null;
	/**
	* Time required for answering the questions
	*/
	protected long answerTimeTotal = 0L, answerTimeUnit = 0L;
	/**
	* Counts of correct and incorrect answers
	*/
	protected int nCorrectTotal = 0, nWrongTotal = 0, nRefusedTotal = 0, nCorrectUnit = 0, nWrongUnit = 0, nRefusedUnit = 0;
	/*
	* Indicates whether the user should be shown the statistics of correct and
	* wrong answers
	*/
	public static boolean showStatisticsToUser = false;
	/**
	* Indicates that the tutorial is currently running
	*/
	protected boolean running = false;

	public void setContent(TutorialContent tc) {
		content = tc;
	}

	public void setTutorSupport(TutorSupport support) {
		tsup = support;
	}

	public void runTutorial() {
		if (win != null) {
			win.toFront();
			return;
		}
		if (content == null || content.getUnitCount() < 1)
			return;
		if (lastUnitN >= content.getUnitCount() - 1) {
			lastUnitN = -1;
		}
		if (lastUnitN < 0) {
			answerTimeTotal = 0L;
			nCorrectTotal = 0;
			nWrongTotal = 0;
			nRefusedTotal = 0;
		}
		if (saver == null) {
			saver = new InfoSaver();
			saver.setIsApplet(tsup.getIsApplet());
			saver.setFileName(tsup.getResultDir() + saver.generateFileName() + ".log");
			saver.setPathToScript(tsup.getPathToResultStoringScript());
		}
		running = true;
		runUnit(selectUnitToStart());
	}

	public int selectUnitToStart() {
		if (content == null || content.getUnitCount() < 1)
			return -1;
		if (lastUnitN >= content.getUnitCount() - 1) {
			lastUnitN = -1;
		}
		if (lastUnitN >= 0)
			return lastUnitN + 1;
		if (content.getUnitCount() == 1)
			return 0;
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("The tutorial/test consists of the following units:"));
		CheckboxGroup cbg = new CheckboxGroup();
		for (int i = 0; i < content.getUnitCount(); i++) {
			p.add(new Checkbox((i + 1) + ") " + content.getUnit(i).name, i == 0, cbg));
		}
		TextCanvas tc = new TextCanvas();
		tc.setBackground(bkgInstructionColor);
		tc.addTextLine("The units will run consequtively.");
		tc.addTextLine("If you did the tutorial earlier but your work was " + "interrupted for some reason (for example, the system crashed), " + "you can resume your work from the unit at which you were " + "interrupted.");
		p.add(tc);
		OKDialog dia = new OKDialog(tsup.getMainFrame(), "Select the unit to start from", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return -1;
		String txt = cbg.getSelectedCheckbox().getLabel();
		int idx = txt.indexOf(')');
		txt = txt.substring(0, idx);
		try {
			return Integer.valueOf(txt).intValue() - 1;
		} catch (NumberFormatException nfe) {
		}
		return 0;
	}

	protected void runUnit(int unitN) {
		if (unitN < 0)
			return;
		TutorialUnit unit = content.getUnit(unitN);
		int idx = content.getIndexInContents(unit);
		if (idx > 0) { //fill all questionnaires preceding to this unit
			int idx0 = idx - 1;
			while (idx0 >= 0 && !(content.getContentItem(idx0) instanceof TutorialUnit)) {
				--idx0;
			}
			++idx0;
			for (int i = idx0; i < idx; i++)
				if (content.getContentItem(i) instanceof Task) {
					Task task = (Task) content.getContentItem(i);
					fillForm((task.name == null) ? "Questionnaire" : task.name, task);
				}
		}
		currUnitN = unitN;
		String title = (unit.uN > 0) ? ("Unit " + unit.uN) : unit.name;
		if (win == null) {
			win = new Frame(title);
			win.setBackground(bkgWinColor);
			win.setLayout(new BorderLayout(5, 5));
		} else {
			win.dispose();
			win.removeAll();
			win.setTitle(title);
		}
		if (tsup != null) {
			if (unit.msp != null && unit.msp.tblId != null) {
				tsup.eraseDataFromMap(unit.msp.tblId);
			}
			tsup.closeAllTools();
			tsup.showWholeTerritory();
		}
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label(unit.name, Label.CENTER));
		if (unit.getDescription() != null) {
			TextCanvas tc = new TextCanvas();
			tc.addTextLine(unit.getDescription());
			p.add(tc);
		}
		if (unit.pathToExample != null) {
			Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 5));
			Button b = new Button("Show example");
			b.setActionCommand("example");
			b.addActionListener(this);
			pp.add(b);
			p.add(pp);
		}
		if (unit.advise != null && unit.advise.getInstructionCount() > 0) {
			if (unit.advise.getInstructionCount() > 1) {
				p.add(new Label("Recommendations:"));
			} else {
				p.add(new Label("Recommendation:"));
			}
			Panel pp = new Panel(new ColumnLayout());
			pp.setBackground(bkgInstructionColor);
			TextCanvas tc = null;
			for (int i = 0; i < unit.advise.getInstructionCount(); i++) {
				if (tc == null) {
					tc = new TextCanvas();
					pp.add(tc);
				}
				tc.addTextLine(unit.advise.getInstruction(i));
				String topicId = unit.advise.getHelpTopicId(i);
				if (topicId != null && Helper.canHelp(topicId)) {
					tc = null;
					Button b = new Button("?");
					b.setActionCommand("help_" + topicId);
					b.addActionListener(this);
					Panel p1 = new Panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
					p1.add(b);
					pp.add(p1);
				}
			}
			p.add(pp);
		}
		win.add(p, "Center");
		p = new Panel(new BorderLayout());
		Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		Button b = new Button("Start");
		b.setActionCommand("start");
		b.addActionListener(this);
		pp.add(b);
		p.add(pp, "Center");
		pp = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		b = new Button("Quit");
		b.setActionCommand("quit");
		b.addActionListener(this);
		pp.add(b);
		p.add(pp, "East");
		win.add(p, "South");
		showWindow();
		if (startTime == null) {
			startTime = Calendar.getInstance();
			saver.saveString("Test start " + dateAsString(startTime) + " at " + timeAsString(startTime));
		}
		unit.start = Calendar.getInstance();
		nCorrectUnit = 0;
		nWrongUnit = 0;
		nRefusedUnit = 0;
		answerTimeUnit = 0L;
		exampleStartTime = 0L;
	}

	protected void showWindow() {
		if (win == null)
			return;
		win.pack();
		Dimension frsz = win.getSize();
		if (frsz.width < 60 * Metrics.mm()) {
			frsz.width = 60 * Metrics.mm();
		}
		int sw = Metrics.scrW(), sh = Metrics.scrH();
		if (frsz.width > sw / 2) {
			frsz.width = sw / 2;
		}
		if (frsz.height > sh / 2) {
			frsz.height = sh / 2;
		}
		win.setBounds(sw - frsz.width - 50, sh - frsz.height - 50, frsz.width, frsz.height);
		win.show();
	}

	protected void startUnit() {
		if (currUnitN < 0)
			return;
		TutorialUnit unit = content.getUnit(currUnitN);
		if (exampleStartTime > 0) {
			long t = System.currentTimeMillis() - exampleStartTime;
			saver.saveString("Unit " + unit.uN + ": viewing the example during " + durationAsString(t));
		} else if (unit.pathToExample != null) {
			saver.saveString("Unit " + unit.uN + ": the example was not read");
		}
		//prepare the map and other tools
		if (tsup != null) {
			String tblId = null;
			if (unit.msp != null && unit.msp.tblId != null) {
				tblId = unit.msp.tblId;
				if (unit.msp.methodId == null) {
					tsup.eraseDataFromMap(unit.msp.tblId);
				} else {
					tsup.showDataOnMap(unit.msp.tblId, unit.msp.attr, unit.msp.methodId);
				}
			}
			for (int i = 0; i < unit.getToolCount(); i++) {
				ToolSpec tsp = unit.getToolSpecification(i);
				if (tsp.toolType != null && tsp.tblId != null) {
					tsup.applyTool(tsp.tblId, tsp.attr, tsp.toolType);
				}
				if (tblId == null) {
					tblId = tsp.tblId;
				}
			}
			if (tblId != null) {
				tsup.activateLayerWithData(tblId);
			}
		}
		win.dispose();
		win.removeAll();
		Panel pp = new Panel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		Button b = new Button("Quit");
		b.setActionCommand("quit");
		b.addActionListener(this);
		pp.add(b);
		win.add(pp, "South");

		putTask(0);
	}

	protected void putTask(int taskN) {
		currTaskN = taskN;
		currQN = -1;
		win.dispose();
		if (mainPanel == null) {
			mainPanel = new Panel(new BorderLayout(5, 5));
		} else {
			mainPanel.removeAll();
		}
		imageView = null;
		questionPanel = null;
		Task task = content.getUnit(currUnitN).getTask(currTaskN);
		if (task == null) {
			finishUnit();
			return;
		}
		if (!win.isAncestorOf(mainPanel)) {
			win.add(mainPanel, "Center");
		}
		if (task.tN > 0) {
			mainPanel.add(new Label("Task " + task.uN + "." + task.tN), "North");
		}
		taskTC = new TextCanvas();
		taskTC.setBackground(bkgTaskColor);
		taskTC.addTextLine(task.getTaskText());
		if (task.pathToPicture == null) {
			taskTC.setPreferredSize(100 * Metrics.mm(), 20 * Metrics.mm());
		} else {
			taskTC.setPreferredSize(70 * Metrics.mm(), 20 * Metrics.mm());
		}
		mainPanel.add(taskTC, "Center");
		if (task.pathToPicture != null) {
			Image img = getImage(content.pathToTutorial + task.pathToPicture);
			if (img != null) {
				imageView = new ImageCanvas(img);
				mainPanel.add(imageView, "East");
			}
		}
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		Button b = new Button((task.getQuestionCount() > 1) ? "Questions" : "Question");
		b.setActionCommand("question");
		b.addActionListener(this);
		p.add(b);
		mainPanel.add(p, "South");
		questionPanel = p;
		showWindow();
	}

	protected void putNextTask() {
		++currTaskN;
		if (currTaskN >= content.getUnit(currUnitN).getTaskCount()) {
			finishUnit();
		} else {
			putTask(currTaskN);
		}
	}

	protected void finishTask() {
		//get estimation of complexity etc.
		//...
		putNextTask();
	}

	protected void fillForm(String title, Task form) {
		//get estimation of complexity etc.
		if (form == null || form.getQuestionCount() < 1)
			return;
		saver.saveString(title);
		Panel p = new Panel(new ColumnLayout());
		Label l = new Label(title, Label.CENTER);
		p.setBackground(bkgWinColor);
		p.add(l);
		if (form.getTaskText() != null) {
			TextCanvas tc = new TextCanvas();
			tc.addTextLine(form.getTaskText());
			p.add(tc);
			tc.setPreferredSize(80 * Metrics.mm(), 20);
		}
		QuestionPanel qp = null;
		for (int i = 0; i < form.getQuestionCount(); i++) {
			if (qp != null) {
				p.remove(qp);
			}
			Question q = form.getQuestion(i);
			qp = new QuestionPanel(q, null, false, true);
			qp.setBackground(bkgQuestionColor);
			p.add(qp);
			OKDialog dia = new OKDialog(tsup.getMainFrame(), title, true);
			dia.addContent(p);
			dia.show();
			if (!dia.wasCancelled()) {
				Object answ = qp.getAnswer();
				if (answ != null) {
					saver.saveString((i + 1) + ") " + q.getQuestionText());
					saver.saveString("  " + Answer.getAnswerAsString(q, answ, true));
				}
			}
		}
	}

	protected void finishUnit() {
		if (currUnitN >= 0) {
			TutorialUnit unit = content.getUnit(currUnitN);
			unit.finish = Calendar.getInstance();
			if (unit.evalForm != null) {
				fillForm((unit.evalForm.name == null) ? unit.name : unit.evalForm.name, unit.evalForm);
			}
			//show unit statistics
			long dur = unit.finish.getTime().getTime() - unit.start.getTime().getTime();
			Panel p = null;
			if (showStatisticsToUser) {
				p = new Panel(new ColumnLayout());
				Label l = new Label(unit.name, Label.CENTER);
				l.setBackground(bkgWinColor);
				p.add(l);
			}
			String str = "Unit start " + timeAsString(unit.start) + ", finish " + timeAsString(unit.finish) + ", duration " + durationAsString(dur);
			saver.saveString(str);
			if (showStatisticsToUser) {
				p.add(new Label(str));
			}
			str = "Time required for answering questions " + durationAsString(answerTimeUnit);
			saver.saveString(str);
			if (showStatisticsToUser) {
				p.add(new Label(str));
			}
			int nq = nCorrectUnit + nWrongUnit + nRefusedUnit;
			str = nCorrectUnit + " (" + StringUtil.floatToStr(nCorrectUnit * 100.0f / nq, 2) + " %) correct answers";
			saver.saveString(str);
			if (showStatisticsToUser) {
				p.add(new Label(str));
			}
			str = nWrongUnit + " (" + StringUtil.floatToStr(nWrongUnit * 100.0f / nq, 2) + " %) wrong answers";
			saver.saveString(str);
			if (showStatisticsToUser) {
				p.add(new Label(str));
			}
			str = nRefusedUnit + " (" + StringUtil.floatToStr(nRefusedUnit * 100.0f / nq, 2) + " %) \"do not know\" answers";
			saver.saveString(str);
			if (showStatisticsToUser) {
				p.add(new Label(str));
			}
			if (showStatisticsToUser) {
				OKDialog dia = new OKDialog(tsup.getMainFrame(), "Unit statistics", false);
				dia.addContent(p);
				dia.show();
			}
		}
		lastUnitN = currUnitN;
		runNextUnit();
	}

	protected String timeAsString(Calendar c) {
		if (c == null)
			return null;
		return c.get(Calendar.HOUR_OF_DAY) + "h " + c.get(Calendar.MINUTE) + "min " + c.get(Calendar.SECOND) + "sec ";
	}

	protected String dateAsString(Calendar c) {
		if (c == null)
			return null;
		return c.get(Calendar.DAY_OF_MONTH) + "/" + (1 + c.get(Calendar.MONTH)) + "/" + c.get(Calendar.YEAR);
	}

	protected String durationAsString(long millis) {
		long sec = Math.round(millis / 1000.0f);
		if (sec < 60)
			return sec + " sec.";
		long min = sec / 60;
		sec = sec % 60;
		if (min < 60)
			return min + " min. " + sec + " sec.";
		long hours = min / 60;
		min = min % 60;
		if (hours == 1)
			return "1 hour " + min + " min. " + sec + " sec.";
		return hours + " hours " + min + " min. " + sec + " sec.";
	}

	protected void runNextUnit() {
		if (currUnitN + 1 >= content.getUnitCount()) {
			finish();
		} else {
			runUnit(currUnitN + 1);
		}
	}

	protected void writeFinalInfo(boolean showToUser) {
		if (!showToUser && saver == null)
			return;
		Panel p = null;
		if (showToUser) {
			p = new Panel(new ColumnLayout());
			Label l = new Label("Tutorial/test summary:", Label.CENTER);
			l.setBackground(bkgWinColor);
			p.add(l);
		}
		Calendar currTime = Calendar.getInstance();
		String str = "Test start " + dateAsString(startTime) + " at " + timeAsString(startTime) + ", finish " + timeAsString(currTime);
		if (saver != null) {
			saver.saveString(str);
		}
		if (showToUser) {
			p.add(new Label(str));
		}
		str = "Test duration " + durationAsString(currTime.getTime().getTime() - startTime.getTime().getTime());
		if (saver != null) {
			saver.saveString(str);
		}
		if (showToUser) {
			p.add(new Label(str));
		}
		str = "Time required for answering questions " + durationAsString(answerTimeTotal);
		if (saver != null) {
			saver.saveString(str);
		}
		if (showToUser) {
			p.add(new Label(str));
		}
		int nq = nCorrectTotal + nWrongTotal + nRefusedTotal;
		str = nCorrectTotal + " (" + StringUtil.floatToStr(nCorrectTotal * 100.0f / nq, 2) + " percent) correct answers";
		if (saver != null) {
			saver.saveString(str);
		}
		if (showToUser) {
			p.add(new Label(str));
		}
		str = nWrongTotal + " (" + StringUtil.floatToStr(nWrongTotal * 100.0f / nq, 2) + " percent) wrong answers";
		if (saver != null) {
			saver.saveString(str);
		}
		if (showToUser) {
			p.add(new Label(str));
		}
		str = nRefusedTotal + " (" + StringUtil.floatToStr(nRefusedTotal * 100.0f / nq, 2) + " %) \"do not know\" answers";
		if (saver != null) {
			saver.saveString(str);
		}
		if (showToUser) {
			p.add(new Label(str));
		}
		if (showToUser) {
			TextArea tar = null;
			if (saver != null) {
				p.add(new Line(false));
				p.add(new Label("Your comments and suggestions:"));
				tar = new TextArea(10, 60);
				p.add(tar);
				p.add(new Line(false));
			}
			Label l = new Label("Thank you for your efforts!", Label.CENTER);
			l.setBackground(bkgTaskColor);
			p.add(l);
			OKDialog dia = new OKDialog(tsup.getMainFrame(), "Tutorial summary", false);
			dia.addContent(p);
			dia.show();
			if (saver != null) {
				String txt = tar.getText().trim();
				if (txt != null && txt.length() > 0) {
					saver.saveString("Comment");
				}
				saver.saveString(txt);
			}
		}
	}

	public void finish() {
		running = false;
		writeFinalInfo(true);
		if (currUnitN >= 0 && currUnitN + 1 >= content.getUnitCount()) {
			int idx = content.getIndexInContents(content.getUnit(currUnitN));
			for (int i = idx + 1; i < content.getContentItemCount(); i++)
				if (content.getContentItem(i) instanceof Task) {
					Task task = (Task) content.getContentItem(i);
					fillForm((task.name == null) ? "Questionnaire" : task.name, task);
				}
		}
		if (saver != null) {
			saver.saveString("END");
			saver.finish();
		}
		win.dispose();
		win = null;
	}

	public void quit() {
		running = false;
		if (win == null)
			return;
		writeFinalInfo(false);
		if (saver != null) {
			Calendar currTime = Calendar.getInstance();
			saver.saveString("!!! Test quit at " + timeAsString(currTime));
			saver.finish();
		}
		win.dispose();
		win = null;
	}

	protected void askNextQuestion() {
		Task task = content.getUnit(currUnitN).getTask(currTaskN);
		win.dispose();
		taskTC.reset();
		if (questionPanel != null) {
			mainPanel.remove(questionPanel);
			questionPanel = null;
		}
		if (currQN >= 0 && task.getQuestion(currQN).pathToPicture != null && imageView != null) {
			mainPanel.remove(imageView);
			imageView = null;
			taskTC.setPreferredSize(100 * Metrics.mm(), 20 * Metrics.mm());
		}
		++currQN;
		if (currQN >= task.getQuestionCount()) {
			finishTask();
			return;
		}
		questionPanel = new QuestionPanel(task.getQuestion(currQN), this, true, false);
		questionPanel.setBackground(bkgQuestionColor);
		mainPanel.add(questionPanel, "South");
		if (task.getQuestion(currQN).pathToPicture != null) {
			if (imageView != null) {
				mainPanel.remove(imageView);
				imageView = null;
			}
			Image img = getImage(content.pathToTutorial + task.getQuestion(currQN).pathToPicture);
			if (img != null) {
				imageView = new ImageCanvas(img);
				mainPanel.add(imageView, "East");
				taskTC.setPreferredSize(70 * Metrics.mm(), 20 * Metrics.mm());
			}
		}
		showWindow();
		//((QuestionPanel)questionPanel).getReady();
		qTime = System.currentTimeMillis();
	}

	protected void saveAnswer(Answer ua) {
		if (ua == null)
			return;
		saver.saveString(ua.question.uN + "." + ua.question.tN + "." + ua.question.qN + ") " + ua.getAnswerAsString());
		if (!ua.isAnswerRefused() && !ua.isAnswerCorrect()) {
			saver.saveString("--- WRONG, must be " + ua.question.getAnswerAsString());
		}
		saver.saveString("   duration " + durationAsString(ua.getAnswerDuration()));
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd == null)
			return;
		if (cmd.equals("quit")) {
			quit();
		} else if (cmd.equals("finish")) {
			finish();
		} else if (cmd.equals("start")) {
			startUnit();
		} else if (cmd.equals("next")) {
			runNextUnit();
		} else if (cmd.startsWith("help_")) {
			Helper.help(cmd.substring(5));
		} else if (cmd.equals("example")) {
			TutorialUnit unit = content.getUnit(currUnitN);
			Helper.showPage(content.pathToTutorial + unit.pathToExample, unit.name);
			exampleStartTime = System.currentTimeMillis();
		} else if (cmd.equals("question")) {
			askNextQuestion();
		} else if (cmd.equals("question_done") || cmd.equals("question_failed")) {
			Object answ = null;
			if (cmd.equals("question_done")) {
				QuestionPanel qp = (QuestionPanel) questionPanel;
				if (!qp.canClose())
					return;
				answ = qp.getAnswer();
				if (answ == null)
					return;
			}
			long currTime = System.currentTimeMillis();
			Answer ua = new Answer();
			Task task = content.getUnit(currUnitN).getTask(currTaskN);
			ua.setQuestion(task.getQuestion(currQN));
			ua.setUserAnswer((answ == null) ? "null" : answ);
			ua.setQuestionTime(qTime);
			ua.setAnswerTime(currTime);
			saveAnswer(ua);
			answerTimeTotal += ua.getAnswerDuration();
			answerTimeUnit += ua.getAnswerDuration();
			if (answ == null) {
				++nRefusedTotal;
				++nRefusedUnit;
			} else if (ua.isAnswerCorrect()) {
				++nCorrectTotal;
				++nCorrectUnit;
			} else {
				++nWrongTotal;
				++nWrongUnit;
			}
			askNextQuestion();
		}
	}

	protected Image getImage(String path) {
		if (path == null)
			return null;
		int idx = path.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = path.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		Image img = null;
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(path);
				img = Toolkit.getDefaultToolkit().getImage(url);
			} else {
				img = Toolkit.getDefaultToolkit().getImage(path);
			}
		} catch (IOException ioe) {
			System.out.println("Error accessing " + path + ": " + ioe);
			return null;
		}
		if (img == null) {
			System.out.println("Cannot load an image from " + path);
			return null;
		}
		MediaTracker mediatracker = new MediaTracker(win);
		try {
			mediatracker.addImage(img, 1);
			mediatracker.waitForAll();
			if (mediatracker.getErrorsAny() != null) {
				System.out.println("Cannot load image " + path);
				return null;
			}
		} catch (InterruptedException ie) {
			System.out.println("Cannot load image " + path + " (interrupted)");
			return null;
		}
		return img;
	}

	public boolean isRunning() {
		return running;
	}
}
