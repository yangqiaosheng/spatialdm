package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.SplitLayout;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.vis.database.AttrCondition;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.CombinedFilter;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.database.TableFilter;
import spade.vis.spec.ConditionSpec;
import spade.vis.spec.QuerySpec;
import spade.vis.spec.RestorableTool;
import spade.vis.spec.SaveableTool;
import ui.AttributeChooser;
import core.ActionDescr;

/**
* A class that allows to set dynamically constraints on values of several
* numeric attributes (i.e. minimum and maximum values). In response, the
* system removes from all the currently visible displays the objects with
* attribute values that do not satisfy these constraints.
* Includes a DynamicQuery and additional controls for adding/removing
* attributes, crearing of the filter, displaying statistics etc.
*/
public class DynamicQueryShell extends Panel implements QueryOrSearchTool, SaveableTool, RestorableTool, ItemListener, ActionListener, PropertyChangeListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");
	protected Checkbox cbStat = null, // Display statistics
			cbDyn = null, // allow dynamic re-query during slider movements
			cbFOutMV = null; // Filter out missing values
	protected Choice delCh = null;
	protected SplitLayout spl = null;
	protected Panel splP = null;
	protected TextField tfmin[] = null, tfmax[] = null;
	protected Label lmin[] = null, lmax[] = null;
	protected Panel minP = null, maxP = null;

	protected DynamicQuery dq = null;
	protected PlotCanvas canvas_dq = new PlotCanvas();
	/**
	 * UI elements for qualitative conditions
	 */
	protected Vector<StringAttrConditionUI> qualUIs = null;
	/**
	 * A panel containing the UI elements for the conditions
	 */
	protected Panel condPan = null;
	/**
	 * A panel containing UI elements for numeric conditions
	 */
	protected Panel numCondPanel = null;
	/**
	 * The preferred height of a single query item
	 */
	protected int itemH = 0;

	protected DynamicQueryStat dqs = null;

	protected AttributeDataPortion dataTable = null;
	protected TableFilter filter = null;
	protected Vector attributes = null;
	protected Supervisor sup = null;
	/**
	* The error message
	*/
	protected String err = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	@Override
	public String getName() {
		if (dataTable == null)
			return res.getString("Dynamic_Query") + " (null)";
		return res.getString("Dynamic_Query") + res.getString("for") + dataTable.getName();
	}

	/**
	* Sets a reference to the system's supervisor used for propagating events
	* among system's components.
	*/
	@Override
	public void setSupervisor(Supervisor sup) {
		this.sup = sup;
	}

	/**
	* Sets a reference to the ObjectContainer this tool must work with
	*/
	@Override
	public void setObjectContainer(ObjectContainer oCont) {
		if (oCont != null && (oCont instanceof AttributeDataPortion)) {
			dataTable = (AttributeDataPortion) oCont;
		}
		if (dataTable != null) {
			getTableFilter();
			dataTable.addPropertyChangeListener(this);
		}
	}

	/**
	* Returns the reference to the ObjectContainer this tool works with
	*/
	@Override
	public ObjectContainer getObjectContainer() {
		if (dataTable != null && (dataTable instanceof ObjectContainer))
			return (ObjectContainer) dataTable;
		return null;
	}

	/**
	* Sets the identifiers of the attributes to be used in the query.
	*/
	@Override
	public void setAttributeList(Vector attr) {
		attributes = attr;
	}

	/**
	* Constructs the tool using the earlier provided references to the supervisor
	* and the object container. Returns true if successfully constructed.
	*/
	@Override
	public boolean construct() {
		if (dataTable == null || !dataTable.hasData()) {
			err = res.getString("no_data");
			return false;
		}
		if (attributes != null && attributes.size() > 0) {
			makeDQ(attributes);
			return true;
		}
		AttributeChooser attrSel = new AttributeChooser();
		if (attrSel.selectColumns(dataTable, null, null, false, res.getString("Select_attributes_to"), sup.getUI()) != null) {
			makeDQ(attrSel.getSelectedColumnIds());
			return true;
		}
		err = res.getString("no_attributes");
		return false;
	}

	public boolean construct(Supervisor sup, AttributeDataPortion dataTable, Vector attributes) {
		if (dataTable == null) {
			err = res.getString("no_data");
			return false;
		}
		if (!dataTable.hasData()) {
			dataTable.loadData();
		}
		if (!dataTable.hasData()) {
			err = res.getString("no_data");
			return false;
		}
		setObjectContainer((ObjectContainer) dataTable);
		setSupervisor(sup);

		if (attributes != null && attributes.size() > 0) {
			makeDQ(attributes);
			return true;
		}
		return construct();
	}

	/**
	* If could not construct itself, returns the error message explaining the
	* reason of the failure
	*/
	@Override
	public String getErrorMessage() {
		return err;
	}

	protected Panel makeLimitPanel(Label lab, TextField tf) {
		Panel ppp = new Panel();
		ppp.setLayout(new ColumnLayout());
		ppp.add(lab);
		ppp.add(tf);
		return ppp;
	}

	protected void getTableFilter() {
		if (dataTable == null) {
			filter = null;
			return;
		}
		ObjectFilter oFilter = dataTable.getObjectFilter();
		if (oFilter instanceof TableFilter) {
			filter = (TableFilter) oFilter;
		} else if (oFilter instanceof CombinedFilter) {
			CombinedFilter cFilter = (CombinedFilter) oFilter;
			for (int i = 0; i < cFilter.getFilterCount(); i++)
				if (cFilter.getFilter(i) instanceof TableFilter) {
					filter = (TableFilter) cFilter.getFilter(i);
				}
		}
	}

	protected void makeDQ(Vector attrIds) {
		if (attrIds == null || attrIds.size() < 1)
			return;
		int nAttr = attrIds.size();
		IntArray qAttrNs = new IntArray(nAttr, 1), //qualitative attributes
		nAttrNs = new IntArray(nAttr, 1); //numeric attributes
		int attrNs[] = new int[nAttr];
		for (int i = 0; i < nAttr; i++) {
			String attrId = (String) attrIds.elementAt(i);
			attrNs[i] = dataTable.getAttrIndex(attrId);
			if (dataTable.isAttributeNumeric(attrNs[i]) || dataTable.isAttributeTemporal(attrNs[i])) {
				nAttrNs.addElement(attrNs[i]);
			} else {
				qAttrNs.addElement(attrNs[i]);
			}
		}
		if (nAttrNs.size() > 0 && qAttrNs.size() > 0) {
			attrIds.removeAllElements(); //insert first qualitative, then numeric
			int nq = qAttrNs.size();
			for (int i = 0; i < nq; i++) {
				int aIdx = qAttrNs.elementAt(i);
				attrNs[i] = aIdx;
				attrIds.addElement(dataTable.getAttributeId(aIdx));
			}
			for (int i = 0; i < nAttrNs.size(); i++) {
				int aIdx = nAttrNs.elementAt(i);
				attrNs[nq + i] = aIdx;
				attrIds.addElement(dataTable.getAttributeId(aIdx));
			}
		}
		removeAll();
		dqs = new DynamicQueryStat(dataTable.getDataItemCount(), nAttr);
		dq = new DynamicQuery(true, sup);
		dq.setDataSource(dataTable);
		dq.enableSelection(sup != null && dataTable != null && sup.getHighlighter(dataTable.getEntitySetIdentifier()) != null);
		dq.setDQS(dqs);
		dq.setIsZoomable(true);
		canvas_dq = new PlotCanvas();
		dq.setCanvas(canvas_dq);
		canvas_dq.setContent(dq);

		if (qAttrNs.size() > 0) {
			if (qualUIs == null) {
				qualUIs = new Vector(10, 10);
			} else {
				qualUIs.removeAllElements();
			}
			for (int i = 0; i < qAttrNs.size(); i++) {
				/*
				QualAttrCondition qCond=new QualAttrCondition();
				qCond.setTable(dataTable);
				qCond.setAttributeIndex(qAttrNs.elementAt(i));
				QualAttrConditionUI qCondUI=new QualAttrConditionUI(qCond,filter,sup);
				*/
				StringAttrConditionUI qUI = new StringAttrConditionUI(dataTable, qAttrNs.elementAt(i), filter, sup);
				qUI.setDQS(dqs);
				qUI.setDQSIndex(i);
				qualUIs.addElement(qUI);
			}
			dq.setDQSStartIndex(qAttrNs.size());
		}
		if (nAttrNs.size() > 0) {
			int fn[] = new int[nAttrNs.size()];
			tfmin = new TextField[fn.length];
			tfmax = new TextField[fn.length];
			lmin = new Label[fn.length];
			lmax = new Label[fn.length];
			for (int i = 0; i < fn.length; i++) {
				fn[i] = nAttrNs.elementAt(i);
				tfmin[i] = new TextField("", 7);
				tfmax[i] = new TextField("", 7);
				lmin[i] = new Label("", Label.CENTER);
				lmax[i] = new Label("", Label.CENTER);
			}
			dq.setFn(fn, tfmin, tfmax, lmin, lmax);
			dq.setup();
			dq.checkWhatSelected();

			numCondPanel = new Panel();
			numCondPanel.setLayout(new BorderLayout());
			numCondPanel.add(canvas_dq, "Center");
			minP = new Panel();
			minP.setLayout(new GridLayout(fn.length + 1, 1));
			for (int i = 0; i < fn.length; i++) {
				minP.add(makeLimitPanel(lmin[i], tfmin[i]));
			}
			numCondPanel.add(minP, "West");
			maxP = new Panel();
			maxP.setLayout(new GridLayout(fn.length + 1, 1));
			for (int i = 0; i < fn.length; i++) {
				maxP.add(makeLimitPanel(lmax[i], tfmax[i]));
			}
			numCondPanel.add(maxP, "East");
		}
		condPan = new Panel();
		layoutCondPanel();

		splP = new Panel();
		spl = new SplitLayout(splP, SplitLayout.VERT);
		splP.setLayout(spl);
		spl.addComponent(condPan, 1f);
		spl.addComponent(dqs, 0.5f);

		//Panel allP=new Panel();
		setLayout(new BorderLayout());
		add(splP, "Center");

		Panel lowP = new Panel();
		lowP.setLayout(new ColumnLayout());
		lowP.add(new Line(false));
		Panel lowSubP = new Panel();
		lowSubP.setLayout(new BorderLayout());
		Panel lowSub2P = new Panel();
		lowSub2P.setLayout(new FlowLayout());
		// following text: "Filter out missing values"
		lowSub2P.add(cbFOutMV = new Checkbox(res.getString("Filter_out_missing"), false));
		cbFOutMV.addItemListener(this);
		// following text:"Clear all filters"
		Button b = new Button(res.getString("Clear_all_filters"));
		b.setActionCommand("clear");
		b.addActionListener(this);
		lowSub2P.add(b);
		lowSubP.add(lowSub2P, "West");
		lowSub2P = new Panel();
		lowSub2P.setLayout(new FlowLayout());
		// following text:"Display statistics"
		lowSub2P.add(cbStat = new Checkbox(res.getString("Display_statistics"), true));
		cbStat.addItemListener(this);
		// following text:"Dynamic update"
		lowSub2P.add(cbDyn = new Checkbox(res.getString("Dynamic_update"), false));
		cbDyn.addItemListener(this);
		lowSubP.add(lowSub2P, "East");
		lowP.add(lowSubP);
		lowSubP = new Panel();
		lowSubP.setLayout(new GridLayout(1, 2));
		lowP.add(lowSubP);
		b = new Button(res.getString("Add_attributes"));
		b.setActionCommand("add");
		b.addActionListener(this);
		Panel p = new Panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p.add(b);
		lowSubP.add(p);
		delCh = new Choice();
		delCh.addItemListener(this);
		// following text: "Remove attributes"
		delCh.addItem(res.getString("Remove_attributes"));
		for (int i = 0; i < nAttr; i++) {
			delCh.addItem(dataTable.getAttributeName(attrNs[i]));
		}
		delCh.setEnabled(delCh.getItemCount() > 2);
		lowSubP.add(delCh);
		lowP.add(new Line(false));
		lowSubP = new Panel();
		lowSubP.setLayout(new FlowLayout());
		lowP.add(lowSubP);
		// following text:"Query results -> Qualitative attribute"
		b = new Button(res.getString("Query_results"));
		lowSubP.add(b);
		b.addActionListener(dq);
		b.setActionCommand(DynamicQuery.saveQueryCmd);
		add(lowP, "South");
		validateFrame();
	}

	private void layoutCondPanel() {
		boolean showing = condPan.isShowing();
		if (showing) {
			condPan.setVisible(false);
		}
		condPan.removeAll();
		GridBagLayout gridbag = new GridBagLayout();
		condPan.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		if (qualUIs != null && qualUIs.size() > 0) {
			for (int i = 0; i < qualUIs.size(); i++) {
				StringAttrConditionUI qCondUI = qualUIs.elementAt(i);
				gridbag.setConstraints(qCondUI, c);
				condPan.add(qCondUI);
			}
		}
		if (dq.getFieldCount() > 0) {
			if (numCondPanel == null) {
				numCondPanel = new Panel();
				numCondPanel.setLayout(new BorderLayout());
				numCondPanel.add(canvas_dq, "Center");
				numCondPanel.add(minP, "West");
				numCondPanel.add(maxP, "East");
			} else if (itemH > 0 && canvas_dq.getSize() != null) {
				int h = itemH * (dq.getFieldCount() + 1), w = canvas_dq.getSize().width;
				canvas_dq.setSize(w, h);
				canvas_dq.setPreferredSize(new Dimension(w, h));
				canvas_dq.invalidate();
			}
			c.gridheight = dq.getFieldCount() + 1;
			gridbag.setConstraints(numCondPanel, c);
			condPan.add(numCondPanel);
		} else {
			Panel p = new Panel(new GridLayout(2, 1));
			p.add(new Label(""));
			p.add(new Label(""));
			gridbag.setConstraints(p, c);
			condPan.add(p);
		}
		if (showing) {
			condPan.setVisible(true);
		}
	}

	public int getQualCondCount() {
		if (qualUIs == null)
			return 0;
		return qualUIs.size();
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getSource() == cbStat) {
			spl.changePart(1, (cbStat.getState()) ? 0.5f : 0f);
			splP.invalidate();
			splP.validate();
			return;
		}
		if (ie.getSource() == cbDyn) {
			dq.setDynamicUpdate(cbDyn.getState());
			return;
		}
		if (ie.getSource() == cbFOutMV) {
			dq.setFOutMV(cbFOutMV.getState());
			if (qualUIs != null) {
				for (int i = 0; i < qualUIs.size(); i++) {
					qualUIs.elementAt(i).setFilterOutMissingValues(cbFOutMV.getState());
				}
			}
			return;
		}
		if (ie.getSource() == delCh) {
			int n = delCh.getSelectedIndex();
			if (n == 0)
				return;
			delCh.remove(n);
			delCh.select(0);
			if (delCh.getItemCount() <= 2) {
				delCh.setEnabled(false);
			}
			int nq = getQualCondCount();
			if (n - 1 >= nq) {
				dq.removeAttributeFromQuery(n - 1 - nq);
				tfmin = dq.getTFMin();
				tfmax = dq.getTFMax();
				lmin = dq.getLMin();
				lmax = dq.getLMax();
				minP.removeAll();
				minP.setLayout(new GridLayout(tfmin.length + 1, 1));
				for (int i = 0; i < tfmin.length; i++) {
					minP.add(makeLimitPanel(lmin[i], tfmin[i]));
				}
				maxP.removeAll();
				maxP.setLayout(new GridLayout(tfmin.length + 1, 1));
				for (int i = 0; i < tfmax.length; i++) {
					maxP.add(makeLimitPanel(lmax[i], tfmax[i]));
				}
			} else {
				StringAttrConditionUI qui = qualUIs.elementAt(n - 1);
				qui.destroy();
				filter.notifyFilterChange();
				if (sup != null && sup.getActionLogger() != null) {
					ActionDescr aDescr = new ActionDescr();
					aDescr.aName = "Dynamic Query: filter condition removed";
					aDescr.addParamValue("Table id", dataTable.getContainerIdentifier());
					aDescr.addParamValue("Table name", dataTable.getName());
					aDescr.addParamValue("Attribute", dataTable.getAttributeName(qui.getAttrIndex()));
					aDescr.startTime = System.currentTimeMillis();
					sup.getActionLogger().logAction(aDescr);
				}
				qualUIs.removeElementAt(n - 1);
				dqs.removeAttr();
				for (int i = n - 1; i < qualUIs.size(); i++) {
					qualUIs.elementAt(i).setDQSIndex(i);
				}
				dq.setDQSStartIndex(qualUIs.size());
				dq.updateStatistics();
			}
			layoutCondPanel();
			validateFrame();
			return;
		}
	}

	protected boolean isAttributeInUse(int attrN) {
		if (attrN < 0)
			return false;
		if (dq.hasAttribute(attrN))
			return true;
		if (qualUIs == null || qualUIs.size() < 1)
			return false;
		for (int j = 0; j < qualUIs.size(); j++)
			if (attrN == qualUIs.elementAt(j).getAttrIndex())
				return true;
		return false;
	}

	public void addAttributes(Vector attrIds) {
		if (attrIds == null || attrIds.size() < 1)
			return;
		if (dq == null) {
			makeDQ(attrIds);
			return;
		}
		IntArray numbers = new IntArray(attrIds.size(), 5);
		for (int i = 0; i < attrIds.size(); i++) {
			int attrN = dataTable.getAttrIndex((String) attrIds.elementAt(i));
			if (numbers.indexOf(attrN) < 0 && !isAttributeInUse(attrN)) {
				numbers.addElement(attrN);
			}
		}
		if (numbers.size() < 1)
			return;
		int nn = 0;
		for (int i = 0; i < numbers.size(); i++) {
			int aidx = numbers.elementAt(i);
			if (dataTable.isAttributeNumeric(aidx) || dataTable.isAttributeTemporal(aidx)) {
				delCh.addItem(dataTable.getAttributeName(aidx));
				dq.addAttributeToQuery(aidx);
				++nn;
			} else {
				int nqc = getQualCondCount();
				delCh.insert(dataTable.getAttributeName(aidx), nqc + 1);
				/*
				QualAttrCondition cond=new QualAttrCondition();
				cond.setTable(dataTable);
				cond.setAttributeIndex(aidx);
				cond.setMissingValuesOK(!cbFOutMV.getState());
				*/
				StringAttrConditionUI qUI = new StringAttrConditionUI(dataTable, aidx, filter, sup);
				qUI.setDQS(dqs);
				qUI.setDQSIndex(i);
				if (qualUIs == null) {
					qualUIs = new Vector(10, 10);
				}
				qualUIs.insertElementAt(qUI, nqc);
				dqs.addAttr();
			}
		}
		if (qualUIs != null) {
			for (int i = 0; i < qualUIs.size(); i++) {
				qualUIs.elementAt(i).setDQSIndex(i);
			}
		}
		dq.setDQSStartIndex(getQualCondCount());
		dq.updateStatistics();
		delCh.setEnabled(delCh.getItemCount() > 2);
		if (nn > 0) {
			tfmin = dq.getTFMin();
			tfmax = dq.getTFMax();
			lmin = dq.getLMin();
			lmax = dq.getLMax();
			if (minP != null) {
				minP.removeAll();
			} else {
				minP = new Panel();
			}
			minP.setLayout(new GridLayout(tfmin.length + 1, 1));
			for (int i = 0; i < tfmin.length; i++) {
				minP.add(makeLimitPanel(lmin[i], tfmin[i]));
			}
			if (maxP != null) {
				maxP.removeAll();
			} else {
				maxP = new Panel();
			}
			maxP.setLayout(new GridLayout(tfmin.length + 1, 1));
			for (int i = 0; i < tfmin.length; i++) {
				maxP.add(makeLimitPanel(lmax[i], tfmax[i]));
			}
		}
		layoutCondPanel();
		validateFrame();
	}

	public AttributeDataPortion getTable() {
		return dataTable;
	}

	protected void validateFrame() {
		if (!isShowing())
			return;
		Frame fr = CManager.getFrame(this);
		if (fr != null) {
			fr.pack();
			Rectangle bounds = fr.getBounds();
			Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
			if (bounds.width > ss.width * 3 / 4) {
				bounds.width = ss.width * 3 / 4;
			}
			if (bounds.height > ss.height * 3 / 4) {
				bounds.height = ss.height * 3 / 4;
			}
			if (bounds.x + bounds.width > ss.width) {
				bounds.x = ss.width - bounds.width;
			}
			if (bounds.y + bounds.height > ss.height) {
				bounds.y = ss.height - bounds.height;
			}
			fr.setBounds(bounds);
		}
		CManager.validateAll(this);
	}

	/**
	* As a SaveableTool, a DynamicQueryShell may be registered somewhere and,
	* hence, must notify the component where it is registered about its destroying.
	* This vector contains the listeners to be notified about destroying of the
	* DynamicQueryShell.
	*/
	protected Vector destroyListeners = null;

	/**
	* Adds a listener to be notified about destroying the tool.
	* A SaveableTool may be registered somewhere and, hence, must notify the
	* component where it is registered about its destroying.
	*/
	@Override
	public void addDestroyingListener(PropertyChangeListener lst) {
		if (lst == null)
			return;
		if (destroyListeners == null) {
			destroyListeners = new Vector(5, 5);
		}
		if (!destroyListeners.contains(lst)) {
			destroyListeners.addElement(lst);
		}
	}

	/**
	* Stops listening of all events, notifies about its destroying
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
		if (dataTable != null) {
			dataTable.removePropertyChangeListener(this);
		}
		if (dq != null) {
			dq.destroy();
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

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() instanceof Button) {
			String cmd = ae.getActionCommand();
			if (cmd.equals("clear")) {
				dq.clearAllFilters();
				if (qualUIs != null && qualUIs.size() > 0) {
					for (int i = 0; i < qualUIs.size(); i++) {
						qualUIs.elementAt(i).filterCleared();
					}
				}
			} else if (cmd.equals("add")) {
				Vector excludeColIds = new Vector(dq.getFieldCount() + getQualCondCount(), 1);
				for (int i = 0; i < dataTable.getAttrCount(); i++)
					if (isAttributeInUse(i)) {
						excludeColIds.addElement(dataTable.getAttributeId(i));
					}
				AttributeChooser attrSel = new AttributeChooser();
				if (attrSel.selectColumns(dataTable, null, excludeColIds, false, res.getString("Select_attributes_to"), sup.getUI()) != null) {
					addAttributes(attrSel.getSelectedColumnIds());
				}
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(dataTable))
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
			}
	}

	public int getInstanceN() {
		return dq.getInstanceN();
	}

	public DynamicQuery getDynamicQuery() {
		return dq;
	}

	/**
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A tool state description (specification) is stored as a
	* sequence of lines starting with <tagName> and ending with </tagName>, where
	* tagName is a unique keyword for a particular class of tools.
	*/
	@Override
	public String getTagName() {
		return "query";
	}

	/**
	* Returns the specification (i.e. state description) of this tool for storing
	* in a file. The specification must allow correct re-construction of the tool.
	*/
	@Override
	public Object getSpecification() {
		QuerySpec spec = new QuerySpec();
		spec.tagName = getTagName();
		spec.methodId = "dynamic_query";
		if (dataTable != null) {
			spec.table = dataTable.getContainerIdentifier();
		}
		int nAttr = 0;
		if (dq != null) {
			nAttr = dq.getFieldCount();
		}
		if (qualUIs != null) {
			nAttr += qualUIs.size();
		}
		if (nAttr > 0) {
			spec.attributes = new Vector(nAttr, 1);
			if (filter != null && filter.getAttrConditionCount() > 0) {
				spec.conditions = new Vector(filter.getAttrConditionCount(), 1);
				for (int i = 0; i < filter.getAttrConditionCount(); i++) {
					AttrCondition cond = filter.getAttrCondition(i);
					spec.attributes.addElement(cond.getAttributeId());
					if (cond.hasLimit()) {
						ConditionSpec csp = new ConditionSpec();
						csp.type = cond.getConditionType();
						csp.description = cond.getDescription();
						if (csp.description != null) {
							spec.conditions.addElement(csp);
						}
					}
				}
				if (spec.conditions.size() < 1) {
					spec.conditions = null;
				}
			}
		}
		spec.properties = getProperties();
		return spec;
	}

	/**
	* Returns custom properties of the tool: String -> String
	*/
	public Hashtable getProperties() {
		if (dq != null)
			return dq.getProperties();
		return null;
	}

	/**
	* After the tool is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	*/
	@Override
	public void setProperties(Hashtable properties) {
		if (properties == null)
			return;
		if (dq != null) {
			dq.setProperties(properties);
		}
	}

	/**
	* Setups the tool according to the given specification, if appropriate.
	*/
	@Override
	public void applySpecification(Object spec) {
		if (dq == null)
			return;
		if (spec != null && (spec instanceof QuerySpec)) {
			QuerySpec qsp = (QuerySpec) spec;
			boolean fOutMV = false;
			if (qsp.attributes != null && qsp.attributes.size() > 0) {
				addAttributes(qsp.attributes);
				if (qsp.conditions != null && qsp.conditions.size() > 0) {
					Vector numConditions = new Vector(qsp.conditions.size(), 1), qualConditions = new Vector(qsp.conditions.size(), 1);
					for (int i = 0; i < qsp.conditions.size(); i++) {
						ConditionSpec csp = (ConditionSpec) qsp.conditions.elementAt(i);
						if (csp.type.equalsIgnoreCase("QualAttrCondition")) {
							qualConditions.addElement(csp);
						} else if (csp.type.equalsIgnoreCase("NumAttrCondition")) {
							numConditions.addElement(csp);
						}
					}
					if (numConditions.size() > 0) {
						int n0 = dq.getFieldCount();
						dq.setConditions(numConditions);
						fOutMV = dq.getFOutMV();
						int n1 = dq.getFieldCount();
						if (n1 > n0) {
							int fn[] = dq.getFn();
							for (int i = n0; i < n1; i++) {
								delCh.addItem(dataTable.getAttributeName(fn[i]));
							}
						}
					}
					if (qualConditions.size() > 0 && qualUIs.size() > 0) {
						for (int i = 0; i < qualConditions.size() && i < qualUIs.size(); i++) {
							StringAttrConditionUI qui = qualUIs.elementAt(i);
							AttrCondition cond = qui.getCondition();
							ConditionSpec csp = (ConditionSpec) qualConditions.elementAt(i);
							cond.setup(csp.description);
							fOutMV = !cond.getMissingValuesOK();
						}
					}
					if (qualUIs != null) {
						for (int i = 0; i < qualUIs.size(); i++) {
							qualUIs.elementAt(i).setDQSIndex(i);
						}
					}
					dq.setDQSStartIndex(getQualCondCount());
					dq.updateStatistics();
					cbFOutMV.setState(fOutMV);
				}
			}
		}
	}

	@Override
	public void validate() {
		super.validate();
		if (itemH == 0 && dqs != null && dqs.getComponentCount() > 0) {
			itemH = dqs.getHeight() / dqs.getComponentCount();
		}
		if (qualUIs != null && qualUIs.size() > 0 && dq != null && dq.getFieldCount() > 0) {
			int h = itemH * (dq.getFieldCount() + 1), w = canvas_dq.getSize().width;
			canvas_dq.setSize(w, h);
			canvas_dq.setPreferredSize(new Dimension(w, h));
			canvas_dq.invalidate();
			condPan.invalidate();
			condPan.validate();
		}
	}
}
