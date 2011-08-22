package spade.analysis.vis3d;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.vis.dmap.MapCanvas;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;

public class MapCanvas3D extends MapCanvas implements EyePositionListener, PropertyChangeListener {
	/**
	* Viewpoint
	*/
	private EyePosition eyePos = null;
	/**
	* MapMetrics contains variables and routines for scaling
	*/
	protected MapMetrics3D mmetr3D = null;
	/**
	* Minimum and maximum Z position (relative)
	*/
	protected float minZ, maxZ;
	/**
	* Z Reference Value
	*/
	protected float ZRef = Float.NaN;

	/**
	* Constructor: preparation to listening of mouse events
	*/
	public MapCanvas3D(EyePosition initialEP, float minZ, float maxZ) {
		super();
		addMouseListener(this);
		addMouseMotionListener(this);
		eyePos = initialEP;
		this.minZ = minZ;
		this.maxZ = maxZ;
		//System.out.println("MapCanvas3D constructed!");
	}

	@Override
	protected void generateMapContext() {
		if (mappable == null)
			return;
		if (mmetr3D == null) {
			mmetr3D = new MapMetrics3D();
		}
		//if (mmetr3D.getVisibleTerritory()==null)
		RealRectangle bounds = mappable.getCurrentTerritoryBounds();
		if (bounds != null) {
			mmetr3D.setVisibleTerritory(bounds);
			mmetr3D.setZLimits(minZ, maxZ);
			mmetr3D.setZ0(0f);
		}
		if (eyePos != null) {
			mmetr3D.setup3D(eyePos.getX(), eyePos.getY(), eyePos.getZ());
		}
	}

	/**
	* Setup MapCanvas3D with new Z dimension
	*/
	public void setupZ(float z_min, float z_max, float z0, float zref) {
		if (mmetr3D != null) {
			mmetr3D.resetZLimits(z_min, z_max);
			mmetr3D.setZ0(z0);
			ZRef = zref;
		}
	}

	/**
	* Sets the Z position of the "flat" map
	*/
	public void setZ0(float z0, float zref) {
		if (mmetr3D != null) {
			mmetr3D.setZ0(z0);
			ZRef = zref;
			imageValid = false;
			redraw();
		}
	}

	/**
	* Sets minimal and maximal Z position values
	* (if changed after visible territory change)
	*/
	public void setMinMaxZ(float z_min, float z_max) {
		if (mmetr3D != null) {
			minZ = z_min;
			maxZ = z_max;
			mmetr3D.setZLimits(z_min, z_max);
			/*
			mmetr3D.setZ0(z0);
			ZRef=zref;
			imageValid=false;
			redraw();
			*/
		}
	}

	/**
	* MapContext provides information necessary for transforming world
	* coordinates into screen coordinates.
	*/
	@Override
	public MapContext getMapContext() {
		return mmetr3D;
	}

	@Override
	protected void draw(Graphics g) {
		if (mappable == null)
			return;
		boolean scaleChanged = false;
		if (mmetr3D == null || mmetr3D.getVisibleTerritory() == null) {
			generateMapContext();
			scaleChanged = true;
		}
		Dimension d = getSize();
		Rectangle r = mmetr3D.getViewportBounds();
		if (r == null || r.width != d.width || r.height != d.height) {
			mmetr3D.setViewportBounds(0, 0, d.width, d.height);
			scaleChanged = mmetr3D.getVisibleTerritory() != null;
		}
		if (scaleChanged) {
			imageValid = false;
		}
		if (offScreenImage == null || !imageValid) {
			paintToImage();
		}
		if (offScreenImage != null) {
			g.drawImage(offScreenImage, 0, 0, null);
		}
		mappable.drawMarkedObjects(g, mmetr3D);
		if (scaleChanged) {
			notifyPropertyChange("MapScale", null, null);
		} else {
			notifyPropertyChange("MapPainting", null, null);
			//System.out.println("MapCanvas3D:: draw()");
		}
	}

	/**
	* Commands the mappable to draw itself to the bitmap in the memory
	*/
	@Override
	public void paintToImage() {
		if (offScreenImage != null && imageValid)
			return;
		Dimension d = getSize();
		Graphics g = null;
		if (offScreenImage == null || d.width != offScreenImage.getWidth(null) || d.height != offScreenImage.getHeight(null)) {
			offScreenImage = createImage(d.width, d.height);
		}
		if (offScreenImage != null) {
			g = offScreenImage.getGraphics();
		}
		if (g == null) {
			offScreenImage = null;
			g = getGraphics();
		}
		g.setColor(getBackground());
		g.fillRect(0, 0, d.width + 1, d.height + 1);
		mappable.drawBackground(g, mmetr3D);
		// put comparison value on the 3D display
		/* g.setColor(Color.blue);
		if (mmetr3D.getZ0()!=minZ || mmetr3D.getZ0()!=maxZ) {
		  java.awt.FontMetrics fm=g.getFontMetrics();
		  String sZRefValue=StringUtil.floatToStr(ZRef,3);
		  g.drawString(sZRefValue,d.width-fm.stringWidth(sZRefValue)-5,d.height-15);
		} */
		g.dispose();
		imageValid = offScreenImage != null;
	}

	/*
	* In repainting the map, uses the earlier stored background and makes
	* the mappable component redraw only the foreground 8diagrams, labels etc.)
	*/
	@Override
	protected void redrawForeground() {
		if (bkgImage == null || offScreenImage == null) { //redraw all
			imageValid = false;
			redraw();
		} else {
			Graphics g = offScreenImage.getGraphics();
			if (g != null) {
				g.drawImage(bkgImage, 0, 0, null);
				mappable.drawBackground(g, mmetr3D);
				mappable.drawForeground(g, mmetr3D);
				Graphics gr = getGraphics();
				if (gr != null) {
					gr.drawImage(offScreenImage, 0, 0, null);
					//mappable.drawMarkedObjects(gr,mmetr3D);
					gr.dispose();
				}
				g.dispose();
			}
		}
	}

	/**
	* A method from the MapDraw interface. Copies the content of its
	* bitmap stored in the memory to the screen.
	* This method is used to clear all kinds of marking (e.g. highlighting)
	* drawn on top of the original map.
	*/
	@Override
	public void restorePicture() {
		imageValid = false;
		redraw();
	}

	/**
	* The method returns false if the current scale cannot be increased.
	* This and following methods can be used, for example, to disable or hide
	* the corresponding buttons. By default, all these methods return true.
	*/
	@Override
	public boolean canZoomIn() {
		return false;
	}

	/**
	* The method returns false if the current scale cannot be decreased.
	*/
	@Override
	public boolean canZoomOut() {
		return false;
	}

	/**
	* The method replies whether the current viewport can be moved in the
	* specified direction.
	* "where" should be one of "North", "South", "East", "West"
	*/
	@Override
	public boolean canMove(String where) {
		return false;
	}

	/**
	* The method returns false if the component does not support the "pan"
	* operation, e.g. drawing of the whole territory in the current viewport.
	*/
	@Override
	public boolean canPan() {
		return false;
	}

	/**
	* Fits the whole represented territory or image to the size of the viewport.
	*/
	@Override
	public void pan() {
	}

	/**
	* "Moves" the viewport over the shown territory (image etc.) by 25% in the
	* specified direction.
	* "where" should be one of "North", "South", "East", "West"
	*/
	@Override
	public void move(String where) {
	}

	/**
	* "Moves" the viewport over the shown territory (image etc.) by
	* the specified horizontal (dx) and vertical (dy) offsets. The offsets
	* are given in screen coordinates.
	*/
	@Override
	public void move(int dx, int dy) {
	}

	/**
	* Enlarges the part of the image specified by the rectangle to the
	* maximum possible scale at which the rectangle still fits in the
	* viewport.
	*/
	@Override
	public void zoomInRectangle(int x, int y, int width, int height) {
	}

	/**
	* Increases the scale by the given factor. If the value of "factor" is
	* between 0 and 1, the scale is actually decreased. The "factor"
	* must be a positive number.
	*/
	@Override
	public void zoomByFactor(float factor) {
	}

	/**
	* Cancels the last zooming operation
	*/
	@Override
	public void undo() {
	}

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* particular types of events.
	*/
	@Override
	public String getIdentifier() {
		return "MapCanvas3D_" + instanceN;
	}

	@Override
	public void eyePositionChanged(EyePosition ep) {
		if (ep == null)
			return;
		eyePos = ep;
		if (mmetr3D == null)
			return;
		mmetr3D.setup3D(ep.getX(), ep.getY(), ep.getZ());
		imageValid = false;
		repaint();
		//System.out.println("MapCanvas3D updated with new viewpoint's position");
	}

	/**
	* Here the parent class detects in which map (among multiple maps the mouse
	* cursor is currently located. For MapCanvas3D this is irrelevant.
	*/
	@Override
	protected void detectMouseLocMap(MouseEvent e) {
		mouseLocMapN = 0;
		mouseLocMapBounds = getBounds();
	}

	/**
	* This function is fired when properties of some of the GeoLayers change.
	* If in original layer the scale changed, this
	* is influenced also to the special layer that is shown in 3D.
	* MapCanvas3D will be updated to show the only area currently visible
	* on the map with original layer.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		String prName = pce.getPropertyName();
		super.propertyChange(pce);
	}

	@Override
	public void setVisibleTerritory(RealRectangle rrVisTerr) {
		if (rrVisTerr != null) {
			mmetr3D.setVisibleTerritory(rrVisTerr);
			imageValid = false;
			repaint();
			// System.out.println("MapCanvas3D updated: new visible territory in main map window!");
		}
	}
}
