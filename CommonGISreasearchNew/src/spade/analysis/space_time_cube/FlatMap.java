package spade.analysis.space_time_cube;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;

import spade.vis.map.Mappable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 25, 2008
 * Time: 6:04:44 PM
 * The flate map to be used in the background of the cube
 */
public class FlatMap extends Shape3D {
	/**
	 * The content of the flat map, which is drawn in the base of the cube
	 */
	protected Mappable map = null;
	/**
	 * Transforms the coordinates for drawing the map
	 */
	protected CubeMetrics mmetr = null;
	/**
	 * The image, in which the map canvas draws the map
	 */
	protected BufferedImage mapImage = null;
	/**
	 * The ImageComponent2d built from this image (used to produce the texture)
	 */
	protected ImageComponent2D im2D = null;
	/**
	 * The texture with the map
	 */
	protected Texture2D mapAsTexture = null;

	/**
	 * Create an opaque flat map
	 */
	public FlatMap(Mappable map, CubeMetrics mmetr, Color bkgColor) {
		this(map, mmetr, bkgColor, 0f);
	}

	/**
	 * Create a flat map
	 */
	public FlatMap(Mappable map, CubeMetrics mmetr, Color bkgColor, float transparency) {
		this.map = map;
		this.mmetr = mmetr;
		this.setGeometry(makePlaneGeometry());

		Appearance app = new Appearance();
		if (map != null) {
			int imSize = 1024;
			mapImage = new BufferedImage(imSize, imSize, BufferedImage.TYPE_INT_ARGB);
			Graphics g = mapImage.getGraphics();
			if (g != null) {
				g.setColor(bkgColor);
				g.fillRect(0, 0, imSize + 1, imSize + 1);
				mmetr.setViewportBounds(0, 0, imSize, imSize);
				map.allowDynamicLoadingWhenDrawn(false);
				map.drawBackground(g, mmetr);
				map.drawForeground(g, mmetr);
				g.dispose();
				map.allowDynamicLoadingWhenDrawn(true);
			}
			im2D = new ImageComponent2D(ImageComponent.FORMAT_RGBA, mapImage);
			mapAsTexture = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA, im2D.getWidth(), im2D.getHeight());
			mapAsTexture.setImage(0, im2D);
			app.setTexture(mapAsTexture);
			this.setAppearance(app);
		} else {
			ColoringAttributes ca = new ColoringAttributes();
			ca.setColor(0f, 1f, 1f);
			app.setColoringAttributes(ca);
		}
		PolygonAttributes polyAppear = new PolygonAttributes();
		polyAppear.setCullFace(PolygonAttributes.CULL_NONE);
		app.setPolygonAttributes(polyAppear);
		if (transparency > 0) {
			TransparencyAttributes trat = new TransparencyAttributes(TransparencyAttributes.NICEST, transparency);
			app.setTransparencyAttributes(trat);
		}
		this.setAppearance(app);
	}

	public QuadArray makePlaneGeometry() {
		QuadArray plane = new QuadArray(4, GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);
		Point3f p = new Point3f();
		p.set(-1.0f, 1.0f, 0.0f);
		plane.setCoordinate(0, p);
		p.set(-1.0f, -1.0f, 0.0f);
		plane.setCoordinate(1, p);
		p.set(1.0f, -1.0f, 0.0f);
		plane.setCoordinate(2, p);
		p.set(1.0f, 1.0f, 0.0f);
		plane.setCoordinate(3, p);
		TexCoord2f q = new TexCoord2f();
		q.set(0.0f, 1.0f);
		plane.setTextureCoordinate(0, 0, q);
		q.set(0.0f, 0.0f);
		plane.setTextureCoordinate(0, 1, q);
		q.set(1.0f, 0.0f);
		plane.setTextureCoordinate(0, 2, q);
		q.set(1.0f, 1.0f);
		plane.setTextureCoordinate(0, 3, q);
		return plane;
	}

	public Dimension getImageSize() {
		if (im2D == null)
			return null;
		return new Dimension(im2D.getWidth(), im2D.getHeight());
	}

	public Shape3D getMapPlaneCopy(float transparency) {
		Shape3D mapPlane = new Shape3D(makePlaneGeometry());
		Appearance app = new Appearance();
		if (mapAsTexture != null) {
			app.setTexture(mapAsTexture);
		} else {
			ColoringAttributes ca = new ColoringAttributes();
			ca.setColor(0.9f, 0.9f, 0.9f);
			PolygonAttributes polyAppear = new PolygonAttributes();
			polyAppear.setCullFace(PolygonAttributes.CULL_NONE);
			app.setColoringAttributes(ca);
			app.setPolygonAttributes(polyAppear);
		}
		PolygonAttributes polyAppear = new PolygonAttributes();
		polyAppear.setCullFace(PolygonAttributes.CULL_NONE);
		app.setPolygonAttributes(polyAppear);
		if (transparency > 0) {
			TransparencyAttributes trat = new TransparencyAttributes(TransparencyAttributes.SCREEN_DOOR, transparency);
			app.setTransparencyAttributes(trat);
		}
		mapPlane.setAppearance(app);
		return mapPlane;
	}
}
