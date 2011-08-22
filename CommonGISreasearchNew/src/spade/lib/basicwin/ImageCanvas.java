package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;

public class ImageCanvas extends Canvas implements ImageObserver {
	/**
	*  Default icon size
	*/
	protected int prefW = 16, prefH = 16;
	protected Image img = null;
	/**
	 * The scale factor applied to the size of the image
	 */
	protected float scaleFactor = Float.NaN;
	/**
	 * The icon may have a frame. In this case, this field specifies the width
	 * of the frame. If 0, no frame is drawn.
	 */
	protected int frameWidth = 0;
	/**
	 * If the icon has a frame, this is the color of the frame
	 */
	protected Color frameColor = null;

	public ImageCanvas() {
	}

	public ImageCanvas(Image img) {
		setImage(img);
	}

	public void setImage(Image img) {
		this.img = img;
		if (this.isShowing()) {
			repaint();
		}
	}

	public void setScaleFactor(float scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

	/**
	 * Sets the width of the frame. If 0, no frame is drawn.
	 */
	public void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth;
	}

	/**
	 * Sets the color of the frame. If null, no frame is drawn.
	 */
	public void setFrameColor(Color frameColor) {
		this.frameColor = frameColor;
	}

	public void setPreferredSize(int width, int height) {
		prefW = width;
		prefH = height;
	}

	@Override
	public Dimension getPreferredSize() {
		if (img == null)
			return new Dimension(prefW, prefH);
		int w = img.getWidth(this), h = img.getHeight(this);
		if (w < 1 || h < 1)
			return new Dimension(prefW, prefH);
		if (!Float.isNaN(scaleFactor)) {
			w = Math.round(scaleFactor * w);
			if (w < 5) {
				w = 5;
			}
			h = Math.round(scaleFactor * h);
			if (h < 5) {
				h = 5;
			}
		}
		return new Dimension(w + 2 * frameWidth, h + 2 * frameWidth);
	}

	@Override
	public void paint(Graphics g) {
		Dimension d = getSize();
		if (img != null) {
			int w = img.getWidth(null), h = img.getHeight(null);
			if (!Float.isNaN(scaleFactor)) {
				w = Math.round(scaleFactor * w);
				h = Math.round(scaleFactor * h);
				if (w < 5) {
					w = 5;
				}
				if (h < 5) {
					h = 5;
				}
			}
			int x = (d.width - w) / 2, y = (d.height - h) / 2;
			g.drawImage(img, x, y, w, h, this);
			if (frameWidth > 0 && frameColor != null) {
				g.setColor(frameColor);
				--w;
				--h;
				for (int i = 0; i < frameWidth; i++) {
					--x;
					--y;
					w += 2;
					h += 2;
					g.drawRect(x, y, w, h);
				}
			}
		} else {
			g.setColor(Color.black);
			g.drawRect(1, 1, d.width - 2, d.height - 2);
			g.drawLine(1, 1, d.width - 1, d.height - 1);
			g.drawLine(1, d.height - 1, d.width - 1, 1);
		}
	}

	@Override
	public boolean imageUpdate(Image image, int infoflags, int x, int y, int width, int height) {
		if ((infoflags & ALLBITS) == ALLBITS) {
			repaint();
			invalidate();
			CManager.validateAll(this);
			return false;
		}
		return true;
	}
}