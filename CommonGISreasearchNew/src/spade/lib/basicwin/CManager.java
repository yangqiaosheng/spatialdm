package spade.lib.basicwin;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.Window;

/**
* The class contains some useful utilities for operating awt components
*/
public class CManager {
	protected static Frame dummyFrame = null;
	/**
	* This may be the main frame of an application, which can be accessed by
	* other components through CManager.
	*/
	protected static Frame mainFrame = null;

	/**
	* Sets the main frame of an application, which can be accessed by
	* other components through CManager.
	*/
	public static void setMainFrame(Frame frame) {
		mainFrame = frame;
	}

	/**
	* If the specified Component  is included in a Frame (directly or
	* indirectly) or is a Frame itself, returns this Frame
	*/
	public static Frame getFrame(Component c) {
		while (c != null && !(c instanceof Frame) && !(c instanceof Dialog)) {
			c = c.getParent();
		}
		if (c == null || (c instanceof Dialog))
			return null;
		return (Frame) c;
	}

	/**
	* Returns either the main frame of the application (if available) or a dummy
	* frame.
	*/
	public static Frame getAnyFrame() {
		if (mainFrame != null)
			return mainFrame;
		return getDummyFrame();
	}

	/**
	* If the specified Component  is included in a Frame (directly or
	* indirectly) or is a Frame itself, returns this Frame. Otherwise
	* returns a reference to an invisible "dummy" frame (may be needed for
	* creation of dialogs and popup windows)
	*/
	public static Frame getAnyFrame(Component c) {
		while (c != null && !(c instanceof Frame) && !(c instanceof Dialog)) {
			c = c.getParent();
		}
		while (c != null && (c instanceof Dialog)) {
			Dialog d = (Dialog) c;
			c = d.getOwner();
		}
		if (c != null && (c instanceof Frame))
			return (Frame) c;
		return getAnyFrame();
	}

	/**
	* Returns a reference to an invisible "dummy" frame (may be needed for
	* creation of dialogs and popup windows)
	*/
	public static Frame getDummyFrame() {
		if (dummyFrame == null) {
			dummyFrame = new Frame();
		}
		return dummyFrame;
	}

	/**
	* If the specified Component  is included in a Window (directly or
	* indirectly) or is a Window itself, returns this Window
	*/
	public static Window getWindow(Component c) {
		while (c != null && !(c instanceof Window)) {
			c = c.getParent();
		}
		if (c == null)
			return null;
		return (Window) c;
	}

	/**
	 * Increases the size of the window by the given number of pixels
	 * in width and height. Cares that the window remains fully on the
	 * screen and does not exceed the size of the screen.
	 * rightMarg and bottomMarg specify the right and bottom margins
	 * to be left on the screen.
	 */
	public static void enlargeWindow(Window win, int dw, int dh, int rightMarg, int bottomMarg) {
		if (win == null)
			return;
		if (dw <= 0 && dh <= 0)
			return;
		if (dw < 0) {
			dw = 0;
		}
		if (dh < 0) {
			dh = 0;
		}
		Dimension d0 = win.getSize(), d1 = win.getToolkit().getScreenSize();
		int w = d0.width + dw, h = d0.height + dh;
		if (w > d1.width - rightMarg) {
			w = d1.width - rightMarg;
		}
		if (h > d1.height - bottomMarg) {
			h = d1.height - bottomMarg;
		}
		Point pos = win.getLocation();
		int x = pos.x, y = pos.y;
		if (x + w > d1.width - rightMarg) {
			x = d1.width - rightMarg - w;
		}
		if (y + h > d1.height - bottomMarg) {
			y = d1.height - h - bottomMarg;
		}
		win.setBounds(x, y, w, h);
	}

	/**
	* If the specified Component  is included in a ScrollPane (directly or
	* indirectly), returns this ScrollPane
	*/
	public static ScrollPane getScrollPane(Component comp) {
		Container c = comp.getParent();
		while (c != null) {
			if (c instanceof ScrollPane)
				return (ScrollPane) c;
			c = c.getParent();
		}
		return null;
	}

	/**
	* If the specified Component  is included in a ScrollPane (directly or
	* indirectly), returns this ScrollPane
	*/
	public static Point getLocationInScrollPane(Component comp) {
		//System.out.println("finding location of "+comp);
		Point p = new Point(0, 0);
		while (comp != null) {
			Container c = comp.getParent();
			if (c instanceof ScrollPane)
				return p;
			Point pp = comp.getLocation();
			p.x += pp.x;
			p.y += pp.y;
			/**
			System.out.println("pp=("+pp.x+","+pp.y+"), p=("+p.x+","+p.y+
			                   "); comp="+comp+"; parent="+c);
			*/
			comp = c;
		}
		return p;
	}

	/**
	* If the specified Component  is included in a ScrollPane (indirectly, through
	* a container including other components), makes the ScrollPane scroll to
	* make this component visible
	*/
	public static void scrollToExpose(Component comp) {
		Dimension csize = comp.getSize();
		if (csize == null || csize.width < 1 || csize.height < 1)
			return;
		ScrollPane scp = getScrollPane(comp);
		if (scp == null)
			return;
		Point p = getLocationInScrollPane(comp);
		scp.setScrollPosition(p);
	}

	public static void invalidateFully(Component c) {
		if (c == null)
			return;
		c.invalidate();
		if (c instanceof Container) {
			Container cc = (Container) c;
			int nComp = cc.getComponentCount();
			for (int i = 0; i < nComp; i++) {
				invalidateFully(cc.getComponent(i));
			}
		}
	}

	public static void validateFully(Component c) {
		if (c == null)
			return;
		invalidateFully(c);
		validateAll(c);
	}

	public static void validateAll(Component c) {
		while (c != null && c.getParent() != null && !(c.getParent() instanceof Dialog)) {
			c.invalidate();
			c = c.getParent();
		}
		if (c != null) {
			c.invalidate();
			c.validate();
		}
	}

	public static void invalidateAll(Component c) {
		while (c != null) {
			c.invalidate();
			c = c.getParent();
		}
	}

	/**
	* "Destroys" the given component and its children, i.e. calls "destroy"
	* method for any component that implements the Destroyable interface.
	*/
	public static void destroyComponent(Component comp) {
		if (comp instanceof Destroyable)
			if (((Destroyable) comp).isDestroyed()) {
				;
			} else {
				((Destroyable) comp).destroy();
			}
		if (comp instanceof Container) {
			Container cont = (Container) comp;
			for (int i = 0; i < cont.getComponentCount(); i++) {
				destroyComponent(cont.getComponent(i));
			}
		}
	}

	/**
	* Checks if the component is destroyed or has any destroyed subcomponents.
	*/
	public static boolean isComponentDestroyed(Component comp) {
		if (comp == null)
			return true;
		if (comp instanceof Destroyable)
			return ((Destroyable) comp).isDestroyed();
		if (!(comp instanceof Container))
			return false;
		Container cont = (Container) comp;
		for (int i = 0; i < cont.getComponentCount(); i++)
			if (isComponentDestroyed(cont.getComponent(i)))
				return true;
		return false;
	}

	/**
	* Tries to get a text from the given text field. If extracted, trims it.
	* If the text is empty (length < 1), returns null.
	* If the text field is disabled, returns null.
	*/
	public static String getTextFromField(TextField tf) {
		if (tf == null)
			return null;
		if (!tf.isEnabled())
			return null;
		String str = tf.getText();
		if (str == null)
			return null;
		str = str.trim();
		if (str.length() < 1)
			return null;
		return str;
	}

}