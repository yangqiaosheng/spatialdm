package spade.lib.basicwin;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.WinSpec;

/**
* Special panel implementing something like MDI Interface inside and allowance of
* layout switching.
*/
public class LayoutSelector extends Panel implements ActionListener, WindowListener, ItemListener, SaveableTool {
	/**
	* The owner of the Frame that waits for a notification about the frame being
	* finished
	*/
	// static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");

	public static final int HORISONTAL = 1;
	public static final int VERTICAL = 0;
	public static final int MDI_VERT = 2;
	public static final int MDI_HOR = 3;
	public static final int MATRIX_HOR = 4;
	public static final int TABBED = 5;
	public static final int WINDOWS = 6;

	String cmd_layouts[] = { "layout_hor_spl", "layout_vert_spl", "layout_matrix_spl_v", "layout_matrix_spl_h", "layout_multi_spl_h", "layout_tabs_spl", "layout_windows_spl" };
	protected Component owner = null;
	//protected ActionListener owner=null;

	protected Component content = null;
	/*
	*  List of currently added components
	*/
	protected Vector components = null;
	/*
	*  List of windows in case of WINDOWS layout
	*/
	protected Vector windows = null;

	protected int maxComponentsPerRow = 2;
	/*
	*  Layout change listeners
	*/
	protected Vector lcList = null;
	/*
	*  Currently selected layout
	*/
	protected int layoutType = VERTICAL;
	/*
	* Control panel with 2 button groups to switch layouts
	* and handle windows apperance (if the case)
	*/
	Panel pControl = null, pAllControls = null;
	TImgButtonGroup tibgrpLayoutType = new TImgButtonGroup();
	TImgButtonGroup tibgrpWindows = new TImgButtonGroup();

	List winList = null;
	Choice compPerRowList = null;
	Panel winListPanel = null;
	Panel compPerRowListPanel = null;
	/*
	* Main panel with current layout
	*/
	Panel pContent = null;

	// to be removed
	Supervisor sup = null;
	// internal counter of currently added elements
	int nElements = 0;

	boolean debug = false;

	int layoutTypes[] = null;
	/**
	* The keyword used in the opening tag of a stored state description
	* of this panel. The keyword may differ depending on the type of tools
	* included in the panel. The default tag is "tool_window".
	*/
	protected String tagName = "tool_window";

	public LayoutSelector() {
		this(null, 0, false, null);
	}

	public LayoutSelector(Component owner) {
		this(owner, 0, false, null);
	}

	public LayoutSelector(int defaultType) {
		this(null, defaultType, false, null);
	}

	public LayoutSelector(Component owner, int defaultType) {
		this(owner, defaultType, false, null);
	}

	public LayoutSelector(Component owner, int defaultType, boolean debug) {
		this(owner, defaultType, debug, null);
	}

	public LayoutSelector(Component owner, int defaultType, boolean debug, int[] layoutTypes) {
		setOwner(owner);
		this.debug = debug;
		this.layoutTypes = layoutTypes;
		setLayout(new BorderLayout());
		layoutType = defaultType;

		switch (layoutType) {
		case HORISONTAL:
			pContent = new SplitPanel(true);
			break;
		case VERTICAL:
			pContent = new SplitPanel(false);
			break;
		case MDI_HOR:
			pContent = new MDIPanel(true);
			break;
		case MDI_VERT:
			pContent = new MDIPanel(false);
			break;
		case MATRIX_HOR:
			pContent = new MultiSplitPanel(3);
			break;
		case TABBED:
			pContent = new TabbedPanel();
			break;
		case WINDOWS:
			;//makeWindowLayout();
		}
		Component comp = null;
		components = new Vector(20, 10);
		/* debugging purposes: creation of sample components to add as content */
		if (debug) {
			for (int i = 0; i < 7; i++) {
				Button btn = new Button("Button " + i);
				Panel pnl = new Panel(new BorderLayout());
				pnl.add(btn, "North");
				pnl.add(new Label("Component " + i, Label.CENTER));
				pnl.setBackground(Color.getHSBColor(i / (1.0f * 7), 0.7f, 0.6f));
				// pnl.getGraphics().drawString("This is test component: "+pnl.getName(), 10,10);
				comp = pnl;
				addElement(comp);
			}
		}
		makeControlPanel();
		if (pContent != null) {
			addContent(pContent);
		} else {
			addContent(winListPanel);
		}
	}

	public void setOwner(Component c) {
		if (owner == null) {
			owner = c;
		}
		if (owner instanceof Window) {
			((Window) owner).addWindowListener(this);
		}
	}

	/**
	* Returns the number of currently existing components
	*/
	public int getElementCount() {
		if (components == null)
			return 0;
		return components.size();
	}

	/**
	* Returns the component with the given index (not its ComponentWindow
	* but the component itself).
	*/
	public Component getElement(int idx) {
		if (components == null || idx < 0 || idx >= components.size())
			return null;
		if (components.elementAt(idx) instanceof ComponentWindow)
			return ((ComponentWindow) components.elementAt(idx)).getContent();
		return (Component) components.elementAt(idx);
	}

	/**
	*  Adds new component to current layout
	*/
	public void addElement(Component c) {
		ComponentWindow cw = new ComponentWindow(c, String.valueOf(++nElements), this);
		cw.setHasExpandButton(false);

		switch (layoutType) {
		case HORISONTAL:
		case VERTICAL: {
			SplitPanel sp = (SplitPanel) pContent;
			int nComp = sp.getSplitComponentCount();
			sp.addSplitComponent(cw);
			sp.forceEqualizeParts();
			break;
		}
		case MDI_HOR:
		case MDI_VERT:
			((MDIPanel) pContent).addWindow(c);
			break;
		case MATRIX_HOR:
			((MultiSplitPanel) pContent).addComponent(cw);
			break;
		case TABBED: {
			TabbedPanel tabP = (TabbedPanel) pContent;
			tabP.addComponent(c.getName(), cw);
			tabP.showTab(tabP.getTabCount() - 1);
			break;
		}
		case WINDOWS:
			addWindow(c);
		}
		cw.getContent().setVisible(true);
		components.addElement(cw);
		CManager.validateFully(this);
	}

	/**
	*  Removes specified component from current layout
	*/
	public void removeElement(Component c) {
		//System.out.println("Component "+c.getName()+" will be removed from "+getLayoutTypeCommand(layoutType));
		pContent.setVisible(false);
		if (layoutType == WINDOWS) {
			Component cInsideWindow = c;
			if (c instanceof ComponentWindow) {
				cInsideWindow = ((ComponentWindow) c).getContent();
			}
			removeWindow(getFrameByContent(cInsideWindow));
		} else {
			switch (layoutType) {
			case HORISONTAL:
			case VERTICAL: {
				SplitPanel sp = (SplitPanel) pContent;
				sp.removeSplitComponent(sp.getComponentIndex(c));
				break;
			}
			case MDI_HOR:
			case MDI_VERT: {
				if (c instanceof ComponentWindow) {
					((MDIPanel) pContent).removeWindow((ComponentWindow) c);
				} else {
					((MDIPanel) pContent).removeComponent(c);
				}
				break;
			}
			case MATRIX_HOR: {
				MultiSplitPanel msp = (MultiSplitPanel) pContent;
				msp.removeComponent(c);
				break;
			}
			case TABBED:
				((TabbedPanel) pContent).removeComponent(c);
				break;
			}
			components.removeElement(c);
			if (!CManager.isComponentDestroyed(c)) {
				CManager.destroyComponent(c);
			}
			if (isEmpty()) {
				Window fr = CManager.getWindow(this);
				if (fr != null) {
					fr.dispose();
				}
			} else {
				validate();
			}
		}
		pContent.setVisible(true);
	}

	protected boolean contains(int lt) {
		if (layoutTypes == null)
			return true;
		for (int layoutType2 : layoutTypes) {
			if (layoutType2 == lt)
				return true;
		}
		return false;
	}

	protected void makeControlPanel() {
		if (pControl != null)
			return;
		pControl = new Panel(new RowLayout(3, 3));
		pAllControls = new Panel(new BorderLayout());
		pAllControls.setBackground(Color.lightGray);
		pAllControls.add(pControl, BorderLayout.CENTER);
		add(pAllControls, BorderLayout.NORTH);

		if (tibgrpLayoutType == null) {
			tibgrpLayoutType = new TImgButtonGroup();
		}
		if (tibgrpWindows == null) {
			tibgrpWindows = new TImgButtonGroup();
		}
		tibgrpLayoutType.addActionListener(this);
		tibgrpWindows.addActionListener(this);

		TImgButton b = null;
		if (contains(VERTICAL)) {
			b = new TImgButton("/icons/vert_layout1.gif");
			b.setActionCommand(cmd_layouts[VERTICAL]);
			b.addActionListener(this);
			tibgrpLayoutType.addButton(b);
			new PopupManager(b, "All components are arranged vertically as a column", true);
		}

		if (contains(HORISONTAL)) {
			b = new TImgButton("/icons/hor_layout1.gif");
			b.setActionCommand(cmd_layouts[HORISONTAL]);
			b.addActionListener(this);
			tibgrpLayoutType.addButton(b);
			new PopupManager(b, "All components are arranged horisontally as a row", true);
		}

		if (contains(MDI_HOR)) {
			b = new TImgButton("/icons/matrix_h2.gif");
			b.setActionCommand(cmd_layouts[MDI_HOR]);
			b.addActionListener(this);
			tibgrpLayoutType.addButton(b);
			new PopupManager(b, "All components are arranged horisontally as a row, except one, which always occupies the whole row", true);
		}

		if (contains(MDI_VERT)) {
			b = new TImgButton("/icons/matrix_v2.gif");
			b.setActionCommand(cmd_layouts[MDI_VERT]);
			b.addActionListener(this);
			tibgrpLayoutType.addButton(b);
			new PopupManager(b, "All components are arranged vertically as a column, except one, which always occupies the whole column", true);
		}
		if (contains(MATRIX_HOR)) {
			b = new TImgButton("/icons/matrix.gif");
			b.setActionCommand(cmd_layouts[MATRIX_HOR]);
			b.addActionListener(this);
			tibgrpLayoutType.addButton(b);
			new PopupManager(
					b,
					"All components are arranged as a matrix with given fixed number of horisontal cells. Each next component is always added to last one. Possible a new row of the matrix is created if no cell is available in current row for adding new component. Components in the last row try to occupy all available space",
					true);
		}

		if (contains(TABBED)) {
			b = new TImgButton("/icons/tab_layout.gif");
			b.setActionCommand(cmd_layouts[TABBED]);
			b.addActionListener(this);
			tibgrpLayoutType.addButton(b);
			new PopupManager(b, "All components are arranged as tabs inside tabbed panel", true);
		}

		//b=new Button("Separate windows");
		if (contains(WINDOWS)) {
			b = new TImgButton("/icons/win_layout.gif");
			b.setActionCommand(cmd_layouts[WINDOWS]);
			b.addActionListener(this);
			tibgrpLayoutType.addButton(b);
			//b.setEnabled(false);
			new PopupManager(b, "All components are arranged as separate windows (function is at debugging stage now)", true);
		}

		pControl.add(tibgrpLayoutType);

		// separate layout types panel from other controls
		pControl.add(new Label("  ", Label.CENTER));
/*
    b=new TImgButton("/icons/shrink2.gif");
    b.setActionCommand("windows_shrink");
    b.addActionListener(this);
    tibgrpWindows.addButton(b);
    new PopupManager(b,"Minimize all windows",true);
*/
		b = new TImgButton("/icons/show_wnd.gif");
		b.setActionCommand("windows_show");
		b.addActionListener(this);
		tibgrpWindows.addButton(b);
		new PopupManager(b, "Show all windows", true);

		b = new TImgButton("/icons/hide_wnd.gif");
		b.setActionCommand("windows_hide");
		b.addActionListener(this);
		tibgrpWindows.addButton(b);

		pAllControls.add(tibgrpWindows, BorderLayout.EAST);

		//winListPanel=new Panel(new FlowLayout(FlowLayout.LEFT,2,2));
		winListPanel = new Panel(new BorderLayout());
		winListPanel.setBackground(pControl.getBackground());
		//pControl.add(winListPanel);
		new PopupManager(b, "Hide all windows", true);

		if (layoutType != WINDOWS) {
			tibgrpWindows.setEnabled(false);
			tibgrpWindows.setVisible(false);
			winListPanel.setVisible(false);
		}
		if (layoutType == MATRIX_HOR) {
			makeNumberSelector();
		}

		for (int i = 0; i < tibgrpLayoutType.buttons.size(); i++) {
			TImgButton bt = (TImgButton) tibgrpLayoutType.buttons.elementAt(i);

			if (bt.getActionCommand().equals(cmd_layouts[layoutType])) {
				tibgrpLayoutType.setSelect(i);
				break;
			}
		}
	}

	private void makeNumberSelector() {
		if (compPerRowList != null)
			return;
		compPerRowList = new Choice();
		for (int i = 2; i < 9; i++) {
			compPerRowList.add(String.valueOf(i));
		}
		compPerRowList.select(maxComponentsPerRow - 2);
		compPerRowListPanel = new Panel(new FlowLayout(FlowLayout.LEFT, 2, 2));
		compPerRowListPanel.add(new Label(" "));
		compPerRowListPanel.add(compPerRowList);
		compPerRowListPanel.add(new Label("components per row"));
		compPerRowList.addItemListener(this);
		pControl.add(compPerRowListPanel);
	}

	public void addLayoutChangeListener(LayoutChangeListener l) {
		if (lcList == null) {
			lcList = new Vector(2, 2);
		} else {
			for (int i = 0; i < lcList.size(); i++)
				if (lcList.elementAt(i).equals(l)) {
					System.out.println("WARNING: Listener " + l + " was already added!");
					return;
				}
		}
		lcList.addElement(l);
	}

	public void removeLayoutChangeListener(LayoutChangeListener l) {
		if (l == null)
			return;
		for (int i = 0; i < lcList.size(); i++)
			if (lcList.elementAt(i).equals(l)) {
				lcList.removeElementAt(i);
			}
	}

	public void notifyLayoutChanged() {
		if (lcList == null || lcList.size() < 1)
			return;
		LayoutChangeListener l;
		for (int i = 0; i < lcList.size(); i++) {
			l = (LayoutChangeListener) (lcList.elementAt(i));
			l.layoutChanged(layoutType);
		}
	}

	/*
	* This is internal function, adds already formed layout
	* and validates all components
	*/
	protected void addContent(Component c) {
		content = c;
		if (c == null)
			return;
		setVisible(false);
		add(content, "Center");
		if (c instanceof Panel) {
			pContent = (Panel) c;
		}
		setVisible(true);
		pControl.invalidate();
		content.invalidate();
		CManager.validateAll(content);
		if (owner == null) {
			owner = CManager.getFrame(this);
		}
		if (owner != null && owner instanceof Frame) {
			Frame fr = ((Frame) owner);
			fr.pack();
			Dimension ss = fr.getToolkit().getScreenSize();
			Rectangle b = fr.getBounds();
			int x = b.x, y = b.y, w = b.width, h = b.height;
			if (w > ss.width - 50) {
				w = ss.width + 5;
			}
			if (h > ss.height - 50) {
				h = ss.height - 50;
			}
			if (x + w > ss.width) {
				x = ss.width - w;
			}
			if (y + h > ss.height) {
				y = ss.height - h;
			}
			fr.setBounds(x, y, w, h);
		}
	}

	/*
	* This is internal function, it removes current layout
	*/
	protected void removeContent() {
		setVisible(false);
		if (content != null) {
			removeAll();
			content = null;
			pContent = null;
		}
		// restore contol panel
		add(pAllControls, BorderLayout.NORTH);
		pControl.invalidate();
		CManager.validateAll(this);
		setVisible(true);
		if (owner == null) {
			owner = CManager.getFrame(this);
		}
		if (owner != null && owner instanceof Frame) {
			((Frame) owner).pack();
		}
	}

	public void close() {
		if (owner != null && owner instanceof ActionListener) {
			((ActionListener) owner).actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "closed"));
		}
		if (layoutType == WINDOWS) {
			destroyAllWindows();
		}
		if (owner != null && owner instanceof Window) {
			((Window) owner).dispose();
		}
		CManager.destroyComponent(this);
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (checkSameLayout(cmd))
			return;
		if (cmd.indexOf("windows_") > -1 && layoutType == WINDOWS) {
			if (cmd.endsWith("show")) {
				showAllWindows();
			} else if (cmd.endsWith("shrink")) {
				minimizeAllWindows();
			}
			if (cmd.endsWith("hide")) {
				hideAllWindows();
			}
		} else if (cmd.indexOf("layout_") > -1) {
			if (layoutType == TABBED) {
				for (int i = 0; i < components.size(); i++) {
					ComponentWindow cw = (ComponentWindow) components.elementAt(i);
					cw.setVisible(true);
					cw.getContent().setVisible(true);
				}
				//System.out.println("...after Tabbed");
			}
			if (layoutType == MATRIX_HOR) {
				compPerRowListPanel.setVisible(false);
			}

			if (layoutType == WINDOWS && !cmd.equals(cmd_layouts[WINDOWS])) {
				closeAllWindows();
			} else {
				removeContent();
			}

			if (layoutType == MDI_HOR || layoutType == MDI_VERT) {
				for (int i = 0; i < components.size(); i++) {
					ComponentWindow cw = (ComponentWindow) components.elementAt(i);
					cw.setHasExpandButton(false);
				}
			}
			changeLayout(cmd);
		} else if (cmd.startsWith("expand") || cmd.startsWith("close")) {
			int breakIdx = cmd.indexOf('-'); // index of break between cmd & src
			String cmd1 = cmd.substring(0, breakIdx); // actually command itself
			String sourceID = cmd.substring(breakIdx + 1); // ID of command's source

			ComponentWindow compWin = getComponentById(sourceID);
			if (compWin == null) {
				System.out.println("Cannot detect source of command " + cmd);
				return;
			}

			if (cmd1.equalsIgnoreCase("expand") && (layoutType == MDI_HOR || layoutType == MDI_VERT)) {
				((MDIPanel) pContent).expandWindow(compWin);
			}
			if (cmd1.equalsIgnoreCase("close")) {
				removeElement(compWin);
			}
		}
	}

	protected ComponentWindow getComponentById(String id) {
		if (id == null || id.length() < 1)
			return null;
		//System.out.println("Trying to find component with ID="+id);
		ComponentWindow cw = null;
		boolean found = false;
		for (int i = 0; i < components.size() && !found; i++) {
			cw = (ComponentWindow) components.elementAt(i);
			if (id.equalsIgnoreCase(cw.getComponentID())) {
				found = true;
			}
		}
		return cw;
	}

	protected Frame getFrameByContent(Component comp) {
		if (comp == null)
			return null;
		return CManager.getFrame(comp);
	}

	protected void makeSplitLayout(int direction) {
		if (components == null || components.size() < 1)
			return; // nothing to do
		pContent = new SplitPanel(direction == SplitLayout.VERT);
		ComponentWindow c = null;
		//Component c=null;

		for (int i = 0; i < components.size(); i++) {
			//c=(Component)components.elementAt(i);
			c = (ComponentWindow) components.elementAt(i);
			((SplitPanel) pContent).addSplitComponent(c);
		}
		addContent(pContent);
	}

	protected void makeMDILayout(boolean horisontal) {
		if (components == null || components.size() < 1)
			return; // nothing to do
		MDIPanel mdip = new MDIPanel(horisontal);
		mdip.setEqualizeComponentsInStack(true);
		ComponentWindow cw = null;
		for (int i = 0; i < components.size(); i++) {
			cw = (ComponentWindow) components.elementAt(i);
			cw.setHasExpandButton(true);
			mdip.addComponent(cw);
			//mdip.addWindow((ComponentWindow)components.elementAt(i));
		}
		addContent(mdip);
	}

	protected void makeMutliSplitLayout() {
		if (components == null || components.size() < 1)
			return; // nothing to do
		MultiSplitPanel msp = new MultiSplitPanel(maxComponentsPerRow);
		for (int i = 0; i < components.size(); i++) {
			msp.addComponent((ComponentWindow) components.elementAt(i));
		}
		addContent(msp);
		makeNumberSelector();
		compPerRowListPanel.setVisible(true);
	}

	protected void makeTabbedLayout() {
		if (components == null || components.size() < 1)
			return; // nothing to do
		TabbedPanel tp = new TabbedPanel();
		ComponentWindow cw = null;

		for (int i = 0; i < components.size(); i++) {
			cw = (ComponentWindow) components.elementAt(i);
			//System.out.println("Found component: "+cw.getName());
			//System.out.println("Making new tab for component: "+cw.getComponentName());
			tp.addComponent(cw.getComponentName(), cw);
		}
		//System.out.println("Tabbed panel debug mode="+debug);
		tp.makeLayout(true);
		//tp.makeLayout();
		if (debug) {
			tp.setHighlightSelectedTab(true);
			tp.setHideTabNamesWhenIcons(true);
			//System.out.println("Setting images for tabbed panel");
			tp.setTabIcon(0, "/icons/vert_layout1.gif");

			//tp.setTabIcon(0,"/icons/arrow.gif");
			tp.setTabIcon(1, "/icons/hor_layout1.gif");
			tp.setTabIcon(2, "/icons/matrix_h2.gif");
			tp.setTabIcon(3, "/icons/matrix_v2.gif");
			tp.setTabIcon(4, "/icons/matrix.gif");
			tp.setTabIcon(5, "/icons/tab_layout.gif");
			tp.setTabIcon(6, "/icons/win_layout.gif");

		}
		tp.showTab(0);
		addContent(tp);
	}

	protected void addWindow(Component c) {
		String sName = c.getName();
		Frame f = new Frame(sName);
		f.setName(sName);
		f.add(c);
		f.pack();
		f.addWindowListener(this);
		Dimension ws = f.getSize();
		Dimension ss = getToolkit().getScreenSize();
		int x = ss.width - ws.width, y = 100;
		if (windows == null || windows.size() < 1) {
			Frame mainFr = CManager.getFrame(this);
			if (mainFr != null) {
				Rectangle b = mainFr.getBounds();
				x = b.x;
				y = b.y + b.height;
			}
		} else {
			Frame fr = (Frame) windows.elementAt(windows.size() - 1);
			Rectangle b = fr.getBounds();
			x = b.x;
			y = b.y + b.height;
		}
		if (x < ss.width && x + ws.width > ss.width) {
			x = ss.width - ws.width;
		}
		if (y + ws.height > ss.height) {
			y = ss.height - ws.height;
		}
		if (x < 0) {
			x = 0;
		}
		if (y < 0) {
			y = 0;
		}
		f.setLocation(x, y);
		f.setVisible(true);
		if (windows == null) {
			windows = new Vector(10, 10);
		}
		windows.addElement(f);
		// locate new window correctly now
		/*
		if (owner!=null && owner instanceof Component) {
		  Component cOwner=(Component)owner;
		  Point ploc=cOwner.getLocation(); // location of layout selector frame
		  for (int i=0; i<windows.size(); i++) {
		    ploc.x+=((Frame)windows.elementAt(i)).getSize().width;
		  }
		  int pos_x=ploc.x, pos_y=ploc.y;
		  pos_y+=pControl.getSize().height;
		  f.setLocation(pos_x,pos_y);
		  if (ploc.x>=Toolkit.getDefaultToolkit().getScreenSize().width) {
		    ploc.x=getLocation().x;
		    ploc.y+=f.getSize().height;
		    f.setLocation(ploc.x,ploc.y);
		  }
		} else f.setLocation(10*windows.size(),10*windows.size());
		*/
		if (winList == null) {
			winList = new List();
			winListPanel.add(winList, "Center");
			winList.addItemListener(this);
		}
		winList.add(sName);
	}

	protected void makeWindowLayout() {
		if (components == null || components.size() < 1)
			return; // nothing to do
		//removeContent();
		//if (owner instanceof Frame) ((Frame)owner).pack();
		addContent(winListPanel);
		Frame f = CManager.getFrame(this);
		if (f != null) {
			f.pack();
		}

		ComponentWindow cw = null;
		Component c = null;
		for (int i = 0; i < components.size(); i++) {
			cw = (ComponentWindow) components.elementAt(i);
			if (cw == null) {
				continue;
			}
			c = cw.getContent();
			//cw.setContent(null);
			cw.remove(c);
			addWindow(c);
		}
		winListPanel.setVisible(true);
		if (!tibgrpWindows.isEnabled()) {
			tibgrpWindows.setEnabled(true);
		}
		if (!tibgrpWindows.isVisible()) {
			tibgrpWindows.setVisible(true);
		}
	}

	/*
	*  Removes frame from Layout Selector completely
	*  It is needed if close button of frame was pressed by user or
	*  all frames should be destroyed if Layout Selector was closed
	*/
	protected void removeWindow(Frame f) {
		if (f == null || windows == null)
			return;
		removeWindow(windows.indexOf(f));
	}

	/**
	 * Removes the frame with the given index
	 */
	protected void removeWindow(int idx) {
		if (idx < 0 || windows == null || idx >= windows.size())
			return;
		Frame f = (Frame) windows.elementAt(idx);
		winList.remove(idx);
		windows.removeElementAt(idx);
		components.removeElementAt(idx);
		CManager.destroyComponent(f);
		f.dispose();
		if (isEmpty()) {
			Window fr = CManager.getWindow(this);
			if (fr != null) {
				fr.dispose();
			}
		}
	}

	public void windowClosing(WindowEvent evt) {
		Object src = evt.getSource();
		if (src.equals(this)) {
			close();
		} else if (src.equals(owner)) {
			close();
		} else {
			removeWindow((Frame) src);
		}

	}

	public void windowActivated(WindowEvent evt) {
	}

	public void windowDeactivated(WindowEvent evt) {
	}

	public void windowOpened(WindowEvent evt) {
	}

	public void windowClosed(WindowEvent evt) {
	}

	public void windowIconified(WindowEvent evt) {
	}

	public void windowDeiconified(WindowEvent evt) {
	}

	public void showAllWindows() {
		if (windows != null) {
			for (int i = 0; i < windows.size(); i++) {
				Frame f = (Frame) windows.elementAt(i);
				if (f == null) {
					continue;
				}
				f.setVisible(true);
				// Java 2 only
				//if (f.getState()==f.ICONIFIED) f.setState(f.NORMAL);
			}
		}
	}

	public void hideAllWindows() {
		if (windows != null) {
			for (int i = 0; i < windows.size(); i++) {
				((Frame) windows.elementAt(i)).setVisible(false);
			}
		}
	}

	public void minimizeAllWindows() {
		// Java 2 only
		/*
		if (windows!=null) {
		  for (int i=0; i<windows.size(); i++) {
		    Frame f=(Frame)windows.elementAt(i);
		    if (f==null) continue;
		    f.setVisible(false);
		    f.setState(Frame.ICONIFIED);
		    f.setVisible(true);
		  }
		}
		*/
	}

/*
*  The function closes all windows without removing components inside.
*  This is needed to switch from WINDOWS layout to other types.
*/
	public void closeAllWindows() {
		if (windows == null)
			return;
		for (int i = 0; i < windows.size(); i++) {
			Frame win = (Frame) windows.elementAt(i);
			if (win == null) {
				continue;
			}
			win.dispose();
			Component cInFrame = win.getComponent(0);
			win.removeAll();
			if (cInFrame != null) {
				System.out.println(cInFrame.getName() + " will be removed from this frame");
				ComponentWindow cw = (ComponentWindow) components.elementAt(i);
				System.out.println(cInFrame.getName() + " will be added to ComponentWindow");
				cw.setContent(cInFrame);
				cInFrame.setVisible(true);
			}
		}
		windows.removeAllElements();
		winList.removeAll();
		tibgrpWindows.setEnabled(false);
		tibgrpWindows.setVisible(false);
		winListPanel.setVisible(false);
	}

/*
*  The function destroys all windows with removing components inside.
*  This is needed to finish work with Layout Selector
*  if WINDOWS layout is active
*/
	public void destroyAllWindows() {
		if (windows != null) {
			for (int i = windows.size() - 1; i >= 0; i--) {
				removeWindow(i);
			}
		}
	}

	/**
	* Removes subwindows that have been "destroyed", i.e. are no more valid
	*/
	public void removeDestroyedComponents() {
		for (int i = components.size() - 1; i >= 0; i--) {
			ComponentWindow c = (ComponentWindow) components.elementAt(i);
			if (CManager.isComponentDestroyed(c.getContent())) {
				removeElement(c);
			}
		}
		if (components.size() < 1) {
			close();
		}
	}

	/**
	* Returns true if contains no components
	*/
	public boolean isEmpty() {
		return components.size() < 1;
	}

	public void setMaxComponentsInRow(int maxN) {
		maxComponentsPerRow = maxN;
		if (layoutType == MATRIX_HOR) {
			removeContent();
			makeMutliSplitLayout();
		}
	}

	public String getLayoutTypeCommand(int type) {
		if (type < 0 || type >= cmd_layouts.length)
			return null;
		return cmd_layouts[type];
	}

	public void setLayoutType(int type) {
		if (layoutType == type)
			return;
//    layoutType=type;
		changeLayout(cmd_layouts[type]);
	}

	private boolean checkSameLayout(String cmd) {
		boolean sameLayout = (cmd.equals(cmd_layouts[HORISONTAL]) && layoutType == HORISONTAL) || (cmd.equals(cmd_layouts[VERTICAL]) && layoutType == VERTICAL) || (cmd.equals(cmd_layouts[MDI_VERT]) && layoutType == MDI_VERT)
				|| (cmd.equals(cmd_layouts[MDI_HOR]) && layoutType == MDI_HOR) || (cmd.equals(cmd_layouts[MATRIX_HOR]) && layoutType == MATRIX_HOR) || (cmd.equals(cmd_layouts[TABBED]) && layoutType == TABBED)
				|| (cmd.equals(cmd_layouts[WINDOWS]) && layoutType == WINDOWS);
		return sameLayout;
	}

	private void changeLayout(String cmd) {
		invalidate();
		setVisible(false);
		if (cmd.equals(cmd_layouts[HORISONTAL])) {
			if (layoutType == HORISONTAL)
				return;
			//System.out.println("Horisontal split layout selected");
			makeSplitLayout(SplitLayout.VERT);
			layoutType = HORISONTAL;
		} else if (cmd.equals(cmd_layouts[VERTICAL])) {
			if (layoutType == VERTICAL)
				return;
			//System.out.println("Vertical split layout selected");
			makeSplitLayout(SplitLayout.HOR);
			layoutType = VERTICAL;
		} else if (cmd.equals(cmd_layouts[MDI_VERT])) {
			if (layoutType == MDI_VERT)
				return;
			//System.out.println("MDI split layout vertical selected");
			makeMDILayout(false);
			layoutType = MDI_VERT;
		} else if (cmd.equals(cmd_layouts[MDI_HOR])) {
			if (layoutType == MDI_HOR)
				return;
			//System.out.println("MDI split layout horisontal selected");
			makeMDILayout(true);
			//makeMatrixLayout();
			layoutType = this.MDI_HOR;
		} else if (cmd.equals(cmd_layouts[MATRIX_HOR])) {
			if (layoutType == MATRIX_HOR)
				return;
			//System.out.println("Multisplit layout (horisontal) selected");
			makeMutliSplitLayout();
			layoutType = this.MATRIX_HOR;
		} else if (cmd.equals(cmd_layouts[TABBED])) {
			if (layoutType == TABBED)
				return;
			//System.out.println("Tabbed layout selected");
			makeTabbedLayout();
			layoutType = TABBED;
		} else if (cmd.equals(cmd_layouts[WINDOWS])) {
			if (layoutType == WINDOWS)
				return;
			makeWindowLayout();
			layoutType = WINDOWS;
			//System.out.println("Windows mode selected");
		}
		setVisible(true);
		//CManager.validateFully(pContent);
		for (int i = 0; i < components.size(); i++) {
			ComponentWindow cw = (ComponentWindow) components.elementAt(i);
			cw.getContent().setVisible(true);
			cw.validateComponentWindow();
		}
		Window wnd = CManager.getFrame(this);
		if (wnd != null) {
			wnd.pack();
			Dimension dScreenSize = getToolkit().getDefaultToolkit().getScreenSize();
			if (dScreenSize != null) {
				int win_height = wnd.getSize().height, win_width = wnd.getSize().width;
				if (win_height > dScreenSize.height) {
					win_height = dScreenSize.height - 10;
				}
				if (win_width > dScreenSize.width) {
					win_width = dScreenSize.width - 10;
				}
				if (wnd.getSize().height != win_height || wnd.getSize().width != win_width) {
					wnd.setSize(win_width, win_height);
				}
			}
		}
		notifyLayoutChanged();
	}

	public void itemStateChanged(ItemEvent e) {
		Object src = e.getSource();
		if (src.equals(winList)) {
			if (windows == null)
				return;
			int idx = winList.getSelectedIndex();
			if (idx < 0 || idx >= windows.size())
				return;
			Frame selF = (Frame) windows.elementAt(idx);
			selF.setVisible(true);
			selF.toFront();
		} else if (src.equals(compPerRowList)) {
			setMaxComponentsInRow(compPerRowList.getSelectedIndex() + 2);
			//System.out.println("Selected "+selIdx+" components per row");
		}
	}

	public Panel getControlPanel() {
		return pControl;
	}

	/**
	* Sets the keyword to be used in the opening tag of a stored state description
	* of this panel. The keyword may differ depending on the type of tools
	* included in the panel.
	*/
	public void setTagName(String name) {
		if (name != null && name.length() > 0) {
			tagName = name;
		}
	}

//ID
	/**
	* The keyword used in the opening tag of a stored state description
	* of this panel. The keyword may differ depending on the type of tools
	* included in the panel. The default tag is "tool_window".
	*/
	public String getTagName() {
		return tagName;
	}

	/**
	 * Returns the specification (i.e. state description) of this tool for storing
	 * in a file. The specification must allow correct re-construction of the tool.
	 */
	public Object getSpecification() {
		WinSpec spec = new WinSpec();
		spec.tagName = getTagName();
		Frame chartFrame = CManager.getFrame(this);
		if (chartFrame != null) {
			spec.title = chartFrame.getTitle();
			spec.bounds = chartFrame.getBounds();
		}
		spec.properties = getProperties();
		return spec;
	}

	private Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		String layout = "";
		switch (layoutType) {
		case HORISONTAL:
			layout = "horizontal";
			break;
		case VERTICAL:
			layout = "vertical";
			break;
		case MDI_HOR:
			layout = "MDI_hor";
			break;
		case MDI_VERT:
			layout = "MDI_vert";
			break;
		case MATRIX_HOR:
			layout = "matrix";
			break;
		case TABBED:
			layout = "tabbed";
			break;
		case WINDOWS:
			layout = "windows";
			break;
		default:
			break;
		}
		prop.put("layout", layout);

		if (layoutType == WINDOWS) {
			String bounds = "";
			for (int i = 0; i < windows.size(); i++) {
				Rectangle bRect = ((Frame) windows.elementAt(i)).getBounds();
				if (bounds.length() > 0) {
					bounds += ",";
				}
				bounds += "(" + bRect.x + "," + bRect.y + "," + bRect.width + "," + bRect.height + ")";
			}
			prop.put("placement", bounds);
		} else if (layoutType == TABBED) {
			prop.put("active", String.valueOf(((TabbedPanel) pContent).getActiveTabN()));
		} else if (layoutType == HORISONTAL || layoutType == VERTICAL) {
			Vector inLayout = new Vector();
			for (int i = 0; i < components.size(); i++) {
				inLayout.addElement(((SplitPanel) pContent).getSplitComponent(i));
			}

			String pos = "";
			String parts = "";

			for (int i = 0; i < inLayout.size(); i++) {
				Component cur = (Component) inLayout.elementAt(i);
				parts += ((SplitLayout) pContent.getLayout()).getComponentPart(i) + " ";
				for (int j = 0; j < components.size(); j++)
					if (components.elementAt(j) == cur) {
						pos += String.valueOf(j) + " ";
					}
			}

/*
      for (int i=0; i<components.size(); i++) {
        Component cur = (Component)components.elementAt(i);
        parts += ((SplitLayout)pContent.getLayout()).getComponentPart(i)+" ";
        for (int j=0; j<inLayout.size(); j++)
          if (inLayout.elementAt(j)==cur)
            pos+=String.valueOf(j)+" ";
      }
*/
			prop.put("order", pos.trim());
			prop.put("sizes", parts.trim());
		} else if (layoutType == MDI_HOR || layoutType == MDI_VERT) {
			MDIPanel p = (MDIPanel) pContent;
			Vector inLayout = new Vector();
			String pos = "";
			String parts = "";
			if (components.size() == 0)
				return prop;
			else if (components.size() == 1) {
				inLayout.addElement(p.getSplitComponent(0));
				pos = "0";
				parts = "1.0";
			} else {
				for (int i = 0; i < components.size() - 1; i++) {
					inLayout.addElement(((SplitPanel) p.getSplitComponent(p.getStackedFrameIndex())).getSplitComponent(i));
					parts += ((SplitLayout) ((SplitPanel) p.getSplitComponent(p.getStackedFrameIndex())).getLayout()).getComponentPart(i) + " ";
				}
				inLayout.addElement(p.getSplitComponent(p.getMainCompIndex()));

				for (int i = 0; i < inLayout.size() - 1; i++) {
					Component cur = (Component) inLayout.elementAt(i);
					for (int j = 0; j < components.size(); j++)
						if (components.elementAt(j) == cur) {
							pos += String.valueOf(j) + " ";
						}
				}
				parts += ((SplitLayout) p.getLayout()).getComponentPart(p.getMainCompIndex()) + " ";
				for (int j = 0; j < components.size(); j++)
					if (components.elementAt(j) == p.getSplitComponent(p.getMainCompIndex())) {
						pos += String.valueOf(j) + " ";
					}
			}

			prop.put("order", pos.trim());
			prop.put("sizes", parts.trim());
			prop.put("main", (p.getMainCompIndex() == 0) ? "first" : "last");
		} else if (layoutType == MATRIX_HOR) {
			Vector inLayout = new Vector();

			int perRow = ((MultiSplitPanel) pContent).getMaxCompPerRow();

			for (int i = 0; i < components.size(); i++) {
				inLayout.addElement(((MultiSplitPanel) pContent).getSplitComponent(i));
			}

			String pos = "";
			String parts = "";
			String rowParts = "";

			for (int i = 0; i < inLayout.size(); i++) {
				Component cur = (Component) inLayout.elementAt(i);
				for (int j = 0; j < components.size(); j++)
					if (components.elementAt(j) == cur) {
						pos += String.valueOf(j) + " ";
					}
			}

			int rowCount = (int) Math.ceil(((MultiSplitPanel) pContent).getSplitComponentCount() / (float) ((MultiSplitPanel) pContent).getMaxCompPerRow());
			for (int i = 0; i < rowCount; i++) {
				parts += ((SplitLayout) pContent.getLayout()).getComponentPart(i) + " ";
				SplitLayout row = (SplitLayout) ((MultiSplitPanel) pContent).getRow(i).getLayout();
				for (int j = 0; j < ((MultiSplitPanel) pContent).getMaxCompPerRow(); j++)
					if (((MultiSplitPanel) pContent).getMaxCompPerRow() * i + j < components.size()) {
						rowParts += row.getComponentPart(j) + " ";
					}
			}
			parts += rowParts;

			prop.put("perRow", String.valueOf(perRow));
			prop.put("order", pos.trim());
			prop.put("sizes", parts.trim());
		}

		return prop;
	}

	/**
	 * After the tool is constructed, it may be requested to setup its individual
	 * properties according to the given list of stored properties.
	 */
	public void setProperties(Hashtable properties) {
		if (properties == null || properties.size() == 0)
			return;
		String layout = (String) properties.get("layout");
		if (layout != null && layout.length() > 0) {
			int lType = 0;
			if (layout.equalsIgnoreCase("horizontal")) {
				lType = HORISONTAL;
			} else if (layout.equalsIgnoreCase("vertical")) {
				lType = VERTICAL;
			} else if (layout.equalsIgnoreCase("MDI_hor")) {
				lType = MDI_HOR;
			} else if (layout.equalsIgnoreCase("MDI_vert")) {
				lType = MDI_VERT;
			} else if (layout.equalsIgnoreCase("matrix")) {
				lType = MATRIX_HOR;
			} else if (layout.equalsIgnoreCase("tabbed")) {
				lType = TABBED;
			} else if (layout.equalsIgnoreCase("windows")) {
				lType = WINDOWS;
			}

			actionPerformed(new ActionEvent(this, 0, cmd_layouts[lType]));
			tibgrpLayoutType.setSelect(lType);

			if (layoutType == WINDOWS) {
				String bounds = (String) properties.get("placement");
				if (bounds == null || bounds.length() == 0)
					return;
				StringTokenizer coords = new StringTokenizer(bounds, " ,()");
				for (int i = 0; i < windows.size(); i++) {
					int bou[] = new int[4];
					boolean gotAll = false;
					for (int j = 0; j < 4 && coords.hasMoreTokens(); j++) {
						try {
							bou[j] = Integer.valueOf(coords.nextToken()).intValue();
							if (j == 3) {
								gotAll = true;
							}
						} catch (NumberFormatException ne) {
							break;
						}
					}
					if (gotAll) {
						((Frame) windows.elementAt(i)).setBounds(new Rectangle(bou[0], bou[1], bou[2], bou[3]));
					}
				}
			} else if (layoutType == TABBED) {
				try {
					((TabbedPanel) pContent).showTab(Integer.parseInt((String) properties.get("active")));
				} catch (Exception ex) {
				}
			} else if (layoutType == HORISONTAL || layoutType == VERTICAL) {
				String order = (String) properties.get("order");
				if (order == null || order.length() == 0)
					return;
				StringTokenizer st = new StringTokenizer(order, " ");
				if (st.countTokens() != components.size())
					return;

				String sizes = (String) properties.get("sizes");
				if (sizes == null || sizes.length() == 0)
					return;
				StringTokenizer stp = new StringTokenizer(sizes, " ");
				if (stp.countTokens() != components.size())
					return;

				int[] places = new int[components.size()];
				float[] parts = new float[components.size()];
				try {
					for (int i = 0; i < components.size(); i++) {
						places[i] = Integer.parseInt(st.nextToken());
						parts[i] = new Float(stp.nextToken()).floatValue();
					}
				} catch (Exception ex) {
					return;
				}

// weird, but works
				removeContent();
				invalidate();
				setVisible(false);
				pContent = new SplitPanel(layoutType != VERTICAL);
				ComponentWindow c = null;
				for (int i = 0; i < components.size(); i++) {
					c = (ComponentWindow) components.elementAt(places[i]);
					((SplitPanel) pContent).addSplitComponent(c);
				}
				addContent(pContent);
				((SplitLayout) pContent.getLayout()).setProportions(parts);

				setVisible(true);
				//CManager.validateFully(pContent);
				for (int i = 0; i < components.size(); i++) {
					ComponentWindow cw = (ComponentWindow) components.elementAt(i);
					cw.getContent().setVisible(true);
					cw.validateComponentWindow();
				}
				notifyLayoutChanged();
			} else if (layoutType == MDI_HOR || layoutType == MDI_VERT) {
				String order = (String) properties.get("order");
				if (order == null || order.length() == 0)
					return;
				StringTokenizer st = new StringTokenizer(order, " ");
				if (st.countTokens() != components.size())
					return;

				String sizes = (String) properties.get("sizes");
				if (sizes == null || sizes.length() == 0)
					return;
				StringTokenizer stp = new StringTokenizer(sizes, " ");
				if (stp.countTokens() != components.size())
					return;

				int[] places = new int[components.size()];
				float[] parts = new float[components.size()];
				try {
					for (int i = 0; i < components.size(); i++) {
						places[i] = Integer.parseInt(st.nextToken());
						parts[i] = new Float(stp.nextToken()).floatValue();
					}
				} catch (Exception ex) {
					return;
				}

				removeContent();
				invalidate();
				setVisible(false);
				pContent = new MDIPanel(layoutType == MDI_HOR);
				ComponentWindow c = null;
				for (int i = 0; i < components.size(); i++) {
					c = (ComponentWindow) components.elementAt(places[i]);
					((MDIPanel) pContent).addComponent(c);
				}
				addContent(pContent);

				((SplitLayout) pContent.getLayout()).setProportions(new float[] { parts[parts.length - 1], 1 - parts[parts.length - 1] });
				float[] stackParts = new float[parts.length - 1];
				for (int i = 0; i < parts.length - 1; i++) {
					stackParts[i] = parts[i];
				}
				((SplitLayout) ((SplitPanel) ((SplitPanel) pContent).getSplitComponent(1)).getLayout()).setProportions(stackParts);

/*swap main*/
				try {
					if (((String) properties.get("main")).equalsIgnoreCase("last")) {
						((SplitLayout) pContent.getLayout()).swapComponents(0, 1);
					}
				} catch (Exception ex) {
				}

				setVisible(true);
				//CManager.validateFully(pContent);
				for (int i = 0; i < components.size(); i++) {
					ComponentWindow cw = (ComponentWindow) components.elementAt(i);
					cw.getContent().setVisible(true);
					cw.validateComponentWindow();
				}
				notifyLayoutChanged();
			} else if (layoutType == MATRIX_HOR) {
				String order = (String) properties.get("order");
				if (order == null || order.length() == 0)
					return;
				StringTokenizer st = new StringTokenizer(order, " ");
				if (st.countTokens() != components.size())
					return;

				int perRow = 0;
				try {
					perRow = Integer.parseInt((String) properties.get("perRow"));
				} catch (Exception ex) {
					return;
				}
				int rows = (int) Math.ceil(((float) components.size()) / perRow);

				String sizes = (String) properties.get("sizes");
				if (sizes == null || sizes.length() == 0)
					return;
				StringTokenizer stp = new StringTokenizer(sizes, " ");
				if (stp.countTokens() != components.size() + rows)
					return;

				int[] places = new int[components.size()];
				float[] rowParts = new float[rows];
				float[] parts = new float[components.size()];
				try {
					for (int i = 0; i < rows; i++) {
						rowParts[i] = new Float(stp.nextToken()).floatValue();
					}
				} catch (Exception ex) {
					return;
				}
				try {
					for (int i = 0; i < components.size(); i++) {
						places[i] = Integer.parseInt(st.nextToken());
						parts[i] = new Float(stp.nextToken()).floatValue();
					}
				} catch (Exception ex) {
					return;
				}

				removeContent();
				invalidate();
				setVisible(false);
				pContent = new MultiSplitPanel(perRow);
				ComponentWindow c = null;
				for (int i = 0; i < components.size(); i++) {
					c = (ComponentWindow) components.elementAt(places[i]);
					((MultiSplitPanel) pContent).addComponent(c);
				}
				addContent(pContent);

				((SplitLayout) pContent.getLayout()).setProportions(rowParts);

				for (int i = 0; i < rows; i++) {
					int rowSize = (i < rows - 1) ? perRow : components.size() - i * perRow;
					float[] rowProp = new float[rowSize];
					for (int j = 0; j < rowSize; j++) {
						rowProp[j] = parts[i * perRow + j];
					}
					((SplitLayout) ((MultiSplitPanel) pContent).getRow(i).getLayout()).setProportions(rowProp);
				}

				setVisible(true);
				//CManager.validateFully(pContent);
				for (int i = 0; i < components.size(); i++) {
					ComponentWindow cw = (ComponentWindow) components.elementAt(i);
					cw.getContent().setVisible(true);
					cw.validateComponentWindow();
				}
				notifyLayoutChanged();
			}
		}
	}

	/**
	 * Adds a listener to be notified about destroying the tool.
	 * A SaveableTool may be registered somewhere and, hence, must notify the
	 * component where it is registered about its destroying.
	 */
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst != null) {
			destroyingListeners.addElement(lst);
		}
	}

	Vector destroyingListeners = new Vector();

	/**
	 * Sends a PropertyChangeEvent with the name "destroyed" to its
	 * destroying listener(s), @see addDestroyingListener.
	 */
	public void destroy() {
		for (int i = 0; i < destroyingListeners.size(); i++) {
			((PropertyChangeListener) destroyingListeners.elementAt(i)).propertyChange(new PropertyChangeEvent(this, "destroyed", null, null));
		}
	}

	public void removeNotify() {
		destroy();
		super.removeNotify();
	}
//~ID
}
