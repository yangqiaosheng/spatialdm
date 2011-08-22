package spade.analysis.space_time_cube;

import java.awt.Color;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedLineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;

import spade.lib.util.IntArray;
import spade.time.TimeReference;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DMovingObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.ObjectAppearance;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 28, 2008
 * Time: 11:23:46 AM
 * Represents trajectories from a given DGeoLayer as java3D objects (line arrays)
 */
public class Tracks3DRepresenter {
	/**
	 * Represents trajectories from a given DGeoLayer as java3D objects (line arrays)
	 * @param moveLayer - a layer with trajectories, instances of DMovingObject
	 * @param cubeMetr - used for transforming the coordinates
	 * @return  a vector of instances of SpaceTimeObject
	 */
	public static Vector<SpaceTimeObject> get3DTracks(DGeoLayer moveLayer, CubeMetrics cubeMetr) {
		if (moveLayer == null || moveLayer.getObjectCount() < 1)
			return null;
		if (cubeMetr == null || cubeMetr.timeLen < 1)
			return null;
		DrawingParameters dp = moveLayer.getDrawingParameters();
		ColoringAttributes ca = new ColoringAttributes();
		Color color = dp.lineColor;
		float cc[] = color.getRGBColorComponents(null);
		ca.setColor(cc[0], cc[1], cc[2]);
		Appearance app = new Appearance();
		app.setColoringAttributes(ca);
		LineAttributes la = new LineAttributes();
		la.setLineWidth(dp.lineWidth);
		app.setLineAttributes(la);
		Vector<SpaceTimeObject> result = new Vector<SpaceTimeObject>(moveLayer.getObjectCount(), 1);
		boolean layerHasVisualizer = moveLayer.hasVisualizer();
		IntArray cross180idx = (moveLayer.isGeographic()) ? new IntArray(10, 10) : null;
		Vector<Point3d> points = new Vector<Point3d>(100, 100);
		for (int i = 0; i < moveLayer.getObjectCount(); i++)
			if ((moveLayer.getObject(i) instanceof DMovingObject)/* && moveLayer.isObjectActive(i)*/) {
				DMovingObject mobj = (DMovingObject) moveLayer.getObject(i);
				Vector track = mobj.getTrack();
				if (track == null || track.size() < 2) {
					continue;
				}
				if (cross180idx != null) {
					cross180idx.removeAllElements();
				}
				points.removeAllElements();
				RealPoint pt0 = null;
				for (int j = 0; j < track.size(); j++) {
					SpatialEntity spe = (SpatialEntity) track.elementAt(j);
					if (spe == null) {
						continue;
					}
					RealPoint pt = spe.getCentre();
					if (pt == null) {
						continue;
					}
					TimeReference tr = spe.getTimeReference();
					if (tr == null || tr.getValidFrom() == null) {
						continue;
					}
					double x = cubeMetr.cubeX(pt.x, pt.y), y = cubeMetr.cubeY(pt.x, pt.y), z = cubeMetr.cubeZ(tr.getValidFrom());
					if (pt0 != null && cross180idx != null && DGeoObject.cross180Meridian(pt0.x, pt.x)) {
						float xx[] = DGeoObject.breakLine(pt0.x, pt.x);
						double x_2 = cubeMetr.cubeX(xx[0], pt.y), x_1 = cubeMetr.cubeX(xx[1], pt0.y);
						points.addElement(new Point3d(x_2, y, z));
						cross180idx.addElement(points.size() - 1);
						points.addElement(new Point3d(x_1, y, z));
					}
					points.addElement(new Point3d(x, y, z));
					pt0 = pt;
				}
				if (points.size() < 2) {
					continue;
				}
				int indexCount = points.size() * 2 - 2;
				if (cross180idx != null) {
					indexCount -= cross180idx.size() * 2;
				}
				IndexedLineArray lar = new IndexedLineArray(points.size(), GeometryArray.COORDINATES, indexCount);
				for (int j = 0; j < points.size(); j++) {
					lar.setCoordinate(j, points.elementAt(j));
				}
				lar.setCoordinateIndex(0, 0);
				int vInd = 1;
				for (int j = 1; j < points.size() - 1; j++) {
					if (cross180idx == null || cross180idx.indexOf(j - 1) < 0) {
						lar.setCoordinateIndex(vInd++, j);
					}
					if (cross180idx == null || cross180idx.indexOf(j) < 0) {
						lar.setCoordinateIndex(vInd++, j);
					}
				}
				lar.setCoordinateIndex(vInd, points.size() - 1);
				Shape3D track3D = new Shape3D(lar, app);
				if (layerHasVisualizer) {
					ObjectAppearance oap = mobj.getCurrentAppearance();
					if (oap != null) {
						Appearance aobj = new Appearance();
						ColoringAttributes caobj = new ColoringAttributes();
						cc = oap.lineColor.getRGBColorComponents(null);
						caobj.setColor(cc[0], cc[1], cc[2]);
						aobj.setColoringAttributes(caobj);
						LineAttributes laobj = new LineAttributes();
						laobj.setLineWidth(oap.lineWidth);
						aobj.setLineAttributes(laobj);
						track3D.setAppearance(aobj);
					}
				}
				SpaceTimeObject stObj = new SpaceTimeObject(mobj, track3D);
				stObj.setEntitySetId(moveLayer.getEntitySetIdentifier());
				stObj.setGeoObjectIdxInContainer(i);
				stObj.setGeoObjectIdxInGroup(result.size());
				track3D.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				result.addElement(stObj);
			}
		if (result.size() < 1)
			return null;
		return result;
	}
}
