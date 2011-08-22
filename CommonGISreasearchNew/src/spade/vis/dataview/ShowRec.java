package spade.vis.dataview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColoredStringColumn;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.ShowRecordLayout;
import spade.lib.basicwin.StringColumn;
import spade.lib.basicwin.TextCanvas;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.time.TimeReference;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.ThematicDataItem;

/**
* ShowRec shows attribute values associated with selected objects.
* An instance of ShowRec is always linked to a single table. If there are
* more tables, more ShowRec instances must be constructed.
* A ShowRec listens to data change notifications from its table. When the table
* is destroyed, the ShowRec is also destroyed.
* A ShowRec can notify registered listeners when it is destroyed.
*/

public class ShowRec extends Panel implements PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.vis.dataview.Res");

	protected Scrollbar sb = null; // scrollbar for ShowRecord, appears if needed
	protected ColoredStringColumn fcol = null;
	protected StringColumn vcol = null; // 2 vertical columns: Field=Value
	/**
	* The panel containing the main components of the ShowRec
	*/
	protected Panel mainPanel = null;
	/**
	* The table with data this ShowRec deals with
	*/
	protected AttributeDataPortion dTable = null;
	/**
	* The list of attributes to show
	*/
	protected Vector shownAttr = null; // set of attrIds
	/**
	* The list ofcolors for the attributes to show. May be null or empty
	*/
	protected Vector attrColors = null; // set of colors
	/**
	* The list of identifiers of objects that were last selected
	*/
	protected Vector lastSelected = null;
	/**
	* Used for registering listeners of property changes and notification
	* about being destroyed.
	*/
	protected PropertyChangeSupport pcSup = null;

	private Container myCont = null;
	public boolean keepLastOutput = false, hasContent = false;
	protected int maxLines = 30; //-1 will mean unlimited number
	/**
	* If exists, regulates showing the data. In particular, specifies whether
	* attributes with null values should be shown. May provide class information.
	*/
	public DataViewRegulator viewReg = null;
	/**
	* If exists, allows ShowRec to display both original and transformed values.
	*/
	public TransformedDataPresenter tPres = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	public ShowRec(AttributeDataPortion dTable) {
		this.dTable = dTable;
		if (dTable != null) {
			dTable.addPropertyChangeListener(this);
		}
		ShowRecordLayout rl = new ShowRecordLayout(); // global
		mainPanel = new Panel(rl);
		fcol = new ColoredStringColumn();
		vcol = new StringColumn();
		mainPanel.add("Left", fcol);
		mainPanel.add("Right", vcol);
		sb = new Scrollbar(Scrollbar.VERTICAL);
		sb.addAdjustmentListener(fcol);
		sb.addAdjustmentListener(vcol);
		mainPanel.add("Scrollbar", sb);
		fcol.init();
		vcol.init();
		fcol.addString("");
		vcol.addString("");
		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
	}

	@Override
	public String getName() {
		return res.getString("object_view");
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(fcol.getPreferredSize().width + vcol.getPreferredSize().width + 10, vcol.getPreferredSize().height);
	}

	public void setShownAttrs(Vector vAttrs, Vector colors) {
		boolean needUpdate = false;
		if (shownAttr != null)
			if (vAttrs != null) {
				if (shownAttr.size() != vAttrs.size()) {
					needUpdate = true;
				} else {
					for (int i = 0; i < vAttrs.size() && !needUpdate; i++)
						if (shownAttr.elementAt(i) != null && !shownAttr.elementAt(i).equals(vAttrs.elementAt(i))) {
							needUpdate = true;
						}
				}
			} else {
				needUpdate = true;
			}
		else {
			needUpdate = vAttrs != null;
		}
		boolean colorsChanged = false;
		if (attrColors == null) {
			colorsChanged = colors != null;
		} else if (colors == null) {
			colorsChanged = true;
		} else if (colors.size() != attrColors.size()) {
			colorsChanged = true;
		} else {
			for (int i = 0; i < colors.size() && !colorsChanged; i++)
				if (colors.elementAt(i) == null) {
					colorsChanged = attrColors.elementAt(i) != null;
				} else {
					colorsChanged = !colors.elementAt(i).equals(attrColors.elementAt(i));
				}
		}
		if (colorsChanged) {
			attrColors = colors;
		}
		if (needUpdate) {
			shownAttr = (vAttrs != null) ? (Vector) vAttrs.clone() : null;
		}
		if (needUpdate || colorsChanged) {
			showLastSelectedObjects();
		}
	}

	public Vector getShownAttrs() {
		return shownAttr;
	}

	public int getNShownAttr() {
		if (dTable == null || shownAttr == null)
			return 0;
		return shownAttr.size();
	}

	public void setDataViewRegulator(DataViewRegulator reg) {
		viewReg = reg;
	}

	/**
	* Sets a reference to a TransformedDataPresenter that will allow ShowRec to
	* display both original and transformed values.
	*/
	public void setTransformedDataPresenter(TransformedDataPresenter presenter) {
		tPres = presenter;
	}

	/**
	 * @param selected - identifiers of the selected objects
	 * @param timeRefs - time references of the selected objects; may be null
	 */
	public void showObjects(Vector selected, Vector timeRefs) {
		if (dTable == null)
			return;
		lastSelected = selected;
		if (selected == null || selected.size() < 1) {
			showRecord(null, null);
		} else if (selected.size() > 1) {
			showMultiple(selected, timeRefs);
		} else {
			TimeReference tref = (timeRefs != null && timeRefs.size() > 0) ? (TimeReference) timeRefs.elementAt(0) : null;
			showRecord(selected.elementAt(0).toString(), tref);
		}
	}

	/**
	 * @param selected - identifiers of the selected objects
	 */
	public void showObjects(Vector selected) {
		showObjects(selected, null);
	}

	/*
	public void highlightSetChanged(Object source, String setId, Vector highlighted){}
	public void selectSetChanged(Object source, String setId, Vector selected){
	  if (dTable!=null && StringUtil.sameStrings(setId,dTable.getEntitySetIdentifier()))
	    showObjects(selected);
	}
	public void showCurrentlySelectedObjects () {
	  if (highlighter!=null) showObjects(highlighter.getSelectedObjects());
	}
	*/
	public void showLastSelectedObjects() {
		if (lastSelected != null) {
			showObjects(lastSelected);
		}
	}

	public void showRecord(String objID) {
		showRecord(objID, null);
	}

	public void showRecord(String objID, TimeReference tref) {
		if (dTable == null)
			return;
		if (objID == null || objID.length() < 1) {
			showDataItem(-1, null);
			return;
		}
		showDataItem(dTable.indexOf(objID), tref);
	}

	public void showMultiple(Vector ids, Vector timeRefs) {
		if (dTable == null || ids == null)
			return;
		if (!isAncestorOf(mainPanel)) {
			removeAll();
			add(mainPanel, BorderLayout.CENTER);
		}
		IntArray attrNs = new IntArray(getNShownAttr(), 1), colNs = new IntArray(getNShownAttr(), 1);
		for (int i = 0; i < getNShownAttr(); i++) {
			String attrId = (String) shownAttr.elementAt(i);
			if (attrId == null) {
				continue;
			}
			int attrIdx = dTable.getAttrIndex(attrId);
			if (attrIdx >= 0) {
				attrNs.addElement(i);
				colNs.addElement(attrIdx);
			}
		}
		fcol.init();
		vcol.init();
		int nLines = 0;
		boolean canMore = true, haveMore = false;
		for (int j = 0; j < ids.size() && canMore; j++) {
			if (ids.elementAt(j) == null) {
				continue;
			}
			int recN = dTable.indexOf((String) ids.elementAt(j));
			if (recN < 0) {
				continue;
			}
			ThematicDataItem dit = (ThematicDataItem) dTable.getDataItem(recN);
			if (dit == null) {
				continue;
			}
			fcol.addString(dit.getName());
			vcol.addString("ID=" + dit.getId());
			++nLines;
			if (timeRefs != null && timeRefs.size() > j) {
				TimeReference tref = (TimeReference) timeRefs.elementAt(j);
				if (tref != null && tref.getValidFrom() != null) {
					fcol.addString(tref.getValidFrom().toString());
					if (tref.getValidUntil() != null) {
						vcol.addString(tref.getValidUntil().toString());
					} else {
						vcol.addString(tref.getValidFrom().toString());
					}
					++nLines;
				}
			}
			canMore = maxLines < 0 || nLines < maxLines;
			if (canMore && viewReg != null) {
				int classN = viewReg.getRecordClassN(recN);
				if (classN >= 0) {
					String className = viewReg.getClassName(classN);
					if (className != null) {
						fcol.addString(className, viewReg.getClassColor(classN));
						vcol.addString("(" + res.getString("class") + ")");
						++nLines;
						canMore = maxLines < 0 || nLines < maxLines;
					}
				}
			}
			for (int i = 0; i < colNs.size() && canMore; i++) {
				int cn = colNs.elementAt(i);
				String attrValue = dit.getAttrValueAsString(cn);
				if (attrValue == null && (viewReg != null && !viewReg.getShowAttrsWithNullValues())) {
					continue;
				}
				Color attrColor = null;
				if (attrColors != null && attrColors.size() > attrNs.elementAt(i)) {
					attrColor = (Color) attrColors.elementAt(attrNs.elementAt(i));
				}
				fcol.addString(dTable.getAttributeName(cn), attrColor);
				if (attrValue != null && tPres != null) {
					String t = tPres.getTransformedValue(recN, cn);
					if (t != null) {
						attrValue += " >> " + t;
					}
				}
				vcol.addString(attrValue);
				++nLines;
				canMore = maxLines < 0 || nLines < maxLines;
				if (!canMore && i < colNs.size() - 1) {
					haveMore = true;
				}
			}
			if (canMore) {
				fcol.addSeparator();
				vcol.addSeparator();
				++nLines;
				canMore = maxLines < 0 || nLines < maxLines;
			}
			if (!canMore && j < ids.size() - 1) {
				haveMore = true;
			}
		}
		if (!canMore && haveMore) {
			fcol.addString("...");
			vcol.addString("...");
		}
		hasContent = true;
		regulateSizes();
	}

	protected void showEmpty() {
		if (isAncestorOf(mainPanel)) {
			remove(mainPanel);
		}
		if (getComponentCount() > 0 && (getComponent(0) instanceof TextCanvas))
			return;
		TextCanvas tc = new TextCanvas();
		tc.addTextLine("");
		tc.addTextLine(res.getString("no_selection"));
		tc.addTextLine("");
		tc.addTextLine(res.getString("when_select") + dTable.getName() + res.getString("see_obj_info"));
		tc.addTextLine(res.getString("default_attr"));
		add(tc, BorderLayout.CENTER);
		if (isShowing()) {
			invalidate();
			validate();
		}
	}

	public void showDataItem(int recN, TimeReference tref) {
		if (dTable == null)
			return;
		ThematicDataItem tdi = (recN < 0) ? null : (ThematicDataItem) dTable.getDataItem(recN);
		if (tdi == null) {
			if (keepLastOutput)
				return;
			clearContent();
			showEmpty();
		} else {
			if (!isAncestorOf(mainPanel)) {
				removeAll();
				add(mainPanel, BorderLayout.CENTER);
			}
			fcol.init();
			vcol.init();
			fcol.addString(tdi.getName());
			vcol.addString("ID=" + tdi.getId());
			int nLines = 1;
			if (tref != null && tref.getValidFrom() != null) {
				fcol.addString(tref.getValidFrom().toString());
				if (tref.getValidUntil() != null) {
					vcol.addString(tref.getValidUntil().toString());
				} else {
					vcol.addString(tref.getValidFrom().toString());
				}
				++nLines;
			}
			if (viewReg != null) {
				int classN = viewReg.getRecordClassN(recN);
				if (classN >= 0) {
					String className = viewReg.getClassName(classN);
					if (className != null) {
						fcol.addString(className, viewReg.getClassColor(classN));
						vcol.addString("");
						fcol.addSeparator();
						vcol.addSeparator();
						nLines += 2;
					}
				}
			}
			boolean canMore = true, haveMore = false;
			;
			for (int i = 0; i < getNShownAttr() && canMore; i++)
				if (shownAttr.elementAt(i) != null) {
					int attrIdx = dTable.getAttrIndex((String) shownAttr.elementAt(i));
					if (attrIdx < 0) {
						continue;
					}
					String attrValue = tdi.getAttrValueAsString(attrIdx);
					if (attrValue == null && (viewReg != null && !viewReg.getShowAttrsWithNullValues())) {
						continue;
					}
					Color attrColor = null;
					if (attrColors != null && attrColors.size() > i) {
						attrColor = (Color) attrColors.elementAt(i);
					}
					fcol.addString(dTable.getAttributeName(attrIdx), attrColor);
					if (attrValue != null && tPres != null) {
						String t = tPres.getTransformedValue(recN, attrIdx);
						if (t != null) {
							attrValue += " >> " + t;
						}
					}
					vcol.addString(attrValue);
					++nLines;
					canMore = maxLines < 0 || nLines < maxLines;
					if (!canMore && i < getNShownAttr() - 1) {
						haveMore = true;
					}
				}
			if (!canMore && haveMore) {
				fcol.addString("...");
				vcol.addString("...");
			}
			hasContent = true;
		}
		regulateSizes();
	}

	public void regulateSizes() {
		if (!isAncestorOf(mainPanel))
			return;
		fcol.countSizes();
		vcol.countSizes();
		fcol.invalidate();
		vcol.invalidate();
		if (isShowing()) {
			CManager.validateAll(this);
		}
	}

	public void setExtContainer(Container c) {
		myCont = c;
	}

	public void setLinesLimit(int limit) {
		maxLines = limit;
	}

	public int getLinesLimit() {
		return maxLines;
	}

	public void clearContent() {
		vcol.init();
		fcol.init();
		hasContent = false;
		fcol.invalidate();
		vcol.invalidate();
		validate();
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible && !hasContent && isAncestorOf(mainPanel)) {
			showEmpty();
		}
		super.setVisible(visible);
	}

	/**
	* Adds a listener of property changes, in particular, destroying of the window.
	*/
	@Override
	public void addPropertyChangeListener(PropertyChangeListener list) {
		if (list == null)
			return;
		if (pcSup == null) {
			pcSup = new PropertyChangeSupport(this);
		}
		pcSup.addPropertyChangeListener(list);
	}

	/**
	* Removes the listener of property changes
	*/
	@Override
	public void removePropertyChangeListener(PropertyChangeListener list) {
		if (list == null || pcSup == null)
			return;
		pcSup.removePropertyChangeListener(list);
	}

	/**
	* Reacts to data changes in the table and to the table being destroyed
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (dTable != null && e.getSource().equals(dTable)) {
			if (e.getPropertyName().equals("destroyed")) {
				destroy();
				dTable = null;
				return;
			}
			if (e.getPropertyName().equals("structure_complete")) {
				if (lastSelected != null && lastSelected.size() > 0) {
					lastSelected = null;
					clearContent();
					showEmpty();
				}
				return;
			}
			if (lastSelected == null || lastSelected.size() < 1)
				return;
			boolean changed = false;
			if (e.getPropertyName().equals("data_added") || e.getPropertyName().equals("data_removed") || e.getPropertyName().equals("data_updated") || e.getPropertyName().equals("names")) {
				changed = true;
			} else if (e.getPropertyName().equals("values")) {
				Vector v = (Vector) e.getNewValue(); // list of changed attributes
				if (v == null || shownAttr == null)
					return;
				for (int i = 0; i < v.size() && !changed; i++)
					if (shownAttr.contains(v.elementAt(i))) {
						changed = true;
					}
			}
			//if the changed attributes are shown, refresh the shown values
			if (changed) {
				showObjects(lastSelected);
			}
		}
	}

	/**
	* Notifies its listeners (if any) about being destroyed.
	*/
	@Override
	public void destroy() {
		if (dTable != null) {
			dTable.removePropertyChangeListener(this);
		}
		if (pcSup != null) {
			pcSup.firePropertyChange("destroy", null, null);
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
}
