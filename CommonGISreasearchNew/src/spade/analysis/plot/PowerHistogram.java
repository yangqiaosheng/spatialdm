package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.LayoutSelector;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.basicwin.TImgButton;
import spade.lib.basicwin.TriangleDrawer;
import spade.lib.lang.Language;
import spade.time.TimeMoment;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

/**
* Includes a frequency histogram of distribution of values of a single numeric
* attribute and interactive controls for manipulating the histogram.
*/
public class PowerHistogram extends Panel implements ActionListener, PropertyChangeListener, ItemListener, SaveableTool, DataTreater, Destroyable, PrintableImage {
	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");
	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;
//ID
	/**
	* Used to generate unique identifiers of instances
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;
//~ID
	/**
	* The canvas, which actually draws the histogram
	*/
	protected HistogramCanvas hist[] = null;
	/**
	 *  The filed for count of histgram bars
	 */
	protected TextField countTF = null;
	protected TImgButton upBt = null;
	protected TImgButton downBt = null;
	protected Checkbox commonMinMaxCB = null;
	protected Checkbox commonMaxFCB = null;
	protected Vector attributes = null;
	protected AttributeDataPortion table = null;
	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Constructs the panel with the histogram canvas and controls for manipulating
	* it. Arguments:
	* table - the table with data, in particular, with values of the attribute to be
	*         represented on the histogram;
	* attrId - the identifier of the attribute to be visualized;
	* supervisor - the component that propagates object events among all system
	*              components.
	*/
	public PowerHistogram(AttributeDataPortion table, Vector attributes, Supervisor supervisor) {
//ID
		instanceN = ++nInstances;
//~ID
		setLayout(new BorderLayout());
		this.attributes = attributes;
		this.table = table;

		table.addPropertyChangeListener(this);

		/*
		if (table!=null && attrId!=null) {
		  int idx=table.getAttrIndex(attrId);
		  if (idx>=0)
		    add(new Label(table.getAttributeName(idx)),"North");
		}
		*/
		hist = new HistogramCanvas[attributes.size()];
		//Panel cp = new Panel(new FlowLayout(FlowLayout.LEFT,2,1));
		Panel cp = null;
		if (attributes.size() > 1) {
			cp = new LayoutSelector(this, LayoutSelector.HORISONTAL, false, new int[] { LayoutSelector.VERTICAL, LayoutSelector.HORISONTAL, LayoutSelector.MATRIX_HOR });
		} else {
			cp = new Panel(new BorderLayout());
		}
		add(cp, "Center");
		for (int i = 0; i < attributes.size(); i++) {
			Panel ccp = new Panel(new BorderLayout());

			hist[i] = new HistogramCanvas();
			ccp.add(hist[i], "Center");
			String attrId = (String) attributes.elementAt(i);
			int idx = table.getAttrIndex(attrId);

			if (attributes.size() > 1) {
				ccp.setName(table.getAttributeName(idx));
				((LayoutSelector) cp).addElement(ccp);
			} else {
				//ccp.add(new Label(table.getAttributeName(idx)),"North");
				cp.add(ccp, "Center");
			}

			hist[i].setTable(table);
			hist[i].setAttribute(attrId);
			hist[i].setSupervisor(supervisor);
			hist[i].addPropertyChangeListener(this);

		}

		Panel sp = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));

		//add(sp,"South");
		if (attributes.size() > 1) {
			((LayoutSelector) cp).getControlPanel().add(sp, 0);
		} else {
			cp.add(sp, "North");
		}
		countTF = new TextField("10");
		countTF.addActionListener(this);
		sp.add(new Label("Intervals:"));
		sp.add(countTF);
		TriangleDrawer td = new TriangleDrawer(TriangleDrawer.N);
		td.setPreferredSize(14, 14);
		td.setMargins(2, 1);
		upBt = new TImgButton(td);
		upBt.addActionListener(this);
		sp.add(upBt);
		td = new TriangleDrawer(TriangleDrawer.S);
		td.setPreferredSize(14, 14);
		td.setMargins(2, 1);
		downBt = new TImgButton(td);
		downBt.addActionListener(this);
		sp.add(downBt);
		Panel p = new Panel(new ColumnLayout());
		sp.add(new Label("Common", Label.RIGHT));
		commonMinMaxCB = new Checkbox("values");
		commonMinMaxCB.addItemListener(this);
		commonMaxFCB = new Checkbox("counts");
		commonMaxFCB.addItemListener(this);
		sp.add(commonMinMaxCB);
		sp.add(commonMaxFCB);

		findAbsMinMax();
	}

	@Override
	public String getName() {
		String name = PlotGeneratorsDescriptor.getToolName(getMethodId()) + " " + getInstanceN();
		if (name == null) {
			name = getMethodId() + " " + getInstanceN();
		}
		return name;
	}

	protected void findAbsMinMax() {
		double absMin = Double.NaN;
		double absMax = Double.NaN;
		TimeMoment absMinTime = null, absMaxTime = null;
		int nTemporalAttr = 0;
		for (int i = 0; i < attributes.size(); i++) {
			String attrId = (String) attributes.elementAt(i);

			int idx = table.getAttrIndex(attrId);
			if (!commonMinMaxCB.getState()) {
				absMin = Double.NaN;
				absMax = Double.NaN;
			}
			boolean temporal = table.isAttributeTemporal(idx);
			if (temporal) {
				++nTemporalAttr;
			}
			TimeMoment minTime = null, maxTime = null;
			for (int j = 0; j < table.getDataItemCount(); j++) {
				if (temporal) {
					Object val = table.getAttrValue(idx, j);
					if (val != null && (val instanceof TimeMoment)) {
						TimeMoment t = (TimeMoment) val;
						if (minTime == null || minTime.compareTo(t) > 0) {
							minTime = t;
						}
						if (maxTime == null || maxTime.compareTo(t) < 0) {
							maxTime = t;
						}
					}
				}
				double val = table.getNumericAttrValue(idx, j);
				if (Double.isNaN(val)) {
					continue;
				}
				if (Double.isNaN(absMin) || val < absMin) {
					absMin = val;
				}
				if (Double.isNaN(absMax) || val > absMax) {
					absMax = val;
				}
			}
			if (!commonMinMaxCB.getState()) {
				hist[i].setAbsMinMax(absMin, absMax);
			}
			if (minTime != null && maxTime != null) {
				hist[i].setMinMaxTime(minTime.getCopy(), maxTime.getCopy());
				if (absMinTime == null || absMinTime.compareTo(minTime) > 0) {
					absMinTime = minTime.getCopy();
				}
				if (absMaxTime == null || absMaxTime.compareTo(maxTime) < 0) {
					absMaxTime = maxTime.getCopy();
				}
			}
		}

		if (commonMinMaxCB.getState()) {
			boolean allTemporal = nTemporalAttr == attributes.size() && absMinTime != null && absMaxTime != null;
			for (int i = 0; i < attributes.size(); i++) {
				hist[i].setAbsMinMax(absMin, absMax);
				if (allTemporal) {
					hist[i].setMinMaxTime(absMinTime, absMaxTime);
				} else {
					hist[i].setMinMaxTime(null, null);
				}
			}
		}

	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		Object so = ev.getSource();
		if (so == countTF) {
			try {
				int bc = Integer.parseInt("" + countTF.getText());
				for (HistogramCanvas element : hist) {
					element.setCount(bc);
				}

				findMaxF();

			} catch (NumberFormatException ex) {

			}
		} else if (so == upBt) {
			for (HistogramCanvas element : hist) {
				element.shiftColors(HistogramCanvas.SHIFT_UP);
			}
		} else if (so == downBt) {
			for (HistogramCanvas element : hist) {
				element.shiftColors(HistogramCanvas.SHIFT_DOWN);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		if (evt.getPropertyName().equals("curr_min_max")) {
			double lmt[] = (double[]) evt.getNewValue();
			if (commonMinMaxCB.getState()) {
				for (HistogramCanvas element : hist) {
					element.setCurrMinMax(lmt[0], lmt[1]);
				}

			}
		} else if (evt.getPropertyName().equals("curr_max_f")) {
			double lmt[] = (double[]) evt.getNewValue();
			if (commonMaxFCB.getState()) {
				for (HistogramCanvas element : hist) {
					element.setCurrMaxF(lmt[0], lmt[1]);
				}
			}
		}
		if (evt.getPropertyName().equals("values")) {

			findAbsMinMax();

		}

	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		Object so = e.getSource();
		if (so == commonMinMaxCB) {
			findAbsMinMax();
		} else if (so == commonMaxFCB) {
			findMaxF();
			/*
			if(commonMaxFCB.getState()){
			  float maxF = 0;
			  for(int i = 0; i < hist.length; i++){
			    float f = hist[i].getMaxF();
			    if(f > maxF) maxF = f;
			  }

			  for(int i = 0; i < hist.length; i++){
			    hist[i].setMaxF(maxF);
			    hist[i].setSynch(true);
			  }

			}else{
			  for(int i = 0; i < hist.length; i++){
			    hist[i].setSynch(false);
			  }
			}
			*/
		}
	}

	protected void findMaxF() {
		if (commonMaxFCB.getState()) {
			double maxF = 0;
			for (HistogramCanvas element : hist) {
				double f = element.getMaxF();
				if (f > maxF) {
					maxF = f;
				}
			}

			for (HistogramCanvas element : hist) {
				element.setMaxF(maxF);
				element.setSynch(true);
			}

		} else {
			for (HistogramCanvas element : hist) {
				element.setSynch(false);
			}
		}

	}

//--------------------- DataTreater interface -----------------------------
	/**
	* Returns vector of IDs of attribute(s) on this display
	*/
	@Override
	public Vector getAttributeList() {
		if (attributes == null)
			return null;
		return (Vector) attributes.clone();

	}

	/**
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return setId != null && table != null && setId.equals(table.getContainerIdentifier());
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with. By default, returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

//---------------- implementation of the SaveableTool interface ------------
	/**
	* Adds a listener to be notified about destroying of the visualize.
	* As a SaveableTool, a visualizer may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying.
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
	* Returns the keyword used in the opening tag of a stored state description
	* of this tool. A visualizer description (specification) is usually stored as
	* a sequence of lines starting with <map> and ending with </map>. Hence,
	* this method returns "map".
	*/
	@Override
	public String getTagName() {
		return "chart";
	}

	/**
	* Returns the specification of this visualizer (normally an instance of the
	* class spade.vis.spec.ToolSpec) for storing in a file.
	*/
	@Override
	public Object getSpecification() {
		ToolSpec spec = new ToolSpec();
		spec.tagName = getTagName();
		spec.methodId = getMethodId();
		if (table != null) {
			spec.table = table.getContainerIdentifier();
		}
		spec.attributes = getAttributeList();
		spec.properties = getProperties();
		return spec;
	}

	/**
	* Returns custom properties of the tool: String -> String
	* By default, returns null.
	*/
	public Hashtable getProperties() {
		Hashtable prop = new Hashtable();
		prop.put("intervals", String.valueOf(hist[0].getCount()));
		prop.put("commonValues", String.valueOf(commonMinMaxCB.getState()));
		prop.put("commonCounts", String.valueOf(commonMaxFCB.getState()));

		String s = "";
		for (HistogramCanvas element : hist) {
			s += String.valueOf(element.getValuesLimit()) + " ";
		}
		prop.put("valuesLimit", s);
		s = "";
		for (HistogramCanvas element : hist) {
			s += String.valueOf(element.getFocusedMin()) + " ";
		}
		prop.put("focuserMin", s);
		s = "";
		for (HistogramCanvas element : hist) {
			s += String.valueOf(element.getFocusedMax()) + " ";
		}
		prop.put("focuserMax", s);

		return prop;
	}

	/**
	* After the plot is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	* The base Plot class does nothing in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
		try {
			commonMinMaxCB.setState(new Boolean((String) properties.get("commonValues")).booleanValue());
			findAbsMinMax();
		} catch (Exception ex) {
		}
		try {
			commonMaxFCB.setState(new Boolean((String) properties.get("commonCounts")).booleanValue());
			findMaxF();
		} catch (Exception ex) {
		}
		try {
			countTF.setText((String) properties.get("intervals"));
			int bc = Integer.parseInt("" + countTF.getText());
			for (HistogramCanvas element : hist) {
				element.setCount(bc);
			}
			findMaxF();
		} catch (Exception ex) {
		}

		try {
			StringTokenizer stVL = new StringTokenizer((String) properties.get("valuesLimit"), " ");
			StringTokenizer stFL = new StringTokenizer((String) properties.get("focuserMin"), " ");
			StringTokenizer stFH = new StringTokenizer((String) properties.get("focuserMax"), " ");
			if (hist.length != stVL.countTokens() || hist.length != stFL.countTokens() || hist.length != stFH.countTokens())
				return;
			for (HistogramCanvas element : hist) {
				element.setFocuserParameters(new Double(stFL.nextToken()).doubleValue(), new Double(stFH.nextToken()).doubleValue(), new Double(stVL.nextToken()).doubleValue());
			}
		} catch (Exception ex) {
		}
	}

	/**
	* Through this function the component constructing the plot can set the
	* identifier of the visualization method.
	*/
	public void setMethodId(String id) {
		methodId = id;
	}

	/**
	* Returns the identifier of the visualization method implemented by this
	* class.
	*/
	public String getMethodId() {
		return methodId;
	}

	/**
	* Notifies the listeners that this tool is destroyed, i.e.
	* sends a PropertyChangeEvent with the name "destroyed" to its
	* destroying listener(s), @see addDestroyingListener.
	*/
	@Override
	public void destroy() {
		if (hist != null) {
			for (HistogramCanvas element : hist) {
				element.destroy();
			}
		}
		destroyed = true;
		if (destroyListeners != null) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "destroyed", null, null);
			for (int i = 0; i < destroyListeners.size(); i++) {
				((PropertyChangeListener) destroyListeners.elementAt(i)).propertyChange(pce);
			}
		}
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

//ID
	public int getInstanceN() {
		return instanceN;
	}

//~ID

//ID
	@Override
	public Image getImage() {
		int gap = 2, indent = 20;
		StringBuffer sb = new StringBuffer();
		StringInRectangle str = new StringInRectangle();
		str.setPosition(StringInRectangle.Left);
		for (int i = 0; i < getAttributeList().size(); i++) {
			if (getAttributeList().size() > 1) {
				sb.append((i + 1) + ": ");
			} else {
				str.setPosition(StringInRectangle.Center);
				indent = 0;
			}
			sb.append(table.getAttributeName(table.getAttrIndex((String) getAttributeList().elementAt(i))));
			if (i < getAttributeList().size() - 1) {
				sb.append("\n");
			}
		}
		str.setString(sb.toString());
		Dimension lsize = str.countSizes(hist[0].getGraphics());
		int h = 0, w = 0, curr = 0;
		for (HistogramCanvas c : hist) {
			Rectangle bounds = c.getBounds();
			h = Math.max(h, bounds.height);
			w += bounds.width;
		}
		str.setRectSize(w - indent, lsize.width * lsize.height / w * 2);
		lsize = str.countSizes(hist[0].getGraphics());
		Image li = hist[0].createImage(w - indent, lsize.height);
		str.draw(li.getGraphics());
		Image img = hist[0].createImage(w, h + lsize.height + 2 * gap);
		for (HistogramCanvas element : hist) {
			Image ic = element.createImage(element.getBounds().width, element.getBounds().height);
			element.paint(ic.getGraphics());
			img.getGraphics().drawImage(ic, curr, 0, null);
			curr += element.getBounds().width;
		}
		img.getGraphics().drawImage(li, indent, h + gap, null);
		return img;
	}
}
//~ID

