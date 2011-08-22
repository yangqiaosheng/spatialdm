//ID
package spade.lib.slider;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Enumeration;
import java.util.Vector;

class Thumb extends Object {
	float value;
	Rectangle bounds;

	Thumb() {
		super();
	}

	Thumb(float value) {
		this.value = value;
	}

	Thumb(float value, Rectangle bounds) {
		this(value);
		this.bounds = bounds;
	}
}

public class MultipleSlider extends Panel implements MouseListener, MouseMotionListener {

	public static final int VERTICAL = 0;
	public static final int HORIZONTAL = 1;
	public static final int LEFT = 2;
	public static final int TOP = 3;
	public static final int RIGHT = 4;
	public static final int BOTTOM = 5;
	public static final int SINGLE = 6;
	public static final int DOUBLE = 7;
	public static final int MULTIPLE = 8;

	public int position = HORIZONTAL;
	public int align = TOP;

	public int arrowSize = 10;
	public int border = 1;
	public int margin = 16;
	public int mode = MULTIPLE;
	public int precision = 2;
	public Dimension thumbSize = new Dimension(8, 12);

	protected float min = 1;
	protected float max = 100;
	protected int scaleSize = 1;
	protected int maxCount = 2;
	protected Vector thumbs;

	private boolean isPressed = false;
	private int offset = 0;
	private int activeThumb = -1;
	private Image thumbImage;
	private Image thumbMask;
	private Image scaleImage;
	private ScaleConverter sc;
	private Rectangle scalerect;
	private Rectangle textrect;
	private Rectangle trackrect;

	int highlightedRange = -1; //range to be highlighted
	Rectangle highlightedRectangle;

	//protected methods

	protected void quickSort(int l, int r) {
		int i = l;
		int j = r;
		float val = getValue(Math.round((l + r) / 2));
		while (i <= j) {
			while (getValue(i) < val) {
				i++;
			}
			while (getValue(j) > val) {
				j--;
			}
			if (i <= j) {
				Thumb ti = (Thumb) thumbs.elementAt(i);
				Thumb tj = (Thumb) thumbs.elementAt(j);
				thumbs.setElementAt(tj, i);
				thumbs.setElementAt(ti, j);
				i++;
				j--;
			}
		}
		if (l < j) {
			quickSort(l, j);
		}
		if (i < r) {
			quickSort(i, r);
		}
	}

	protected Rectangle trackRect() {
		Rectangle r = new Rectangle();
		if (position == HORIZONTAL) {
			r.x = margin + border;
			r.width = getSize().width - 2 * (margin + border);
			r.height = thumbSize.height + 2;
			if (align == TOP) {
				r.y = border;
			} else if (align == BOTTOM) {
				r.y = getSize().height - r.height - border;
			}
		} else if (position == VERTICAL) {
			r.y = margin + border;
			r.height = getSize().height - 2 * (margin + border);
			r.width = thumbSize.height + 2;
			if (align == LEFT) {
				r.x = border;
			} else if (align == RIGHT) {
				r.x = getSize().width - r.width - border;
			}
		}
		return r;
	}

	protected Rectangle scaleRect() {
		Rectangle r = trackRect();
		if (position == HORIZONTAL) {
			r.x = r.x + thumbSize.width / 2 + 1;
			r.width = r.width - thumbSize.width - 2;
			r.y = r.y + 1;
			r.height = r.height - 2;
		} else if (position == VERTICAL) {
			r.x = r.x + 1;
			r.width = r.width - 2;
			r.y = r.y + thumbSize.width / 2 + 1;
			r.height = r.height - thumbSize.width - 2;
		}
		return r;
	}

	protected Rectangle thumbRect(int index) {
		Rectangle r = null;
		if (index >= 0 && index < thumbs.size()) {
			r = ((Thumb) thumbs.elementAt(index)).bounds;
		}
		return r;
	}

	protected Rectangle thumbRect(int x, int y, int m) {
		Rectangle r = new Rectangle();
		if (position == HORIZONTAL) {
			r.x = x - Math.round(thumbSize.width / 2) - m;
			r.width = thumbSize.width + 2 * m;
			r.height = thumbSize.height + 2 * m;
			if (align == TOP) {
				r.y = border + 1 - m;
			} else if (align == BOTTOM) {
				r.y = getSize().height - border - 1 - m - thumbSize.height;
			}
		} else if (position == VERTICAL) {
			r.y = y - Math.round(thumbSize.width / 2) - m;
			r.width = thumbSize.height + 2 * m;
			r.height = thumbSize.width + 2 * m;
			if (align == LEFT) {
				r.x = border + 1 - m;
			} else if (align == RIGHT) {
				r.x = getSize().width - thumbSize.height - border - 1 - m;
			}
		}
		return r;
	}

	protected Dimension labelDim(String label) {
		Dimension d = new Dimension();
		FontMetrics fm = getFontMetrics(getFont());
		d.height = fm.getAscent() + fm.getDescent();
		d.width = fm.stringWidth(label);
		return d;
	}

	protected Rectangle textRect() {
		FontMetrics fm = getFontMetrics(getFont());
		Rectangle r = scaleRect();
		if (position == HORIZONTAL) {
			r.height = fm.getAscent() + fm.getDescent();
			if (align == TOP) {
				r.y = getSize().height - r.height - border;
			} else if (align == BOTTOM) {
				r.y = border;
			}
		} else if (position == VERTICAL) {
			r.width = labelDim(getLabel(max)).width;
			if (align == LEFT) {
				r.x = getSize().width - r.width - border;
			} else if (align == RIGHT) {
				r.x = border;
			}
		}
		return r;
	}

	protected boolean overlap(int x, int y, int index) {
		boolean result = false;
		Rectangle tmb = thumbRect(x, y, 1);
		if (thumbs.size() > 1) {
			Rectangle r1 = thumbRect(index - 1);
			Rectangle r2 = thumbRect(index + 1);
			if (position == HORIZONTAL) {
				if (r1 != null && tmb.x < r1.x + r1.width) {
					result = true;
				} else if (r2 != null && tmb.x + tmb.width > r2.x) {
					result = true;
				}
			} else if (position == VERTICAL) {
				if (r1 != null && tmb.y + tmb.height > r1.y) {
					result = true;
				} else if (r2 != null && tmb.y < r2.y + r2.height) {
					result = true;
				}
			}
		}
		return result;
	}

	protected boolean deadlock(int x, int y, int index) {
		boolean result = false;
		Rectangle tmb = thumbRect(x, y, 1);
		Rectangle trk = trackRect();
		if (position == HORIZONTAL) {
			if (tmb.x < trk.x || tmb.x + tmb.width > trk.x + trk.width) {
				result = true;
			}
		} else if (position == VERTICAL) {
			if (tmb.y < trk.y || tmb.y + tmb.height > trk.y + trk.height) {
				result = true;
			}
		}
		return result;
	}

	protected void moveThumb(int x, int y, int index) {
		Thumb t = (Thumb) thumbs.elementAt(activeThumb);
		if (overlap(x, y, index)) {
			removeSlider(activeThumb);
			fireSliderCountChanged(new SliderEvent(this));
			isPressed = false;
			repaint();
			return;
		}
		if (deadlock(x, y, index))
			return;
		Rectangle r = thumbRect(index);
		drawMask(r);
		drawLink(activeThumb); //clear link
		if (position == HORIZONTAL) {
			r.x = x - Math.round(thumbSize.width / 2);
			setValue(sc.toWorld(x), index);
		} else if (position == VERTICAL) {
			r.y = y - Math.round(thumbSize.width / 2);
			setValue(sc.toWorld(y), index);
		}
		drawThumb(r);
		drawLabel(index);
		drawLink(activeThumb);
		fireSliderDragged(new SliderEvent(this));
	}

	private int[] getThumbCoords() {
		int[] coords = new int[thumbs.size()];
		for (int i = 0; i < thumbs.size(); i++) {
			Rectangle rt = ((Thumb) thumbs.elementAt(i)).bounds;
			coords[i] = rt.x + thumbSize.width / 2;
		}
		return coords;
	}

	protected void moveThumbs() {
		int[] coords = getThumbCoords();
		for (int i = 0; i < thumbs.size(); i++) {
			setValue(sc.toWorld(coords[i]), i);
		}
	}

	//painting methods

	protected void setMargin() {
		FontMetrics fm = getFontMetrics(getFont());
	}

	protected void createScaleImage() {
		scaleImage = createImage(getSize().width, getSize().height);
		Graphics g = scaleImage.getGraphics();
		g.setColor(getBackground());
		g.fillRect(0, 0, getSize().width, getSize().height); //!
		draw3DRect(g, trackRect(), false);
		Color bc = background;
		float hsb[] = Color.RGBtoHSB(bc.getRed(), bc.getGreen(), bc.getBlue(), new float[3]);
		hsb[2] = 1 - (1 - hsb[2]) / 2;
		g.setColor(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
		Rectangle r = scaleRect();
		g.fillRect(r.x, r.y, r.width, r.height);
	}

	public void draw3DRect(Graphics g, Rectangle r, boolean raised) {
		g.setColor(background);
		g.fillRect(r.x, r.y, r.width, r.height);
		if (raised) {
			g.setColor(background.brighter());
			g.drawLine(r.x, r.y + r.height - 1, r.x, r.y);
			g.drawLine(r.x, r.y, r.x + r.width - 1, r.y);
			g.setColor(background.darker());
			g.drawLine(r.x + r.width - 1, r.y, r.x + r.width - 1, r.y + r.height - 1);
			g.drawLine(r.x + r.width - 1, r.y + r.height - 1, r.x, r.y + r.height - 1);
		} else {
			g.setColor(background.darker());
			g.drawLine(r.x, r.y + r.height - 1, r.x, r.y);
			g.drawLine(r.x, r.y, r.x + r.width - 1, r.y);
			g.setColor(background.brighter());
			g.drawLine(r.x + r.width - 1, r.y, r.x + r.width - 1, r.y + r.height - 1);
			g.drawLine(r.x + r.width - 1, r.y + r.height - 1, r.x, r.y + r.height - 1);
		}
	}

	protected void createThumbImage() {
		Graphics g;
		int w = thumbSize.width;
		int h = thumbSize.height;
		if (position == HORIZONTAL) {
			thumbImage = createImage(w, h);
			g = thumbImage.getGraphics();
			draw3DRect(g, new Rectangle(0, 0, w, h), true);
			g.setColor(Color.black);
			g.drawLine((int) Math.floor((w - 1) / 2), 0, (int) Math.floor((w - 1) / 2), h - 1);
			g.setColor(Color.white);
			g.drawLine((int) Math.floor((w) / 2), 0, (int) Math.floor((w) / 2), h - 2);
		} else if (position == VERTICAL) {
			thumbImage = createImage(h, w);
			g = thumbImage.getGraphics();
			draw3DRect(g, new Rectangle(0, 0, h, w), true);
			g.setColor(Color.black);//new Color(128,0,64));
			g.drawLine(0, (int) Math.floor((w - 1) / 2), h - 1, (int) Math.floor((w - 1) / 2));
			g.setColor(Color.white);
			g.drawLine(0, (int) Math.floor((w) / 2), h - 2, (int) Math.floor((w) / 2));
		}
	}

	protected void drawThumb(Rectangle r) {
		Graphics g = getGraphics();
		g.drawImage(thumbImage, r.x, r.y, r.width, r.height, this);
	}

	protected void drawScale(Graphics g) {
		createScaleImage();
		g.drawImage(scaleImage, 0, 0, this);
	}

	protected void drawMask(Rectangle r) {
		try {
			Graphics g = getGraphics().create(r.x, r.y, r.width, r.height);
			g.drawImage(scaleImage, 0 - r.x, 0 - r.y, this);
		} catch (Exception ex) {
		}
	}

	protected void drawLabel(int index) {
		Point p = new Point();
		Rectangle r = textRect();
		Rectangle rm = new Rectangle();
		String label = new String();
		if (index >= 0 && index < thumbs.size()) {
			label = getLabel(index);
		} else if (index < 0) {
			label = getLabel(min);
		} else if (index >= thumbs.size()) {
			label = getLabel(max);
		}
		Dimension d = labelDim(label);
		FontMetrics fm = getFontMetrics(getFont());
		if (position == HORIZONTAL) {
			p.x = r.x + Math.round((index + 1) * r.width / (thumbs.size() + 1) - d.width / 2);
			p.y = r.y + fm.getAscent();
			rm.x = r.x + Math.round((index + 1) * r.width / (thumbs.size() + 1) - r.width / (2 * (thumbs.size() + 1)));
			rm.width = Math.round(r.width / (thumbs.size() + 1));
			rm.y = r.y;
			rm.height = r.height;
		} else if (position == VERTICAL) {
			if (align == LEFT) {
				p.x = r.x;
			} else if (align == RIGHT) {
				p.x = r.x + (r.width - d.width);
			}
			p.y = r.y + r.height - Math.round((index + 1) * r.height / (thumbs.size() + 1) - d.height / 2 + fm.getDescent());
			rm = r;
			rm.y = p.y - fm.getAscent();
			rm.height = d.height;
		}
		Graphics g = getGraphics();
		g.clearRect(rm.x, rm.y, rm.width, rm.height);
		g.setColor(Color.black);
		g.setClip(rm);
		g.drawString(label, p.x, p.y);
		g.setClip(0, 0, getSize().width, getSize().height);
	}

	protected void drawLink(int index) {
		Rectangle rx = textRect();
		Rectangle rt = ((Thumb) thumbs.elementAt(index)).bounds;
		Polygon p = new Polygon();
		for (int i = 0; i < 4; i++) {
			p.addPoint(0, 0);
		}
		if (position == HORIZONTAL) {
			p.xpoints[0] = rt.x + (int) Math.floor((thumbSize.width - 1) / 2);
			p.xpoints[1] = p.xpoints[0];
			p.xpoints[3] = rx.x + Math.round((index + 1) * rx.width / (thumbs.size() + 1));
			p.xpoints[2] = p.xpoints[3];
			if (align == TOP) {
				p.ypoints[0] = rt.y + rt.height + 1;
				p.ypoints[1] = p.ypoints[0] + 4;
				p.ypoints[3] = rx.y - 1;
				p.ypoints[2] = p.ypoints[3] - 5;
			} else if (align == BOTTOM) {
				p.ypoints[0] = rt.y - 1;
				p.ypoints[1] = p.ypoints[0] - 4;
				p.ypoints[3] = rx.y + rx.height + 1;
				p.ypoints[2] = p.ypoints[3] + 5;
			}
		} else if (position == VERTICAL) {
			p.ypoints[0] = rt.y + (int) Math.floor((thumbSize.width - 1) / 2);
			p.ypoints[1] = p.ypoints[0];
			p.ypoints[3] = rx.y + rx.height - Math.round((index + 1) * rx.height / (thumbs.size() + 1));
			p.ypoints[2] = p.ypoints[3];
			if (align == RIGHT) {
				p.xpoints[0] = rt.x - 1;
				p.xpoints[1] = p.xpoints[0] - 4;
				p.xpoints[3] = rx.x + rx.width + 1;
				p.xpoints[2] = p.xpoints[3] + 4;
			} else if (align == LEFT) {
				p.xpoints[0] = rt.x + rt.width + 1;
				p.xpoints[1] = p.xpoints[0] + 4;
				p.xpoints[3] = rx.x - 1;
				p.xpoints[2] = p.xpoints[3] - 4;
			}
		}
		Graphics g = getGraphics();
		g.setXORMode(Color.white);
		g.drawPolyline(p.xpoints, p.ypoints, p.npoints);
		g.setPaintMode();
	}

	protected void drawBounds() {
		Rectangle tr = textRect();
		Rectangle sr = scaleRect();
		Graphics g = getGraphics();
		g.setColor(Color.black);
		if (position == HORIZONTAL) {
			if (align == TOP) {
				g.drawLine(sr.x, sr.y + sr.height + 1, sr.x, tr.y - 1);
				g.drawLine(sr.x + sr.width, sr.y + sr.height + 1, sr.x + sr.width, tr.y - 1);
			} else if (align == BOTTOM) {
				g.drawLine(sr.x, tr.y + tr.height + 1, sr.x, sr.y - 1);
				g.drawLine(sr.x + sr.width, tr.y + tr.height + 1, sr.x + sr.width, sr.y - 1);
			}
		} else if (position == VERTICAL) {
			if (align == LEFT) {
				g.drawLine(sr.x + sr.height + 1, sr.y, sr.x, tr.y - 1);
				g.drawLine(sr.x + sr.width, sr.y + sr.height + 1, sr.x + sr.width, tr.y - 1);
			} else if (align == RIGHT) {

			}
		}
	}

	protected int[] getCoords() {
		int size = thumbs.size() + 2;
		int[] coords = new int[size];
		Rectangle sr = scaleRect();
		switch (position) {
		case HORIZONTAL:
		default:
			coords[0] = sr.x;
			coords[size - 1] = sr.x + sr.width;
			for (int i = 0; i < thumbs.size(); i++) {
				Rectangle rt = ((Thumb) thumbs.elementAt(i)).bounds;
				coords[i + 1] = rt.x + (int) Math.floor((thumbSize.width - 1) / 2);
			}
			break;
		case VERTICAL:
			coords[0] = sr.y;
			coords[size - 1] = sr.y + sr.height;
			for (int i = 0; i < thumbs.size(); i++) {
				Rectangle rt = ((Thumb) thumbs.elementAt(i)).bounds;
				coords[size - 2 - i] = rt.y + (int) Math.floor((thumbSize.width - 1) / 2);
			}
		} //switch
		return coords;
	}

	public void resetScale() {
		int hw = Math.round(thumbSize.width / 2) + 1;
		Rectangle r = scaleRect();
		if (position == HORIZONTAL) {
			sc.Reset(r.x, r.x + r.width, getMin(), getMax());
		} else if (position == VERTICAL) {
			sc.Reset(r.y + r.height, r.y, getMin(), getMax());
		}
		resetThumbs();
		fireSliderResized(new SliderEvent(this));
	}

	protected void setThumb(int index) {
		Rectangle r = thumbRect(index);
		int pos = sc.toScreen(getValue(index));
		if (position == HORIZONTAL) {
			r.x = pos - Math.round(thumbSize.width / 2);
			if (align == TOP) {
				r.y = border + 1;
			} else if (align == BOTTOM) {
				r.y = getSize().height - border - 1 - thumbSize.height;
			}
		} else if (position == VERTICAL) {
			r.y = pos - Math.round(thumbSize.width / 2);
			if (align == LEFT) {
				r.x = border + 1;
			} else if (align == RIGHT) {
				r.x = getSize().width - border - 1 - thumbSize.height;
			}
		}
	}

	protected void resetThumbs() {
		for (int i = 0; i < thumbs.size(); i++) {
			setThumb(i);
		}
	}

	public String getLabel(float value) {
		String s = new String();
		if (precision != 0) {
			s = "" + value;
			int p = s.indexOf(".");
			if (p != -1) {
				String sub = new String(s);
				int d = s.length() - p - 1;
				if (d >= precision) {
					sub = s.substring(0, p) + s.substring(p, p + precision + 1);
					s = sub;
				} else {
					for (int i = 0; i < d; i++) {
						s = s + "0";
					}
				}
			}
		} else {
			s = "" + Math.round(value);
		}
		return s;
	}

	public String getLabel(int index) {
		return getLabel(getValue(index));
	}

	public float getValue(int index) {
		Thumb t = (Thumb) thumbs.elementAt(index);
		return t.value;
	}

	public void setValue(float value, int index) {
		Thumb t = (Thumb) thumbs.elementAt(index);
		t.value = value;
		thumbs.setElementAt(t, index);
	}

	public int getIndex(int x, int y) {
		int i = 0;
		int index = -1;
		Thumb t;
		for (Enumeration e = thumbs.elements(); e.hasMoreElements();) {
			t = (Thumb) e.nextElement();
			if (t.bounds.contains(x, y)) {
				index = i;
				break;
			}
			i++;
		}
		return index;
	}

	private void init() {
		sc = new ScaleConverter();
		thumbs = new Vector(5);
		ComponentListener l = new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				resetScale();
			}
		};
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(l);
	}

	public MultipleSlider(float min, float max) {
		super();
		init();
		this.min = min;
		this.max = max;
	}

	public MultipleSlider(float min, float max, int mode) {
		super();
		init();
		this.min = min;
		this.max = max;
		this.mode = mode;
		if (mode == SINGLE) {
			addSlider((min + max) / 2);
			activeThumb = 0;
		}
	}

	public MultipleSlider(float min, float max, int position, int align) {
		super();
		init();
		this.position = position;
		this.align = align;
		this.min = min;
		this.max = max;
	}

	public void setLimits(float min, float max) {
		this.min = (int) min;
		this.max = (int) (max + 1);
		int hw = (thumbSize.width / 2);
		Rectangle r = scaleRect();
		if (position == HORIZONTAL) {
			sc.Reset(r.x, r.x + r.width, getMin(), getMax());
		} else if (position == VERTICAL) {
			sc.Reset(r.y + r.height, r.y, getMin(), getMax());
		}
		moveThumbs();
		fireSliderLimitsChanged(new SliderEvent(this));
		repaint();
	}

	public void setMin(float min) {
		this.min = min;
		resetScale();
		repaint();
	}

	public void setMax(float max) {
		this.max = max;
		resetScale();
		repaint();
	}

	public float getMin() {
		return min;
	}

	public float getMax() {
		return max;
	}

	public void addSlider(int x, int y) {
		Rectangle r = thumbRect(x, y, 0);
		if (position == HORIZONTAL) {
			thumbs.addElement(new Thumb(sc.toWorld(x), r));
		} else if (position == VERTICAL) {
			thumbs.addElement(new Thumb(sc.toWorld(y), r));
		}
		quickSort(0, thumbs.size() - 1);
	}

	public void addSlider(float value) {
		if (position == HORIZONTAL) {
			thumbs.addElement(new Thumb(value, thumbRect(sc.toScreen(value), 1, 0)));
		} else if (position == VERTICAL) {
			thumbs.addElement(new Thumb(value, thumbRect(1, sc.toScreen(value), 0)));
		}
		quickSort(0, thumbs.size() - 1);
	}

	public void removeSlider(int index) {
		drawMask(thumbRect(index));
		thumbs.removeElementAt(index);
	}

	public void reset() {
		for (int i = thumbs.size() - 1; i >= 0; i--) {
			removeSlider(i);
		}
		fireSliderCountChanged(new SliderEvent(this));
		repaint();
	}

	public Vector getSliders() {
		return (Vector) thumbs.clone();
	}

	public void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}

	public int getCount() {
		return thumbs.size();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		highlightedRange = -1;
		drawHighlightedRange();
		fireSliderHighlighted(new SliderEvent(this));
		if (trackRect().contains(e.getX(), e.getY())) {
			int index = getIndex(e.getX(), e.getY());
			if (index != -1) {
				isPressed = true;
				activeThumb = index;
			} else {
				if (mode == MULTIPLE) {
					addSlider(e.getX(), e.getY());
					repaint();
					fireSliderCountChanged(new SliderEvent(this));
				} else if (mode == SINGLE) {
					activeThumb = 0;
					isPressed = true;
					moveThumb(e.getX(), e.getY(), activeThumb);
				}
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (isPressed) {
			moveThumb(e.getX(), e.getY(), activeThumb);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isPressed) {
			isPressed = false;
			activeThumb = -1;
			fireSliderReleased(new SliderEvent(this));
		}
	}

	public void drawHighlightedRange() {
		Graphics g = getGraphics();
		removeOldHighlighting();
		if (highlightedRange == -1)
			return;
		int x, y, wid, hei;
		Rectangle sr = scaleRect();
		y = sr.y;
		hei = sr.height - 1;
		int thumbsCount = thumbs.size();
		if (highlightedRange == 0) {
			x = sr.x;
		} else {
			Rectangle rt = ((Thumb) thumbs.elementAt(highlightedRange - 1)).bounds;
			x = rt.x + rt.width;
		}
		if (highlightedRange == thumbsCount) {
			wid = sr.x + sr.width - x;
		} else {
			Rectangle rt = ((Thumb) thumbs.elementAt(highlightedRange)).bounds;
			wid = rt.x - x - 1;
		}
		g.setColor(Color.yellow);
		g.drawRect(x, y, wid, hei);
		highlightedRectangle = new Rectangle(x, y, wid, hei);
	}

	private void removeOldHighlighting() {
		if (highlightedRectangle == null)
			return;
		Graphics g = getGraphics();
		Color bc = background;
		float hsb[] = Color.RGBtoHSB(bc.getRed(), bc.getGreen(), bc.getBlue(), new float[3]);
		hsb[2] = 1 - (1 - hsb[2]) / 2;
		g.setColor(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
		g.drawRect(highlightedRectangle.x, highlightedRectangle.y, highlightedRectangle.width, highlightedRectangle.height);
		highlightedRectangle = null;
	}

	@Override
	public void paint(Graphics g) {
		drawScale(g);
		drawBounds();
//      drawHighlightedRange();
		createThumbImage();
		for (int i = 0; i < thumbs.size(); i++) {
			drawThumb(thumbRect(i));
			drawLink(i);
		}
		for (int i = -1; i <= thumbs.size(); i++) {
			drawLabel(i);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		Rectangle sr = scaleRect();
		int x = e.getX();
		int y = e.getY();
		if (((x < sr.x) || (x > sr.x + sr.width) || (y < sr.y) || (y > sr.y + sr.height))) {
			if (highlightedRange == -1)
				return;
			highlightedRange = -1;
			removeOldHighlighting();
			fireSliderHighlighted(new SliderEvent(this));
			return;
		}
		int oldHighlightedRange = highlightedRange;
		int[] coords = getCoords();
		switch (position) {
		case HORIZONTAL:
		default:
			for (int i = 0; i < (coords.length - 1); i++) {
				if (x >= coords[i] && x < coords[i + 1]) {
					highlightedRange = i;
				}
			}
			break;
		case VERTICAL: //need to be implemented
		} //switch
		if (oldHighlightedRange != highlightedRange) {
			drawHighlightedRange();
			fireSliderHighlighted(new SliderEvent(this));
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (highlightedRange == -1)
			return;
		highlightedRange = -1;
		removeOldHighlighting();
		fireSliderHighlighted(new SliderEvent(this));
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public Dimension getMinimumSize() {
		int ws = 16;//whiskers space
		Dimension d = new Dimension(0, 0);
		if (position == HORIZONTAL) {
			d.width = (margin + border) * 2 + labelDim(getLabel(max)).width * 3;
			d.height = border * 2 + thumbSize.height + labelDim("0").height + ws;
		} else if (position == VERTICAL) {
			d.width = border * 2 + thumbSize.height + labelDim(getLabel(max)).width + ws;
			d.height = (margin + border) * 2 + labelDim(getLabel(max)).height * 3;
		}
		return d;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = getMinimumSize();
		if (position == HORIZONTAL) {
			d.width = getParent().getSize().width;
		} else if (position == VERTICAL) {
			d.height = getParent().getSize().height;
		}
		return d;
	}

	public float[] getValues() {
		int size = thumbs.size() + 2;
		float[] values = new float[size];
		values[0] = getMin();
		values[size - 1] = getMax();
		for (int i = 1; i < size - 1; i++) {
			values[i] = ((Thumb) thumbs.elementAt(i - 1)).value;
		}
		return values;
	}

	public String[] getLabels() {
		int size = thumbs.size() + 2;
		String[] labels = new String[size];
		labels[0] = getLabel(getMin());
		labels[size - 1] = getLabel(getMax());
		for (int i = 1; i < size - 1; i++) {
			labels[i] = getLabel(i - 1);
		}
		return labels;
	}

	public float getHighlightedMin() {
		if (highlightedRange == -1)
			return -1;
		else if (highlightedRange == 0)
			return getMin();
		else
			return ((Thumb) thumbs.elementAt(highlightedRange - 1)).value;
	}

	public float getHighlightedMax() {
		if (highlightedRange == -1)
			return -1;
		else if (highlightedRange == thumbs.size())
			return getMax();
		else
			return ((Thumb) thumbs.elementAt(highlightedRange)).value;
	}

// events firing ---------------------------------------------------------------

	private transient Vector SliderListeners;

	public synchronized void removeSliderListener(SliderListener l) {
		if (SliderListeners != null && SliderListeners.contains(l)) {
			Vector v = (Vector) SliderListeners.clone();
			v.removeElement(l);
			SliderListeners = v;
		}
	}

	public synchronized void removeAllSliderListeners() {
		if (SliderListeners != null) {
			Vector v = (Vector) SliderListeners.clone();
			v.removeAllElements();
			SliderListeners = v;
		}
	}

	public synchronized void addSliderListener(SliderListener l) {
		Vector v = SliderListeners == null ? new Vector(2) : (Vector) SliderListeners.clone();
		if (!v.contains(l)) {
			v.addElement(l);
			SliderListeners = v;
		}
	}

	protected void fireSliderDragged(SliderEvent e) {
		if (SliderListeners != null) {
			Vector listeners = SliderListeners;
			int count = listeners.size();
			for (int i = 0; i < count; i++) {
				((SliderListener) listeners.elementAt(i)).SliderDragged(e);
			}
		}
	}

	protected void fireSliderReleased(SliderEvent e) {
		if (SliderListeners != null) {
			Vector listeners = SliderListeners;
			int count = listeners.size();
			for (int i = 0; i < count; i++) {
				((SliderListener) listeners.elementAt(i)).SliderReleased(e);
			}
		}
	}

	protected void fireSliderCountChanged(SliderEvent e) {
		e.count = thumbs.size() + 1;
		if (SliderListeners != null) {
			Vector listeners = SliderListeners;
			int count = listeners.size();
			for (int i = 0; i < count; i++) {
				((SliderListener) listeners.elementAt(i)).SliderCountChanged(e);
			}
		}
	}

	protected void fireSliderLimitsChanged(SliderEvent e) {
		if (SliderListeners != null) {
			Vector listeners = SliderListeners;
			int count = listeners.size();
			for (int i = 0; i < count; i++) {
				((SliderListener) listeners.elementAt(i)).SliderLimitsChanged(e);
			}
		}
	}

	protected void fireSliderHighlighted(SliderEvent e) {
		e.setHighlightedRange(highlightedRange);
		e.setHighlightedMin(this.getHighlightedMin());
		e.setHighlightedMax(this.getHighlightedMax());
		if (SliderListeners != null) {
			Vector listeners = SliderListeners;
			int count = listeners.size();
			for (int i = 0; i < count; i++) {
				((SliderListener) listeners.elementAt(i)).SliderHighlighted(e);
			}
		}
	}

	protected void fireSliderResized(SliderEvent e) {
		if (SliderListeners != null) {
			Vector listeners = SliderListeners;
			int count = listeners.size();
			for (int i = 0; i < count; i++) {
				((SliderListener) listeners.elementAt(i)).SliderResized(e);
			}
		}
	}

	public void setValue(float value) {
		if (mode != SINGLE || value > max || value < min)
			return;
		int index = 0;
		setValue(value, index);
		Thumb t = (Thumb) thumbs.elementAt(index);
		Rectangle r = thumbRect(index);
		drawMask(r);
		drawLink(index); //clear link
		if (position == HORIZONTAL) {
			int x = sc.toScreen(value);
			float val = sc.toWorld(x);
			r.x = x - Math.round(thumbSize.width / 2);
		} else if (position == VERTICAL) {
//         r.y=y-(int)Math.round(thumbSize.width/2);
//         setValue(sc.toWorld(y), index);
		}
		drawThumb(r);
		drawLabel(index);
		drawLink(index);
		fireSliderReleased(new SliderEvent(this));
	}

	public Color background = Color.lightGray;
}
//~ID