package spade.analysis.space_time_cube;

import java.awt.Color;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedLineArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Node;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;

import spade.time.TimeReference;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.dmap.ObjectAppearance;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealLine;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolyline;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Sep 23, 2009
 * Time: 4:08:11 PM
 * Represents geo objects from a given DGeoLayer as java3D objects
 */
public class GeoObj3DRepresenter {
	/**
	 * Represents geo objects from a given DGeoLayer as java3D objects
	 * @param layer - a layer with geo objects
	 * @param cubeMetr - used for transforming the coordinates
	 * @return  a vector of instances of SpaceTimeObject
	 */
	public static Vector<SpaceTimeObject> get3DObjects(DGeoLayer layer, CubeMetrics cubeMetr) {
		if (layer == null || layer.getObjectCount() < 1)
			return null;
		if (cubeMetr == null || cubeMetr.timeLen < 1)
			return null;
		DrawingParameters dp = layer.getDrawingParameters();
		ColoringAttributes ca = new ColoringAttributes();
		Color color = dp.lineColor;
		if (layer.getType() != spade.vis.geometry.Geometry.line && dp.fillContours && dp.fillColor != null) {
			color = dp.fillColor;
		}
		float cc[] = color.getRGBColorComponents(null);
		ca.setColor(cc[0], cc[1], cc[2]);
		Appearance app = new Appearance();
		app.setColoringAttributes(ca);
		if (layer.getType() == spade.vis.geometry.Geometry.area) {
			LineAttributes la = new LineAttributes();
			la.setLineWidth(dp.lineWidth);
			app.setLineAttributes(la);
			PolygonAttributes pa = new PolygonAttributes();
			if (dp.fillContours) {
				pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
			} else {
				pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
			}
			pa.setCullFace(PolygonAttributes.CULL_NONE);
			app.setPolygonAttributes(pa);
		} else if (layer.getType() == spade.vis.geometry.Geometry.line) {
			LineAttributes la = new LineAttributes();
			la.setLineWidth(dp.lineWidth);
			app.setLineAttributes(la);
		} else if (layer.getType() == spade.vis.geometry.Geometry.point) {
			app.setPointAttributes(new PointAttributes(SpaceTimeObject.pointSize, true));
		}

		Vector<SpaceTimeObject> result = new Vector<SpaceTimeObject>(layer.getObjectCount(), 1);
		boolean layerHasVisualizer = layer.hasVisualizer();
		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObject gobj = layer.getObject(i);
			Node obj3d = make3DObject(gobj.getGeometry(), gobj.getTimeReference(), cubeMetr);
			if (obj3d == null) {
				continue;
			}
			SpaceTimeObject stObj = new SpaceTimeObject(gobj, obj3d);
			stObj.setEntitySetId(layer.getEntitySetIdentifier());
			stObj.setGeoObjectIdxInContainer(i);
			stObj.setGeoObjectIdxInGroup(result.size());
			result.addElement(stObj);
			boolean appIsSet = false;
			if (layerHasVisualizer) {
				ObjectAppearance oap = gobj.getCurrentAppearance();
				if (oap != null) {
					Appearance aobj = new Appearance();
					color = oap.lineColor;
					if (layer.getType() != spade.vis.geometry.Geometry.line && oap.fillColor != null) {
						color = oap.fillColor;
					}
					if (layer.getType() != spade.vis.geometry.Geometry.line && oap.fillColor != null) {
						color = oap.fillColor;
					}
					if (layer.getType() == spade.vis.geometry.Geometry.point && oap.signColor != null) {
						color = oap.signColor;
					}
					float pointSize = (oap.signSize > 0) ? oap.signSize : SpaceTimeObject.pointSize;
					ColoringAttributes caobj = new ColoringAttributes();
					cc = color.getRGBColorComponents(null);
					caobj.setColor(cc[0], cc[1], cc[2]);
					aobj.setColoringAttributes(caobj);
					LineAttributes laobj = new LineAttributes();
					laobj.setLineWidth(oap.lineWidth);
					aobj.setLineAttributes(laobj);
					aobj.setPointAttributes(new PointAttributes(pointSize, true));
					stObj.setAppearance(aobj);
					appIsSet = true;
				}
			}
			if (!appIsSet) {
				stObj.setAppearance(app);
			}
		}
		if (result.size() < 1)
			return null;
		return result;
	}

	protected static Node make3DObject(spade.vis.geometry.Geometry geom, TimeReference tr, CubeMetrics cubeMetr) {
		if (geom == null)
			return null;
		if (tr == null || tr.getValidFrom() == null)
			return null;
		double z = cubeMetr.cubeZ(tr.getValidFrom());
		if (geom instanceof RealPoint) {
			RealPoint p = (RealPoint) geom;
			double x = cubeMetr.cubeX(p.x, p.y), y = cubeMetr.cubeY(p.x, p.y);
			PointArray par = new PointArray(1, GeometryArray.COORDINATES);
			par.setCoordinate(0, new Point3d(x, y, z));
			Shape3D shape3d = new Shape3D(par);
			shape3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
			return shape3d;
/*
      Sphere s=new Sphere(0.01f);
      s.getShape().setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
      Transform3D translate=new Transform3D();
      translate.setTranslation(new Vector3d(x,y,z));
      TransformGroup ts=new TransformGroup();
      ts.addChild(s);
      ts.setCapability(Group.ALLOW_CHILDREN_READ);
      ts.setTransform(translate);
      return ts;
*/
		} else if (geom instanceof RealLine) {
			RealLine line = (RealLine) geom;
			LineArray lar = new LineArray(2, GeometryArray.COORDINATES);
			lar.setCoordinate(0, new Point3d(cubeMetr.cubeX(line.x1, line.y1), cubeMetr.cubeY(line.x1, line.y1), z));
			if (tr.getValidUntil() != null) {
				z = cubeMetr.cubeZ(tr.getValidUntil());
			}
			lar.setCoordinate(1, new Point3d(cubeMetr.cubeX(line.x2, line.y2), cubeMetr.cubeY(line.x2, line.y2), z));
			Shape3D shape3d = new Shape3D(lar);
			shape3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
			return shape3d;
		} else if (geom instanceof RealPolyline) {
			RealPoint line[] = ((RealPolyline) geom).p;
			if (line == null || line.length < 2)
				return null;
			Point3d points[] = new Point3d[line.length];
			int np = 0;
			for (RealPoint element : line) {
				double x = cubeMetr.cubeX(element.x, element.y), y = cubeMetr.cubeY(element.x, element.y);
				points[np++] = new Point3d(x, y, z);
			}
			IndexedLineArray lar = new IndexedLineArray(np, GeometryArray.COORDINATES, np * 2 - 2);
			for (int j = 0; j < np; j++) {
				lar.setCoordinate(j, points[j]);
			}
			lar.setCoordinateIndex(0, 0);
			int vInd = 1;
			for (int j = 1; j < np - 1; j++) {
				lar.setCoordinateIndex(vInd++, j);
				lar.setCoordinateIndex(vInd++, j);
			}
			lar.setCoordinateIndex(vInd, np - 1);
			Shape3D shape3d = new Shape3D(lar);
			shape3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
			return shape3d;
		} else if (geom instanceof MultiGeometry) {
			MultiGeometry mg = (MultiGeometry) geom;
			if (mg.getPartsCount() < 1)
				return null;
			if (mg.getPart(0) instanceof RealPoint) {
				PointArray par = new PointArray(mg.getPartsCount(), GeometryArray.COORDINATES);
				for (int j = 0; j < mg.getPartsCount(); j++) {
					spade.vis.geometry.Geometry part = mg.getPart(j);
					RealPoint p = null;
					if (part instanceof RealPoint) {
						p = (RealPoint) part;
					} else {
						float b[] = part.getBoundRect();
						p = new RealPoint((b[0] + b[2]) / 2, (b[1] + b[3]) / 2);
					}
					double x = cubeMetr.cubeX(p.x, p.y), y = cubeMetr.cubeY(p.x, p.y);
					if (part.getTimeReference() != null) {
						z = cubeMetr.cubeZ(part.getTimeReference().getValidFrom());
					}
					par.setCoordinate(j, new Point3d(x, y, z));
				}
				Shape3D shape3d = new Shape3D(par);
				shape3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				return shape3d;
			}
		}
		return null;
	}
}
