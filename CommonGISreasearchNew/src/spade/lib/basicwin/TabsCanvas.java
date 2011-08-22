package spade.lib.basicwin;

// no texts

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

public class TabsCanvas extends Canvas implements MouseListener, MouseMotionListener {
	Vector texts = null; // names of tabs
	Vector imgDr = null; // attached iconified images for tabs
	int X[] = null;
	public int Active = 0, MouseOver = -1;;

	TabSelector owner = null;
	boolean highlightNameofSelectedTab = false;
	/*
	* Flag indicates that TabsCanvas is located at the bottom and
	* tabs are down oriented
	*/
	boolean drawTabsBottomAligned = false;
	/*
	* Flag indicates that we do not want to see tab names
	* if the icons are present and there is not enough space to put
	* simultaneously icons and tab names
	*/
	boolean hideTabNamesWhenIcons = false;
	/*
	* Flag indicates that we do not want to see tab icons
	* because of limited horisontal space available
	*/
	boolean forceHideTabIcons = false;
	/*
	* Flag indicates that we do not want to see tab names
	* because of limited horisontal space available
	*/
	boolean forceHideTabNames = false;

	public TabsCanvas(Vector names, TabSelector owner) {
		super();
		this.owner = owner;
		texts = names;
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void checkCalculate() {
		if (texts != null && (X == null || X.length != texts.size())) {
			calculate();
		}
	}

	public void calculate() {
		if (texts == null || texts.size() < 1) {
			X = null;
			return;
		}
		// used to recalculate horizontal positions of tabs after changing their names
		// how to use: see method Iris.SetLanguage
		if (X == null || X.length != texts.size()) {
			X = new int[texts.size()];
		}
		int totalW = 0;

		for (int i = 0; i < texts.size(); i++) {
			Dimension iconSize = getTabIconSize(i);
			int iconW = 0;
			int textSize = Metrics.stringWidth((String) texts.elementAt(i));
			if (iconSize != null && !forceHideTabIcons) {
				iconW = iconSize.width + Metrics.mm();
			}
			totalW += (textSize + 2 * Metrics.mm() + iconW);
		}
		int availableW = owner.getPanelWidth();
		forceHideTabNames = (totalW > availableW) && hideTabNamesWhenIcons;
		for (int i = 0; i < texts.size(); i++) {
			Dimension iconSize = getTabIconSize(i);
			int textSize = ((forceHideTabNames && imgDr != null && !imgDr.isEmpty()) ? 0 : Metrics.stringWidth((String) texts.elementAt(i)));
			int iconW = 0;
			if (iconSize != null && !forceHideTabIcons) {
				iconW = iconSize.width + Metrics.mm();
			}
			X[i] = textSize + 2 * Metrics.mm() + iconW;
		}
		invalidate();
		CManager.validateAll(this);
		//getParent().validate();
	}

	public Drawer getTabIcon(int idx) {
		if (idx < 0 || idx >= texts.size())
			return null;
		Drawer tabIcon = null;
		if (imgDr != null && imgDr.size() > 0 && idx < imgDr.size() && imgDr.elementAt(idx) != null) {
			tabIcon = (Drawer) imgDr.elementAt(idx);
		}
		return tabIcon;
	}

	public Dimension getTabIconSize(int idx) {
		Drawer tabIcon = getTabIcon(idx);
		if (tabIcon == null)
			return null;
		return tabIcon.getIconSize();
	}

	public void setTabIcon(Drawer dr, int idx) {
		if (imgDr == null) {
			imgDr = new Vector(5, 5);
		}
		for (int i = imgDr.size(); i <= idx; i++) {
			imgDr.addElement(null);
		}
		imgDr.setElementAt(dr, idx);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (X == null)
			return;
		int xx = 0;
		for (int i = 0; i < X.length; i++) {
			xx += X[i];
			if (e.getX() < xx) {
				if (Active == i)
					return;
				activateTab(i);
				if (owner != null) {
					owner.selectTab(i);
				}
				return;
			}
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (X == null)
			return;
		int xx = 0;
		boolean stop = false;
		for (int i = 0; i < X.length && !stop; i++) {
			xx += X[i];
			if (e.getX() < xx) {
				MouseOver = i;
				//System.out.println("Mouse entered: tab N= "+Integer.toString(MouseOver));
				stop = true;
			}
		}
		if (owner != null) {
			owner.selectTabToolTip(MouseOver);
			//System.out.println("Mouse entered: "+((MouseOver>0) ? Integer.toString(MouseOver) :"no tab"));
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		//System.out.println("Mouse exited: "+((MouseOver>0) ? Integer.toString(MouseOver) :"no tab"));
		MouseOver = -1;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (X == null && MouseOver < 0)
			return;
		int xx = 0;
		boolean stop = false;
		for (int i = 0; i < X.length && !stop; i++) {
			xx += X[i];
			if (e.getX() < xx) {
				if (MouseOver == i)
					return;
				MouseOver = i;
				stop = true;
			}
		}
		if (owner != null) {
			owner.selectTabToolTip(MouseOver);
			//System.out.println("Mouse moved: "+((MouseOver>0) ? Integer.toString(MouseOver) :"no tab"));
		}
	}

	protected void setImageDrawerForTab(Drawer imgDrawer, int tabN) {
		if (tabN >= 0 && tabN < texts.size()) {
			if (imgDr == null) {
				imgDr = new Vector(5, 5);
			}
			for (int i = imgDr.size(); i <= tabN; i++) {
				imgDr.addElement(null);
			}
			imgDr.setElementAt(imgDrawer, tabN);
		} else {
			System.out.println("Cannot set image for tab: invalid tab index given: " + tabN);
		}
	}

	protected void removeImageDrawerFromTab(int tabN) {
		if (imgDr != null && tabN >= 0 && tabN < imgDr.size()) {
			imgDr.removeElementAt(tabN);
		} else {
			System.out.println("Cannot remove image for tab: invalid tab index given: " + tabN);
		}
	}

	public void activateTab(int n) {
		if (Active != n && n >= 0 && n < texts.size()) {
			Active = n;
			repaint();
		}
	}

	public int activeTabN() {
		return Active;
	}

	public void reset() {
		calculate();
		if (texts == null || Active < 0 || Active >= texts.size()) {
			Active = 0;
/*
		Dimension d=getSize();
		Dimension d_pref=getParent().getSize();
		System.out.println("TabSelection Canvas: width="+d.width+" -- parent width="+d_pref.width);
		forceHideTabNames=(d.width<d_pref.width) && hideTabNamesWhenIcons;
		if (forceHideTabNames) {
		  System.out.println("There is not enough space to put long names of tabs!");
		}
*/
		}
	}

	@Override
	public void paint(Graphics g) {
		if (Metrics.fmetr == null) {
			Metrics.setFontMetrics(g.getFontMetrics());
			calculate();
		} else if (hideTabNamesWhenIcons) {
			calculate();
		} else {
			checkCalculate();
		}
		Dimension d = getSize();
		g.setColor(Color.gray);
		g.fillRect(0, 0, d.width, d.height);
		if (texts == null || texts.size() < 1)
			return;
		for (int k = 0; k <= 1; k++) { // k==0 -> false; k==1 -> true
			int x = Metrics.mm();
			for (int i = 0; i < texts.size(); i++) {
				x += X[i];
			}
			if (drawTabsBottomAligned) {
				if (k == 1) {
					g.drawLine(x, 0, d.width, 0);
					//g.drawLine(x,d.height-1,d.width,d.height-1);
				}
			} else {
				if (k == 1) {
					g.drawLine(x, d.height - 1, d.width, d.height - 1);
				}
			}
			for (int i = texts.size() - 1; i >= 0; i--) {
				x -= X[i];
				if ((Active == i) == (k == 1)) {
					int xx[] = new int[5], yy[] = new int[5];
					xx[0] = x - Metrics.mm();
					yy[0] = drawTabsBottomAligned ? 0 : d.height;
					xx[1] = x + Metrics.mm();
					yy[1] = drawTabsBottomAligned ? d.height - 1 : 0;
					xx[2] = x + X[i] - Metrics.mm();
					yy[2] = drawTabsBottomAligned ? d.height - 1 : 0;
					xx[3] = x + X[i] + Metrics.mm();
					yy[3] = drawTabsBottomAligned ? 0 : d.height;
					xx[4] = xx[0];
					yy[4] = yy[0];
					g.setColor(Color.lightGray);
					g.fillPolygon(xx, yy, 4);
					g.setColor((i == Active) ? Color.black : Color.darkGray);
					int dy = (i == Active) ? 2 : 0;
					if (drawTabsBottomAligned) {
						dy = -dy;
					}
					// P.G.: if tabs have icons-images, take them into account
					int iconW = 0;
					Drawer dr = getTabIcon(i);
					Dimension iconSize = getTabIconSize(i);
					if (dr != null && iconSize != null && !forceHideTabIcons) {
						dr.draw(g, x + Metrics.mm() + Metrics.mm() / 2, Metrics.mm() + dy);
						iconW = iconSize.width + Metrics.mm();
					}
					if (highlightNameofSelectedTab) {
						Font fnt = g.getFont();
						if (fnt != null) {
							fnt = new Font(fnt.getName(), ((i == Active) ? Font.BOLD : Font.PLAIN), fnt.getSize());
							if (!fnt.equals(g.getFont())) {
								g.setFont(fnt);
							}
						}
					}
					g.setColor(Color.black);
					if (!forceHideTabNames || imgDr == null || imgDr.isEmpty()) {
						g.drawString((String) texts.elementAt(i), x + Metrics.mm() + iconW, Metrics.asc + Metrics.mm() + dy);
						// ~P.G.
					}

					if (!drawTabsBottomAligned) {
						yy[0] -= 1;
						yy[3] -= 1;
						yy[4] -= 1;
					} else {
						//yy[0]+=1; yy[3]+=1; yy[4]+=1;
					}
					g.drawPolygon(xx, yy, (i == Active) ? 4 : 5);
				}
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		checkCalculate();
		int w = 0;
		int h = Metrics.fh + 2 * Metrics.mm();
		int iconH = 0;
		if (X != null) {
			for (int element : X) {
				w += element;
			}
		}
		if (imgDr != null) {
			for (int i = 0; i < imgDr.size(); i++) {
				Drawer dr = getTabIcon(i);
				Dimension iconSize = getTabIconSize(i);
				if (dr != null && iconSize != null && iconH < iconSize.height) {
					iconH = iconSize.height;
					//System.out.println("TabsCanvas new currIconHeight="+iconH);
				}
			}
		}
		if (h < iconH) {
			h = iconH;
		}
		//System.out.println("TabsCanvas PrefSize="+w+","+h);
		return new Dimension(w, h);
	}

	protected void setHighlightNameofSelectedTab(boolean highlight) {
		highlightNameofSelectedTab = highlight;
		if (isVisible()) {
			repaint();
		}
	}

	protected void setTabsAtTheBottom(boolean tabsAtTheBottom) {
		drawTabsBottomAligned = tabsAtTheBottom;
		if (isVisible()) {
			repaint();
		}
	}

	protected void setHideTabNamesWhenIcons(boolean hideTabNames) {
		hideTabNamesWhenIcons = hideTabNames;
		if (isVisible()) {
			repaint();
		}
	}

	protected void setForceHideTabIcons(boolean hideTabIcons) {
		forceHideTabIcons = hideTabIcons;
		if (isVisible()) {
			repaint();
		}
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

}
