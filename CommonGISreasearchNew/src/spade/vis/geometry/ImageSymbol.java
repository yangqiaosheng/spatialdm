package spade.vis.geometry;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

public class ImageSymbol implements Diagram {
	protected Image img = null;
	protected boolean isFramed = false, isDoubleFramed = false; //a black line arownd the frame
	protected Color frameColor = Color.red;
	protected int frameWidth = 1;
	protected int drawedW = 0;
	protected int drawedH = 0;
	protected float scaleFactor = Float.NaN;
	/**
	* The position of the label
	*/
	protected int labelX = 0, labelY = 0;

	public void setImage(Image img) {
		this.img = img;
	}

	public void setIsFramed(boolean value) {
		isFramed = value;
	}

	public void setIsDoubleFramed(boolean value) {
		isDoubleFramed = value;
	}

	public void setFrameColor(Color color) {
		frameColor = color;
	}

	public void setFrameWidth(int width) {
		frameWidth = width;
	}

	public void setScaleFactor(float factor) {
		scaleFactor = factor;
	}

	public float getScaleFactor() {
		return scaleFactor;
	}

	public void setDrawedSize(int width, int height) {
		drawedW = width;
		drawedH = height;
	}

	@Override
	public int getWidth() {
		if (img == null)
			return 0;
		int w = 0;
		if (drawedW > 0 && drawedH > 0) {
			w = drawedW;
		} else {
			w = img.getWidth(null);
		}
		if (!Float.isNaN(scaleFactor) && scaleFactor > 0) {
			w = Math.round(scaleFactor * w);
		}
		if (isFramed) {
			w += 2 * frameWidth;
		}
		if (isDoubleFramed) {
			w += 2;
		}
		return w;
	}

	@Override
	public int getHeight() {
		if (img == null)
			return 0;
		int h = 0;
		if (drawedW > 0 && drawedH > 0) {
			h = drawedH;
		} else {
			h = img.getHeight(null);
		}
		if (!Float.isNaN(scaleFactor) && scaleFactor > 0) {
			h = Math.round(scaleFactor * h);
		}
		if (isFramed) {
			h += 2 * frameWidth;
		}
		if (isDoubleFramed) {
			h += 2;
		}
		return h;
	}

	/**
	* Returns the position of the label. This method should be called after draw(...).
	*/
	@Override
	public Point getLabelPosition() {
		return new Point(labelX, labelY);
	}

	/**
	* Replies whether this diagram is centered, i.e. the center of the diagram
	* coinsides with the center of the object
	*/
	@Override
	public boolean isCentered() {
		return true;
	}

	/**
	* This function is to be used for point objects.
	*/
	@Override
	public void draw(Graphics g, int x, int y) {
		if (img == null)
			return;
		//int w=img.getWidth(null), h=img.getHeight(null);
		int w = getWidth(), h = getHeight();
		int x0 = x - w / 2, y0 = y - h / 2;
		//g.drawImage(img,x0,y0,null);
		g.drawImage(img, x0, y0, w, h, null);
		if (isFramed) {
			g.setColor(frameColor);
			--w;
			--h;
			for (int i = 0; i < frameWidth; i++) {
				--x0;
				--y0;
				w += 2;
				h += 2;
				g.drawRect(x0, y0, w, h);
			}
			if (isDoubleFramed) {
				g.setColor(Color.black);
				--x0;
				--y0;
				w += 2;
				h += 2;
				g.drawRect(x0, y0, w, h);
			}
		}
		labelX = x0;
		labelY = y0 + h + 2;
	}

	/**
	* This function is to be used for area objects.
	* The rectangle in which to fit the diagram is specified.
	*/
	@Override
	public void draw(Graphics g, Rectangle r) {
		if (r == null)
			return;
		draw(g, r.x, r.y, r.width, r.height);
	}

	/**
	* Same as previous, only the arguments are not wrapped into a Rectangle object
	*/
	@Override
	public void draw(Graphics g, int x, int y, int w, int h) {
		draw(g, x + w / 2, y + h / 2);
	}
}