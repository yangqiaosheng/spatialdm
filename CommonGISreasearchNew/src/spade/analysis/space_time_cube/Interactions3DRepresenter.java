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

import spade.analysis.tools.moves.InteractionData;
import spade.time.TimeReference;
import spade.vis.dmap.DAggregateLayer;
import spade.vis.dmap.DAggregateObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Jun 6, 2008
 * Time: 3:30:19 PM
 * Represents interactions from a given DAggregateLayer as java3D objects.
 */
public class Interactions3DRepresenter {
	/**
	 * Represents interactions from a given DAggregateLayer as java3D objects
	 * @param layer - a layer with interactions, instances of DAggregateObject
	 * @param cubeMetr - used for transforming the coordinates
	 * @return a vector of instances of SpaceTimeObject
	 */
	public static Vector<SpaceTimeObject> get3DInteractions(DAggregateLayer layer, CubeMetrics cubeMetr) {
		if (layer == null || layer.getObjectCount() < 1)
			return null;
		if (cubeMetr == null || cubeMetr.timeLen < 1)
			return null;
		DrawingParameters dp = layer.getDrawingParameters();
		ColoringAttributes ca = new ColoringAttributes();
		Color color = dp.lineColor;
		if (color == null) {
			color = Color.darkGray;
		}
		float cc[] = color.getRGBColorComponents(null);
		ca.setColor(cc[0], cc[1], cc[2]);
		Appearance app = new Appearance();
		app.setColoringAttributes(ca);
		LineAttributes la = new LineAttributes();
		la.setLineWidth(dp.lineWidth);
		app.setLineAttributes(la);
		Vector<SpaceTimeObject> result = new Vector<SpaceTimeObject>(layer.getObjectCount(), 1);
		for (int i = 0; i < layer.getObjectCount(); i++)
			if (layer.getObject(i) instanceof DAggregateObject) {
				DAggregateObject aobj = (DAggregateObject) layer.getObject(i);
				if (aobj.getExtraInfo() == null || !(aobj.getExtraInfo() instanceof InteractionData)) {
					continue;
				}
				InteractionData inter = (InteractionData) aobj.getExtraInfo();
				if (inter.points == null || inter.links == null || inter.points.size() < 2 || inter.links.size() < 1) {
					continue;
				}
				Point3d points[] = new Point3d[inter.points.size()];
				int np = 0;
				for (int j = 0; j < inter.points.size(); j++) {
					RealPoint pt = (RealPoint) inter.points.elementAt(j);
					if (pt == null) {
						continue;
					}
					TimeReference tr = (TimeReference) inter.timeRefs.elementAt(j);
					if (tr == null || tr.getValidFrom() == null) {
						continue;
					}
					double x = cubeMetr.cubeX(pt.x, pt.y), y = cubeMetr.cubeY(pt.x, pt.y), z = cubeMetr.cubeZ(tr.getValidFrom());
					points[np++] = new Point3d(x, y, z);
				}
				if (np < 2) {
					continue;
				}
				IndexedLineArray lar = new IndexedLineArray(np, GeometryArray.COORDINATES, inter.links.size() * 2);
				for (int j = 0; j < np; j++) {
					lar.setCoordinate(j, points[j]);
				}
				for (int j = 0; j < inter.links.size(); j++) {
					int link[] = (int[]) inter.links.elementAt(j);
					if (link[0] < 0 || link[0] >= np || link[1] < 0 || link[1] >= np) {
						continue;
					}
					lar.setCoordinateIndex(j * 2, link[0]);
					lar.setCoordinateIndex(j * 2 + 1, link[1]);
				}
				Shape3D inter3D = new Shape3D(lar, app);
				SpaceTimeObject stObj = new SpaceTimeObject(aobj, inter3D);
				stObj.setEntitySetId(layer.getEntitySetIdentifier());
				stObj.setGeoObjectIdxInContainer(i);
				stObj.setGeoObjectIdxInGroup(result.size());
				inter3D.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				result.addElement(stObj);
			}
		if (result.size() < 1)
			return null;
		return result;
	}
}
