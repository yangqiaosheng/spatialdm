package spade.time.ui;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.time.FocusInterval;
import spade.time.TimeMoment;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 12, 2010
 * Time: 5:32:24 PM
 * Supports selection of time intervals in the time filter by clicking in time-aware displays
 */
public class TimeIntervalSelectorPanel extends Panel implements PropertyChangeListener, ActionListener, Destroyable {
	/**
	 * The supervisor can propagate events about selection of time moments
	 */
	protected Supervisor supervisor = null;
	/**
	 * Enables or disables the selector
	 */
	protected Checkbox cbEnable = null;
	/**
	 * Specify the left and right ends of the interval relative to the
	 * current time
	 */
	protected TextField tfLeft = null, tfRight = null;

	protected FocusInterval focusInterval = null;

	public TimeIntervalSelectorPanel() {
		setLayout(new ColumnLayout());
		cbEnable = new Checkbox("Select time intervals by right-clicking in time-aware data views", true);
		add(cbEnable);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(new Label("Interval start: -"));
		tfLeft = new TextField("10", 5);
		p.add(tfLeft);
		p.add(new Label("from the selected moment"));
		add(p);
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 2));
		p.add(new Label("Interval end: +"));
		tfRight = new TextField("10", 5);
		p.add(tfRight);
		p.add(new Label("from the selected moment"));
		add(p);
		p = new Panel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
		Button b = new Button("Clear time filter");
		b.setActionCommand("clear");
		b.addActionListener(this);
		p.add(b);
		add(p);
	}

	public void setFocusInterval(FocusInterval focusInterval) {
		this.focusInterval = focusInterval;
	}

	/**
	 * The supervisor can propagate events about selection of time moments
	 */
	public void setSupervisor(Supervisor supervisor) {
		this.supervisor = supervisor;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("clear")) {
			if (focusInterval == null)
				return;
			focusInterval.setCurrInterval(focusInterval.getDataIntervalStart(), focusInterval.getDataIntervalEnd());
		}
	}

	/**
	 * Reacts to supervisor's notifications about selection of time moments
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (focusInterval != null && cbEnable.getState() && e.getPropertyName().equals("time_moment_selection") && (e.getNewValue() instanceof TimeMoment)) {
			TimeMoment t = (TimeMoment) e.getNewValue();
			if (t == null)
				return;
			char unit = focusInterval.getPrecision();
			TimeMoment t1 = t.getCopy(), t2 = t.getCopy();
			if (unit != 0) {
				t1.setPrecision(unit);
				t2.setPrecision(unit);
			}
			int n1 = 0, n2 = 0;
			try {
				n1 = Integer.parseInt(tfLeft.getText());
			} catch (Exception ex) {
				tfLeft.setText("1");
				n1 = 1;
			}
			try {
				n2 = Integer.parseInt(tfRight.getText());
			} catch (Exception ex) {
				tfRight.setText("1");
				n2 = 1;
			}
			t1.add(-n1);
			t2.add(n2);
			focusInterval.setCurrInterval(t1, t2);
		}
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
		}
		destroyed = true;
	}
}
