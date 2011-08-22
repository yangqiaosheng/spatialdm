package spade.time.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.TImgButton;
import spade.lib.lang.Language;
import spade.time.Animator;
import spade.time.FocusInterval;
import spade.time.TimeMoment;

/**
* Panel with animation controls
*/
public class AnimationPanel extends Panel implements ActionListener, PropertyChangeListener, MouseListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.time.ui.Res");
	/**
	* The focus time interval manipulated through animation
	*/
	protected FocusInterval focusInt = null;
	/**
	* The non-interface component that actually performs animation
	*/
	protected Animator animator = null;
	protected boolean destroyed = false;

	protected TextField stepTF = null, delayTF = null;
	protected boolean loop = false;
	protected TImgButton[] b = null; // 0-2 = step +-,rewind; 3-5 - amination; 6,7 - delay
	protected String buttonURLBase = "/icons/";
	protected String[] buttonImgURL = { "StepForward.gif", "StepBack.gif", "Rewind.gif", "Play.gif", "FastForward.gif", "Stop.gif", "Up.gif", "Down.gif" };
	protected String[] buttonCmd = { "StepForward", "StepBack", "Rewind", "Play", "FastForward", "Pause", "Up", "Down" };
	protected String[] commentKeys = { "forth", "back", "rewind", "animate", "loop", "stop", "incdelay", "decdelay" };
	/**
	 * This label contains the name of the current time unit, if exists.
	 * For abstract time moments, no units exist.
	 */
	protected Label timeUnitLabel = null;

	public AnimationPanel(FocusInterval fint) {
		this(fint, 1);
	}

	public AnimationPanel(FocusInterval fint, int step) {
		if (fint == null)
			return;
		focusInt = fint;
		animator = new Animator();
		animator.setFocusInterval(focusInt);
		animator.setStep(step);
		focusInt.addPropertyChangeListener(this);

		makeTextFields();
		makeButtons();

		setLayout(new BorderLayout());
		//animation buttons
		Panel p = new Panel(new GridLayout(1, 3, 3, 0));
		p.setBackground(Color.pink);
		for (int i = 3; i < 6; i++) {
			p.add(b[i]);
		}
		Panel pp = new Panel(new BorderLayout());
		pp.add(p, "West");
		Panel pan = new Panel(new ColumnLayout());
		pan.add(pp);
		//animation delay controls
		p = new Panel(new BorderLayout(2, 0));
		p.setBackground(Color.pink);
		p.add("West", new Label(res.getString("delay"), Label.RIGHT));
		p.add("Center", delayTF);
		pp = new Panel(new GridLayout(1, 2, 3, 0));
		pp.add(b[6]); // increase button
		pp.add(b[7]); // decrease button
		p.add("East", pp);
		pan.add(p);
		add(pan, "West");
		//stepwise movement buttons
		p = new Panel(new GridLayout(1, 3, 3, 0));
		for (int i = 0; i < 3; i++) {
			p.add(b[i]);
		}
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Centered);
		pan = new Panel(cl);
		pan.add(p);
		add(pan, "Center");
		//animation step controls
		String str = focusInt.getDataIntervalStart().getUnits();
		if (str != null) {
			timeUnitLabel = new Label(str);
			pp.add(timeUnitLabel);
			if (fint.getDataIntervalStart().getMinPrecision() != fint.getDataIntervalStart().getMaxPrecision()) {
				timeUnitLabel.addMouseListener(this);
				//by clicking on this label, the user will be able to change the
				//precision of specifying the interval length
			}
		}
		p = new Panel(new BorderLayout());
		p.add("West", new Label(res.getString("step"), Label.RIGHT));
		p.add("Center", stepTF);
		if (timeUnitLabel != null) {
			p.add("East", timeUnitLabel);
		}
		pan = new Panel(new ColumnLayout());
		pan.add(p);
		add(pan, "East");
		b[5].setEnabled(false);
	}

	protected void makeButtons() {
		b = new TImgButton[buttonImgURL.length];
		for (int i = 0; i < buttonImgURL.length; i++) {
			b[i] = new TImgButton(buttonURLBase + buttonImgURL[i]);
			b[i].setActionCommand(buttonCmd[i]);
			b[i].addActionListener(this);
			new PopupManager(b[i], res.getString(commentKeys[i]), true);
		}
	}

	protected void makeTextFields() {
		stepTF = new TextField(String.valueOf(animator.getStep()), 2);
		delayTF = new TextField(String.valueOf(animator.getDelay()), 4);
		stepTF.addActionListener(this);
		delayTF.addActionListener(this);
	}

	public void updateUI() {
		stepTF.setText(String.valueOf(animator.getStep()));
		delayTF.setText(String.valueOf(animator.getDelay()));
		if (timeUnitLabel != null) {
			timeUnitLabel.setText(focusInt.getDataIntervalStart().getUnits());
		}
	}

	public Animator getAnimator() {
		return animator;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src instanceof TImgButton) {
			if (animator == null || !animator.hasValidInterval())
				return;
			String cmd = e.getActionCommand();
			int i;
			for (i = 0; i < b.length; i++)
				if (cmd.equals(buttonCmd[i])) {
					break;
				}

			switch (i) {
			case 0: {
				animator.stepForth();
				break;
			}
			case 1: {
				animator.stepBack();
				break;
			}
			case 2: {
				animator.reset();
				break;
			}
			case 3: {
				animator.run();
				break;
			}
			case 4: {
				loop = true;
				animator.run();
				break;
			}
			case 5: {
				loop = false;
				animator.stop();
				break;
			}
			case 6: {
				animator.setDelay(animator.getDelay() + 100);
				b[7].setEnabled(true);
				delayTF.setText(String.valueOf(animator.getDelay()));
				break;
			}
			case 7: {
				long delay = animator.getDelay() - 100;
				if (delay < 0) {
					delay = 0;
				}
				animator.setDelay(delay);
				if (delay == 0) {
					b[7].setEnabled(false);
				}
				delayTF.setText(String.valueOf(delay));
				break;
			}
			}
		} else if (src instanceof TextField) {
			TextField tfCurrent = (TextField) src;
			String txt = tfCurrent.getText();
			if (tfCurrent == delayTF) {
				try {
					long delay = Long.valueOf(txt).longValue();
					if (delay == 0) {
						b[7].setEnabled(false);
					} else if (!b[7].isButtonEnabled()) {
						b[7].setEnabled(true);
					}
					animator.setDelay(delay);
				} catch (NumberFormatException nfe) {
					delayTF.setText(String.valueOf(animator.getDelay()));
				}
			} else if (tfCurrent == stepTF) {
				try {
					int step = Integer.valueOf(txt).intValue();
					if (step < 1) {
						step = 1;
						stepTF.setText("1");
					}
					animator.setStep(step);
				} catch (NumberFormatException nfe) {
					stepTF.setText(String.valueOf(animator.getStep()));
				}
			}
		} else if (e.getActionCommand().startsWith("granularity_")) {
			String cmd = e.getActionCommand();
			char symb = cmd.charAt(cmd.length() - 1);
			if (symb != focusInt.getPrecision()) {
				focusInt.setPrecision(symb);
				//startTF.setText(fint.getCurrIntervalStart().toString());
				//endTF.setText(fint.getCurrIntervalEnd().toString());
				timeUnitLabel.setText(focusInt.getDataIntervalStart().getUnits());
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(focusInt) && e.getPropertyName().equals("animation")) {
			if (e.getNewValue().equals("start")) {
				for (int i = 0; i < 5; i++) {
					b[i].setEnabled(false);
				}
				b[5].setEnabled(true);
				stepTF.setEnabled(false);
			} else if (e.getNewValue().equals("stop")) {
				if (loop) {
					animator.reset();
					animator.run();
					return;
				}
				for (int i = 0; i < 5; i++) {
					b[i].setEnabled(true);
				}
				b[5].setEnabled(false);
				stepTF.setEnabled(true);
			}
		} else if (e.getSource().equals(focusInt) && e.getPropertyName().equals("granularity")) {
			if (timeUnitLabel != null) {
				timeUnitLabel.setText(e.getNewValue().toString());
			}
		}
	}

	protected void checkTextFields() {
		String txt = delayTF.getText();
		try {
			long delay = Long.valueOf(txt).longValue();
			if (delay < 0) {
				delay = 0;
				delayTF.setText("0");
			}
			animator.setDelay(delay);
		} catch (NumberFormatException nfe) {
			delayTF.setText(String.valueOf(animator.getDelay()));
		}
		txt = stepTF.getText();
		try {
			int step = Integer.valueOf(txt).intValue();
			if (step < 1) {
				step = 1;
				stepTF.setText("1");
			}
			animator.setStep(step);
		} catch (NumberFormatException nfe) {
			stepTF.setText(String.valueOf(animator.getStep()));
		}
	}

	@Override
	public void destroy() {
		if (!destroyed) {
			animator.stop();
			focusInt.removePropertyChangeListener(this);
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* Writes its individual properties to the specified hashtable. If the
	* argument is null, constructs a new hashtable and returns it.
	*/
	public Hashtable getProperties(Hashtable prop) {
		if (prop == null) {
			prop = new Hashtable();
		}
		prop.put("step", String.valueOf(animator.getStep()));
		prop.put("delay", String.valueOf(animator.getDelay()));
		if (prop.isEmpty())
			return null;
		return prop;
	}

	/**
	* Restores the properties, which could be, for example, retrieved from a file.
	* Each property has its unique key and a value, both are strings.
	*/
	public void setProperties(Hashtable param) {
		if (param == null || param.isEmpty())
			return;
		String str = (String) param.get("step");
		if (str != null) {
			try {
				int s = Integer.valueOf(str).intValue();
				if (s > 0) {
					animator.setStep(s);
					if (stepTF != null) {
						stepTF.setText(String.valueOf(animator.getStep()));
					}
				}
			} catch (NumberFormatException e) {
			}
		}
		str = (String) param.get("delay");
		if (str != null) {
			try {
				int s = Integer.valueOf(str).intValue();
				if (s > 0) {
					animator.setDelay(s);
					if (delayTF != null) {
						delayTF.setText(String.valueOf(animator.getDelay()));
					}
				}
			} catch (NumberFormatException e) {
			}
		}
	}

	private PopupMenu timeGranularityPopupMenu = null;

	/**
	 * Reaction to mouse clicks on the label showing the time units in which the
	 * length of the interval is specified
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if (timeUnitLabel == null)
			return;
		if (timeGranularityPopupMenu == null) {
			TimeMoment t = focusInt.getDataIntervalStart();
			if (t == null)
				return;
			char elements[] = t.getAvailableTimeElements();
			if (elements == null || elements.length < 2)
				return;
			TimeMoment t2 = focusInt.getDataIntervalEnd();
			int len = elements.length;
			for (int i = elements.length - 1; i > 0; i--) {
				int n1 = t.getElementValue(elements[i]), n2 = t2.getElementValue(elements[i]);
				if (n2 > n1) {
					break;
				}
				--len;
			}
			if (len < 2)
				return;
			timeGranularityPopupMenu = new PopupMenu();
			for (int i = 0; i < len; i++) {
				MenuItem mi = new MenuItem(t.getTextForUnit(elements[i]));
				mi.setActionCommand("granularity_" + String.valueOf(elements[i]));
				timeGranularityPopupMenu.add(mi);
			}
			add(timeGranularityPopupMenu);
			timeGranularityPopupMenu.addActionListener(this);
		}
		timeGranularityPopupMenu.show(timeUnitLabel, e.getX(), e.getY());
	}

	/**
	 * Invoked when a mouse button has been pressed on a component.
	 */
	@Override
	public void mousePressed(MouseEvent e) {
	}

	/**
	 * Invoked when a mouse button has been released on a component.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * Invoked when the mouse enters a component.
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * Invoked when the mouse exits a component.
	 */
	@Override
	public void mouseExited(MouseEvent e) {
	}
}