package spade.time.ui;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.ConnectingAgent;
import spade.lib.basicwin.ConnectionCanvas;
import spade.lib.basicwin.Destroyable;
import spade.lib.lang.Language;
import spade.time.Date;
import spade.time.FocusInterval;
import spade.time.TimeCount;
import spade.time.TimeMoment;

public class TimeSliderPanel extends Panel implements PropertyChangeListener, ActionListener, ItemListener, MouseListener, ConnectingAgent, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.time.ui.Res");

	protected TimeSlider tsl = null;
	protected FocusInterval fint = null;
	protected TextField startTF = null, endTF = null, lengthTF = null;
	protected Checkbox startCB = null, endCB = null, lengthCB = null;
	protected Label l1 = null, l2 = null;
	protected ConnectionCanvas cc = null;
	protected boolean destroyed = false;
	/**
	 * This label contains the name of the current time unit, if exists.
	 * For abstract time moments, no units exist.
	 */
	protected Label timeUnitLabel = null;
	/**
	* The component to be notified when the panel is destroyed
	*/
	protected PropertyChangeListener owner = null;
	/**
	* Allows or prohibits selection of intervals. If intervals are prohibited,
	* the panel may only be used for selection of moments. By default,
	* selection of intervals is allowed.
	*/
	protected boolean maySelectIntervals = true;
	/**
	 * "Slave" mode means that the panel does not allow the user to change the
	 * current time granularity.
	 */
	protected boolean slaveMode = false;

	/**
	* "Owner" is the component to be notified when the panel is destroyed.
	* The argument "maySelectIntervals" allows or prohibits selection of intervals.
	* If intervals are prohibited, the panel may only be used for selection of
	* moments.
	*/
	public TimeSliderPanel(TimeSlider tsl, PropertyChangeListener owner, boolean maySelectIntervals) {
		this.tsl = tsl;
		this.owner = owner;
		this.maySelectIntervals = maySelectIntervals;
		fint = tsl.getFocusInterval();
		setLayout(new ColumnLayout());
		Panel p = new Panel(new BorderLayout());
		p.add(l1 = new Label(fint.getDataIntervalStart().toString(), Label.RIGHT), "West");
		p.add(l2 = new Label(fint.getDataIntervalEnd().toString(), Label.LEFT), "East");
		p.add(tsl, "Center");
		add(p);
		cc = new ConnectionCanvas();
		cc.setConnectingAgent(this);
		add(cc);
		p = new Panel();
		if (fint.getCurrIntervalStart() == null) {
			fint.setCurrIntervalStart(fint.getDataIntervalStart());
		}
		if (fint.getCurrIntervalEnd() == null) {
			fint.setCurrIntervalEnd(fint.getDataIntervalEnd());
		}
		if (maySelectIntervals) {
			p.setLayout(new BorderLayout());
			startTF = new TextField(fint.getCurrIntervalStart().toString());
			startTF.addActionListener(this);
			lengthTF = new TextField(String.valueOf(fint.getCurrIntervalLength()));
			lengthTF.addActionListener(this);
			p.add(startTF, "West");
			Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			pp.add(lengthTF);
			String str = fint.getDataIntervalStart().getUnits();
			if (str != null) {
				timeUnitLabel = new Label(str);
				pp.add(timeUnitLabel);
				if (fint.getDataIntervalStart().getMinPrecision() != fint.getDataIntervalStart().getMaxPrecision()) {
					timeUnitLabel.addMouseListener(this);
					//by clicking on this label, the user will be able to change the
					//precision of specifying the interval length
				}
			}
			p.add(pp, "Center");
			endTF = new TextField(fint.getCurrIntervalEnd().toString());
			p.add(endTF, "East");
		} else {
			endTF = new TextField(fint.getCurrIntervalStart().toString());
			ColumnLayout cl = new ColumnLayout();
			cl.setAlignment(ColumnLayout.Hor_Centered);
			p.setLayout(cl);
			p.add(endTF);
		}
		endTF.addActionListener(this);
		add(p);
		if (maySelectIntervals) {
			p = new Panel(new BorderLayout());
			CheckboxGroup cbg = new CheckboxGroup();
			startCB = new Checkbox(res.getString("fix"), fint.getWhatIsFixed() == FocusInterval.START, cbg);
			startCB.addItemListener(this);
			p.add(startCB, "West");
			lengthCB = new Checkbox(res.getString("fix"), fint.getWhatIsFixed() == FocusInterval.LENGTH, cbg);
			lengthCB.addItemListener(this);
			Panel pp = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			pp.add(lengthCB);
			p.add(pp, "Center");
			endCB = new Checkbox(res.getString("fix"), fint.getWhatIsFixed() == FocusInterval.END, cbg);
			endCB.addItemListener(this);
			p.add(endCB, "East");
			add(p);
		} else {
			fint.setWhatIsFixed(FocusInterval.LENGTH);
			TimeMoment t = fint.getDataIntervalStart().getCopy();
			fint.setCurrIntervalEnd(t);
		}
		fint.addPropertyChangeListener(this);
	}

	/**
	 * "Slave" mode means that the panel does not allow the user to change the
	 * current time granularity.
	 */
	public void setSlaveMode(boolean value) {
		slaveMode = value;
		if (startTF != null) {
			startTF.setEnabled(!slaveMode);
		}
		if (endTF != null) {
			endTF.setEnabled(!slaveMode);
		}
		if (lengthTF != null) {
			lengthTF.setEnabled(!slaveMode);
		}
		if (startCB != null) {
			startCB.setEnabled(!slaveMode);
		}
		if (endCB != null) {
			endCB.setEnabled(!slaveMode);
		}
		if (lengthCB != null) {
			lengthCB.setEnabled(!slaveMode);
		}
		if (slaveMode && lengthCB != null && !lengthCB.getState()) {
			fint.setWhatIsFixed(FocusInterval.LENGTH);
			lengthCB.setState(true);
		}
	}

	public FocusInterval getFocusInterval() {
		return fint;
	}

	public TimeSlider getTimeSlider() {
		return tsl;
	}

	private void setValuesInTextFields() {
		if (startTF != null)
			if (fint.getCurrIntervalStart() != null) {
				startTF.setText(fint.getCurrIntervalStart().toString());
			} else {
				startTF.setText(fint.getDataIntervalStart().toString());
			}
		if (endTF != null)
			if (fint.getCurrIntervalEnd() != null) {
				endTF.setText(fint.getCurrIntervalEnd().toString());
			} else {
				endTF.setText(fint.getDataIntervalEnd().toString());
			}
		if (lengthTF != null) {
			lengthTF.setText(String.valueOf(fint.getCurrIntervalLength()));
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(fint))
			if (e.getPropertyName().equals("current_interval")) {
				setValuesInTextFields();
				cc.redraw();
			} else if (e.getPropertyName().equals("granularity")) {
				setValuesInTextFields();
				if (timeUnitLabel != null) {
					timeUnitLabel.setText(fint.getDataIntervalStart().getUnits());
				}
				l1.setText(fint.getDataIntervalStart().toString());
				l2.setText(fint.getDataIntervalEnd().toString());
				CManager.validateAll(timeUnitLabel);
				Window win = CManager.getWindow(this);
				if (win != null) {
					win.pack();
				}
			} else if (e.getPropertyName().equals("animation"))
				if (e.getNewValue().equals("start")) {
					if (startTF != null) {
						startTF.setEnabled(false);
					}
					endTF.setEnabled(false);
					if (lengthTF != null) {
						lengthTF.setEnabled(false);
					}
					if (startCB != null) {
						startCB.setEnabled(false);
					}
					if (endCB != null) {
						endCB.setEnabled(false);
					}
					if (lengthCB != null) {
						lengthCB.setEnabled(false);
					}
				} else if (e.getNewValue().equals("stop")) {
					if (startTF != null) {
						startTF.setEnabled(!slaveMode);
					}
					endTF.setEnabled(!slaveMode);
					if (lengthTF != null) {
						lengthTF.setEnabled(!slaveMode);
					}
					if (startCB != null) {
						startCB.setEnabled(!slaveMode);
					}
					if (endCB != null) {
						endCB.setEnabled(!slaveMode);
					}
					if (lengthCB != null) {
						lengthCB.setEnabled(!slaveMode);
					}
				}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(startTF) || e.getSource().equals(endTF)) {
			TextField tf = (TextField) e.getSource();
			String str = tf.getText();
			if (str == null || str.trim().length() < 1) {
				if (tf.equals(startTF)) {
					startTF.setText(fint.getCurrIntervalStart().toString());
				} else {
					endTF.setText(fint.getCurrIntervalEnd().toString());
				}
				return;
			}
			TimeMoment t = null;
			if (fint.getDataIntervalStart() instanceof Date) {
				Date d = new Date(), startD = (Date) fint.getDataIntervalStart();
				d.setDateScheme(startD.scheme);
				if (d.setMoment(str)) {
					t = d;
					for (int i = 0; i < Date.time_symbols.length; i++)
						if (!d.hasElement(Date.time_symbols[i]) && startD.hasElement(Date.time_symbols[i])) {
							d.setElementValue(Date.time_symbols[i], startD.getElementValue(Date.time_symbols[i]));
						}
				}
			} else if (fint.getDataIntervalStart() instanceof TimeCount) {
				TimeCount tc = new TimeCount();
				if (tc.setMoment(str)) {
					t = tc;
				}
			}
			if (t == null) {
				if (tf.equals(startTF)) {
					startTF.setText(fint.getCurrIntervalStart().toString());
				} else {
					endTF.setText(fint.getCurrIntervalEnd().toString());
				}
				return;
			}
			t.setPrecision(fint.getDataIntervalStart().getPrecision());
			if (t.compareTo(fint.getDataIntervalStart()) < 0) {
				t = fint.getDataIntervalStart().getCopy();
			}
			if (t.compareTo(fint.getDataIntervalEnd()) > 0) {
				t = fint.getDataIntervalEnd().getCopy();
			}
			if (tf.equals(startTF))
				if (!fint.setCurrIntervalStart(t)) {
					startTF.setText(fint.getCurrIntervalStart().toString());
				} else {
					;
				}
			else if (!fint.setCurrIntervalEnd(t)) {
				endTF.setText(fint.getCurrIntervalEnd().toString());
			}
		} else if (e.getSource().equals(lengthTF)) {
			String str = lengthTF.getText();
			long l = 0L;
			try {
				l = Long.valueOf(str).longValue();
			} catch (NumberFormatException nfe) {
			}
			if (l < 1) {
				lengthTF.setText(String.valueOf(fint.getCurrIntervalLength()));
				return;
			}
			if (!fint.setCurrIntervalLength(l)) {
				lengthTF.setText(String.valueOf(fint.getCurrIntervalLength()));
			}
		} else if (e.getActionCommand().startsWith("granularity_")) {
			String cmd = e.getActionCommand();
			char symb = cmd.charAt(cmd.length() - 1);
			if (symb != fint.getPrecision()) {
				fint.setPrecision(symb);
				//startTF.setText(fint.getCurrIntervalStart().toString());
				//endTF.setText(fint.getCurrIntervalEnd().toString());
				lengthTF.setText(String.valueOf(fint.getCurrIntervalLength()));
				timeUnitLabel.setText(fint.getDataIntervalStart().getUnits());
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof Checkbox) {
			if (startCB != null && startCB.getState()) {
				fint.setWhatIsFixed(FocusInterval.START);
			} else if (endCB != null && endCB.getState()) {
				fint.setWhatIsFixed(FocusInterval.END);
			} else {
				fint.setWhatIsFixed(FocusInterval.LENGTH);
			}
		}
	}

//-------------- methods for controlling the ConnectionCanvas ------------------
	/**
	* Returns the number of connections to be shown by connecting lines in the
	* ConnectionCanvas
	*/
	@Override
	public int getConnectionCount() {
		if (maySelectIntervals)
			return 3;
		return 1;
	}

	/**
	* Returns the absolute x-coordinate of the beginning of the connection
	* with the given index. Everywhere absolute screen coordinates are used!
	*/
	@Override
	public int getConnectionStartX(int idx) {
		Point p = tsl.getLocationOnScreen();
		switch (idx) {
		case 0:
			if (maySelectIntervals)
				return p.x + tsl.getStartPosScr();
			return p.x + tsl.getEndPosScr();
		case 1:
			return p.x + (tsl.getStartPosScr() + tsl.getEndPosScr()) / 2;
		case 2:
			return p.x + tsl.getEndPosScr();
		}
		return p.x;
	}

	/**
	* Returns the absolute y-coordinate of the beginning of the connection
	* with the given index. Everywhere absolute screen coordinates are used!
	*/
	@Override
	public int getConnectionStartY(int idx) {
		return tsl.getLocationOnScreen().y + tsl.getSize().height;
	}

	/**
	* Returns the absolute x-coordinate of the end of the connection
	* with the given index. Everywhere absolute screen coordinates are used!
	*/
	@Override
	public int getConnectionEndX(int idx) {
		TextField tf = (maySelectIntervals) ? startTF : endTF;
		if (maySelectIntervals)
			if (idx == 1) {
				tf = lengthTF;
			} else if (idx == 2) {
				tf = endTF;
			}
		return tf.getLocationOnScreen().x + tf.getSize().width / 2;
	}

	/**
	* Returns the absolute y-coordinate of the end of the connection
	* with the given index. Everywhere absolute screen coordinates are used!
	*/
	@Override
	public int getConnectionEndY(int idx) {
		return endTF.getLocationOnScreen().y;
	}

	/**
	* Returns the preferred size of the ConnectionCanvas
	*/
	@Override
	public Dimension getConnectionCanvasPrefSize() {
		int w = 0;
		Dimension d = l1.getPreferredSize();
		if (d != null) {
			w += d.width;
		}
		d = l2.getPreferredSize();
		if (d != null) {
			w += d.width;
		}
		d = tsl.getPreferredSize();
		if (d != null) {
			w += d.width;
		}
		if (w < 50) {
			w = 50;
		}
		return new Dimension(w, 15);
	}

	/**
	* After the tool is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	*/
	public void setProperties(Hashtable properties) {
		if (properties != null && tsl != null) {
			tsl.setProperties(properties);
		}
	}

	/**
	* Returns its individual properties.
	*/
	public Hashtable getProperties() {
		return tsl.getProperties();
	}

	/**
	* Destroys the time controls when they are no more used, in particular, stops
	* listening to events. Destroys the subcomponents.
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		fint.showWholeInterval();
		fint.removePropertyChangeListener(this);
		tsl.destroy();
		destroyed = true;
		PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
		if (owner != null) {
			owner.propertyChange(pce);
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	private PopupMenu timeGranularityPopupMenu = null;

	/**
	 * Reaction to mouse clicks on the label showing the time units in which the
	 * length of the interval is specified
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if (timeUnitLabel == null || slaveMode)
			return;
		if (timeGranularityPopupMenu == null) {
			TimeMoment t = fint.getDataIntervalStart();
			if (t == null)
				return;
			char elements[] = t.getAvailableTimeElements();
			if (elements == null || elements.length < 2)
				return;
			TimeMoment t2 = fint.getDataIntervalEnd();
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