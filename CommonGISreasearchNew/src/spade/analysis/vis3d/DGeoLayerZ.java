package spade.analysis.vis3d;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.NoSuchElementException;
import java.util.Vector;

import spade.lib.util.QSortAlgorithm;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;

/**
* DGeoLayerZ is a collection of DGeoObjectZs. A DGeoLayerZ can handle these
* special geo objects with third Z-coordinate for representation of 3rd
* attribute, for example, time reference, in 3D-space.
* DGeoObjectZ and DGeoLayerZ are classes of Descartes that extends
* DGeoLayer and DGeoObject for enabling this feature, respectively.
*/

public class DGeoLayerZ extends DGeoLayer {

	protected int attr3DIndex = -1;
	protected String attr3DName = "";
/*
* Here are min/max values of the Z-attribute and scale factor
*/
	protected double minZ = Double.NaN, maxZ = Double.NaN;
	protected double ZFactor = Double.NaN;
	protected double minRelativeZ = Double.NaN, maxRelativeZ = Double.NaN;
	protected double minFocusZ = Double.NaN, maxFocusZ = Double.NaN;
	protected boolean usePointsInstead = false;

	private DGeoObjectZ[] objectsSorted = null;
	// private DGeoObjectZ[] objectsVisible=null;

	private float xe = Float.NaN, ye = Float.NaN, ze = Float.NaN;

	// private
	public void addGeoObject(DGeoObjectZ objZ) {
		this.addGeoObject(objZ, false);
	}

	public void addGeoObject(DGeoObjectZ objZ, boolean notifyAll) {
		if (objZ == null)
			return;
		if (geoObj == null) {
			geoObj = new Vector(50, 50);
		}
		geoObj.addElement(objZ);
		objZ.getSpatialData().setIndexInContainer(geoObj.size() - 1);
		hasLabels = hasLabels || objZ.getLabel() != null;
		absBounds = currBounds = null;
		if (notifyAll) {
			notifyPropertyChange("ObjectSet", null, null);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		// System.out.println("DGeoLayerZ:: received property change event = "+pce.getPropertyName());
		if (dTable != null && pce.getSource() == dTable) {
			if (pce.getPropertyName().equals("values")) {
				//System.out.println("notification from the table received");
				Vector attrs = (Vector) pce.getNewValue();
				if (attrs != null) {
					String myAttr = dTable.getAttributeId(attr3DIndex);
					if (myAttr != null) {
						for (int j = 0; j < attrs.size(); j++)
							if (myAttr.equalsIgnoreCase((String) attrs.elementAt(j))) {
								updateZGeoObjects();
								notifyPropertyChange("VisParameters", null, vis);
								return;
							}
					}
				}
			}
			/*
			if (pce.getPropertyName().equals("data_updated")) {
			  //System.out.println("notification from the table received");
			  Vector attrs=(Vector)pce.getNewValue();
			  this.removeAllObjects();
			  //this.setGeoObjects(dTable.getd);
			  if (attrs!=null) {
			    String myAttr=dTable.getAttributeId(attr3DIndex);
			    if (myAttr!=null)
			      for (int j=0; j<attrs.size(); j++)
			        if (myAttr.equalsIgnoreCase((String)attrs.elementAt(j))) {
			          updateZGeoObjects();
			          return;
			        }
			  }
			}
			*/
		} else {
			super.propertyChange(pce);
		}
	}

	@Override
	public Vector findObjectsAt(int x, int y, MapContext mc, boolean findOne) {
		if (!drawParm.drawLayer)
			return null;
		if (mc == null)
			return null;
		if (!(mc instanceof MapMetrics3D))
			return super.findObjectsAt(x, y, mc, findOne);
		MapMetrics3D mm3d = (MapMetrics3D) mc;

		Vector pointed = new Vector(10, 10);
		if (objectsSorted == null)
			return null; // sometimes such happens...

		if (usePointsInstead || getType() == Geometry.point) {
			for (DGeoObjectZ gobj : objectsSorted) {
				if (isObjectActive(gobj) && gobj.getIdentifier() != null && gobj.getIdentifier().length() > 0 && gobj.isPointInside(x, y)) {
					pointed.addElement(gobj.getIdentifier());
					if (findOne)
						return pointed;
				}
			}
		}
		if (pointed.size() < 1)
			return null;
		return pointed;
	}

	/**
	* Finds objects fitting in the specified rectangle.
	*/
	@Override
	public Vector findObjectsIn(int x1, int y1, int x2, int y2, MapContext mc) {
		if (!drawParm.drawLayer)
			return null;
		if (mc == null)
			return null;
		if (!(mc instanceof MapMetrics3D))
			return super.findObjectsIn(x1, y1, x2, y2, mc);
		if (x1 == x2 && y1 == y2)
			return findObjectsAt(x1, y1, mc, false);
		if (objectsSorted == null)
			return null; // sometimes such happens...
		Vector fit = new Vector(20, 10);
		for (DGeoObjectZ gobj : objectsSorted) {
			if (isObjectActive(gobj) && gobj.getIdentifier() != null && gobj.getIdentifier().length() > 0 && gobj.isInRectangle(x1, y1, x2, y2)) {
				fit.addElement(gobj.getIdentifier());
			}
		}
		return fit;
	}

	@Override
	public boolean highlightObject(String objectId, boolean isHighlighted, Graphics g, MapContext mc) {
		if (mc == null)
			return false;
		MapMetrics3D mm3d = (MapMetrics3D) mc;
		if (!drawParm.drawLayer)
			return false;
		DGeoObjectZ gobj = (DGeoObjectZ) findObjectById(objectId);

		if (gobj == null)
			return false;
		//if (!isObjectVisible(gobj,mm3d)) return false;
		gobj.highlight(g, mc, isHighlighted);
		return true;
	}

	/**
	* A method from the basic GeoLayer interface.
	* This method is used to switch on/off selection (durable highlighting).
	* The GeoLayer should draw the entity in "selected" state.
	*/
	@Override
	public boolean selectObject(String objectId, boolean isSelected, Graphics g, MapContext mc) {
		if (mc == null)
			return false;
		MapMetrics3D mm3d = (MapMetrics3D) mc;
		if (!drawParm.drawLayer)
			return false;
		DGeoObjectZ gobj = (DGeoObjectZ) findObjectById(objectId);
		if (gobj == null)
			return false;
		//if (!isObjectVisible(gobj,mm3d)) return false;
		gobj.select(g, mc, isSelected);
		return true;
	}

	@Override
	public void showHighlighting(Graphics g, MapContext mc) {
		if (!drawParm.drawLayer || g == null || mc == null)
			return;
		if (geoObj == null)
			return;
		MapMetrics3D mm3d = null;
		if (mc instanceof MapMetrics3D) {
			mm3d = (MapMetrics3D) mc;
		}

		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObjectZ gobj = (DGeoObjectZ) getObject(i);
			if (isObjectVisible(gobj, mm3d) && gobj.isHighlighted()) {
				gobj.showHighlight(g, mc);
			}
		}
	}

	@Override
	public void drawSelectedObjects(Graphics g, MapContext mc) {
		if (geoObj == null || g == null || mc == null)
			return;
		MapMetrics3D mm3d = null;
		if (mc instanceof MapMetrics3D) {
			mm3d = (MapMetrics3D) mc;
			//System.out.println("SpecLayer::drawSelectedObjects!");
		}

		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObjectZ gobj = (DGeoObjectZ) getObject(i);
			if (isObjectVisible(gobj, mm3d) && gobj.isSelected()) {
				gobj.showSelection(g, mc);
			}
		}

	}

	private void updateZGeoObjects() {
		for (int i = 0; i < getObjectCount(); i++) {
			DGeoObjectZ zobj = (DGeoObjectZ) getObject(i);
			if (zobj == null || zobj.getData() == null) {
				continue;
			}
			double val = dTable.getNumericAttrValue(attr3DIndex, i);
			if (Double.isNaN(val)) {
				continue;
			}
			// initialize 3DGeoObjects with absolute attribute values
			zobj.setZPosition(val);
			zobj.setZAttrValue(val);
			if (Double.isNaN(minZ) || minZ > val) {
				minZ = val;
			}
			if (Double.isNaN(maxZ) || maxZ < val) {
				maxZ = val;
			}
		}
		updateZValues();
	}

	public void setZAttrIndex(int NAttr3D) {
		attr3DIndex = NAttr3D;
	}

	public void setZFactor(double factor) {
		ZFactor = factor;

	}

	public void setUsePointGeometry(boolean flag) {
		if (getType() != Geometry.point) {
			usePointsInstead = flag;
			int nObj = getObjectCount();
			if (nObj > 0) {
				for (int i = 0; i < nObj; i++) {
					DGeoObjectZ zobj = (DGeoObjectZ) getObject(i);
					zobj.setDrawAsPoint(flag);
				}
			}
		}
	}

	public void updateZValues() {
		updateZValues(minZ, maxZ);
	}

	public void updateZValues(double low, double hi) {
		maxZ = hi;
		minZ = low;
		double val = Double.NaN;
		if (Double.isNaN(minFocusZ)) {
			minFocusZ = low;
		}
		if (Double.isNaN(maxFocusZ)) {
			maxFocusZ = hi;
		}
		if (getObjectCount() > 0) {
			for (int i = 0; i < getObjectCount(); i++) {
				DGeoObjectZ zobj = (DGeoObjectZ) getObject(i);
				val = ZFactor * (zobj.getZAttrValue() - low);
				zobj.setZPosition(val);
				if (Double.isNaN(minRelativeZ) || minRelativeZ > val) {
					minRelativeZ = val;
				}
				if (Double.isNaN(maxRelativeZ) || maxRelativeZ < val) {
					maxRelativeZ = val;
				}
			}
		}
	}

	public void updateBounds(MapMetrics3D mm3d) {
		if (mm3d == null)
			return;
		int objCount = getObjectCount();
		if (objCount < 1)
			return;

		for (int i = 0; i < objCount; i++) {
			DGeoObjectZ zobj = (DGeoObjectZ) getObject(i);
			RealPoint rp = null;
			rp = zobj.getCenter();
			zobj.setBounds3D(new Rectangle(mm3d.get3DTransform(rp.x, rp.y, zobj.getZPosition())));
		}
	}

	/**
	*  The funcion orders DGeoObjects using as criterium distance from this object
	*  to viewpoint
	*/
	protected void makeOrder(MapMetrics3D mm3d) {
		if (mm3d == null)
			return;
		int objCount = getObjectCount();
		if (objCount < 1)
			return;
		float fdist = Float.NaN;
		// if space distance index of objects has been not created yet: initialize
		if (objectsSorted == null) {
			objectsSorted = new DGeoObjectZ[objCount];
		} else if (objectsSorted.length != objCount) {
			objectsSorted = new DGeoObjectZ[objCount];
		}

		for (int i = 0; i < objCount; i++) {
			DGeoObjectZ zobj = (DGeoObjectZ) getObject(i);
			RealPoint rp = zobj.getCenter();

			fdist = mm3d.getDistanceToViewPoint(rp.x, rp.y, (float) zobj.getZPosition());
			zobj.setDistanceToViewPoint(fdist);
			//zobj.setBounds3D(new Rectangle(mm3d.get3DTransform(rp.x,rp.y,zobj.getZPosition())));
			objectsSorted[i] = zobj;
		}
		// now sorting
		QSortAlgorithm.sort(objectsSorted, true);
		// for (int i=0; i<objCount; i++) objectsSorted[i].setOrder(i);
	}

	/**
	*  Internal counter for issued ordered objects
	*/
	private int currObjN = -1;

	/**
	*  The function returns the first object in the order.
	*  It calls ordering procedure first.
	*/
	public DGeoObjectZ getFirst(MapMetrics3D mm3d) throws NoSuchElementException {
		if (mm3d == null)
			return null;
		// If the objects are absent yet: nothing to do
		if (getObjectCount() < 1)
			throw (new NoSuchElementException("No objects in the layer yet"));
		else
		// If current known amount of Z objects is not equal to the actual amount
		// in the reference layer: update needed definitely
		if (objectsSorted != null && objectsSorted.length != getObjectCount()) {
			makeOrder(mm3d);
		}

		if (Float.isNaN(xe) || Float.isNaN(xe) || Float.isNaN(xe)) {
			RealPoint rp = mm3d.getViewpointXY();
			xe = rp.x;
			ye = rp.y;
			ze = (float) mm3d.getViewpointZ();
			makeOrder(mm3d);
		} else if (mm3d.isViewPointChanged(xe, ye, ze)) {
			makeOrder(mm3d);
		}
		if (currObjN != -1) {
			currObjN = -1;
		}
		currObjN++;
		//return (DGeoObjectZ)getObject(index[currObjN]);
		return objectsSorted[0];
	}

	/**
	*  The function returns the next object in the order.
	*/
	public DGeoObjectZ getNext() throws NoSuchElementException {
		if (currObjN >= getObjectCount()) {
			currObjN = -1;
			throw (new NoSuchElementException("No more objects in the layer"));
		}
		//return (DGeoObjectZ)getObject(index[++currObjN]);
		return objectsSorted[++currObjN];
	}

	public DGeoObjectZ getNext(MapMetrics3D mm3d) throws NoSuchElementException {
		if (currObjN < 0)
			return getFirst(mm3d);
		if (currObjN >= getObjectCount() - 1) {
			currObjN = -1;
			throw (new NoSuchElementException("No more objects in the layer"));
		}
		return objectsSorted[++currObjN];
		//return (DGeoObjectZ)getObject(index[++currObjN]);
	}

	public boolean hasMoreObjects() {
		return currObjN < getObjectCount();
	}

	public DGeoObjectZ getObjectSorted(int index) {
		if (objectsSorted == null)
			return null;
		return objectsSorted[index];
	}

	public double getZAttrMinValue() {
		return minZ;
	}

	public double getZAttrMaxValue() {
		return maxZ;
	}

	public double getZAttrMinValueRelative() {
		return minRelativeZ;
	}

	public double getZAttrMaxValueRelative() {
		return maxRelativeZ;
	}

	public int getZAttributeIndex() {
		return attr3DIndex;
	}

	public String getZAttributeName() {
		return dTable.getAttributeName(attr3DIndex);
	}

	public boolean isObjectVisible(DGeoObjectZ obj, MapMetrics3D mm3d) {
		if (obj == null || mm3d == null)
			return false;
		RealPoint rpObjCenter = obj.getCenter();
		RealRectangle rrVisTerr = mm3d.getVisibleTerritory();
		if (rpObjCenter == null || rrVisTerr == null)
			return false;

		RealPolyline rplMapRefFrame = new RealPolyline();
		float x1 = rpObjCenter.x, y1 = rpObjCenter.y;
		float x[] = { rrVisTerr.rx1, rrVisTerr.rx1, rrVisTerr.rx2, rrVisTerr.rx2, rrVisTerr.rx1 }, y[] = { rrVisTerr.ry1, rrVisTerr.ry2, rrVisTerr.ry2, rrVisTerr.ry1, rrVisTerr.ry1 };
		rplMapRefFrame.p = new RealPoint[x.length];

		Point pObject = mm3d.get3DTransform(rpObjCenter.x, rpObjCenter.y, obj.getZPosition());
		Point p = null;
		for (int i = 0; i < x.length; i++) {
			p = mm3d.get3DTransform(x[i], y[i], mm3d.getZ0());
			rplMapRefFrame.p[i] = new RealPoint();
			rplMapRefFrame.p[i].x = p.x;
			rplMapRefFrame.p[i].y = p.y;
		}

		boolean objectIsBehind = rplMapRefFrame.contains(pObject.x, pObject.y, 0);

		objectIsBehind = ((mm3d.getViewpointZ() > mm3d.getZ0()) ? ((obj.getZPosition() < mm3d.getZ0()) && objectIsBehind) : ((obj.getZPosition() >= mm3d.getZ0()) && objectIsBehind));
		return !objectIsBehind && isInsideRange(minFocusZ, maxFocusZ, obj.getZAttrValue()) && obj.canBeDrawn(mm3d.getMinZ(), mm3d.getMaxZ()) && obj.fitsInRectangle(rrVisTerr.rx1, rrVisTerr.ry1, rrVisTerr.rx2, rrVisTerr.ry2)
				&& rrVisTerr.contains(rpObjCenter.x, rpObjCenter.y, 0.0f) && obj.hasValueZ() && isObjectActive(obj);
	}

	public boolean canObjectBeDrawn(DGeoObjectZ obj, double lowZ, double hiZ) {
		boolean canBeDrawn = obj != null && isInsideRange(minFocusZ, maxFocusZ, obj.getZAttrValue()) && obj.canBeDrawn(lowZ, hiZ) && isObjectActive(obj);
		return canBeDrawn;
	}

	public void setFocusChanged(double lowerLimit, double upperLimit) {
		minFocusZ = lowerLimit;
		maxFocusZ = upperLimit;
		//System.out.println("DGeoLayerZ:: focusMin: "+lowerLimit+"focusMax: "+upperLimit);
	}

	public void setMinLimitChanged(double lowerLimit) {
		minFocusZ = lowerLimit;
	}

	public void setMaxLimitChanged(double upperLimit) {
		maxFocusZ = upperLimit;
	}

	public boolean isInsideRange(double v1, double v2, double v) {
		return ((v >= v2 && v <= v1) || (v >= v1 && v <= v2));
	}
}
