package spade.lib.basicwin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import spade.vis.mapvis.Visualizer;

public class VisIconDrawer implements Drawer, PropertyChangeListener {

	Visualizer vis = null;
	boolean drawEnabled = true;
	Image img = null;
	int imgWidth = 16, imgHeight = 16;
	Component parent = null;

	public VisIconDrawer(Visualizer vis) {
		this.vis = vis;
		vis.addVisChangeListener(this);
	}

	public VisIconDrawer(Visualizer vis, Component cParent) {
		this.vis = vis;
		vis.addVisChangeListener(this);
		parent = cParent;
	}

	protected void init(Image img) {
		this.img = img;
		if (img != null) {
			imgWidth = img.getWidth(null);
			imgHeight = img.getHeight(null);
		}
	}

	/**
	* Returns the image
	*/
	public Image getImage() {
		return img;
	}

	/**
	* Returns the preferred icon size
	*/
	@Override
	public Dimension getIconSize() {
		return new Dimension(imgWidth, imgHeight);
	}

	/**
	* Makes the drawer to draw the icon in enabled or disabled state
	*/
	@Override
	public void setDrawEnabled(boolean value) {
		drawEnabled = value;
	}

	/**
	* Draws the icon in a component with the specified size and origin.
	* The argument "stretch" indicated whether the icon should be stretched
	* if the component is larger than the preferred size of the icon.
	*/
	@Override
	public void draw(Graphics g, int x0, int y0, int w, int h, boolean stretch) {
		if (vis == null)
			//System.out.println("VisIcon cannot be drawn: no visualizer");
			return;
		vis.drawIcon(g, x0, y0, w, h);
		//Image i=g.get
		//System.out.println("VisIcon drawn = "+vis.getVisualizationName());
	}

	/**
	* Draws the icon of the preferred size at the specified position
	*/
	@Override
	public void draw(Graphics g, int x0, int y0) {
		this.draw(g, x0, y0, imgWidth, imgHeight, false);
	}

	public void drawInactive(Graphics g, int x0, int y0) {
	}

	public void drawInactive(Graphics g, int x0, int y0, int w, int h) {
		g.drawImage(img, x0, y0, null);
		//g.setXORMode(Color.lightGray);
		g.setColor(Color.lightGray);
		for (int i = x0; i < x0 + w; i += 4) {
			for (int j = y0; j < y0 + h; j += 4) {
				g.drawRect(i, j, 2, 2);
			}
		}
		//g.drawImage(img,x0,y0,null);
		g.setPaintMode();
	}

	/**
	* When visualisation parameters change, icon may also change
	*/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() instanceof Visualizer && (evt.getPropertyName().equals("Visualization") || evt.getPropertyName().equals("VisParameters"))) {
			if (parent != null) {
				if (parent instanceof TabbedPanel) {
					((TabbedPanel) parent).forceRepaintTabSelector();
				} else {
					parent.repaint();
				}
			}
		}
	}

}