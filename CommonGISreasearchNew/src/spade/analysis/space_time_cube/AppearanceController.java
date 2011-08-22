package spade.analysis.space_time_cube;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PointAttributes;

import spade.analysis.classification.ObjectColorer;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.ObjectAppearance;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 08-May-2008
 * Time: 12:42:06
 * Linked to a layer represented in the 3rd dimension of a STcube
 * and changes the appearance of the objects when the visual parameters
 * of the layer on a map change.
 */
public class AppearanceController implements PropertyChangeListener, Destroyable {
	/**
	 * The layer with the objects
	 */
	protected DGeoLayer layer = null;
	/**
	 * The drawn 3D objects, instances of SpaceTimeObject
	 */
	protected Vector<SpaceTimeObject> drawnObjects = null;
	/**
	 * Propagates colours of objects (results of classification)
	 */
	protected Supervisor supervisor = null;

	/**
	 * Stores the reference to the layer and starts listening to changes
	 * of its visual representation
	 */
	public void setLayer(DGeoLayer layer) {
		this.layer = layer;
		if (layer != null) {
			layer.addPropertyChangeListener(this);
		}
	}

	public void setDrawnObjects(Vector<SpaceTimeObject> drawnObjects) {
		this.drawnObjects = drawnObjects;
	}

	/**
	* Sets a reference to the system's supervisor used for propagating events
	* among system's components.
	*/
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
		if (supervisor != null) {
			supervisor.addPropertyChangeListener(this);
		}
	}

	/**
	 * Reacts to changes of visual parameters of objects in the layer
	 */
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(layer)) {
			if (pce.getPropertyName().equalsIgnoreCase("destroyed")) {
				destroy();
			} else if (pce.getPropertyName().equalsIgnoreCase("DrawingParameters") || pce.getPropertyName().equalsIgnoreCase("Visualization") || pce.getPropertyName().equalsIgnoreCase("VisParameters")) {
				changeObjectAppearance();
			}
		} else if (pce.getSource().equals(supervisor)) {
			String prName = pce.getPropertyName();
			if (prName.equalsIgnoreCase(Supervisor.eventObjectColors)) {
				changeObjectAppearance();
			}
		}
	}

	/**
	 * Changes the appearance of the 3D objects accordingly to the
	 * appearance of the 2D objects
	 */
	public void changeObjectAppearance() {
		if (layer == null || drawnObjects == null || drawnObjects.size() < 1)
			return;
		Appearance app = null; //common appearance
		ObjectColorer colorer = null;
		if (supervisor != null) {
			colorer = supervisor.getObjectColorer();
			if (colorer != null && !layer.getEntitySetIdentifier().equalsIgnoreCase(colorer.getEntitySetIdentifier())) {
				colorer = null;
			}
		}
		DrawingParameters dp = layer.getDrawingParameters();
		Color commonColor = dp.lineColor;
		if (layer.getType() != spade.vis.geometry.Geometry.line && dp.fillContours && dp.fillColor != null) {
			commonColor = dp.fillColor;
		}
		int lineWidth = dp.lineWidth;
		float pointSize = SpaceTimeObject.pointSize;
		if (!layer.hasVisualizer() && colorer == null) {
			app = new Appearance();
			ColoringAttributes ca = new ColoringAttributes();
			float cc[] = commonColor.getRGBColorComponents(null);
			ca.setColor(cc[0], cc[1], cc[2]);
			app.setColoringAttributes(ca);
			LineAttributes la = new LineAttributes();
			la.setLineWidth(lineWidth);
			app.setLineAttributes(la);
			app.setPointAttributes(new PointAttributes(pointSize, true));
		}
		for (int i = 0; i < drawnObjects.size(); i++) {
			SpaceTimeObject obj3D = drawnObjects.elementAt(i);
			if (app != null) {
				obj3D.setAppearance(app);
			} else {
				DGeoObject obj2D = layer.getObject(obj3D.objIdxCont);
				Color color = null;
				if (colorer != null) {
					color = colorer.getColorForObject(obj2D.getIdentifier());
				}
				ObjectAppearance oap = obj2D.getCurrentAppearance();
				if (oap != null) {
					if (color == null) {
						color = oap.lineColor;
						if (layer.getType() != spade.vis.geometry.Geometry.line && oap.fillColor != null) {
							color = oap.fillColor;
						}
						if (layer.getType() == spade.vis.geometry.Geometry.point && oap.signColor != null) {
							color = oap.signColor;
						}
					}
					lineWidth = oap.lineWidth;
					if (oap.signSize > 0) {
						pointSize = oap.signSize;
					}
				}
				if (color == null) {
					color = commonColor;
				}
				Appearance aobj = new Appearance();
				ColoringAttributes caobj = new ColoringAttributes();
				float cc[] = color.getRGBColorComponents(null);
				caobj.setColor(cc[0], cc[1], cc[2]);
				aobj.setColoringAttributes(caobj);
				LineAttributes laobj = new LineAttributes();
				laobj.setLineWidth(lineWidth);
				aobj.setLineAttributes(laobj);
				aobj.setPointAttributes(new PointAttributes(pointSize, true));
				obj3D.setAppearance(aobj);
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
		System.out.println("AppearanceController is destroyed");
		if (layer != null) {
			layer.removePropertyChangeListener(this);
		}
		if (supervisor != null) {
			supervisor.removePropertyChangeListener(this);
		}
		destroyed = true;
	}
}
