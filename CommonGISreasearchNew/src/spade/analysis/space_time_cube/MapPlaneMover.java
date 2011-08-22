package spade.analysis.space_time_cube;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3d;

import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.RowLayout;
import spade.lib.basicwin.Slider;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.TimeReference;
import spade.time.manage.TemporalDataManager;
import spade.time.ui.TimeUI;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 30, 2008
 * Time: 3:28:16 PM
 * Moves a semi-transparent map in z-dimension to show the
 * position of a selected time moment within the view
 */
public class MapPlaneMover implements PropertyChangeListener, ActionListener, Destroyable {
	/**
	 * The object to move
	 */
	protected TransformGroup movable = null;
	/**
	 * Perform translation of the object in z-dimension
	 */
	protected Transform3D translate = null;
	/**
	 * Used for getting the minimum and maximum times available in the data
	 */
	protected TemporalDataManager timeMan = null;
	/**
	 * Notifies about creation and deleting of time filtering controls
	 * and provides access to the current FocusInterval
	 */
	protected TimeUI timeUI = null;
	/**
	 * Used for selection of time intervals
	 */
	protected FocusInterval focusInterval = null;
	/**
	 * The slider for selecting time moments
	 */
	protected Slider slider = null;
	/**
	 * The labels show the current time interval
	 * (selected in the focusInterval)
	 */
	protected Label minTimeLab = null, maxTimeLab = null;
	/**
	 * The text field shows the selected time moment
	 */
	protected TextField currTimeTF = null;

	protected Panel mainP = null;

	public MapPlaneMover(TransformGroup movable) {
		this.movable = movable;
		if (movable == null)
			return;
		movable.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	}

	/**
	 * Used for getting the minimum and maximum times available in the data
	 */
	public void setTemporalDataManager(TemporalDataManager timeMan) {
		this.timeMan = timeMan;
	}

	/**
	 * The TimeUI notifies about creation and deleting of time filtering controls
	 * and provides access to the current FocusInterval
	 */
	public void setTimeUI(TimeUI timeUI) {
		this.timeUI = timeUI;
		if (timeUI != null) {
			timeUI.addPropertyChangeListener(this);
			setFocusInterval(timeUI.getTimeFilterFocusInterval());
		}
	}

	/**
	 * A FocusInterval is used for selection of time intervals
	 */
	public void setFocusInterval(FocusInterval focusInterval) {
		if (this.focusInterval != null) {
			if (this.focusInterval.equals(focusInterval))
				return;
			this.focusInterval.removePropertyChangeListener(this);
			this.focusInterval = null;
		}
		this.focusInterval = focusInterval;
		if (focusInterval != null) {
			focusInterval.addPropertyChangeListener(this);
		}
		accountForFocusInterval();
	}

	/**
	 * Creates and returns an interface for moving the plane
	 */
	public Component getInterface() {
		if (mainP != null)
			return mainP;
		TimeMoment t1 = null, t2 = null;
		if (focusInterval != null) {
			t1 = focusInterval.getCurrIntervalStart();
			t2 = focusInterval.getCurrIntervalEnd();
		} else if (timeMan != null) {
			TimeReference minMaxTimes = timeMan.getMinMaxTimes();
			if (minMaxTimes != null) {
				t1 = minMaxTimes.getValidFrom();
				t2 = minMaxTimes.getValidUntil();
			}
		}
		if (t1 == null || t2 == null)
			return null;
		mainP = new Panel(new BorderLayout());
		minTimeLab = new Label(t1.toString());
		maxTimeLab = new Label(t2.toString());
		mainP.add(minTimeLab, BorderLayout.WEST);
		Panel p = new Panel(new RowLayout(5, 0));
		p.add(maxTimeLab);
		currTimeTF = new TextField(t1.toString());
		currTimeTF.addActionListener(this);
		p.add(currTimeTF);
		mainP.add(p, BorderLayout.EAST);
		slider = new Slider(this, 0.0, 1.0, 0.0);
		slider.setAbsMinMaxTime(t1.getCopy(), t2.getCopy());
		slider.setNAD(true);
		slider.setBackground(Color.lightGray);
		slider.setShowMinMaxLabels(false);
		slider.setTextField(currTimeTF);
		mainP.add(slider, BorderLayout.CENTER);
		return mainP;
	}

	/**
	 * Reacts to changes of the current focus interval
	 */
	protected void accountForFocusInterval() {
		if (slider == null)
			return;
		TimeMoment t1 = null, t2 = null;
		if (focusInterval == null) {
			if (timeMan != null) {
				TimeReference minMaxTimes = timeMan.getMinMaxTimes();
				if (minMaxTimes != null) {
					t1 = minMaxTimes.getValidFrom();
					t2 = minMaxTimes.getValidUntil();
				}
			}
		} else {
			t1 = focusInterval.getCurrIntervalStart();
			t2 = focusInterval.getCurrIntervalEnd();
		}
		if (t1 == null || t2 == null)
			return;
		minTimeLab.setText(t1.toString());
		maxTimeLab.setText(t2.toString());
		double slPos = slider.getValue();
		if (slPos <= 0) {
			slider.setAbsMinMaxTime(t1.getCopy(), t2.getCopy());
			slider.setValue(0.0);
		} else if (slPos >= 1) {
			slider.setAbsMinMaxTime(t1.getCopy(), t2.getCopy());
			slider.setValue(1.0);
		} else {
			TimeMoment t = slider.getCurrTime();
			t.setPrecision(t1.getPrecision());
			slider.setAbsMinMaxTime(t1.getCopy(), t2.getCopy());
			if (t.compareTo(t1) <= 0) {
				slider.setValue(0.0);
			} else if (t.compareTo(t2) >= 0) {
				slider.setValue(1.0);
			} else {
				double fValue = 1.0 * t.subtract(t1) / slider.getTimeIntervalLength();
				slider.setValue(fValue);
			}
		}
		accountForSliderPosition();
	}

	/**
	 * Moves the plane according to the current slider position
	 */
	protected void accountForSliderPosition() {
		if (slider == null || movable == null)
			return;
		if (translate == null) {
			translate = new Transform3D();
		}
		double val = slider.getValue(), min = slider.getAbsMin(), max = slider.getAbsMax();
		double shift = (val - min) / (max - min);
		translate.setTranslation(new Vector3d(0, 0, shift));
		movable.setTransform(translate);
	}

	/**
	 * Reacts to changes of the selected time interval
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(timeUI)) {
			if (pce.getPropertyName().equals("open_time_filter_controls")) {
				setFocusInterval(timeUI.getTimeFilterFocusInterval());
			} else if (pce.getPropertyName().equals("close_time_filter_controls")) {
				setFocusInterval(null);
			}
		} else if (pce.getSource().equals(focusInterval)) {
			if (pce.getPropertyName().equals("current_interval") || pce.getPropertyName().equals("granularity")) {
				accountForFocusInterval();
			}
		}
	}

	/**
	 * Reacts to changes of the slider position
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(currTimeTF)) {
			TimeMoment minTime = slider.getMinTime();
			if (minTime == null)
				return;
			String sValue = currTimeTF.getText();
			if (sValue == null || sValue.trim().length() < 1) {
				slider.showValueInField();
				return;
			}
			double fValue = Double.NaN;
			TimeMoment t = minTime.getCopy();
			if (t.setMoment(sValue)) {
				fValue = 1.0 * t.subtract(minTime) / slider.getTimeIntervalLength();
			} else {
				slider.showValueInField();
				return;
			}
			if (fValue < slider.getAbsMin()) {
				fValue = slider.getAbsMin();
			} else if (fValue > slider.getAbsMax()) {
				fValue = slider.getAbsMax();
			}
			if (fValue != slider.getValue()) {
				slider.setValue(fValue);
				accountForSliderPosition();
			} else {
				slider.showValueInField();
			}
		} else if (e.getSource().equals(slider)) {
			accountForSliderPosition();
		}
	}

	protected boolean destroyed = false;

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void destroy() {
		if (destroyed)
			return;
		System.out.println("TimeFocuser is destroyed");
		if (focusInterval != null) {
			focusInterval.removePropertyChangeListener(this);
		}
		if (timeUI != null) {
			timeUI.removePropertyChangeListener(this);
		}
		destroyed = true;
	}
}
