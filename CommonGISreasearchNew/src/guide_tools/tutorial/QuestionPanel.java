package guide_tools.tutorial;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.DialogContent;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.NotificationLine;
import spade.lib.basicwin.TextCanvas;
import spade.lib.util.IntArray;

public class QuestionPanel extends Panel implements ActionListener, ItemListener, DialogContent {
	protected Question question = null;

	protected TextField answerTF = null;
	protected Checkbox answerCB[] = null;
	protected TextField ftf[] = null;
	protected NotificationLine status = null;
	protected ActionListener list = null;
	protected boolean mustGetAnswer = true;
	protected String err = null;

	public QuestionPanel(Question q, ActionListener list, boolean includeDoNotKnow, boolean allowNoAnswer) {
		question = q;
		this.list = list;
		this.mustGetAnswer = !allowNoAnswer;
		setLayout(new ColumnLayout());
		TextCanvas tc = new TextCanvas();
		tc.addTextLine(q.getQuestionText());
		tc.setPreferredSize(100 * Metrics.mm(), 20 * Metrics.mm());
		add(tc);
		Panel pp = null;
		switch (q.answerType) {
		case Question.STRING:
			answerTF = new TextField(25);
			answerTF.addActionListener(this);
			pp = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			pp.add(answerTF);
			add(pp);
			break;
		case Question.NUMBER:
			answerTF = new TextField(5);
			answerTF.addActionListener(this);
			pp = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			pp.add(answerTF);
			add(pp);
			break;
		case Question.SELECT_ONE:
		case Question.SELECT_MULTIPLE:
			answerCB = new Checkbox[q.getVariantCount()];
			CheckboxGroup cbg = null;
			if (q.answerType == Question.SELECT_ONE) {
				cbg = new CheckboxGroup();
			}
			for (int i = 0; i < q.getVariantCount(); i++) {
				answerCB[i] = new Checkbox(q.getAnswerVariant(i), cbg, false);
				answerCB[i].addItemListener(this);
				add(answerCB[i]);
			}
			break;
		case Question.ENTER_MULTIPLE:
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1.0f;
			c.weighty = 1.0f;
			Panel p = new Panel(gridbag);
			ftf = new TextField[q.getFieldCount()];
			for (int i = 0; i < q.getFieldCount(); i++) {
				Label l = new Label(q.getAnswerFieldName(i));
				c.gridwidth = 1;
				c.fill = GridBagConstraints.NONE;
				gridbag.setConstraints(l, c);
				p.add(l);
				ftf[i] = new TextField((q.getAnswerFieldType(i) == Question.NUMBER) ? 5 : 25);
				ftf[i].addActionListener(this);
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.fill = GridBagConstraints.NONE;
				gridbag.setConstraints(ftf[i], c);
				p.add(ftf[i]);
			}
			pp = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 0));
			pp.add(p);
			add(pp);
			break;
		case Question.SCORE:
			if (q.nGrades < 2) {
				q.nGrades = 5; //default
			}
			p = new Panel(new BorderLayout(10, 0));
			p.add(new Label(q.getLeftPole()), "West");
			p.add(new Label(q.getRightPole()), "East");
			pp = new Panel(new GridLayout(1, q.nGrades));
			cbg = new CheckboxGroup();
			answerCB = new Checkbox[q.nGrades];
			for (int j = 0; j < q.nGrades; j++) {
				answerCB[j] = new Checkbox("", cbg, false);
				pp.add(answerCB[j]);
			}
			p.add(pp, "Center");
			add(p);
			break;
		}
		if (list != null) {
			status = new NotificationLine("");
			add(status);
			pp = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 5));
			Button b = new Button("OK");
			b.setActionCommand("question_done");
			b.addActionListener(list);
			pp.add(b);
			if (includeDoNotKnow) {
				Panel p = new Panel(new BorderLayout());
				p.add(pp, "Center");
				pp = new Panel(new FlowLayout(FlowLayout.CENTER, 5, 5));
				b = new Button("Don\'t know");
				b.setActionCommand("question_failed");
				b.addActionListener(list);
				pp.add(b);
				p.add(pp, "East");
				add(p);
			} else {
				add(pp);
			}
		}
	}

	public void getReady() {
		if (answerTF != null) {
			answerTF.requestFocus();
		} else if (ftf != null) {
			ftf[0].requestFocus();
		}
	}

	/**
	* Gets the user's input and returns it as an instance of String, Float,
	* Integer (number of the option selected), IntArray (with numbers of multiple
	* options), or Vector (if the answer consists of several fields). If
	* the user did not enter an answer or did this incorrectly, displays an
	* error message and returns null.
	*/
	public Object getAnswer() {
		if (question == null)
			return null;
		switch (question.answerType) {
		case Question.STRING:
		case Question.NUMBER:
			String txt = answerTF.getText();
			if (txt != null) {
				txt = txt.trim();
				if (txt.length() > 0)
					if (question.answerType == Question.STRING)
						return txt;
					else {
						try {
							return Float.valueOf(txt);
						} catch (NumberFormatException nfe) {
							answerTF.setText("");
						}
					}
			}
			break;
		case Question.SELECT_ONE:
		case Question.SCORE:
			for (int i = 0; i < answerCB.length; i++)
				if (answerCB[i].getState())
					return new Integer(i);
			break;
		case Question.SELECT_MULTIPLE:
			IntArray selected = new IntArray(question.getVariantCount(), 5);
			for (int i = 0; i < answerCB.length; i++)
				if (answerCB[i].getState()) {
					selected.addElement(i);
				}
			if (selected.size() > 0)
				return selected;
			break;
		case Question.ENTER_MULTIPLE:
			Vector v = new Vector(ftf.length, 5);
			for (int i = 0; i < ftf.length; i++) {
				txt = ftf[i].getText();
				if (txt != null) {
					txt = txt.trim();
				}
				if (txt == null || txt.length() < 1) {
					v.addElement(null);
				} else if (question.getAnswerFieldType(i) == Question.STRING) {
					v.addElement(txt);
				} else {
					try {
						v.addElement(Float.valueOf(txt));
					} catch (NumberFormatException nfe) {
						v.addElement(null);
					}
				}
			}
			return v;
		}
		return null;
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (status != null) {
			status.showMessage(null, false);
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (status != null) {
			status.showMessage(null, false);
		}
		if (!canClose())
			return;
		if (list != null) {
			list.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "question_done"));
		}
	}

	@Override
	public boolean canClose() {
		if (question == null)
			return true;
		err = "No answer is entered!";
		boolean error = false;
		switch (question.answerType) {
		case Question.STRING:
		case Question.NUMBER:
			String txt = answerTF.getText();
			if (txt != null) {
				txt = txt.trim();
			}
			if (txt == null || txt.length() < 1) {
				error = mustGetAnswer;
			} else if (question.answerType == Question.STRING)
				return true;
			else {
				try {
					float val = Float.valueOf(txt).floatValue();
				} catch (NumberFormatException nfe) {
					err = "A number is expected!";
					answerTF.setText("");
					error = true;
				}
			}
			break;
		case Question.SELECT_ONE:
		case Question.SELECT_MULTIPLE:
		case Question.SCORE:
			for (Checkbox element : answerCB)
				if (element.getState())
					return true;
			error = mustGetAnswer;
			break;
		case Question.ENTER_MULTIPLE:
			for (int i = 0; i < ftf.length && !error; i++) {
				txt = ftf[i].getText();
				if (txt != null) {
					txt = txt.trim();
				}
				if (txt == null || txt.length() < 1)
					if (mustGetAnswer) {
						err = "No value entered in the field \"" + question.getAnswerFieldName(i) + "\"!";
						error = true;
					} else {
						;
					}
				else if (question.getAnswerFieldType(i) == Question.NUMBER) {
					try {
						float val = Float.valueOf(txt).floatValue();
					} catch (NumberFormatException nfe) {
						err = "A number is expected in the field \"" + question.getAnswerFieldName(i) + "\"!";
						error = true;
					}
				}
			}
			break;
		}
		if (!error)
			return true;
		if (status != null) {
			status.showMessage(err, true);
		}
		return false;
	}

	@Override
	public String getErrorMessage() {
		return err;
	}
}