package spade.lib.basicwin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.net.URL;

import spade.lib.util.IconUtil;

public class ImageDrawer implements Drawer {
	protected Image img;
	protected int imgWidth = 16, imgHeight = 16;
	public boolean drawEnabled = true;

	public ImageDrawer(String imageURL, Component owner) {
		init(loadImageFromURL(imageURL, owner));
	}

	public ImageDrawer(Image image) {
		init(image);
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
		if (img == null)
			return;
		if (w < imgWidth || h < imgHeight) {
			float r1 = ((float) imgWidth) / w, r2 = ((float) imgHeight) / h;
			if (r2 < r1) {
				r1 = r2;
			}
			int w1 = Math.round(r1 * imgWidth), h1 = Math.round(r1 * imgHeight);
			if (w > w1) {
				x0 += (w - w1) / 2;
			}
			if (h > h1) {
				y0 += (h - h1) / 2;
			}
			w = w1;
			h = h1;
			stretch = true;
		}
		if (!stretch) {
			if (w > imgWidth) {
				x0 += (w - imgWidth) / 2;
			}
			if (h > imgHeight) {
				y0 += (h - imgHeight) / 2;
			}
			draw(g, x0, y0);
		} else {
			if (drawEnabled) {
				g.drawImage(img, x0, y0, w, h, null);
			} else {
				drawInactive(g, x0, y0, w, h);
			}
		}
	}

	/**
	* Draws the icon of the preferred size at the specified position
	*/
	@Override
	public void draw(Graphics g, int x0, int y0) {
		if (img != null) {
			if (drawEnabled) {
				g.drawImage(img, x0, y0, null);
			} else {
				drawInactive(g, x0, y0);
			}
		}
	}

	public void drawInactive(Graphics g, int x0, int y0) {
		drawInactive(g, x0, y0, img.getWidth(null), img.getHeight(null));
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

	protected Image loadImageFromURL(String s, Component component) {
		MediaTracker mediatracker = new MediaTracker(component);
		Image image = IconUtil.loadImage(this.getClass(), s, 1500);
		if (image == null) {
			URL url = this.getClass().getResource(s); //  URLSupport.makeURLbyPath(null,s);
			if (url == null) {
				System.out.println("Cannot load image from <" + s + ">");
				return null;
			}
			image = Toolkit.getDefaultToolkit().getImage(url);
		}
		if (image == null) {
			System.out.println("Cannot load image from <" + s + ">");
			return null;
		}
		try {
			mediatracker.addImage(image, 1);
			mediatracker.waitForAll();
			if (mediatracker.getErrorsAny() != null) {
				System.out.println("Cannot load image " + s);
				return null;
			}
		} catch (InterruptedException ie) {
			System.out.println("Cannot load image " + s + " (interrupted)");
			return null;
		}
		return image;
	}
}