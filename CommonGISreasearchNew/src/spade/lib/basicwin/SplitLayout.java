package spade.lib.basicwin;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Vector;

public class SplitLayout implements LayoutManager, SplitListener {
	public static final int HOR = 0, VERT = 1; //direction of the split lines
	static final int minSizemm = 4;
	static int minSize = 0;
	int dir = HOR;
	Vector parts = null, borders = null;
	Container owner = null;
	boolean wasNormalised = false;
	boolean allowSwapParts = true;
	static boolean canGetAllGraphics = true;

	public SplitLayout(Container anOwner, int direction) {
		if (direction == VERT) {
			dir = VERT;
		}
		owner = anOwner;
	}

	public static void setCanGetAllGraphics(boolean flag) {
		canGetAllGraphics = flag;
	}

	public void setAllowSwapParts(boolean flag) {
		allowSwapParts = flag;
		if (borders != null) {
			for (int i = 0; i < borders.size(); i++) {
				((Border) borders.elementAt(i)).setHasTButtonSwap(flag);
			}
		}
	}

	public void addComponent(Component cp, float part) {
		if (owner == null || cp == null)
			return;
		if (part <= 0) {
			part = 0.1f;
		}
		if (parts == null) {
			parts = new Vector();
		}
		if (owner.getComponentCount() > 0) {
			if (borders == null) {
				borders = new Vector();
			}
			Border b = new Border(dir);
			b.setHasTButtonSwap(allowSwapParts);
			b.addSplitListener(this);
			owner.add(b);
			borders.addElement(b);
			//if (minSize==0) minSize=minSizemm*Border.mm;
		}
		owner.add(cp);
		int nparts = (owner.getComponentCount() + 1) / 2;
		while (parts.size() < nparts) {
			parts.addElement(new Float(part));
		}
		wasNormalised = false;
	}

	public void addComponentAt(Component cp, float part, int idx) {
		if (owner == null || cp == null)
			return;
		if (idx < 0) {
			idx = 0;
		}
		if (parts == null || idx >= parts.size()) {
			addComponent(cp, part);
			return;
		}
		if (part <= 0) {
			part = 0.1f;
		}
		owner.add(cp, idx * 2);
		if (borders == null) {
			borders = new Vector();
		}
		Border b = new Border(dir);
		b.setHasTButtonSwap(allowSwapParts);
		b.addSplitListener(this);
		owner.add(b, idx * 2 + 1);
		borders.addElement(b);
		parts.insertElementAt(new Float(part), idx);
		wasNormalised = false;
	}

	public Component getComponent(int idx) {
		if (parts == null || idx < 0 || idx >= parts.size())
			return null;
		return owner.getComponent(idx * 2);
	}

	public float getComponentPart(int idx) {
		if (parts == null || idx < 0 || idx >= parts.size())
			return 0f;
		return ((Float) parts.elementAt(idx)).floatValue();
	}

	public int getComponentIndex(Component c) {
		if (c != null) {
			for (int i = 0; i < parts.size(); i++)
				if (c.equals(owner.getComponent(i * 2)))
					return i;
		}
		return -1;
	}

	public void removeComponent(int idx) {
		if (owner == null || parts == null)
			return;
		if (idx < 0 || idx >= parts.size())
			return;
		Component c = owner.getComponent(idx * 2), b = null;
		if (parts.size() > 1)
			if (idx == 0) {
				b = owner.getComponent(idx * 2 + 1);
			} else {
				b = owner.getComponent(idx * 2 - 1);
			}
		parts.removeElementAt(idx);
		if (b != null) {
			owner.remove(b);
			borders.removeElement(b);
		}
		owner.remove(c);
		if (parts != null && parts.size() == 1 && ((Float) parts.elementAt(0)).floatValue() < 0.01f) {
			parts.setElementAt(new Float(0.01f), 0);
		}
		wasNormalised = false;
	}

	public void replaceComponent(Component c, int idx) {
		if (owner == null || parts == null)
			return;
		if (idx < 0 || idx >= parts.size())
			return;
		Component c_old = owner.getComponent(idx * 2);
		owner.remove(c_old);
		owner.add(c, idx * 2);
		owner.getComponent(idx * 2).invalidate();
		CManager.validateAll(owner.getComponent(idx * 2));
	}

	public void swapComponents(int idx1, int idx2) {
		if ((idx1 < 0 || idx1 >= parts.size()) || (idx2 < 0 || idx2 >= parts.size()))
			return;
		if (idx1 == idx2)
			return;
		// make order of swappable components increasing: exchange indices if needed
		if (idx1 > idx2) {
			idx1 = idx1 - idx2;
			idx2 += idx1;
			idx1 = idx2 - idx1;
		}
		Component c1 = owner.getComponent(idx1 * 2), c2 = owner.getComponent(idx2 * 2);
		Float p1 = (Float) parts.elementAt(idx1), p2 = (Float) parts.elementAt(idx2);
		parts.setElementAt(p1, idx2);
		parts.setElementAt(p2, idx1);
		owner.setVisible(false);
		owner.remove(c1);
		owner.remove(c2);
		owner.add(c2, idx1 * 2);
		owner.add(c1, idx2 * 2);
		owner.setVisible(true);
		owner.invalidate();
		CManager.validateAll(owner.getComponent(idx1 * 2));
		CManager.validateAll(owner.getComponent(idx2 * 2));
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		Dimension d = new Dimension(0, 0);
		if (parent != null) {
			for (int i = 0; i < parent.getComponentCount(); i += 2) { //each second element is a border
				Dimension d1 = parent.getComponent(i).getPreferredSize();
				if (dir == HOR) {
					d.height += d1.height;
					if (d.width < d1.width) {
						d.width = d1.width;
					}
				} else {
					d.width += d1.width;
					if (d.height < d1.height) {
						d.height = d1.height;
					}
				}
			}
			int nborders = parent.getComponentCount() / 2;
			if (dir == HOR) {
				d.height += nborders * Border.maxW;
			} else {
				d.width += nborders * Border.maxW;
			}
		}
		return d;
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		Dimension d = new Dimension(0, 0);
		if (owner != null) {
			for (int i = 0; i < owner.getComponentCount(); i += 2) { //each second element is a border
				Dimension d1 = owner.getComponent(i).getPreferredSize();
				if (dir == HOR) {
					d.height += d1.height;
					if (d.width < d1.width) {
						d.width = d1.width;
					}
				} else {
					d.width += d1.width;
					if (d.height < d1.height) {
						d.height = d1.height;
					}
				}
			}
			int nborders = owner.getComponentCount() / 2;
			if (dir == HOR) {
				d.height += nborders * Border.maxW;
			} else {
				d.width += nborders * Border.maxW;
			}
		}
		return d;
	}

	public void changePart(int compN, float part) {
		if (part <= 0.01f && parts.size() < 2)
			return;
		if (compN >= 0 && compN < parts.size()) {
			parts.setElementAt(new Float(part), compN);
			wasNormalised = false;
			if (part <= 0.01f)
				if (compN < parts.size() - 1) {
					Border b = (Border) borders.elementAt(compN);
					b.setHasTButtonLeft(false);
					b.setHasTButtonRight(true);
				} else {
					Border b = (Border) borders.elementAt(compN - 1);
					b.setHasTButtonLeft(true);
					b.setHasTButtonRight(false);
				}
		}
	}

	public void setProportions(float p[]) {
		if (p == null || p.length < 2 || parts == null || parts.size() < 2)
			return;
		wasNormalised = false;
		for (int i = 0; i < p.length && i < parts.size(); i++) {
			if (Float.isNaN(p[i])) {
				p[i] = 0;
			}
			parts.setElementAt(new Float(p[i]), i);
			if (p[i] <= 0.01f)
				if (i < parts.size() - 1) {
					Border b = (Border) borders.elementAt(i);
					b.setHasTButtonLeft(false);
					b.setHasTButtonRight(true);
				} else {
					Border b = (Border) borders.elementAt(i - 1);
					b.setHasTButtonLeft(true);
					b.setHasTButtonRight(false);
				}
		}
	}

	public void forceEqualParts() {
		if (parts == null)
			return;
		for (int i = 0; i < parts.size(); i++) {
			parts.setElementAt(new Float(1.0f / parts.size()), i);
		}
		//wasNormalised=false;
	}

	void normalise() {
		if (wasNormalised)
			return;
		int nparts = (owner.getComponentCount() + 1) / 2;
		if (parts == null) {
			parts = new Vector(5, 5);
		}
		while (parts.size() < nparts) {
			parts.addElement(new Float(0.5f));
		}
		float sum = 0.0f;
		for (int i = 0; i < nparts; i++) {
			sum += ((Float) parts.elementAt(i)).floatValue();
		}
		if (sum == 0) {
			sum = nparts;
		}
		if (sum == 1.0f)
			return;
		float ratio = 1.0f / sum;
		for (int i = 0; i < nparts; i++) {
			float f = ((Float) parts.elementAt(i)).floatValue() * ratio;
			parts.setElementAt(new Float(f), i);
		}
		wasNormalised = true;
	}

	@Override
	public void layoutContainer(Container parent) {
		normalise();
		int nparts = (owner.getComponentCount() + 1) / 2;
		Dimension dtot = parent.getSize();
		if (borders != null)
			if (dir == HOR) {
				dtot.height -= borders.size() * Border.maxW;
			} else {
				dtot.width -= borders.size() * Border.maxW;
			}
		int x = 0, y = 0, wsum = 0, hsum = 0;
		for (int i = 0; i < Math.min(nparts, parts.size()); i++) {
			Component c = owner.getComponent(i * 2);//each second element is a border
			float part = ((Float) parts.elementAt(i)).floatValue();
			int w = dtot.width, h = dtot.height;
			if (part > 0.01f) {
				if (dir == HOR) {
					if (i < nparts - 1) {
						h *= part;
					} else {
						h -= hsum;
					}
					hsum += h;
				} else {
					if (i < nparts - 1) {
						w *= part;
					} else {
						w -= wsum;
					}
					wsum += w;
				}
				//System.out.println("* "+i+", part="+part+", w="+w+", h="+h);
				c.setBounds(x, y, w, h);
				c.setVisible(true);
				if (dir == HOR) {
					y += h;
				} else {
					x += w;
				}
			} else {
				c.setVisible(false);
			}
			if (borders != null && i < borders.size()) {
				if (dir == HOR) {
					h = Border.maxW;
				} else {
					w = Border.maxW;
				}
				((Component) borders.elementAt(i)).setBounds(x, y, w, h);
				if (dir == HOR) {
					y += h;
				} else {
					x += w;
				}
			}
		}
	}

	void addCompGraphics(Component c) {
		if (canGetAllGraphics || (c instanceof Canvas)) {
			if (gr == null) {
				gr = new Vector();
			}
			if (cgr == null) {
				cgr = new Vector();
			}
			gr.addElement(c.getGraphics());
			cgr.addElement(c);
		}
		if (c instanceof Container) {
			Container ct = (Container) c;
			for (int i = 0; i < ct.getComponentCount(); i++) {
				addCompGraphics(ct.getComponent(i));
			}
		}
	}

	Point getLocInOwner(Component cp) {
		Point p = cp.getLocationOnScreen(), p1 = owner.getLocationOnScreen();
		p.x -= p1.x;
		p.y -= p1.y;
		return p;
	}

	boolean overlap(Component cp, Point loc, Rectangle r) {
		if (dir == VERT) {
			if (r.x + r.width <= loc.x)
				return false;
			Dimension d = cp.getSize();
			if (r.x >= loc.x + d.width)
				return false;
			return true;
		}
		if (r.y + r.height <= loc.y)
			return false;
		Dimension d = cp.getSize();
		if (r.y >= loc.y + d.height)
			return false;
		return true;
	}

	Rectangle rb = null; // rectangle of the border
	int nMovingBorder = -1;
	Component c1 = null, c2 = null; // components affected by moving border
	Vector gr = null, cgr = null;

	void getGraphics() {
		if (gr == null) {
			gr = new Vector();
		}
		addCompGraphics(c1);
		addCompGraphics(c2);
	}

	void disposeGraphics() {
		if (gr == null)
			return;
		for (int i = 0; i < gr.size(); i++) {
			((Graphics) gr.elementAt(i)).dispose();
		}
		gr.removeAllElements();
		cgr.removeAllElements();
	}

	void drawRectXOR(Rectangle r) {
		if (rb != null) {
			for (int i = 0; i < gr.size(); i++) {
				Component c = (Component) cgr.elementAt(i);
				if (!c.isShowing()) {
					continue;
				}
				Point p = getLocInOwner(c);
				if (overlap(c, p, rb)) {
					Graphics g = (Graphics) gr.elementAt(i);
					g.drawRect(rb.x - p.x, rb.y - p.y, rb.width, rb.height);
				}
			}
		} else {
			for (int i = 0; i < gr.size(); i++) {
				Graphics g = (Graphics) gr.elementAt(i);
				g.setColor(Color.white);
				g.setXORMode(Color.darkGray);
			}
		}
		if (r != null) {
			for (int i = 0; i < gr.size(); i++) {
				Component c = (Component) cgr.elementAt(i);
				if (!c.isShowing()) {
					continue;
				}
				Point p = getLocInOwner(c);
				if (overlap(c, p, r)) {
					Graphics g = (Graphics) gr.elementAt(i);
					g.drawRect(r.x - p.x, r.y - p.y, r.width, r.height);
				}
			}
		} else {
			for (int i = 0; i < gr.size(); i++) {
				Graphics g = (Graphics) gr.elementAt(i);
				g.setPaintMode();
			}
		}
		rb = r;
	}

	int x1 = 0, y1 = 0, x2 = 0, y2 = 0;

	@Override
	public void mouseDragged(Object source, int x, int y) {
		if (parts == null || parts.size() < 2)
			return;
		if (borders == null || borders.size() < 1)
			return;
		int k = borders.indexOf(source);
		if (k < 0)
			return;
		nMovingBorder = k;
		Border b = (Border) borders.elementAt(k);
		Rectangle r = b.getBounds();
		if (rb == null) { //the dragging has just begun
			//the components affected by dragging
			c1 = owner.getComponent(k * 2);
			c2 = owner.getComponent((k + 1) * 2);
			getGraphics(); //to draw in these components
			//we should determine the limits for dragging
			Dimension d = new Dimension(minSize, minSize);
			Point p = c1.getLocation();
			x1 = x2 = p.x;
			y1 = y2 = p.y;
			if (dir == HOR) {
				y1 += d.height;
			} else {
				x1 += d.width;
			}
			Rectangle rc = c2.getBounds();
			if (dir == HOR) {
				y2 = rc.y + rc.height - d.height - Border.maxW;
			} else {
				x2 = rc.x + rc.width - d.width - Border.maxW;
			}
		}
		if (dir == HOR) {
			r.y += y;
			if (r.y < y1) {
				r.y = y1;
				b.setHasTButtonLeft(false);
			} else if (r.y > y2) {
				r.y = y2;
				b.setHasTButtonRight(false);
			} else {
				b.setHasTButtonLeft(true);
				b.setHasTButtonRight(true);
			}
		} else {
			r.x += x;
			if (r.x < x1) {
				r.x = x1;
				b.setHasTButtonLeft(false);
			} else if (r.x > x2) {
				r.x = x2;
				b.setHasTButtonRight(false);
			} else {
				b.setHasTButtonLeft(true);
				b.setHasTButtonRight(true);
			}
		}
		drawRectXOR(r);
	}

	@Override
	public void stopDrag() {
		int k = nMovingBorder;
		Rectangle r = rb;
		drawRectXOR(null);

		Rectangle r1 = c1.getBounds(), r2 = c2.getBounds();
		if (!c1.isVisible()) {
			r1.width = r1.height = 0;
		}
		if (!c2.isVisible()) {
			r2.width = r2.height = 0;
		}
		int sumW = 0;
		if (dir == HOR) {
			sumW = r1.height + r2.height;
		} else {
			sumW = r1.width + r2.width;
		}
		float p1 = ((Float) parts.elementAt(k)).floatValue();
		float p2 = ((Float) parts.elementAt(k + 1)).floatValue();
		float sumP = p1 + p2, ratio = sumP / sumW;
		if (dir == HOR) {
			r1.height = r.y - r1.y;
			r2.height = sumW - r1.height;
			p1 = r1.height * ratio;
			p2 = sumP - p1;
		} else {
			r1.width = r.x - r1.x;
			r2.width = sumW - r1.width;
			p1 = r1.width * ratio;
			p2 = sumP - p1;
		}
		parts.setElementAt(new Float(p1), k);
		parts.setElementAt(new Float(p2), k + 1);
		Border b = (Border) borders.elementAt(k);
		c1.invalidate();
		c2.invalidate();
		b.invalidate();

		//restoring...
		nMovingBorder = -1;
		c1 = null;
		c2 = null;
		disposeGraphics();

		CManager.validateAll(owner);
	}

	@Override
	public void arrowClicked(Object source, int where) {
		int k = borders.indexOf(source);
		if (k < 0)
			return;
		Border b = (Border) borders.elementAt(k);
		float p1 = ((Float) parts.elementAt(k)).floatValue(), p2 = ((Float) parts.elementAt(k + 1)).floatValue(), sumP = p1 + p2;
		switch (where) {
		case (SplitListener.dragToLeft):
			parts.setElementAt(new Float(0), k);
			parts.setElementAt(new Float(sumP), k + 1);
			b.setHasTButtonLeft(false);
			b.setHasTButtonRight(true);
			break;
		case (SplitListener.dragToCenter):
			parts.setElementAt(new Float(sumP / 2), k);
			parts.setElementAt(new Float(sumP / 2), k + 1);
			b.setHasTButtonLeft(true);
			b.setHasTButtonRight(true);
			break;
		case (SplitListener.dragToRight):
			parts.setElementAt(new Float(sumP), k);
			parts.setElementAt(new Float(0), k + 1);
			b.setHasTButtonLeft(true);
			b.setHasTButtonRight(false);
		}
		owner.getComponent(k * 2).invalidate();
		owner.getComponent((k + 1) * 2).invalidate();
		b.invalidate();
		CManager.validateAll(owner);
		b.repaint();
	}

	@Override
	public void bulletClicked(Object source) {
		int k = borders.indexOf(source);
		if (k < 0)
			return;
		swapComponents(k, k + 1);
	}

	@Override
	public void addLayoutComponent(String name, Component c) {
	}

	@Override
	public void removeLayoutComponent(Component c) {
	}

}