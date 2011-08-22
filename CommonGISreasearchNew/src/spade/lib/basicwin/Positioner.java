package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import spade.lib.util.Aligner;

public class Positioner implements Drawable, MouseListener, MouseMotionListener, PropertyChangeListener {
	public static final int mm = Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenResolution() / 25.33f);
	public static final int trW = 8, trH = 4;
	protected boolean isHorisontal = true;
	protected TriangleDrawer trd = null;
	/**
	* The canvas in which this object should draw itself
	*/
	protected Canvas canvas = null;
	/**
	* The boundaries in which the object should fit itself
	* origBounds are used in case of alignment with other graphics
	*/
	protected Rectangle bounds = null, origBounds = null;
	/**
	* Represented numeric interval and the position of the slider in world
	* coordinates.
	*/
	protected double min, max, absPos = 0.0f;
	/**
	* Screen coordinates of the beginning and the end of the segment
	*/
	protected int x0 = 0, x1 = 0;
	/**
	* Position of the slider (triangle) in screen coordinated
	*/
	protected int scrPos = 0;
	/**
	* Registers listeners of slider events and sends them the events
	*/
	protected PropertyChangeSupport pcSupport = null;
	/**
	* Aligner is used to align horisontally or vertically several plots
	*/
	protected Aligner aligner = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Sets the orientation of the slider
	*/
	public void setIsHorisontal(boolean value) {
		isHorisontal = value;
	}

	/**
	* Sets the canvas in which this object should draw itself
	*/
	@Override
	public void setCanvas(Canvas c) {
		canvas = c;
		if (canvas != null) {
			canvas.addMouseListener(this);
			canvas.addMouseMotionListener(this);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return (isHorisontal) ? new Dimension(30 * mm, trH + 2) : new Dimension(trH + 2, 30 * mm);
	}

	/**
	* Sets boundaries in which the object should fit itself
	*/
	@Override
	public void setBounds(Rectangle bounds) {
		this.bounds = bounds;
		if (aligner != null) {
			origBounds = new Rectangle(bounds);
			if (isHorisontal)
				if (aligner.getLeft() >= 0 && aligner.getRight() >= 0) {
					bounds.x += aligner.getLeft();
					bounds.width -= aligner.getLeft() + aligner.getRight();
				} else {
					;
				}
			else if (aligner.getTop() >= 0 && aligner.getBottom() >= 0) {
				bounds.y += aligner.getTop();
				bounds.height -= aligner.getTop() + aligner.getBottom();
			}
		}
		if (bounds != null)
			if (isHorisontal) {
				x0 = bounds.x;
				x1 = x0 + bounds.width;
			} else {
				x0 = bounds.y;
				x1 = bounds.y + bounds.height;
			}
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	public boolean containsPoint(int x, int y) {
		if (bounds == null)
			return false;
		return bounds.contains(x, y);
	}

	/**
	* Sets minimum and maximum values (the numeric interval to be represented)
	*/
	public void setMinMax(double min, double max) {
		this.min = min;
		this.max = max;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	/**
	* Sets the "zero" value (in world units)
	* The "zero" value is used for producing bichromatic color scales.
	*/
	public void setAbsPos(double f) {
		absPos = f;
	}

	/**
	* Returns the slider position value (in world units)
	*/
	public double getAbsPos() {
		return absPos;
	}

	/**
	* Transforms world units into screen coordinates
	*/
	protected int abs2Scr(double val) {
		if (x1 <= x0)
			return 0;
		return x0 + (int) Math.round((val - min) * (x1 - x0) / (max - min));
	}

	/**
	* Transforms screen coordinates into world units
	*/
	protected double scr2Abs(int x) {
		if (x < x0 || x > x1)
			return Double.NaN;
		return min + (x - x0) * (max - min) / (x1 - x0);
	}

	protected void countSliderPosition() {
		scrPos = abs2Scr(absPos);
	}

	/**
	* Draws the object in the given graphics.
	*/
	@Override
	public void draw(Graphics g) {
		countSliderPosition();
		//draw the background
		g.setColor((canvas == null) ? Color.white : canvas.getBackground());
		if (bounds == null)
			return;
		int w = bounds.width, h = bounds.height;
		if (canvas != null) {
			Dimension d = canvas.getSize();
			w = d.width;
			h = d.height;
		}
		if (isHorisontal) {
			g.fillRect(0, bounds.y, w, bounds.height + 1);
		} else {
			g.fillRect(bounds.x, 0, bounds.width + 1, h + 1);
		}
		int x = scrPos;
		if (x >= x0 && x <= x1) {
			if (trd == null) {
				trd = new TriangleDrawer((isHorisontal) ? TriangleDrawer.N : TriangleDrawer.W);
				if (isHorisontal) {
					trd.setPreferredSize(trW, trH);
				} else {
					trd.setPreferredSize(trH, trW);
				}
			}
			if (isHorisontal) {
				trd.draw(g, x - trW / 2, bounds.y);
			} else {
				trd.draw(g, bounds.x, bounds.y + x1 - x - trW / 2);
			}
		}
	}

	public void redraw() {
		if (canvas != null) {
			Graphics g = canvas.getGraphics();
			if (g != null) {
				draw(g);
				g.dispose();
			}
		}
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (canvas != null) {
			canvas.removeMouseListener(this);
			canvas.removeMouseMotionListener(this);
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

//-------------- notification about slider events --------------------

	public void addPropertyChangeListener(PropertyChangeListener slist) {
		if (slist == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(slist);
	}

	public void removePropertyChangeListener(PropertyChangeListener slist) {
		if (slist == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(slist);
	}

	public void notifyPosChange() {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("pos_moved", null, new Double(absPos));
	}

	public void notifyPosMoving(double currValue) {
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange("pos_moving", null, new Double(currValue));
	}

//-------------- processing of mouse events --------------------------

	private int mpos0 = -1;
	private boolean pressedOnSlider = false, isDragging = false;

	@Override
	public void mousePressed(MouseEvent e) {
		pressedOnSlider = false;
		if (!containsPoint(e.getX(), e.getY()))
			return;
		if (isHorisontal) {
			mpos0 = e.getX();
		} else {
			mpos0 = bounds.y + x1 - e.getY();
		}
		pressedOnSlider = mpos0 >= scrPos - trW / 2 && mpos0 <= scrPos + trW / 2;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (mpos0 < 0)
			return;
		boolean changed = false;
		if (!isDragging) {
			if (pressedOnSlider) {
				mpos0 = -1;
				pressedOnSlider = false;
				return; //clicked in an existing slider
			}
			if (mpos0 != scrPos) {
				scrPos = mpos0;
				changed = true;
			}
		} else if (pressedOnSlider) {
			int lowLim = x0, upLim = x1;
			int mpos = (isHorisontal) ? e.getX() : e.getY();
			if (mpos < lowLim) {
				mpos = lowLim;
			} else if (mpos > upLim) {
				mpos = upLim;
			}
			if (!isHorisontal) {
				mpos = bounds.y + x1 - mpos;
			}
			scrPos = mpos;
			changed = true;
		}
		isDragging = false;
		pressedOnSlider = false;
		mpos0 = -1;
		if (changed) {
			setAbsPos(scr2Abs(scrPos));
			redraw();
			notifyPosChange();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!pressedOnSlider)
			return;
		isDragging = true;
		int lowLim = x0, upLim = x1;
		int mpos = (isHorisontal) ? e.getX() : e.getY();
		if (mpos < lowLim) {
			mpos = lowLim;
		}
		if (mpos > upLim) {
			mpos = upLim;
		}
		if (!isHorisontal) {
			mpos = bounds.y + x1 - mpos;
		}
		if (mpos != scrPos) {
			setAbsPos(scr2Abs(mpos));
			redraw();
			notifyPosMoving(absPos);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	public void setAligner(Aligner al) {
		aligner = al;
		aligner.addPropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource() == aligner) {
			if (origBounds != null) {
				setBounds(origBounds);
			}
			if (canvas != null) {
				canvas.repaint();
			}
		}
	}

}
