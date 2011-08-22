package spade.vis.space;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;

/**
* A utility class used for a user to select a layer or layers from a map
*/
public class SelectLayer {
	static ResourceBundle res = Language.getTextResource("spade.vis.space.Res");

	/**
	* Selects a single layer of the specified type. If the type is irrelevant,
	* the argument type must be 0.
	* The argument win is the reference to the main window of the application.
	* May return null if no layer was selected.
	*/
	public static GeoLayer selectLayer(LayerManager lman, char type, String prompt, Frame win) {
		return (GeoLayer) select(lman, type, prompt, win, false);
	}

	/**
	* Selects a single layer of one of the types listed in the string argument
	* types. If the type is irrelevant, the argument types must be null.
	* The argument win is the reference to the main window of the application.
	* May return null if no layer was selected.
	*/
	public static GeoLayer selectLayer(LayerManager lman, String types, String prompt, Frame win) {
		return (GeoLayer) select(lman, types, prompt, win, false);
	}

	/**
	* Selects several layers of the specified type. If the type is irrelevant,
	* the argument type must be 0.
	* The argument win is the reference to the main window of the application.
	* May return null if no layer was selected.
	*/
	public static Vector selectLayers(LayerManager lman, char type, String prompt, Frame win) {
		return (Vector) select(lman, type, prompt, win, true);
	}

	/**
	* Selects a single layer of one of the types listed in the string argument
	* types. If the type is irrelevant, the argument types must be null.
	* The argument win is the reference to the main window of the application.
	* May return null if no layer was selected.
	*/
	public static Vector selectLayers(LayerManager lman, String types, String prompt, Frame win) {
		return (Vector) select(lman, types, prompt, win, true);
	}

	/**
	* Constructs and runs a dialog for user selecting layers from a map.
	* The allowed type is specified by the argument type.
	* Returns a GeoLayer if a single layer was requested, a Vector of
	* layers if selection of several layers was allowed, and null if nothing was
	* selected. The argument lids is a Vector containing indentifiers of the
	* layers included in the list (in the same order).
	*/
	protected static Object select(LayerManager lman, char type, String prompt, Frame win, boolean multiple) {
		if (lman == null || lman.getLayerCount() < 1)
			return null;
		List lst = new List(8, multiple);
		Vector lids = fillLayerList(lst, lman, type);
//ID
		if (lst.getItemCount() > 0) {
			lst.select(0);
		}
//~ID
		return select(lman, lst, lids, prompt, win, multiple);
	}

	/**
	* Constructs and runs a dialog for user selecting layers from a map.
	* The allowed types are listed in the string types.
	* Returns a GeoLayer if a single layer was requested, a Vector of
	* layers if selection of several layers was allowed, and null if nothing was
	* selected. The argument lids is a Vector containing indentifiers of the
	* layers included in the list (in the same order).
	*/
	protected static Object select(LayerManager lman, String types, String prompt, Frame win, boolean multiple) {
		if (lman == null || lman.getLayerCount() < 1)
			return null;
		List lst = new List(8, multiple);
		Vector lids = fillLayerList(lst, lman, types);
//ID
		if (lst.getItemCount() > 0) {
			lst.select(0);
		}
//~ID
		return select(lman, lst, lids, prompt, win, multiple);
	}

	/**
	* Constructs and runs a dialog for user selecting layers from the given
	* list. Returns a GeoLayer if a single layer was requested, a Vector of
	* layers if selection of several layers was allowed, and null if nothing was
	* selected. The argument lids is a Vector containing indentifiers of the
	* layers included in the list (in the same order).
	*/
	protected static Object select(LayerManager lman, List lst, Vector lids, String prompt, Frame win, boolean multiple) {
		if (win == null) {
			win = CManager.getAnyFrame();
		}
		if (lst == null || lids == null || lids.size() < 1) {
			OKDialog okd = new OKDialog(win, prompt, false);
			// following string: "No appropriate layers available!"
			okd.addContent(new Label(res.getString("No_appropriate_layers")));
			okd.show();
			return null;
		}
//ID
		OKDialog okd;
		if (multiple) {
			okd = new OKDialog(win, prompt, true) {
				@Override
				public void actionPerformed(ActionEvent e) {
					String cmd = e.getActionCommand();
					if (!cmd.equalsIgnoreCase("all") && !cmd.equalsIgnoreCase("none")) {
						super.actionPerformed(e);
					} else {
						if (lst == null)
							return;
						if (cmd == "all") {
							for (int i = 0; i < lst.getItemCount(); i++) {
								lst.select(i);
							}
						} else {
							for (int i = 0; i < lst.getItemCount(); i++) {
								lst.deselect(i);
							}
						}
					}
				}

				List lst;

				@Override
				public void addContent(Component c) {
					if (c instanceof List) {
						lst = (List) c;
					} else if (c instanceof Container) {
						Component[] comp = ((Container) c).getComponents();
						for (Component element : comp)
							if (element instanceof List) {
								lst = (List) element;
							}
					}
					super.addContent(c);
				}
			};
		} else {
			okd = new OKDialog(win, prompt, true);
		}

		Panel p = new Panel(new BorderLayout());
		if (prompt != null) {
			p.add(new Label(prompt), "North");
		}
		p.add(lst, "Center");
		if (multiple) {
			Panel buttons = new Panel(new GridLayout(1, 2));
			Button all = new Button("Select all");
			Button none = new Button("Select none");
			all.setActionCommand("all");
			none.setActionCommand("none");
			all.addActionListener(okd);
			none.addActionListener(okd);
			buttons.add(all);
			buttons.add(none);
			p.add(buttons, "South");
		}
		okd.addContent(p);
//~ID
		okd.show();
		if (okd.wasCancelled())
			return null;
		int sel[] = lst.getSelectedIndexes();
		if (sel == null)
			return null;
		Vector layers = new Vector(sel.length, 1);
		for (int element : sel) {
			int idx = lman.getIndexOfLayer((String) lids.elementAt(element));
			if (idx >= 0) {
				layers.addElement(lman.getGeoLayer(idx));
			}
		}
		if (layers.size() < 1)
			return null;
		if (!multiple)
			return layers.elementAt(0);
		return layers;
	}

	/**
	* Fills the list of layers to select from. Returns a vector with identifiers
	* of the layers. If there are no suitable layers, returns null.
	*/
	protected static Vector fillLayerList(List lst, LayerManager lman, char type) {
		if (lst == null || lman == null || lman.getLayerCount() < 1)
			return null;
		Vector lids = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer gl = lman.getGeoLayer(i);
//ID
			if (!gl.hasThematicData()) {
				gl.loadGeoObjects();
			}
//~ID
			if (gl.getObjectCount() > 0 && (type == 0 || type == gl.getType())) {
				lst.add(gl.getName());
				lids.addElement(gl.getContainerIdentifier());
			}
		}
		if (lids.size() < 1)
			return null;
		return lids;
	}

	/**
	* Fills the list of layers to select from. Returns a vector with identifiers
	* of the layers. If there are no suitable layers, returns null.
	*/
	protected static Vector fillLayerList(List lst, LayerManager lman, String types) {
		if (lst == null || lman == null || lman.getLayerCount() < 1)
			return null;
		Vector lids = new Vector(lman.getLayerCount(), 1);
		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer gl = lman.getGeoLayer(i);
			if (gl.getObjectCount() > 0 && (types == null || types.indexOf(gl.getType()) >= 0)) {
				lst.add(gl.getName());
				lids.addElement(gl.getContainerIdentifier());
			}
		}
		if (lids.size() < 1)
			return null;
		return lids;
	}
}
