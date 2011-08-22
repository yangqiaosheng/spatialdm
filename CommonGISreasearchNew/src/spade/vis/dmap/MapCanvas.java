package spade.vis.dmap;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.EventObject;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.HotSpot;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.color.ColorDlg;
import spade.lib.color.ColorListener;
import spade.lib.lang.Language;
import spade.lib.util.StringUtil;
import spade.vis.event.DEvent;
import spade.vis.event.DMouseEvent;
import spade.vis.event.DelayEventThread;
import spade.vis.event.DelayedEventListener;
import spade.vis.event.EventConsumer;
import spade.vis.event.EventManager;
import spade.vis.event.EventMeaningManager;
import spade.vis.event.EventReceiver;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.Legend;
import spade.vis.map.LegendDrawer;
import spade.vis.map.MapContext;
import spade.vis.map.MapDraw;
import spade.vis.map.Mappable;
import spade.vis.map.Zoomable;

public class MapCanvas extends Canvas implements MapDraw, Zoomable, Destroyable, ColorListener, PropertyChangeListener, LegendDrawer, ActionListener, MouseListener, MouseMotionListener, DelayedEventListener, EventConsumer {
	static ResourceBundle res = Language.getTextResource("spade.vis.dmap.Res");
	/**
	* Used to generate unique identifiers of instances of MapCanvas
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;
//----------------- notification about properties changes---------------
	/**
	* A MapCanvas may have listeners of its properties changes, for example,
	* a component used to zoom the map.
	* To handle the list of listeners and notify them about changes of the
	* properties (e.g. color of drawing or parameters of the Visualizer),
	* a MapCanvas uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	protected float minPixelValue = Float.NaN;

	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
	}

	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
	}

	protected void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(propName, oldValue, newValue);
	}

	/**
	* Mappable is an entity that draws itself in the map and thereby completely
	* defines the content of the map.
	*/
	protected Mappable mappable = null;
	/**
	* Legend is a component that draws the legend of the map.
	*/
	protected Legend legend = null;
	/**
	* EventManager helps the MapCanvas in distributing mouse events among
	* MapListeners. If an object wishes to listen to mouse events occurring
	* on the map, it should implement the MapListener interface and register
	* at the EventManager of this MapCanvas.
	* A MapCanvas always has an EventManager.
	*/
	protected EventManager evtMan = null;
	/**
	* EventMeaningManager helps to handle mouse events that may have alternative
	* meanings, e.g. mouse drag may be used for zooming, shifting, and selection.
	* One of the interpretation is current.
	*/
	protected EventMeaningManager evtMeanMan = null;
	/**
	* Screen coordinates of the mouse at the moment of beginning of dragging.
	*/
	private int x0 = -1, y0 = -1, prevX = -1, prevY = -1;
	private boolean mouseIsDragging = false;
	/**
	* The thread is used for delayed reaction to mouse pressed events in order to
	* capture events of a mouse button being pressed for a long time. The thread
	* is invoked when a MousePressed event occurs. It waits for a specified time
	* and then, if not cancelled, calls the method DelayExpired. The thread is
	* cancelled when the mouse button is released or the mouse is dragged.
	*/
	private DelayEventThread delayer = null;
	/**
	* MapMetrics contains variables and routines for scaling
	*/
	protected MapMetrics mmetr = null;
	/**
	* A bitmap in the memory where the map copy is stored - for quick restoration
	* after highlighting/selection
	*/
	protected Image offScreenImage = null;
	/**
	* A bitmap in the memory where a partly drawn map is stored (typically
	* background, without diagrams and labels). This is done for optimization
	* of the speed of redrawing when in the course of interactive exploration
	* only diagrams change.
	* To make the map canvas use the saved background when it is redrawn, the
	* mappable component sends a property change event with the name "foreground".
	* An event with any other property name makes the map canvas repaint all,
	* including the background.
	*/
	protected Image bkgImage = null;
	protected boolean imageValid = false;
	/**
	* Previous visible territory boundaries - used for "undo" zoom operation
	*/
	protected RealRectangle prevBounds = null;
	/**
	 * The absolute spatial (geographic) position that needs to be marked on the map.
	 */
	protected float markedPosX = Float.NaN, markedPosY = Float.NaN;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* This is needed to define active (clickable) area in legend's canvas:
	* in this case to invoke ColorDlg for changing background color of the map
	*/
	protected HotSpot hsInLegend = null;

	/**
	* Constructor: preparation to listening of mouse events
	*/
	public MapCanvas() {
		instanceN = ++nInstances;
//    addMouseListener(this);
//    addMouseMotionListener(this);
		setBackground(Color.lightGray); // default application background color
		//setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (!(ae.getSource() instanceof HotSpot))
			return;
		Color cBg = getBackground();
		// following string: "Choose background color for project"
		ColorDlg cDlg = new ColorDlg(CManager.getAnyFrame(), res.getString("Choose_background"));
		cDlg.selectColor(this, hsInLegend, cBg);
	}

	protected int prefW = 200, prefH = 200;

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(prefW, prefH);
	}

	public void setPreferredSize(int w, int h) {
		prefW = w;
		prefH = h;
	}

	/**
	* MapContext provides information necessary for transforming world
	* coordinates into screen coordinates.
	*/
	@Override
	public MapContext getMapContext() {
		if (mmetr == null) {
			generateMapContext();
		}
		return mmetr;
	}

	protected void generateMapContext() {
		if (mappable == null)
			return;
		if (mmetr == null) {
			mmetr = new MapMetrics();
		}
		if (mmetr.getVisibleTerritory() == null) {
			mmetr.setGeographic(mappable.isGeographic());
			mmetr.setVisibleTerritory(mappable.getCurrentTerritoryBounds());
		}
	}

	/**
	* Makes preparations for drawing the map with the given index in the
	* "small multiples"
	*/
	protected void prepareDrawMap(Graphics g, int mapIdx) {
		mmetr.setCurrentMapN(mapIdx);
		mappable.setCurrentMapN(mapIdx);
		Rectangle mbounds = mmetr.getMapBounds(mapIdx);
		g.setClip(mbounds.x, mbounds.y, mbounds.width, mbounds.height);
	}

	/**
	* Draws the name of the map with the given index in the
	* "small multiples"
	*/
	protected void drawMapName(Graphics g, int mapIdx) {
		String mapName = mappable.getMapName(mapIdx);
		if (mapName != null) {
			int x0 = 3, y0 = 3;
			Rectangle r = g.getClipBounds();
			if (r != null) {
				x0 = r.x + 3;
				y0 = r.y + 3;
			}
			FontMetrics fm = g.getFontMetrics();
			y0 += fm.getAscent();
			g.setColor(Color.white);
			g.drawString(mapName, x0, y0);
			g.setColor(Color.black);
			g.drawString(mapName, x0 - 1, y0 - 1);
		}
	}

	/**
	* Draws the mappable in the given graphical context. Depending on the
	* arguments, may draw only background, only foreground, and both.
	* The background image may be internally remembered, depending on the value
	* of the argument storeBackground.
	* The arguments width and height specify the width and height of the
	* viewport. For "small multiples" this is the area where all the maps must
	* be fit.
	*/
	public void drawMap(Graphics g, boolean drawBackground, boolean drawForeground, boolean storeBackground, int width, int height) {
		int nMaps = mappable.getMapCount();
		mmetr.setMapCount(nMaps);
		if (drawBackground) {
			if (nMaps <= 1) {
				mappable.drawBackground(g, mmetr);
			} else {
				for (int i = 0; i < nMaps; i++) {
					prepareDrawMap(g, i);
					mappable.drawBackground(g, mmetr);
					drawMapName(g, i);
				}
				g.setClip(0, 0, width, height);
			}
			if (storeBackground) {
				makeBackgroundImage();
			}
		}
		if (drawForeground) {
			if (nMaps <= 1) {
				mappable.drawForeground(g, mmetr);
			} else {
				for (int i = 0; i < nMaps; i++) {
					prepareDrawMap(g, i);
					mappable.drawForeground(g, mmetr);
					drawMapName(g, i);
				}
				g.setClip(0, 0, width, height);
			}
		}
	}

	/**
	* Stores a partly drawn map in a bitmap in the memory (typically only the
	* background, without diagrams and labels). This is done for optimization
	* of the speed of redrawing when in the course of interactive exploration
	* only diagrams change.
	*/
	protected void makeBackgroundImage() {
		if (offScreenImage == null)
			return;
		int w = offScreenImage.getWidth(null), h = offScreenImage.getHeight(null);
		if (bkgImage == null || bkgImage.getWidth(null) != w || bkgImage.getHeight(null) != h) {
			bkgImage = createImage(w, h);
		}
		if (bkgImage == null)
			return;
		Graphics img = bkgImage.getGraphics();
		img.drawImage(offScreenImage, 0, 0, null);
		img.dispose();
	}

	public Image getBackgroundImage() {
		if (offScreenImage == null)
			return null;
		int w = offScreenImage.getWidth(null), h = offScreenImage.getHeight(null);
		Image bkIm = createImage(w, h);
		if (bkIm == null)
			return null;
		Graphics img = bkIm.getGraphics();
		mappable.drawBackground(img, mmetr);
		img.dispose();
		return bkIm;
	}

	/**
	 * Forces the map canvas to redraw the background image
	 */
	public void invalidateImage() {
		imageValid = false;
	}

	/**
	* Asks the mappable to draw itself to the bitmap in the memory
	*/
	public void paintToImage() {
		if (offScreenImage != null && imageValid)
			return;
		Dimension d = getSize();
		if (d == null || d.width < 10 || d.height < 10)
			return;
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
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(getBackground());
		g.fillRect(0, 0, d.width + 1, d.height + 1);
		drawMap(g, true, true, true, d.width, d.height);
		g.dispose();
		imageValid = offScreenImage != null;
	}

	/**
	* Draws the map content to the specified bitmap in the memory
	*/
	@Override
	public void paintToImage(Image img, int width, int height) {
		if (img == null)
			return;
		Graphics g = img.getGraphics();
		if (g == null)
			return;
		g.setColor(getBackground());
		g.fillRect(0, 0, width + 1, height + 1);
		Rectangle r = mmetr.getViewportBounds();
		mmetr.setViewportBounds(0, 0, width, height);
		drawMap(g, true, true, false, width, height);
		mmetr.setViewportBounds(r);
		g.dispose();
	}

	/**
	 * Returns the current size of the map on the screen
	 */
	@Override
	public Dimension getMapSize() {
		return getSize();
	}

	/**
	* Returns the image with the drawn map
	*/
	@Override
	public Image getMapImage() {
		if (offScreenImage == null) {
			paintToImage();
		}
		return offScreenImage;
	}

	/**
	* If there is a bitmap in the memory, and it is valid, copies the
	* content of the bitmap. If not, first paints to the bitmap, and then
	* copies its content to the screen.
	*/
	@Override
	public void paint(Graphics g) {
		draw(g);
	}

	protected void draw(Graphics g) {
		clearMarkedPosition();
		if (mappable == null)
			return;
		boolean scaleChanged = false;
		if (mmetr == null || mmetr.getVisibleTerritory() == null) {
			generateMapContext();
			scaleChanged = true;
		}
		Dimension d = getSize();
		Rectangle r = mmetr.getViewportBounds();
		if (r == null || r.width != d.width || r.height != d.height) {
			mmetr.setViewportBounds(0, 0, d.width, d.height);
			scaleChanged = mmetr.getVisibleTerritory() != null;
		}
		if (scaleChanged) {
			imageValid = false;
		}
		//System.out.println("Map drawing; map pixel value="+mmetr.getPixelValue());
		if (offScreenImage == null || !imageValid) {
			paintToImage();
		}
		if (offScreenImage != null) {
			g.drawImage(offScreenImage, 0, 0, null);
		}
		mmetr.setMapCount(mappable.getMapCount());
		mappable.drawMarkedObjects(g, mmetr);
		if (scaleChanged) {
			if (legend != null) {
				legend.redraw();
			}
			notifyPropertyChange("MapScale", null, mmetr.getVisibleTerritory());
		} else {
			notifyPropertyChange("MapPainting", null, null);
		}
	}

	@Override
	public void redraw() {
		Graphics g = getGraphics();
		if (g != null) {
			draw(g);
			g.dispose();
		}
	}

	@Override
	public Image getMapAsImage() {
		Dimension size = getSize();
		if (size == null || size.width < 1 || size.height < 1)
			return null;
		Image img = createImage(size.width, size.height);
		if (img == null)
			return null;
		Graphics g = img.getGraphics();
		if (g == null)
			return null;
		draw(g);
		g.dispose();
		return img;
	}

	/**
	*   printing map and legend
	*/
	final float cm = java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 2.533f;
	final int icm = Math.round(cm);
	final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	final int field = 15 * mm;

	@Override
	public void print(Graphics g) {
		Dimension d = getSize();
		print(g, d, false, false, false, null);
	}

	public Image print(Graphics g, Dimension size, boolean fitToPage, boolean useFields, boolean copyrighted, String about_project) {
		if (size == null)
			return null;
		Image imgOffScreen = null;
		Graphics gImg = null;
		int w = size.width - (useFields ? 2 * field : 0);
		int h = size.height - (useFields ? 2 * field : 0);
		if (useFields) {
			g.translate(field, field);
		}

		Rectangle rMapViewport = mmetr.getViewportBounds();
		int viewportW = rMapViewport.width, viewportH = rMapViewport.height;
		//System.out.println("Map Viewport w="+viewportW+" h="+viewportH);
		String crString = "Map exported from V-Analytics/CommonGIS";
		int crX = g.getFontMetrics().stringWidth(crString);
		int crY = g.getFontMetrics().getHeight();
		int crShiftH = 7 + crY;

		if (fitToPage) {
			w = h = ((size.width < size.height) ? size.width : size.height) - 2 * field;
		}

		imgOffScreen = createImage(w, h);
		if (imgOffScreen != null) {
			gImg = imgOffScreen.getGraphics();
		}
		if (gImg == null) {
			gImg = getGraphics();
		}

		int indent = 5;
		if (copyrighted) {
			mmetr.setViewportBounds(indent, indent, w - 2 * indent, h - 2 * indent - crY);
		} else {
			mmetr.setViewportBounds(indent, indent, w - 2 * indent, h - 2 * indent);
		}

		drawMap(gImg, true, true, false, w, h);
		mappable.drawMarkedObjects(gImg, mmetr);

		// Draw a frame around graphics, print copyright notice in lower right corner
		if (copyrighted) {
			gImg.setColor(Color.black);
			gImg.drawRect(0, 0, w - 1, h - 1);
			gImg.drawRect(1, 1, w - 3, h - 3);
			gImg.setColor(Color.white);
			gImg.drawRect(2, 2, w - 5, h - 5);
			gImg.drawRect(3, 3, w - 7, h - 7);
			gImg.setClip(w - 10 - crX - 1, h - 4 - crY, crX + 2, crY);

			gImg.setColor(Color.white);
			gImg.drawString(crString, w - 10 - crX, h - 7);
			gImg.setColor(Color.black);
			gImg.drawString(crString, w - 10 - crX - 1, h - 7 - 1);
			gImg.setClip(0, 0, w, h);

			if (about_project != null && !about_project.equals("")) {
				int numberOfLines = 0;
				int maxLength = 0;
				StringTokenizer st = new StringTokenizer(about_project, "\n");
				while (st.hasMoreTokens()) {
					String s = st.nextToken();
					int length = g.getFontMetrics().stringWidth(s);
					if (length > maxLength) {
						maxLength = length;
					}
					numberOfLines++;
				}
				gImg.setColor(Color.white);
				StringInRectangle.drawText(gImg, about_project, w - 10 - maxLength, h - crY - 2 - numberOfLines * crY, maxLength, false);
				gImg.setColor(Color.black);
				StringInRectangle.drawText(gImg, about_project, w - 10 - maxLength - 1, h - crY - 2 - numberOfLines * crY - 1, maxLength, false);
			}
		}
		g.drawImage(imgOffScreen, 0, 0, null);
		return imgOffScreen;
	}

	public Image printLegend(Graphics g, Dimension size, boolean useFields) {
		if (size == null)
			return null;
		Image imgOffScreen = null;
		Graphics gImg = null;
		int w = size.width - (useFields ? 2 * field : 0);
		int h = size.height - (useFields ? 2 * field : 0);
		if (useFields) {
			g.translate(field, field);
		}
		imgOffScreen = createImage(w, h);
		if (imgOffScreen != null) {
			gImg = imgOffScreen.getGraphics();
		}
		if (gImg == null) {
			gImg = getGraphics();
		}
		gImg.setColor(Color.white);
		gImg.fillRect(0, 0, w, h);

		mappable.drawLegend(null, gImg, field, field, w);

		g.drawImage(imgOffScreen, 0, 0, null);
		return imgOffScreen;
	}

	public Image printMapAndLegend(Graphics g, Dimension size, boolean fitToPage, boolean useFields, boolean copyrighted, String about_project) {
		if (size == null)
			return null;
		Image imgOffScreen = null;
		Graphics gImg = null;
		int w = size.width - (useFields ? 2 * field : 0);
		int h = size.height - (useFields ? 2 * field : 0);
		if (useFields) {
			g.translate(field, field);
		}
		imgOffScreen = createImage(w, h);
		if (imgOffScreen != null) {
			gImg = imgOffScreen.getGraphics();
		}
		if (gImg == null) {
			gImg = getGraphics();
		}
		gImg.setColor(Color.white);
		gImg.fillRect(0, 0, w, h);

		Rectangle legendSize = mappable.drawLegend(null, gImg, 0, 0, w);
		h -= legendSize.height;
		gImg.translate(0, legendSize.height);

		if (size == null)
			return null;
		int indent = 5;
		Rectangle rMapViewport = mmetr.getViewportBounds();
		int viewportW = rMapViewport.width, viewportH = rMapViewport.height;
		//System.out.println("Map Viewport w="+viewportW+" h="+viewportH);
		String crString = "Map exported from V-Analytics/CommonGIS";
		int crX = g.getFontMetrics().stringWidth(crString);
		int crY = g.getFontMetrics().getHeight();
		int crShiftH = 7 + crY;
		if (fitToPage) {
			w = h = ((size.width < size.height) ? size.width : size.height) - 2 * field;
		}
		if (copyrighted) {
			mmetr.setViewportBounds(indent, indent, w - 2 * indent, h - 2 * indent - crY);
		} else {
			mmetr.setViewportBounds(indent, indent, w - 2 * indent, h - 2 * indent);
		}
		gImg.setColor(getBackground());
		gImg.fillRect(0, 0, w, h);
		gImg.setClip(0, 0, w, h);
		drawMap(gImg, true, true, false, w, h);
		mappable.drawMarkedObjects(gImg, mmetr);

		// Draw a frame around graphics, print copyright notice in lower right corner
		if (copyrighted) {
			gImg.setColor(Color.black);
			gImg.drawRect(0, 0, w - 1, h - 1);
			gImg.drawRect(1, 1, w - 3, h - 3);
			gImg.setColor(Color.white);
			gImg.drawRect(2, 2, w - 5, h - 5);
			gImg.drawRect(3, 3, w - 7, h - 7);

			gImg.setClip(w - 10 - crX - 1, h - 4 - crY, crX + 2, crY);
			gImg.setColor(Color.white);
			gImg.drawString(crString, w - 10 - crX, h - 7);
			gImg.setColor(Color.black);
			gImg.drawString(crString, w - 10 - crX - 1, h - 7 - 1);
			gImg.setClip(0, 0, w, h);

			if (about_project != null && !about_project.equals("")) {
				int numberOfLines = 0;
				int maxLength = 0;
				StringTokenizer st = new StringTokenizer(about_project, "\n");
				while (st.hasMoreTokens()) {
					String s = st.nextToken();
					int length = g.getFontMetrics().stringWidth(s);
					if (length > maxLength) {
						maxLength = length;
					}
					numberOfLines++;
				}
				gImg.setColor(Color.white);
				StringInRectangle.drawText(gImg, about_project, w - 10 - maxLength, h - crY - 2 - numberOfLines * crY, maxLength, false);
				gImg.setColor(Color.black);
				StringInRectangle.drawText(gImg, about_project, w - 10 - maxLength - 1, h - crY - 2 - numberOfLines * crY - 1, maxLength, false);
			}
		}
		g.drawImage(imgOffScreen, 0, 0, null);
		return imgOffScreen;
	}

	/*
	* In repainting the map, uses the earlier stored background and makes
	* the mappable component redraw only the foreground 8diagrams, labels etc.)
	*/
	protected void redrawForeground() {
		clearMarkedPosition();
		if (bkgImage == null || offScreenImage == null) { //redraw all
			imageValid = false;
			redraw();
		} else {
			Dimension d = getSize();
			Graphics g = offScreenImage.getGraphics();
			if (g != null) {
				g.drawImage(bkgImage, 0, 0, null);
				drawMap(g, false, true, false, d.width, d.height);
				Graphics gr = getGraphics();
				if (gr != null) {
					gr.drawImage(offScreenImage, 0, 0, null);
					mappable.drawMarkedObjects(gr, mmetr);
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
		clearMarkedPosition();
		if (offScreenImage != null) {
			Graphics g = getGraphics();
			if (g != null) {
				g.drawImage(offScreenImage, 0, 0, null);
				g.dispose();
				notifyPropertyChange("MapPainting", null, null);
			}
		}
	}

	/**
	* The method from the LegendDrawer interface. First of all, the MapCanvas
	* activates the method drawLegend of its Mappable (content) component.
	* The MapCanvas can also draw in the legend itself,
	* for example, the current scale in the legend.
	*/

	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftmarg, int prefW) {
		if (mappable == null)
			return null;
		//((Canvas)legend).setBackground(Color.white);
		Rectangle r = mappable.drawLegend(c, g, startY, leftmarg, prefW);
		Point p = null;
		int gap = 2 * mm, width = 8 * mm, height = 5 * mm, y = r.y + r.height + gap, maxW = 0;
		// following string: "Background"
		String sChBgColorLabel = res.getString("Background");

		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(sChBgColorLabel);
		Color origC = g.getColor();

		if (mappable != null && mmetr != null) {
			float fScale = Float.NaN;
			// following string: "Territory: "
			String sMasstab = "", terrName = res.getString("Territory_");
			DLayerManager lman = null;
			if (mappable instanceof DLayerManager) {
				lman = (DLayerManager) mappable;
			}
			if (lman != null) {
				// Printing the name of a territory
				if (lman.terrName != null && lman.show_terrname /* and if territory name is shown */) {
					terrName += lman.terrName;
					g.setColor(Color.black);
					p = StringInRectangle.drawText(g, terrName, leftmarg, y, prefW - leftmarg, false);
					y = p.y;
					if (p.x > maxW) {
						maxW = p.x;
					}
				}
				// Drawing background color box
				if (lman.show_bgcolor /* background is shown */) {
					g.setColor(getBackground());
					g.fillRect(leftmarg, y, width, height);
					g.setColor(Color.black);
					g.drawRect(leftmarg, y, width, height);
					w += width + gap;
					p = StringInRectangle.drawText(g, sChBgColorLabel, leftmarg + width + gap, y + height / 2 - fm.getHeight() / 2, prefW - leftmarg - height - gap, false);
					if (hsInLegend == null) {
						hsInLegend = new HotSpot((java.awt.Component) legend);
						hsInLegend.addActionListener(this);
						// following string: "Click to change"
						hsInLegend.setPopup(res.getString("Click_to_change"));
					}
					hsInLegend.setLocation(leftmarg, y);
					hsInLegend.setSize(w, height);
					if (p.y < y + height) {
						y += height + gap;
					} else {
						y = p.y + fm.getAscent();
					}
					if (p.x > maxW) {
						maxW = p.x;
					}
				}
				// Drawing scale
				if (lman.show_scale /* scale is shown */) {
					g.setColor(Color.black);
					fScale = mmetr.getPixelValue() * cm * lman.user_factor;
					//System.out.println("Legend drawing; map pixel value="+mmetr.getPixelValue());
					sMasstab += StringUtil.floatToStr(fScale, 2) + " " + lman.user_unit;
					p = StringInRectangle.drawText(g, sMasstab, leftmarg, y, prefW - leftmarg, false);
					if (p.x > maxW) {
						maxW = p.x;
					}
					y = p.y;
					g.drawLine(leftmarg, y, leftmarg, y + gap);
					g.drawLine(leftmarg, y + gap / 2, leftmarg + icm, y + gap / 2);
					g.drawLine(leftmarg + icm, y, leftmarg + icm, y + gap);
					y += gap + 5;
				}
			}
		}
		g.setColor(origC);
		return new Rectangle(startY, 0, maxW, y - startY);
	}

	/**
	* A MapCanvas has references to a Mappable and a Legend.
	* When a Mappable and a Legend is set in the MapCanvas, it
	* 1) registers itself as a legend drawer of the Legend;
	* 2) registers itself and the Legend as listeners of properties changes
	*    of the Mappable.
	*/
	public void setMapContent(Mappable mapContent) {
		if (mappable == mapContent)
			return;
		if (mappable != null) {
			mappable.removePropertyChangeListener(this);
			if (legend != null) {
				mappable.removePropertyChangeListener(legend);
				if (mappable instanceof EventReceiver) {
					legend.removeMouseEventReceiver((EventReceiver) mappable);
				}
			}
		}
		mmetr = null;
		mappable = mapContent;
		if (mappable != null) {
			mappable.addPropertyChangeListener(this, true, false);
			if (legend != null) {
				mappable.addPropertyChangeListener(legend, false, true);
				if (mappable instanceof EventReceiver) {
					legend.addMouseEventReceiver((EventReceiver) mappable);
				}
			}
		}
		notifyPropertyChange("MapContent", null, null);
	}

	public void setLegend(Legend leg) {
		if (legend == leg)
			return;
		if (legend != null) {
			legend.removeLegendDrawer(this);
			if (mappable != null) {
				mappable.removePropertyChangeListener(legend);
				if (mappable instanceof EventReceiver) {
					legend.removeMouseEventReceiver((EventReceiver) mappable);
				}
			}
		}
		legend = leg;
		if (legend != null) {
			legend.addLegendDrawer(this);
			if (mappable != null) {
				mappable.addPropertyChangeListener(legend, false, true);
				if (mappable instanceof EventReceiver) {
					legend.addMouseEventReceiver((EventReceiver) mappable);
				}
			}
		}
	}

	/**
	* When some properties of the Mappable change, the map is redrawn.
	* To make the map canvas use the saved background
	* when it is redrawn, the mappable component sends a property change event
	* with the name "foreground". An event with any other property name makes the
	* map canvas repaint all, including the background.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() == mappable) {
			String prName = evt.getPropertyName();
			if (prName.equals("foreground")) {
				redrawForeground();
			} else { //redraw all
				imageValid = false;
				redraw();
			}
			if (prName.equals("content") || prName.equals("LayerNumber") || prName.equals("LayerAdded") || prName.equals("LayerRemoved")) {
				notifyPropertyChange("MapContent", null, null);
			} else {
				notifyPropertyChange("MapAppearance", null, null);
			}
		}
		if (evt.getPropertyName().equals("MapScale"))
			if (legend != null) {
				legend.redraw();
			}

	}

//-------------- the functions of the Zoomable interface --------------------
	/*
	* After zooming the MapCanvas should call the "update" method of the Mappable.
	* If the MapCanvas shows the current scale in the legend,
	* it should call the "redraw" method of the legend.
	* The MapCanvas should also notify its listeners about the change of its
	* properties.
	*/
//---------------- information functions --------------------
	/**
	* The method returns false if the current scale cannot be increased.
	* This and following methods can be used, for example, to disable or hide
	* the corresponding buttons. By default, all these methods return true.
	*/
	@Override
	public boolean canZoomIn() {
		return mappable != null || mmetr == null;
	}

	/**
	* The method returns false if the current scale cannot be decreased.
	*/
	@Override
	public boolean canZoomOut() {
		return true;
		/*
		if (mappable==null || mmetr==null) return false;
		RealRectangle current=mmetr.getVisibleTerritory();
		if (current==null) return false;
		RealRectangle whole=mappable.getWholeTerritoryBounds();
		if (whole==null) return true;
		return whole.rx1<current.rx1 || whole.ry1<current.ry1 ||
		       whole.rx2>current.rx2 || whole.ry2>current.ry2;
		*/
	}

	/**
	* The method replies whether the current viewport can be moved in the
	* specified direction.
	* "where" should be one of "North", "South", "East", "West"
	*/
	@Override
	public boolean canMove(String where) {
		if (where == null || mappable == null || mmetr == null)
			return false;
		RealRectangle current = mmetr.getVisibleTerritory();
		if (current == null)
			return false;
		RealRectangle whole = mappable.getWholeTerritoryBounds();
		if (whole == null)
			return true;
		if (where.equalsIgnoreCase("North"))
			return whole.ry2 > current.ry2;
		if (where.equalsIgnoreCase("South"))
			return whole.ry1 < current.ry1;
		if (where.equalsIgnoreCase("West"))
			return whole.rx1 < current.rx1;
		if (where.equalsIgnoreCase("East"))
			return whole.rx2 > current.rx2;
		return false;
	}

	/**
	* The method returns false if the component does not support the "pan"
	* operation, e.g. drawing of the whole territory in the current viewport.
	*/
	@Override
	public boolean canPan() {
		if (mappable == null)
			return false;
		RealRectangle rr = mappable.getWholeTerritoryBounds();
		if (rr == null)
			return false;
		if (mmetr == null || mmetr.getVisibleTerritory() == null) {
			generateMapContext();
		}
		RealRectangle vt = mmetr.getVisibleTerritory();
		if (vt == null)
			return false;
		if (rr.rx1 < vt.rx1 || rr.rx2 > vt.rx2 || rr.ry1 < vt.ry1 || rr.ry2 > vt.ry2)
			return true; //the territory is larger than can be seen
		return rr.rx2 - rr.rx1 < vt.rx2 - vt.rx1 && rr.ry2 - rr.ry1 < vt.ry2 - vt.ry1;
		//the territory is smaller than can be seen
	}

//------------------- actual zooming-panning functions ------------------
	/**
	* Tells the mappable that it should be updated, then repaints and
	* notifies about scale change.
	*/
	protected void performZoom() {
		if (mappable == null)
			return;
		mappable.update(getMapContext());
		imageValid = false;
		repaint();
		if (legend != null) {
			legend.redraw();
		}
		notifyPropertyChange("MapScale", null, mmetr.getVisibleTerritory());
	}

	/**
	* Enlarges the current scale by 25%
	*/
	@Override
	public void zoomIn() {
		zoomByFactor(2f);
	}

	/**
	* Decreases the current scale by 25%
	*/
	@Override
	public void zoomOut() {
		zoomByFactor(0.5f);
	}

	/**
	* Fits the whole represented territory or image to the size of the viewport.
	*/
	@Override
	public void pan() {
		if (mappable == null || mmetr == null)
			return;
		RealRectangle rr = mappable.getWholeTerritoryBounds();
		if (rr == null)
			return;
		prevBounds = mmetr.getVisibleTerritory();
		mmetr.setVisibleTerritory(rr);
		if (mmetr.changedByFit) {
			performZoom();
		}
	}

	/**
	* "Moves" the viewport over the shown territory (image etc.) by 25% in the
	* specified direction.
	* "where" should be one of "North", "South", "East", "West"
	*/
	@Override
	public void move(String where) {
		if (where == null || mappable == null || mmetr == null)
			return;
		RealRectangle rr = mmetr.getVisibleTerritory();
		if (rr == null)
			return;
		if (where.equalsIgnoreCase("North") || where.equalsIgnoreCase("South")) {
			float dy = (rr.ry2 - rr.ry1) / 4;
			if (where.charAt(0) == 'S' || where.charAt(0) == 's') {
				dy = -dy;
			}
			rr.ry1 += dy;
			rr.ry2 += dy;
		} else if (where.equalsIgnoreCase("East") || where.equalsIgnoreCase("West")) {
			float dx = (rr.rx2 - rr.rx1) / 4;
			if (where.charAt(0) == 'W' || where.charAt(0) == 'w') {
				dx = -dx;
			}
			rr.rx1 += dx;
			rr.rx2 += dx;
		} else
			return;
		prevBounds = mmetr.getVisibleTerritory();
		mmetr.setVisibleTerritory(rr);
		if (mmetr.changedByFit) {
			performZoom();
		}
	}

	/**
	* "Moves" the viewport over the shown territory (image etc.) by
	* the specified horizontal (dx) and vertical (dy) offsets. The offsets
	* are given in screen coordinates.
	*/
	@Override
	public void move(int dx, int dy) {
		if (dx == 0 && dy == 0)
			return;
		if (mappable == null || mmetr == null)
			return;
		RealRectangle rr = mmetr.getVisibleTerritory();
		if (rr == null)
			return;
		float step = mmetr.getPixelValue();
		if (step <= 0)
			return;
		float rdx = dx * step, rdy = dy * step;
		rr.rx1 += rdx;
		rr.rx2 += rdx;
		rr.ry1 += rdy;
		rr.ry2 += rdy;
		prevBounds = mmetr.getVisibleTerritory();
		mmetr.setVisibleTerritory(rr);
		performZoom();
	}

	/**
	* Shows the specified territory extent in the map viewport.
	*/
	@Override
	public void showTerrExtent(float x1, float y1, float x2, float y2) {
		if (mappable == null || mmetr == null)
			return;
		RealRectangle rr = new RealRectangle(x1, y1, x2, y2);
		prevBounds = mmetr.getVisibleTerritory();
		mmetr.setVisibleTerritory(rr);
		if (mmetr.changedByFit) {
			performZoom();
		}
	}

	/**
	 * Adjusts the extent to make the given rectangular area visible
	 */
	@Override
	public void adjustExtentToShowArea(float x1, float y1, float x2, float y2) {
		if (mappable == null || mmetr == null)
			return;
		if (Float.isNaN(x1) || Float.isNaN(x2) || Float.isNaN(y1) || Float.isNaN(y2))
			return;
		if (x1 > x2) {
			float x = x1;
			x1 = x2;
			x2 = x;
		}
		if (y1 > y2) {
			float y = y1;
			y1 = y2;
			y2 = y;
		}
		RealRectangle ext = mmetr.getVisibleTerritory();
		float dw = (Math.max(x2, ext.rx2) - Math.min(x1, ext.rx1)) / 20;
		float dh = (Math.max(y2, ext.ry2) - Math.min(y1, ext.ry1)) / 20;
		x1 -= dw;
		x2 += dw;
		y1 -= dh;
		y2 += dh;
		if (x1 < ext.rx1 || x2 > ext.rx2 || y1 < ext.ry1 || y2 > ext.ry2) {
			showTerrExtent(Math.min(x1, ext.rx1), Math.min(y1, ext.ry1), Math.max(x2, ext.rx2), Math.max(y2, ext.ry2));
		}
	}

	/**
	* Enlarges the part of the image specified by the rectangle to the
	* maximum possible scale at which the rectangle still fits in the
	* viewport.
	*/
	@Override
	public void zoomInRectangle(int x, int y, int width, int height) {
		if (x < 0 || y < 0 || width < Metrics.mm() || height < Metrics.mm())
			return;
		if (mmetr == null || mmetr.getVisibleTerritory() == null)
			return;
		RealRectangle rr = new RealRectangle();
		rr.rx1 = mmetr.absX(x);
		rr.rx2 = mmetr.absX(x + width);
		rr.ry1 = mmetr.absY(y + height);
		rr.ry2 = mmetr.absY(y);
		prevBounds = mmetr.getVisibleTerritory();
		mmetr.setVisibleTerritory(rr);
		if (mmetr.changedByFit) {
			performZoom();
		}
	}

	/**
	* Increases the scale by the given factor. If the value of "factor" is
	* between 0 and 1, the scale is actually decreased. The "factor"
	* must be a positive number.
	*/
	@Override
	public void zoomByFactor(float factor) {
		if (mmetr == null || Float.isNaN(factor) || factor <= 0)
			return;
		RealRectangle rr = mmetr.getVisibleTerritory();
		if (rr == null)
			return;
		float r = (rr.rx1 + rr.rx2) / 2, w = (rr.rx2 - rr.rx1) / 2 / factor;
		rr.rx1 = r - w;
		rr.rx2 = r + w;
		r = (rr.ry1 + rr.ry2) / 2;
		w = (rr.ry2 - rr.ry1) / 2 / factor;
		rr.ry1 = r - w;
		rr.ry2 = r + w;
		prevBounds = mmetr.getVisibleTerritory();
		mmetr.setVisibleTerritory(rr);
		if (mmetr.changedByFit) {
			performZoom();
		}
	}

	/**
	* The method replies whether the last zooming operation can be cancelled.
	*/
	@Override
	public boolean canUndo() {
		return prevBounds != null;
	}

	/**
	* Cancels the last zooming operation
	*/
	@Override
	public void undo() {
		if (prevBounds != null) {
			mmetr.setVisibleTerritory(prevBounds);
			if (mmetr.changedByFit) {
				performZoom();
			}
			prevBounds = null;
		}
	}

	//----------------- event management --------------------------------
	protected void makeEventManager() {
		if (evtMan == null) {
			evtMan = new EventManager();
			evtMan.addEventReceiver(this);
		}
	}

	/**
	* Sets the EventManager to distribute mouse events from the map among
	* listeners, depending on current interpretation of an event
	*/
	public void setEventMeaningManager(EventMeaningManager man) {
		evtMeanMan = man;
		makeEventManager();
		evtMan.setEventMeaningManager(evtMeanMan);
		evtMeanMan.addEvent(DMouseEvent.mDrag, DMouseEvent.getEventFullText(DMouseEvent.mDrag));
		evtMeanMan.addEventMeaning(DMouseEvent.mDrag, "zoom", "zoom in");
		evtMeanMan.addEventMeaning(DMouseEvent.mDrag, "shift", "shift the viewport");
	}

	/**
	* Returns the manager of meanings of mouse events occurring in the map.
	* The manager may be used to change the current meaning (interpretation) of
	* some event.
	*/
	@Override
	public EventMeaningManager getEventMeaningManager() {
		return evtMeanMan;
	}

	/**
	* Listeners to map events may treat these events in their own way. For
	* example, mouse dragging may be used for zooming, shifting, or selection
	* of objects in the active layer. MapCanvas, with the help of its
	* EventManager, takes into account current (user-selected) meaning of
	* each event and sends the event to those listeners that "understand"
	* this meaning.
	* The method addMapEventMeaning is used by a map listener to register
	* the meaning it would like to process. The MapCanvas sends this
	* meaning to its EventMeaningManager.
	*/
	@Override
	public void addMapEventMeaning(String eventId, String meaningId, String meaningText) {
		if (evtMeanMan != null) {
			evtMeanMan.addEventMeaning(eventId, meaningId, meaningText);
		}
	}

	/**
	* Registers a map listener.
	*/
	@Override
	public void addMapListener(EventReceiver ml) {
		if (ml == null)
			return;
		makeEventManager();
		evtMan.addEventReceiver(ml);
	}

	@Override
	public void removeMapListener(EventReceiver ml) {
		if (ml == null)
			return;
		makeEventManager();
		evtMan.removeEventReceiver(ml);
	}

	/**
	* The number of the map (index in the "small multiples") where the mouse
	* cursor is currently located
	*/
	protected int mouseLocMapN = -1;
	/**
	* The screen boundaries of the map containing the mouse cursor
	*/
	protected Rectangle mouseLocMapBounds = null;

	/**
	* Detects in which map (possibly, among multiple maps) the mouse cursor
	* is currently located. Sets the clip region according to this map.
	*/
	protected void detectMouseLocMap(MouseEvent e) {
		if (mmetr == null)
			return;
		if (mouseIsDragging)
			return;
		int k = mmetr.getMapNWithPoint(e.getX(), e.getY());
		if (mouseLocMapN == k)
			return;
		mouseLocMapN = k;
		if (mouseLocMapN >= 0) {
			mouseLocMapBounds = mmetr.getMapBounds(mouseLocMapN);
		} else {
			mouseLocMapBounds = null;
		}
	}

	//-------------- processing of mouse events --------------------------

	protected void sendEventToListeners(MouseEvent sourceME, String eventId, boolean rightButtonPressed) {
		if (sourceME.getID() != MouseEvent.MOUSE_ENTERED && sourceME.getID() != MouseEvent.MOUSE_EXITED && (mouseLocMapN < 0 || mouseLocMapN >= mappable.getMapCount()))
			return;
		makeEventManager();
		DMouseEvent me = new DMouseEvent(this, eventId, sourceME);
		me.setRightButtonPressed(rightButtonPressed);
		Vector list = evtMan.getEventListeners(me);
		if (list == null || list.size() < 1)
			return;
		for (int i = 0; i < list.size(); i++) {
			EventReceiver ml = (EventReceiver) list.elementAt(i);
			ml.eventOccurred(me);
		}
	}

	/**
	* Makes a correction of the given x-coordinate of the mouse (during dragging)
	* in order to avoid drawing outside the current map area.
	*/
	@Override
	public int correctMouseX(int x) {
		if (mouseLocMapBounds == null) {
			if (x < 0) {
				x = 0;
			} else {
				Dimension d = getSize();
				if (x > d.width - 1) {
					x = d.width - 1;
				}
			}
		} else {
			if (x < mouseLocMapBounds.x) {
				x = mouseLocMapBounds.x;
			} else if (x > mouseLocMapBounds.x + mouseLocMapBounds.width - 1) {
				x = mouseLocMapBounds.x + mouseLocMapBounds.width - 1;
			}
		}
		return x;
	}

	/**
	* Makes a correction of the given y-coordinate of the mouse (during dragging)
	* in order to avoid drawing outside the current map area.
	*/
	@Override
	public int correctMouseY(int y) {
		if (mouseLocMapBounds == null) {
			if (y < 0) {
				y = 0;
			} else {
				Dimension d = getSize();
				if (y > d.height - 1) {
					y = d.height - 1;
				}
			}
		} else {
			if (y < mouseLocMapBounds.y) {
				y = mouseLocMapBounds.y;
			} else if (y > mouseLocMapBounds.y + mouseLocMapBounds.height - 1) {
				y = mouseLocMapBounds.y + mouseLocMapBounds.height - 1;
			}
		}
		return y;
	}

	protected void sendDragEventToListeners(MouseEvent sourceME) {
		makeEventManager();
		int x = correctMouseX(sourceME.getX()), y = correctMouseY(sourceME.getY());
		if (!mouseIsDragging || prevX < 0 || prevY < 0 || Math.abs(x - prevX) > 1 || Math.abs(y - prevY) > 1) {
			DMouseEvent me = new DMouseEvent(this, DMouseEvent.mDrag, sourceME, prevX, prevY, x0, y0, !mouseIsDragging);
			Vector list = evtMan.getEventListeners(me);
			if (list == null || list.size() < 1)
				return;
			for (int i = 0; i < list.size(); i++) {
				EventReceiver ml = (EventReceiver) list.elementAt(i);
				ml.eventOccurred(me);
			}
			prevX = x;
			prevY = y;
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (!mouseIsDragging) {
			detectMouseLocMap(e);
		}
		sendEventToListeners(e, DMouseEvent.mEntered, false);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		stopEventDelayer();
		if (!mouseIsDragging) {
			mouseLocMapN = -1;
			mouseLocMapBounds = null;
		}
		sendEventToListeners(e, DMouseEvent.mExited, false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	/**
	* Indicates whether the mouse has been last pressed for a long time.
	*/
	private boolean mouseLongPressed = false;

	@Override
	public void mousePressed(MouseEvent e) {
		stopEventDelayer();
		detectMouseLocMap(e);
		x0 = e.getX();
		y0 = e.getY();
		delayer = new DelayEventThread(e, this);
		delayer.start();
		mouseLongPressed = false;
	}

	protected boolean checkRightButtonPressed(MouseEvent e) {
		if (e == null)
			return false;
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK)
			return true;
		if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)
			return true;
		return false;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (mouseLongPressed) { //an event has been sent
			mouseLongPressed = false;
			return;
		}
		stopEventDelayer();
		if (mouseIsDragging) {
			mouseIsDragging = false;
			sendDragEventToListeners(e);
			prevX = -1;
			prevY = -1;
		} else {
			if (e.getClickCount() > 1) {
				sendEventToListeners(e, DMouseEvent.mDClicked, checkRightButtonPressed(e));
			} else {
				sendEventToListeners(e, DMouseEvent.mClicked, checkRightButtonPressed(e));
			}
		}
		x0 = y0 = -1;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!mouseIsDragging && x0 >= 0 && y0 >= 0 && (Math.abs(e.getX() - x0) > 3 || Math.abs(e.getY() - y0) > 3)) {
			stopEventDelayer();
			mouseIsDragging = true;
			prevX = -1;
			prevY = -1;
		}
		if (mouseIsDragging) {
			sendDragEventToListeners(e);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (!mouseIsDragging) {
			detectMouseLocMap(e);
		}
		sendEventToListeners(e, DMouseEvent.mMove, false);
	}

	/**
	* "Cancels" the thread is used for delayed reaction to mouse pressed events.
	* The thread is cancelled when the mouse button is released or the mouse is
	* dragged.
	*/
	protected void stopEventDelayer() {
		if (delayer != null) {
			delayer.cancelEvent();
			delayer = null;
		}
	}

	/**
	* Used for capturing events of a mouse button being pressed for a long time.
	*/
	@Override
	public void DelayExpired(EventObject e) {
		delayer = null;
		mouseLongPressed = true;
		MouseEvent me = (MouseEvent) e;
		sendEventToListeners(me, DMouseEvent.mLongPressed, checkRightButtonPressed(me));
	}

	/**
	* Tells the object drawing in the map canvas to destroy itself, e.g. to
	* unregister from listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (mappable != null) {
			mappable.destroy();
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	* A method from the EventConsumer interface.
	* A MapCanvas consumes its own mouse dragging events.
	*/
	@Override
	public boolean doesConsumeEvent(DEvent evt, String eventMeaning) {
		if (evt == null)
			return false;
		return doesConsumeEvent(evt.getId(), eventMeaning);
	}

	/**
	* A method from the EventConsumer interface.
	* A MapCanvas consumes its own mouse dragging events.
	*/
	@Override
	public boolean doesConsumeEvent(String evtType, String eventMeaning) {
		if (evtType == null || eventMeaning == null)
			return false;
		if (evtType.equals(DMouseEvent.mDrag))
			return eventMeaning.equals("zoom") || eventMeaning.equals("shift");
		return false;
	}

	/**
	* A method from the EventConsumer (EventReceiver) interface.
	* A MapCanvas is interested in getting mouse drag events.
	*/
	@Override
	public boolean doesListenToEvent(String eventId) {
		return eventId != null && eventId.equals(DMouseEvent.mDrag);
	}

	/**
	* This method is used for delivering events to the Event Receiver.
	*/
	@Override
	public void eventOccurred(DEvent evt) {
		if (evt == null || !(evt instanceof DMouseEvent))
			return;
		DMouseEvent mevt = (DMouseEvent) evt;
		if (mevt.getId().equals(DMouseEvent.mDrag)) {
			boolean zooming = true;
			if (evtMeanMan != null) {
				String meaning = evtMeanMan.getCurrentEventMeaning(DMouseEvent.mDrag);
				if (meaning != null)
					if (meaning.equals("shift")) {
						zooming = false;
					}
			}
			int x0 = mevt.getDragStartX(), y0 = mevt.getDragStartY(), x = correctMouseX(mevt.getX()), y = correctMouseY(mevt.getY()), prx = mevt.getDragPrevX(), pry = mevt.getDragPrevY();
			//erase previous frame or arrow
			if (prx >= 0 && pry >= 0)
				if (zooming) {
					drawFrame(x0, y0, prx, pry);
				} else {
					drawArrow(x0, y0, prx, pry);
				}
			if (mevt.isDraggingFinished()) {
				if (zooming) {
					int w = x - x0, h = y - y0;
					if (w < 0) {
						w = -w;
						x0 = x;
					}
					if (h < 0) {
						h = -h;
						y0 = y;
					}
					zoomInRectangle(x0, y0, w, h);
				} else {
					move(x0 - x, y - y0);
				}
			} else //draw frame or arrow
			if (zooming) {
				drawFrame(x0, y0, x, y);
			} else {
				drawArrow(x0, y0, x, y);
			}
		}
	}

	/**
	* Implementation of ColorListener interface:
	* this function sets up project's background color
	*/
	@Override
	public void colorChanged(Color c, Object src) {
		if (src instanceof HotSpot && src == hsInLegend) {
			imageValid = false;
			setBackground(c);
			if (legend != null) {
				legend.redraw();
			}
			if (mappable != null && mappable instanceof DLayerManager) {
				((DLayerManager) mappable).terrBgColor = c;
			}
		}
	}

	protected void drawFrame(int x0, int y0, int x, int y) {
		if (x - x0 != 0 || y - y0 != 0) {
			Graphics gr = getGraphics();
			if (gr == null)
				return;
			gr.setColor(Color.magenta);
			gr.setXORMode(getBackground());
			gr.drawLine(x0, y0, x, y0);
			gr.drawLine(x, y0, x, y);
			gr.drawLine(x, y, x0, y);
			gr.drawLine(x0, y, x0, y0);
			gr.setPaintMode();
			gr.dispose();
		}
	}

	protected void drawArrow(int x0, int y0, int x, int y) {
		if (x - x0 != 0 || y - y0 != 0) {
			Graphics gr = getGraphics();
			gr.setColor(Color.magenta);
			gr.setXORMode(getBackground());
			gr.drawLine(x0, y0, x, y);
			gr.setPaintMode();
			gr.dispose();
		}
	}

	/**
	* Returns a unique identifier of the component
	* (used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* particular types of events.
	*/
	@Override
	public String getIdentifier() {
		return "MapCanvas_" + instanceN;
	}

	@Override
	public void setVisibleTerritory(RealRectangle rr) {
		if (mappable == null || mmetr == null)
			return;
		prevBounds = mmetr.getVisibleTerritory();
		mmetr.setVisibleTerritory(rr);
		if (mmetr.changedByFit) {
			performZoom();
		}

	}

	public void setMinPixelValue(float val) {
		if (mappable == null)
			return;
		if (mmetr == null) {
			mmetr = new MapMetrics();
		}
		mmetr.minPixelValue = val;
	}

	/**
	 * Marks the specified absolute spatial (geographic) position on the map.
	 */
	public void markPosition(float posX, float posY) {
		drawCrossInMarkedPosition(); //to erase the previous mark, if any
		markedPosX = posX;
		markedPosY = posY;
		drawCrossInMarkedPosition();
	}

	/**
	 * Erases the mark of the previously specified absolute spatial
	 * (geographic) position, if any.
	 */
	public void erasePositionMark() {
		drawCrossInMarkedPosition(); //to erase the previous mark, if any
		markedPosX = Float.NaN;
		markedPosY = Float.NaN;
	}

	/**
	 * Erases all marks such as position marks, if any.
	 */
	@Override
	public void eraseAllMarks() {
		erasePositionMark();
	}

	/**
	 * Clears the position that must be marked
	 */
	protected void clearMarkedPosition() {
		markedPosX = Float.NaN;
		markedPosY = Float.NaN;
	}

	protected void drawCrossInMarkedPosition() {
		if (mmetr == null || Float.isNaN(markedPosX) || Float.isNaN(markedPosY))
			return;
		Graphics gr = getGraphics();
		if (gr == null)
			return;
		int x = mmetr.scrX(markedPosX, markedPosY), y = mmetr.scrY(markedPosX, markedPosY);
		if (x < 0 || y < 0)
			return;
		Dimension size = getSize();
		if (x >= size.width || y >= size.height)
			return;
		gr.setColor(Color.black);
		gr.setXORMode(getBackground());
		gr.drawLine(x, 0, x, size.height);
		gr.drawLine(0, y, size.width, y);
		gr.setColor(Color.yellow);
		gr.drawLine(x - 1, y - 10, x - 1, y + 10);
		gr.drawLine(x + 1, y - 10, x + 1, y + 10);
		gr.drawLine(x - 10, y - 1, x + 10, y - 1);
		gr.drawLine(x - 10, y + 1, x + 10, y + 1);
		gr.setPaintMode();
		gr.dispose();
	}

}
