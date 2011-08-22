package spade.analysis.space_time_cube;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.ModelClip;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

import spade.lib.basicwin.Destroyable;
import spade.time.FocusInterval;
import spade.time.TimeMoment;
import spade.time.ui.TimeUI;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 30, 2008
 * Time: 12:42:39 PM
 * Clips and scales the space-time cube in the z-dimension according
 * to the current selection of a time interval in a time filetr
 */
public class TimeFocuser implements PropertyChangeListener, Destroyable {
	/**
	 * The group of objects to manipulate
	 */
	protected TransformGroup stObjects = null;
	/**
	 * Performs the clipping
	 */
	protected ModelClip clipper = null;
	/**
	 * Perform scaling and translation of the objects in z-dimension
	 */
	protected Transform3D scale = null, translate = null, transform = null;
	/**
	 * Identity transform: restores the view to the non-scaled state
	 */
	protected Transform3D identity = null;
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
	 * Indicates whether focusing has been applied to the current view
	 */
	protected boolean isFocused = false;

	/**
	 * @param stObjects - the group of objects to manipulate
	 */
	public TimeFocuser(TransformGroup stObjects) {
		if (stObjects == null)
			return;
		this.stObjects = stObjects;
		stObjects.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		clipper = new ModelClip();
		//upper plane
		clipper.setPlane(0, new Vector4d(0, 0, 1, -1));
		//lower plane
		clipper.setPlane(1, new Vector4d(0, 0, -1, 0));
		boolean enables[] = { true, true, false, false, false, false };
		clipper.setEnables(enables);
		clipper.setInfluencingBounds(new BoundingSphere());
		clipper.addScope(stObjects);
		//clipper.setCapability(ModelClip.ALLOW_PLANE_WRITE);
		/*
		Transform3D scale=new Transform3D();
		scale.setScale(new Vector3d(1,1,2));
		Transform3D translate=new Transform3D();
		translate.setTranslation(new Vector3d(0,0,-1));
		Transform3D transform=new Transform3D();
		transform.mul(translate,scale);
		stObjects.setTransform(transform);
		*/
	}

	public Node getTransformer() {
		return clipper;
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
		doFocusing();
	}

	/**
	 * Scales the graphical objects in the z-dimension according to
	 * the currently selected time interval
	 */
	public void doFocusing() {
		if (focusInterval == null || !focusInterval.hasTimeLimits()) {
			if (!isFocused)
				return;
			if (identity == null) {
				identity = new Transform3D();
			}
			stObjects.setTransform(identity);
			isFocused = false;
			return;
		}
		TimeMoment start = focusInterval.getDataIntervalStart(), t1 = focusInterval.getCurrIntervalStart();
		long fLen = focusInterval.getDataIntervalLength(), cLen = focusInterval.getCurrIntervalLength();
		double scaleFactor = 1.0 * fLen / cLen;
		double shift = 1.0 * t1.subtract(start) / cLen;
		if (scale == null) {
			scale = new Transform3D();
			translate = new Transform3D();
			transform = new Transform3D();
		}
		scale.setScale(new Vector3d(1, 1, scaleFactor));
		translate.setTranslation(new Vector3d(0, 0, -shift));
		transform.mul(translate, scale);
		stObjects.setTransform(transform);
		isFocused = true;
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
			if (pce.getPropertyName().equals("current_interval")) {
				doFocusing();
			}
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
