package spade.lib.basicwin;

// no texts

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Panel;
import java.util.Vector;

public class TabbedPanel extends Panel implements TabSelector {
	/*
	* Components and their names located inside the tabbed panel
	*/
	Vector comp = null, names = null;
	/*
	* Popups and icons are associated with tabs of the tabbed panel, if any
	*/
	Vector tooltips = null, icons = null;
	/*
	* Internal layout of components inside tabbed panel
	*/
	CardLayout cl = null;
	/*
	* Internal panel containing all components are inside tabbed panel
	*/
	Panel cpan = null;
	/*
	* Internal control for tabs selection: catches mouse clicks and selects tabs
	*/
	TabsCanvas tc = null;
	/*
	* Internal manager responsible for showing tabs popups
	*/
	PopupManager pm = null;
	/*
	* Indicates if tab selection control is located at the bottom and
	* tabs are down oriented
	*/
	boolean areTabsAtTheBottom = false;
	/*
	* Flag indicates that we do not want to see tab selection panel
	* if the only one tab is present
	*/
	boolean showTabSelectorOnDemand = false;
	/*
	* Flag indicates that we do not want to see tab names in selection panel
	* if the icons are attached to tabs and there is not enough space to put
	* simultaneously icons and tab names
	*/
	boolean hideTabNamesWhenIcons = false;
	/*
	* Flag indicates that we do not want to see tab icons in selection panel
	* even if they are attached to tabs
	*/
	boolean forceHideTabIcons = false;
	/*
	* Listeners of selections this tabbed panel
	*/
	Vector tsList = null;

	/**
	* Adds object interested in listening tab selection events
	*/
	public void addTabSelectionListener(TabSelectionListener l) {
		if (tsList == null) {
			tsList = new Vector(2, 2);
		} else {
			for (int i = 0; i < tsList.size(); i++)
				if (tsList.elementAt(i).equals(l)) {
					System.out.println("WARNING: Listener " + l + " was already added!");
					return;
				}
		}
		tsList.addElement(l);
	}

	/**
	* Removes the object was listening tab selection events
	*/
	public void removeTabSelectionListener(TabSelectionListener l) {
		if (l == null)
			return;
		for (int i = 0; i < tsList.size(); i++)
			if (tsList.elementAt(i).equals(l)) {
				tsList.removeElementAt(i);
			}
	}

	/**
	* Notifies all objects are registered for listening tab selection events
	*/
	public void notifyTabSelected() {
		if (tsList == null || tsList.size() < 1)
			return;
		TabSelectionListener l;
		for (int i = 0; i < tsList.size(); i++) {
			l = (TabSelectionListener) (tsList.elementAt(i));
			l.tabSelected(getActiveTabN(), this);
		}
	}

	/**
	* Adds component to the tabbed panel to the last tab (at the end)
	*/
	public void addComponent(String name, Component p) {
		if (p == null)
			return;
		if (name == null) {
			name = p.getName();
		}
		if (names == null) {
			names = new Vector(5, 5);
		}
		if (comp == null) {
			comp = new Vector(5, 5);
		}
		if (tooltips == null) {
			tooltips = new Vector(5, 5);
		}
		if (icons == null) {
			icons = new Vector(5, 5);
		}
		names.addElement(name);
		comp.addElement(p);
		tooltips.addElement(null);
		icons.addElement(null);
		if (cl != null && tc != null && cpan != null) {
			if (showTabSelectorOnDemand && getTabCount() == 2) {
				Component c[] = getComponents();
				boolean tc_exists = false;
				if (c != null) {
					for (int i = 0; i < c.length && !tc_exists; i++)
						if (c[i] != null && c[i].equals(tc)) {
							tc_exists = true;
						}
				}
				if (!tc_exists) {
					add(areTabsAtTheBottom ? "South" : "North", tc);
				}
			}
			cpan.add(name, p);
			tc.reset();
			tc.repaint();
		}
	}

	/**
	* Returns number of components currently added to the tabbed panel,
	* or, in other words, number of tabs.
	*/
	public int getTabCount() {
		if (comp == null)
			return 0;
		return comp.size();
	}

	/**
	* Returns the name assigned to the tab with given index
	*/
	public String getTabName(int idx) {
		if (idx < 0 || idx > getTabCount() - 1)
			return null;
		if (names.elementAt(idx) == null)
			return null;
		return (String) names.elementAt(idx);
	}

	/**
	* Returns the object that performs drawing of the icon assigned to
	* the tab with given index
	*/
	public Drawer getTabIcon(int idx) {
		if (idx < 0 || idx > getTabCount() - 1)
			return null;
		if (icons.elementAt(idx) == null)
			return null;
		return (Drawer) icons.elementAt(idx);
	}

	/**
	* Removes the component with given name and corresponding tab from the tabbed panel
	*/
	public void removeComponent(String tabName) {
		if (tabName == null || names == null)
			return;
		int idx = names.indexOf(tabName);
		removeComponentAt(idx);
	}

	/**
	* Removes the component and corresponding tab from the tabbed panel
	*/
	public void removeComponent(Component c) {
		if (c == null || comp == null)
			return;
		int idx = comp.indexOf(c);
		removeComponentAt(idx);
	}

	/**
	* Removes the component and corresponding tab with given index from the tabbed panel
	*/
	public void removeComponentAt(int idx) {
		if (idx < 0)
			return;
		boolean wasActive = tc != null && idx == tc.activeTabN();
		names.removeElementAt(idx);
		Component c = (Component) comp.elementAt(idx);
		comp.removeElementAt(idx);
		if (icons != null && idx < icons.size()) {
			icons.removeElementAt(idx);
		}
		if (tooltips != null && idx < tooltips.size()) {
			tooltips.removeElementAt(idx);
		}
		if (cl != null && tc != null && cpan != null) {
			cpan.remove(c);
			tc.removeImageDrawerFromTab(idx);
			tc.reset();
			if (wasActive) {
				tc.activateTab(0);
				selectTab(0);
			}
			tc.repaint();
			if (showTabSelectorOnDemand && getTabCount() == 1) {
				remove(tc);
			}
		}
	}

	/**
	* Removes the given component and replaces it with a new one
	* with a given name in the corresponding tab of the tabbed panel
	*/
	public void replaceComponent(Component c, Component newComp, String newName) {
		if (c == null || comp == null)
			return;
		int idx = comp.indexOf(c);
		replaceComponentAt(newComp, idx, newName);
	}

	/**
	* Removes the component from tab with given index and replaces it
	* with a new one with a given name in this tab of the tabbed panel
	*/
	public void replaceComponentAt(Component newComp, int idx, String newName) {
		if (idx < 0)
			return;
		boolean wasActive = tc != null && idx == tc.activeTabN();
		names.setElementAt(newName, idx);
		Component c = (Component) comp.elementAt(idx);
		comp.setElementAt(newComp, idx);
		if (icons != null && idx < icons.size()) {
			icons.setElementAt(null, idx);
		}
		if (tooltips != null && idx < tooltips.size()) {
			tooltips.setElementAt(null, idx);
		}

		if (cl != null && tc != null && cpan != null) {
			int cIdx = -1;
			for (int i = 0; i < cpan.getComponentCount() && cIdx < 0; i++) {
				Component cc = cpan.getComponent(i);
				if (cc != null && cc.equals(c)) {
					cIdx = i;
				}
			}
			if (cIdx < 0)
				return;
			cpan.setVisible(false);
			cpan.remove(cIdx);
			cpan.add(newComp, cIdx);
			cpan.setVisible(true);
			tc.reset();
			if (wasActive) {
				tc.activateTab(0);
				selectTab(0);
			}
			tc.repaint();
		}
	}

	/**
	* Returns the component with a given name in the tabbed panel
	*/
	public Component getComponent(String tabName) {
		if (names == null)
			return null;
		int idx = names.indexOf(tabName);
		if (idx < 0)
			return null;
		return (Component) comp.elementAt(idx);
	}

	/**
	* Returns the component located in the tab with a given index in the tabbed panel
	*/
	public Component getTabContent(int tabN) {
		if (tabN < 0 || tabN >= comp.size() || comp == null)
			return null;
		return (Component) comp.elementAt(tabN);
	}

	/**
	* Returns the number of tab where the given component located in the tabbed panel
	*/
	public int getTabIndex(Component c) {
		if (c == null)
			return -1;
		return comp.indexOf(c);
	}

	/**
	* Returns the number of tab where the given component located in the tabbed panel
	*/
	public int getTabIndex(String tabId) {
		if (tabId == null || names == null)
			return -1;
		return names.indexOf(tabId);

	}

	/**
	* Removes all components from the tabbed panel
	*/
	public void removeAllComponents() {
		if (names == null || comp == null)
			return;
		names.removeAllElements();
		comp.removeAllElements();
		if (icons != null) {
			icons.removeAllElements();
		}
		if (tooltips != null) {
			tooltips.removeAllElements();
		}

		if (cl != null && tc != null && cpan != null) {
			tc.reset();
			tc.repaint();
			cpan.removeAll();
		}
	}

	/**
	* Creates all internal controls, initializes layouts of the tabbed panel,
	* and adds already assigned components, names of tabs, icons, popups to.
	* Variant without parameters makes tab selector on the top of the panel.
	*/
	public boolean makeLayout(boolean tabsAtTheBottom) {
		areTabsAtTheBottom = tabsAtTheBottom;
		return makeLayout();
	}

	public boolean makeLayout() {
		if (cl != null && tc != null && cpan != null)
			return false;
		if (comp == null || comp.size() < 1)
			return true;
		tc = new TabsCanvas(names, this);
		tc.setTabsAtTheBottom(areTabsAtTheBottom);
		tc.setHideTabNamesWhenIcons(hideTabNamesWhenIcons);
		tc.setForceHideTabIcons(forceHideTabIcons);
		BorderLayout bl = new BorderLayout();
		setLayout(bl);
		if (!showTabSelectorOnDemand || getTabCount() > 1) {
			add(areTabsAtTheBottom ? "South" : "North", tc);
		}
		Panel p = new Panel();
		p.setLayout(cl = new CardLayout());
		for (int i = 0; i < comp.size(); i++) {
			p.add((String) names.elementAt(i), (Component) comp.elementAt(i));
		}
		add("Center", p);
		cpan = p;
		return true;
	}

	/**
	* Forces to show at the front the tab with given name of the tabbed panel,
	*/
	public void showTab(String tabName) { // may be called from within a program
		if (tabName == null)
			return;
		if (cl == null || cpan == null)
			return;
		for (int i = 0; i < names.size(); i++)
			if (tabName.equals(names.elementAt(i))) {
				cl.show(cpan, tabName);
				tc.activateTab(i);
				notifyTabSelected();
				return;
			}
	}

	/**
	* Forces to show at the front the tab with given index
	*/
	public void showTab(int tabN) { // may be called from within a program
		if (cl == null || cpan == null)
			return;
		cl.show(cpan, (String) names.elementAt(tabN));
		tc.activateTab(tabN);
		notifyTabSelected();
	}

	/**
	* Internal call to assign currently shown tab with given number.
	*/
	@Override
	public void selectTab(int tabN) { //called from TabsCanvas upon mouse click
		if (comp == null || tabN < 0 || tabN >= comp.size())
			return;
		cl.show(cpan, (String) names.elementAt(tabN));
		notifyTabSelected();
	}

	/**
	* Returns the number (index) of tab that is currently displayed at the front
	*/
	public int getActiveTabN() {
		if (tc != null)
			return tc.activeTabN();
		return -1;
	}

	/**
	* Returns the name of tab that is currently displayed at the front
	*/
	public String getActiveTabName() {
		if (tc != null)
			return getTabName(tc.activeTabN());
		return null;
	}

	/**
	* Sets the Object that will be shown by PopupManager for the tab with given index
	*/
	public void setToolTip(int tabN, Object tt) {
		if (tc == null || tabN < 0 || tabN >= comp.size())
			return;
		if (tt != null && !((tt instanceof Component) || (tt instanceof String))) {
			System.out.println("Invalid tooltip has been provided");
			tt = null;
		}
		if (tooltips == null) {
			tooltips = new Vector(5, 5);
		}
		for (int i = tooltips.size(); i < comp.size(); i++) {
			tooltips.addElement(null);
		}
		tooltips.setElementAt(tt, tabN);
	}

	/**
	* Internally selects the tab with given index to activate PopupManager with
	* a content was previously assigned to this tab
	*/
	@Override
	public void selectTabToolTip(int n) {
		if (tooltips == null)
			return;
		if (pm == null && tc != null) {
			pm = new PopupManager(tc, "", true);
		}
		if (n < 0 || n >= tooltips.size() || tooltips.elementAt(n) == null) {
			pm.setContent(null);
			return;
		}
		Object tt = tooltips.elementAt(n);
		if (tt instanceof String) {
			pm.setText(tt.toString());
		} else {
			pm.setContent((Component) tt);
		}
	}

	/**
	* Sets the Drawer object for the tab with given index
	* that will use given URL to fetch the image to be drawn in the tab
	*/
	public void setTabIcon(int tabN, String imgURL) {
		if (tabN < 0 || tabN >= comp.size() || imgURL == null)
			return;
		if (icons == null) {
			icons = new Vector(5, 5);
		}
		for (int i = icons.size(); i < comp.size(); i++) {
			icons.addElement(null);
		}
		Drawer imgDrawer = new ImageDrawer(imgURL, this);
		icons.setElementAt(imgDrawer, tabN);
		if (tc != null) {
			tc.setTabIcon(imgDrawer, tabN);
			//System.out.println("ImageDrawer set="+imgDrawer.toString());
		} else {
			System.out.println("Cannot set image for tab: no canvas created yet");
		}
	}

	/**
	* Sets the Drawer object for the tab with given index
	*/
	public void setTabIconDrawer(int tabN, Drawer drw) {
		if (tabN < 0 || tabN >= comp.size())
			return;
		if (icons == null) {
			icons = new Vector(5, 5);
		}
		for (int i = icons.size(); i < comp.size(); i++) {
			icons.addElement(null);
		}
		Drawer imgDrawer = drw;
		icons.setElementAt(imgDrawer, tabN);
		if (tc != null) {
			tc.setTabIcon(imgDrawer, tabN);
			//System.out.println("ImageDrawer set="+imgDrawer.toString());
		} else {
			System.out.println("Cannot set image for tab: no canvas created yet");
		}
	}

	/**
	* Sets the option to show name of currently selected tab in Bold style font
	*/
	public void setHighlightSelectedTab(boolean highlight) {
		if (tc != null) {
			tc.setHighlightNameofSelectedTab(highlight);
		}
	}

	/**
	* Sets the option to show TabSelector control in the position at the bottom
	*/
	public void setTabsAtTheBottom(boolean flag) {
		if (areTabsAtTheBottom == flag)
			return;
		areTabsAtTheBottom = !areTabsAtTheBottom;
		makeLayout();
	}

	/**
	* Sets the option to hide tab names if icons are also assigned to them
	*/
	public void setHideTabNamesWhenIcons(boolean flag) {
		if (hideTabNamesWhenIcons == flag)
			return;
		// try to prevent simultaneous hiding icons and names of tabs
		if (forceHideTabIcons && flag) {
			System.out.println("Cannot set this flag simultaneously with: forceHideTabIcons=" + forceHideTabIcons);
			return;
		}
		hideTabNamesWhenIcons = !hideTabNamesWhenIcons;
		makeLayout();
	}

	/**
	* Sets the option to force tab icons to be hidden
	*/
	public void setHideTabIcons(boolean flag) {
		if (forceHideTabIcons == flag)
			return;
		// try to prevent simultaneous hiding icons and names of tabs
		if (hideTabNamesWhenIcons && flag) {
			System.out.println("Cannot set this flag simultaneously with: hideTabNamesWhenIcons=" + hideTabNamesWhenIcons);
			return;
		}
		forceHideTabIcons = !forceHideTabIcons;
		makeLayout();
	}

	/**
	* Sets the option to force tab selector to be hidden if the only one component
	* has been added to the tabbed panel
	*/
	public void setShowTabSelectorOnDemand(boolean flag) {
		if (showTabSelectorOnDemand == flag)
			return;
		showTabSelectorOnDemand = !showTabSelectorOnDemand;
		makeLayout();
	}

	/**
	* Forces tab selector control to be redrawn
	* It is needed to actualize names and icons if they were possibly updated
	*/
	public void forceRepaintTabSelector() {
		if (tc != null) {
			tc.repaint();
		}
	}

	@Override
	public int getPanelWidth() {
		return getSize().width;
	}
	/*
	public Dimension getPreferredSize () {
	  Dimension d=cpan.getPreferredSize();
	  System.out.println("cpan: "+d.height);
	  for (int i=0; i<cpan.getComponentCount(); i++) {
	    d=cpan.getComponent(i).getPreferredSize();
	    System.out.println(i+") "+cpan.getComponent(i).getName()+": "+d.height);
	  }
	  d=super.getPreferredSize();
	  System.out.println("TabbedPanel: "+d.height);
	  return d;
	}
	*/
}
