package spade.vis.database;

import java.util.Vector;

import spade.time.TimeReference;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealCircle;
import spade.vis.geometry.RealPoint;

public class SpatialEntity implements SpatialDataItem, ThematicDataOwner, java.io.Serializable {
	/**
	* The identifier of the spatial entity.
	*/
	protected String id = null;
	/**
	* The name of the spatial entity (may be null).
	*/
	protected String name = null;
	/**
	* Geometry: contour or coordinates of the entity
	*/
	protected Geometry geom = null;
	/**
	* A SpatialDataItem may have a ThematicDataItem associated with it.
	*/
	protected ThematicDataItem thema = null;
	/**
	* The index of this data item in the container in which it is included.
	* -1 means that it is not included in any container.
	*/
	protected int index = -1;
	/**
	* A SpatialDataItem may have a time reference
	*/
	protected TimeReference tref = null;
	/**
	 * A SpatialEntity may have multiple different states corresponding to
	 * different time moments or intervals. In this case, it may contain
	 * a vector of these states. The elements of the vector are instances
	 * of SpatialEntity.
	 */
	protected Vector states = null;

	public SpatialEntity(String identifier) {
		id = identifier;
	}

	public SpatialEntity(String identifier, String name) {
		id = identifier;
		this.name = name;
	}

	/**
	* The method copyTo(DataItem) is used for updating data items and spatial
	* objects derived from them when data change events occur, for example,
	* in visualisation of temporal data.
	* The DataItem passed as an argument should be an instance of SpatialEntity.
	* The identifier of the data item is not copied! It is assumed that the
	* DataItem passed as an argument has the same identifier as this DataItem.
	*/
	@Override
	public void copyTo(DataItem dit) {
		if (dit == null)
			return;
		if (dit instanceof SpatialEntity) {
			SpatialEntity spe = (SpatialEntity) dit;
			spe.setName(getName());
			spe.setGeometry(getGeometry());
			spe.setThematicData(thema);
			spe.setTimeReference(tref);
		}
	}

	/**
	* Produces and returns a copy of itself.
	*/
	@Override
	public Object clone() {
		SpatialEntity spe = new SpatialEntity(id, name);
		spe.setName(getName());
		spe.setGeometry(geom);
		spe.setThematicData(thema);
		if (tref != null) {
			spe.setTimeReference((TimeReference) tref.clone());
		}
		return spe;
	}

	@Override
	public String getId() {
		return id;
	}

	/**
	* Changes the identifier of the entity; use cautiously!
	*/
	@Override
	public void setId(String ident) {
		id = ident;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String aName) {
		name = aName;
	}

	@Override
	public boolean hasName() {
		return name != null && name.length() > 0;
	}

	@Override
	public char getSpatialType() {
		if (geom == null)
			return Geometry.undefined;
		return geom.getType();
	}

	@Override
	public Geometry getGeometry() {
		return geom;
	}

	@Override
	public void setGeometry(Geometry g) {
		geom = g;
	}

	/**
	* Associated this SpatialDataItem with a ThematicDataItem.
	*/
	@Override
	public void setThematicData(ThematicDataItem item) {
		thema = item;
	}

	/**
	* Returns its ThematicDataItem
	*/
	@Override
	public ThematicDataItem getThematicData() {
		return thema;
	}

	/**
	* Returns the index of this data item in the container in which it is included.
	* May return -1 if not included in any container.
	*/
	@Override
	public int getIndexInContainer() {
		return index;
	}

	/**
	* Sets the index of this data item in the container in which it is included.
	*/
	@Override
	public void setIndexInContainer(int idx) {
		index = idx;
	}

	/**
	* Associated this SpatialDataItem with a time reference.
	*/
	@Override
	public void setTimeReference(TimeReference ref) {
		tref = ref;
	}

	/**
	* Returns its time reference
	*/
	@Override
	public TimeReference getTimeReference() {
		return tref;
	}

	/**
	 * A SpatialEntity may have multiple different states corresponding to
	 * different time moments or intervals. This method returns a vector of
	 * these states. The elements of the vector are instances of SpatialEntity.
	 */
	@Override
	public Vector getStates() {
		return states;
	}

	/**
	 * A SpatialEntity may have multiple different states corresponding to
	 * different time moments or intervals. This method sets a vector of these states.
	 * The elements of the vector are instances of SpatialEntity.
	 */
	@Override
	public void setStates(Vector states) {
		this.states = states;
	}

	/**
	 * If the Geometry is an instance of RealPoint, returns it, otherwise,
	 * returns the centre of the bounding rectangle.
	 */
	public RealPoint getCentre() {
		return getCentre(geom);
	}

	/**
	 * If the Geometry is an instance of RealPoint, returns it, otherwise,
	 * returns the centre of the bounding rectangle.
	 */
	public static RealPoint getCentre(Geometry geom) {
		if (geom == null)
			return null;
		if (geom instanceof RealPoint)
			return (RealPoint) geom;
		if (geom instanceof RealCircle) {
			RealCircle c = (RealCircle) geom;
			return new RealPoint(c.cx, c.cy);
		}
		if (geom instanceof MultiGeometry) {
			MultiGeometry multi = (MultiGeometry) geom;
			int nParts = multi.getPartsCount();
			if (nParts < 1)
				return null;
			if (multi.getPart(0) instanceof RealPoint) {
				if (nParts == 1)
					return (RealPoint) multi.getPart(0);
				float sumX = 0, sumY = 0;
				boolean allPoints = true;
				for (int i = 0; i < nParts && allPoints; i++) {
					allPoints = multi.getPart(i) instanceof RealPoint;
					RealPoint pt = (RealPoint) multi.getPart(i);
					sumX += pt.x;
					sumY += pt.y;
				}
				if (allPoints)
					return new RealPoint(sumX / nParts, sumY / nParts);
			}
		}
		float c[] = geom.getCentroid();
		if (c != null)
			return new RealPoint(c[0], c[1]);
		float bounds[] = geom.getBoundRect();
		if (bounds == null)
			return null;
		return new RealPoint((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2);
	}

	@Override
	public String toString() {
		String str = "SpatialEntity;id=" + id;
		if (tref != null && tref.getValidFrom() != null)
			if (tref.getValidUntil() == null || tref.getValidUntil().equals(tref.getValidFrom())) {
				str += ";t=" + tref.getValidFrom().toString();
			} else {
				str += ";t=[" + tref.getValidFrom().toString() + "," + tref.getValidUntil().toString() + "]";
			}
		if (geom == null) {
			str += ";geom==null";
		}
		return str;
	}
}