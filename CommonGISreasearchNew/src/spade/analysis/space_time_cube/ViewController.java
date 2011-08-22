package spade.analysis.space_time_cube;

import java.awt.AWTEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.media.j3d.Behavior;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.media.j3d.WakeupOr;
import javax.vecmath.Vector3d;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 29, 2008
 * Time: 11:47:24 AM
 * Controls the view: rotates, translates, etc.
 */
public class ViewController extends Behavior {
	/**
	 * The object to manipulate
	 */
	protected TransformGroup targetTG;
	/**
	 * The event activating the behaviour
	 */
	protected WakeupOnAWTEvent dragEvent = new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED);
	protected WakeupOnAWTEvent releaseEvent = new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED);
	protected WakeupCriterion events[] = { dragEvent, releaseEvent };
	protected WakeupOr orEvents = new WakeupOr(events);

	protected Transform3D rotate = null;
	protected Transform3D translate = null;
	protected Transform3D transform = null;
	protected double angle = 0.0, dist = 0.0, height = 0.0, shift = 0.0;

	/**
	 * Create the behavior
	 * @param targetTG - the object to manipulate
	 */
	public ViewController(TransformGroup targetTG) {
		this.targetTG = targetTG;
	}

	/**
	 * Initialize the behavior: set the initial wakeup condition.
	 * This method is called when the behavior becomes live.
	 */
	@Override
	public void initialize() {
		// set initial wakeup condition
		this.wakeupOn(dragEvent);
	}

	protected int mouseX0 = -1, mouseY0 = -1;
	protected boolean rightButtonPressed = false;

	/**
	 * Called by Java3D when appropriate stimulus occurs
	 */
	@Override
	public void processStimulus(Enumeration criteria) {
		//decode the stimulus...
		if (criteria == null || !criteria.hasMoreElements())
			return;
		int dx = 0, dy = 0;
		boolean released = false;
		while (criteria.hasMoreElements() && dx == 0 && dy == 0) {
			Object elem = criteria.nextElement();
			if (elem instanceof WakeupOnAWTEvent) {
				WakeupOnAWTEvent ae = (WakeupOnAWTEvent) elem;
				AWTEvent events[] = ae.getAWTEvent();
				if (events == null || events.length < 1) {
					continue;
				}
				for (AWTEvent event : events)
					if (event instanceof MouseEvent) {
						MouseEvent me = (MouseEvent) event;
						if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
							if (mouseX0 < 0 && mouseY0 < 0) {
								mouseX0 = me.getX();
								mouseY0 = me.getY();
								int mod = me.getModifiersEx();
								rightButtonPressed = ((mod & InputEvent.BUTTON2_DOWN_MASK) == InputEvent.BUTTON2_DOWN_MASK) || ((mod & InputEvent.BUTTON3_DOWN_MASK) == InputEvent.BUTTON3_DOWN_MASK);
								//System.out.println("rightButtonPressed="+rightButtonPressed);
								this.wakeupOn(orEvents);
								return;
							} else {
								int x = me.getX(), y = me.getY();
								dx = x - mouseX0;
								dy = y - mouseY0;
								mouseX0 = -1;
								mouseY0 = -1;
								break;
							}
						} else if (me.getID() == MouseEvent.MOUSE_RELEASED) {
							int x = me.getX(), y = me.getY();
							dx = x - mouseX0;
							dy = y - mouseY0;
							mouseX0 = -1;
							mouseY0 = -1;
							released = true;
							break;
						}
					}
			}
		}
		//do what is necessary in response to stimulus
		if (dx != 0 || dy != 0) {
			if (rotate == null || translate == null) {
				rotate = new Transform3D();
				translate = new Transform3D();
				transform = new Transform3D();
			}
			if (rightButtonPressed) {
				if (Math.abs(dx) > Math.abs(dy)) {
					if (dx > 0) {
						angle += Math.PI / 80;
					} else {
						angle -= Math.PI / 80;
					}
					if (angle > 2 * Math.PI) {
						angle -= 2 * Math.PI;
					} else if (angle < -2 * Math.PI) {
						angle += 2 * Math.PI;
					}
					rotate.rotY(angle);
				} else {
					if (dy < 0) {
						dist -= 0.025;
					} else {
						dist += 0.025;
					}
				}
			} else {
				if (Math.abs(dx) > Math.abs(dy)) {
					if (dx < 0) {
						shift -= 0.025;
					} else {
						shift += 0.025;
					}
					if (shift < -1.5) {
						shift = -1.5;
					} else if (shift > 1.5) {
						shift = 1.5;
					}
				} else {
					if (dy > 0) {
						height -= 0.025;
					} else {
						height += 0.025;
					}
					if (height < -1.5) {
						height = -1.5;
					} else if (height > 1.5) {
						height = 1.5;
					}
				}
			}
			translate.setTranslation(new Vector3d(shift, height, dist));
			transform.mul(translate, rotate);
			targetTG.setTransform(transform);
		}
		if (released) {
			rightButtonPressed = false;
		}
		this.wakeupOn((released) ? dragEvent : orEvents);
	}
}
