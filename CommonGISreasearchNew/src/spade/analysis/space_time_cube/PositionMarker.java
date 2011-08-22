package spade.analysis.space_time_cube;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Sep 23, 2009
 * Time: 12:51:58 PM
 * Marks a selected spatial position in the cube by a vertical line.
 * Moves the line when the position changes.
 */
public class PositionMarker implements PropertyChangeListener {
	/**
	 * The vertical line to be moved
	 */
	protected Shape3D line = null;
	/**
	 * The line is included in the TransformGroup
	 */
	protected TransformGroup moveableLine = null;
	/**
	 * Perform translation of the marker
	 */
	protected Transform3D translate = null;
	/**
	 * The switcher of drawing of the marker
	 */
	protected Switch drawSwitch = null;
	/**
	 * Used for transforming the coordinates
	 */
	protected CubeMetrics cubeMetr = null;

	/**
	 * Creates the necessary 3D object(s) and adds it or them to the specified TransformGroup
	 */
	public void initialize(CubeMetrics cubeMetr, TransformGroup stCube) {
		this.cubeMetr = cubeMetr;
		LineArray lar = new LineArray(2, GeometryArray.COORDINATES);
		lar.setCoordinate(0, new Point3d(0, 0, 0));
		lar.setCoordinate(1, new Point3d(0, 0, 1));
		lar.setCapability(Geometry.ALLOW_INTERSECT);
		line = new Shape3D(lar);
		Appearance ap = new Appearance();
		ColoringAttributes catt = new ColoringAttributes();
		catt.setColor(1f, 0f, 0f);
		ap.setColoringAttributes(catt);
		LineAttributes latt = new LineAttributes();
		latt.setLineWidth(1);
		ap.setLineAttributes(latt);
		line.setAppearance(ap);

		moveableLine = new TransformGroup();
		moveableLine.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		moveableLine.addChild(line);

		drawSwitch = new Switch(Switch.CHILD_NONE);
		drawSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		drawSwitch.addChild(moveableLine);

		stCube.addChild(drawSwitch);
	}

	/**
	 * Moves the line when the selected position changes.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("position_selection")) {
			if (e.getNewValue() == null || !(e.getNewValue() instanceof float[])) {
				//hide the position marker
				drawSwitch.setWhichChild(Switch.CHILD_NONE);
			} else {
				float coord[] = (float[]) e.getNewValue();
				//move the position marker to the specified spatial position
				double x = cubeMetr.cubeX(coord[0], coord[1]), y = cubeMetr.cubeY(coord[0], coord[1]);
				if (translate == null) {
					translate = new Transform3D();
				}
				translate.setTranslation(new Vector3d(x, y, 0));
				moveableLine.setTransform(translate);
				drawSwitch.setWhichChild(Switch.CHILD_ALL);
			}
		}
	}
}
