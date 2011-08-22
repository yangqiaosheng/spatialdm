package spade.analysis.space_time_cube;

import java.awt.Toolkit;

import javax.media.j3d.Appearance;
import javax.media.j3d.Geometry;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;

import spade.vis.dmap.DGeoObject;

import com.sun.j3d.utils.geometry.Primitive;
import com.sun.j3d.utils.geometry.Sphere;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 29, 2008
 * Time: 3:59:52 PM
 * Used to represent time-referenced geo object of CommonGIS as 3D objects
 * in a ST-cube
 */
public class SpaceTimeObject {
	/**
	 * The default size in pixels of the points representing point objects
	 */
	public static float pointSize = Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f * 2.5f;
	/**
	 * The minimum size in pixels of the points representing point objects
	 */
	public static float minPointSize = Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f;
	/**
	 * The geo object represented by a 3D object in the ST-cube
	 */
	public DGeoObject geoObj = null;
	/**
	 * The identifier of the entity set this object belongs to
	 */
	public String setId = null;
	/**
	 * The index of the geo object in its container (map layer)
	 */
	public int objIdxCont = -1;
	/**
	 * The index of the geo object in the corresponding group of shapes in the 3D world
	 */
	public int objIdxGroup = -1;
	/**
	 * The 3D object representing this geo object. This may be Shape3D or Primitive
	 * or Group including Shape3D or Primitive
	 */
	protected Node obj3d = null;

	/**
	 * @param geoObj - the geo object represented by a 3D object in the ST-cube
	 * @param obj3d - the 3D object representing this geo object.
	 * This may be Shape3D or Primitive or Group including Shape3D or Primitive.
	 */
	public SpaceTimeObject(DGeoObject geoObj, Node obj3d) {
		this.geoObj = geoObj;
		this.obj3d = obj3d;
		obj3d.setPickable(true);
		Shape3D shape = getShape3D(obj3d);
		if (shape == null) {
			Primitive prim = getPrimitive(obj3d);
			if (prim != null) {
				prim.setPickable(true);
				if (prim instanceof Sphere) {
					shape = ((Sphere) prim).getShape();
				}
			}
		}
		if (shape != null) {
			shape.getGeometry().setCapability(Geometry.ALLOW_INTERSECT);
			shape.setPickable(true);
		}
	}

	/**
	 * The 3D object representing this geo object. This may be Shape3D or Primitive
	 * or Group including Shape3D or Primitive.
	 */
	public Node getObj3d() {
		return obj3d;
	}

	/**
	 * Checks if this object has the given node
	 */
	public boolean hasNode(Node node) {
		if (node == null || obj3d == null)
			return false;
		if (obj3d instanceof Shape3D)
			return node.equals(obj3d);
		if (obj3d instanceof Group)
			return includes((Group) obj3d, node);
		return false;
	}

	protected static boolean includes(Group group, Node node) {
		if (group == null || node == null)
			return false;
		for (int i = 0; i < group.numChildren(); i++) {
			Node child = group.getChild(i);
			if (node.equals(child))
				return true;
			if (child instanceof Group) {
				boolean found = includes((Group) child, node);
				if (found)
					return true;
			}
		}
		return false;
	}

	protected static Shape3D getShape3D(Node node) {
		if (node instanceof Shape3D)
			return (Shape3D) node;
		if (node instanceof Group)
			return getShape3D(((Group) node).getChild(0));
		return null;
	}

	protected static Primitive getPrimitive(Node node) {
		if (node instanceof Primitive)
			return (Primitive) node;
		if (node instanceof Group)
			return getPrimitive(((Group) node).getChild(0));
		return null;
	}

	/**
	 * Applies the given appearance to its 3D object
	 */
	public void setAppearance(Appearance ap) {
		if (obj3d == null)
			return;
		Shape3D shape = getShape3D(obj3d);
		if (shape != null) {
			shape.setAppearance(ap);
		} else {
			Primitive prim = getPrimitive(obj3d);
			if (prim != null) {
				prim.setAppearance(ap);
			}
		}
	}

	/**
	 * Returns the appearance of its 3D object
	 */
	public Appearance getAppearance() {
		if (obj3d == null)
			return null;
		Shape3D shape = getShape3D(obj3d);
		if (shape != null)
			return shape.getAppearance();
		else {
			Primitive prim = getPrimitive(obj3d);
			if (prim != null)
				return prim.getAppearance();
		}
		return null;
	}

	/**
	 * Returns the geo object corresponding to this 3D object
	 */
	public DGeoObject getGeoObject() {
		return geoObj;
	}

	/**
	 * Returns the identifier of the geo object corresponding to this 3D object
	 */
	public String getGeoObjectId() {
		if (geoObj == null)
			return null;
		return geoObj.getIdentifier();
	}

	/**
	 * Sets the identifier of the entity set this object belongs to
	 */
	public void setEntitySetId(String setId) {
		this.setId = setId;
	}

	/**
	 * Returns the identifier of the entity set this object belongs to
	 */
	public String getEntitySetId() {
		return setId;
	}

	/**
	 * Sets the index of the geo object in its container (map layer)
	 */
	public void setGeoObjectIdxInContainer(int idx) {
		objIdxCont = idx;
	}

	/**
	 * Returns the index of the geo object in its container (map layer)
	 */
	public int getGeoObjectIdxInContainer() {
		return objIdxCont;
	}

	/**
	 * Sets the index of the geo object in the corresponding group of shapes in the 3D world
	 */
	public void setGeoObjectIdxInGroup(int idx) {
		objIdxGroup = idx;
	}

	/**
	 * Returns the index of the geo object in the corresponding group of shapes in the 3D world
	 */
	public int getGeoObjectIdxInGroup() {
		return objIdxGroup;
	}
}
