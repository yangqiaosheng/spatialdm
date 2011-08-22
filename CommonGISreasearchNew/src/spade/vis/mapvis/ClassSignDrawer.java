package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Graphics;

import spade.vis.database.DataItem;
import spade.vis.geometry.Circle;
import spade.vis.geometry.Sign;
import spade.vis.map.MapContext;

/**
* Represents results of any classification on a map by returning circle signs
* with the corresponding colors from the getPresentation(...) method.
* Used for point objects while the ancestor class ClassDrawer is used for
* area objects.
*/
public class ClassSignDrawer extends ClassDrawer implements SignDrawer {
	protected Circle c = null;

	/**
	* Constructs an instance of Circle for further use.
	*/
	protected void checkCreateSign() {
		if (c != null)
			return;
		c = new Circle();
		int diameter = Math.round(2.0f * java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
		c.setSizes(diameter, diameter);
		c.setDrawBorder(false);
	}

	/**
	* This method informs whether the Visualizer produces diagrams.
	* This is important for defining the order of drawing of GeoLayers on the
	* map: the diagrams should be drawn on top of all geography.
	*/
	@Override
	public boolean isDiagramPresentation() {
		return true;
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public Object getPresentation(DataItem dit, MapContext mc) {
		if (dit == null)
			return null;
		if (sdController != null && !sdController.mustDrawObject(dit.getId()))
			return null;
		Object pres = super.getPresentation(dit, mc);
		if (pres != null && !(pres instanceof Color))
			return pres;
		checkCreateSign();
		if (pres == null) {
			c.setColor(Color.lightGray);
		} else {
			c.setColor((Color) pres);
		}
		return c;
	}

	/**
	* Draws a small picture symbolizing this presentation technique
	*/
	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		checkCreateSign();
		if (classifier != null && classifier.getNClasses() > 0) {
			c.setColor(classifier.getClassColor(0));
		} else {
			c.setColor(Color.lightGray);
		}
		if (w < c.getDiameter() * 2) {
			c.draw(g, x, y, w, h);
		} else {
			c.draw(g, x, y, w / 2, h);
			if (classifier != null && classifier.getNClasses() > 1) {
				c.setColor(classifier.getClassColor(classifier.getNClasses() - 1));
			}
			c.draw(g, x + w / 2, y, w / 2, h);
		}
	}

	/**
	* Methods from the interface SignDrawer
	*/
	@Override
	public Sign getSignInstance() {
		checkCreateSign();
		Circle circle = new Circle();
		circle.setDrawBorder(false);
		circle.setMayChangeProperty(Sign.BORDER_COLOR, false);
		circle.setMayChangeProperty(Sign.COLOR, false);
		circle.setMayChangeProperty(Sign.MAX_SIZE, false);
		circle.setMayChangeProperty(Sign.MIN_SIZE, false);
		circle.setMayChangeProperty(Sign.SIZE, true);
		circle.setSizes(c.getDiameter(), c.getDiameter());
		if (classifier != null && classifier.getNClasses() > 0) {
			circle.setColor(classifier.getClassColor(0));
		} else {
			circle.setColor(Color.lightGray);
		}
		return circle;
	}

	/**
	* Through this method a SignDrawer is informed that some of the sign properties
	* have changed. The sign drawer must care about repainting the map.
	* The possible identifiers of sign properties are listed in the class
	* @see spade.vis.geometry.Sign.
	*/
	@Override
	public void signPropertyChanged(int propertyId, Sign sgn) {
		if (sgn == null || c == null)
			return;
		switch (propertyId) {
		case Sign.SIZE:
			c.setSizes(sgn.getWidth(), sgn.getHeight());
			break;
		/*
		case Sign.BORDER_COLOR:
		  c.setBorderColor(sgn.getBorderColor());
		  break;
		*/
		default:
			return;
		}
		notifyVisChange();
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: size of the signs.
	*/
	@Override
	public void startChangeParameters() {
		SignParamsController pc = new SignParamsController();
		pc.startChangeParameters(this);
	}
}
