package spade.analysis.plot;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;
import spade.lib.util.FloatArray;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

public class StatisticsMultiPanel extends Panel implements ItemListener, DataTreater, SaveableTool, Destroyable, PrintableImage {
	static ResourceBundle res = Language.getTextResource("spade.analysis.plot.Res");

	protected Supervisor supervisor = null;
	protected AttributeDataPortion dataTable = null;
	protected Vector attributes = null;

	protected StatisticsPanel sps[] = null;
	protected Checkbox cb = null;
	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	/**
	* As a SaveableTool, a plot may be registered somewhere and, hence, must
	* notify the component where it is registered about its destroying. This vector
	* contains the listeners to be notified about destroying of the visualizer.
	*/
	protected Vector destroyListeners = null;

	public StatisticsMultiPanel(Supervisor supervisor, AttributeDataPortion dataTable, Vector attributes) {
		super();
		this.supervisor = supervisor;
		this.dataTable = dataTable;
		this.attributes = attributes;
		sps = new StatisticsPanel[attributes.size()];
		setLayout(new BorderLayout());
		ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		add(sp, BorderLayout.CENTER);
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		sp.add(p);
		setName("Statistics");
		for (int i = 0; i < attributes.size(); i++) {
			p.add(new Label(dataTable.getAttributeName(dataTable.getAttrIndex((String) attributes.elementAt(i)))));
			sps[i] = new StatisticsPanel(supervisor, (String) attributes.elementAt(i), dataTable, true);
			p.add(sps[i]);
			supervisor.registerDataDisplayer(sps[i]);
			p.add(new Line(false));
		}
		// following String: "Common Min and Max"
		cb = new Checkbox(res.getString("Common_Min_and_Max"), false);
		cb.addItemListener(this);
		add(cb, BorderLayout.SOUTH);
	}

	public StatisticsMultiPanel(Vector data) {
		super();
		sps = new StatisticsPanel[data.size() / 2];
		setLayout(new BorderLayout());
		ScrollPane sp = new ScrollPane();
		add(sp, BorderLayout.CENTER);
		Panel p = new Panel();
		p.setLayout(new ColumnLayout());
		sp.add(p);
		setName("Statistics");
		int n = -1;
		for (int i = 0; i < data.size(); i += 2) {
			String str = (String) data.elementAt(i);
			p.add(new Label(str));
			FloatArray fa = (FloatArray) data.elementAt(i + 1);
			n++;
			sps[n] = new StatisticsPanel(fa, true);
			p.add(sps[n]);
			//supervisor.registerDataDisplayer(sps[i]);
			p.add(new Line(false));
		}
		// following String: "Common Min and Max"
		cb = new Checkbox(res.getString("Common_Min_and_Max"), false);
		cb.addItemListener(this);
		add(cb, BorderLayout.SOUTH);
	}

	protected void reset() {
		double gmin = Double.NaN, gmax = Double.NaN;
		for (StatisticsPanel sp : sps) {
			if (dataTable != null) {
				sp.reset();
			}
			if (cb != null && cb.getState()) {
				double f = sp.getMin();
				if (!Double.isNaN(f))
					if (Double.isNaN(gmin) || gmin > f) {
						gmin = f;
					}
				f = sp.getMax();
				if (!Double.isNaN(f))
					if (Double.isNaN(gmax) || gmax < f) {
						gmax = f;
					}
			}
		}
		if (cb != null && cb.getState()) {
			for (StatisticsPanel sp : sps) {
				sp.setMultiAttrMinMax(gmin, gmax);
			}
		} else {
			for (StatisticsPanel sp : sps) {
				sp.setMultiAttrMinMax(sp.getMin(), sp.getMax());
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (cb != null && cb.equals(ie.getSource())) {
			reset();
		}
	}

	/**
	* A method from the DataTreater interface.
	* Returns a vector of IDs of the attributes this Data Treater deals with.
	*/
	@Override
	public Vector getAttributeList() {
		return attributes;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return setId != null && dataTable != null && setId.equals(dataTable.getContainerIdentifier());
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
		if (dataTable != null) {
			spec.table = dataTable.getContainerIdentifier();
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
		return null;
	}

	/**
	* After the plot is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	* The base Plot class does nothing in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
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
		if (sps != null) {
			for (StatisticsPanel sp : sps) {
				sp.destroy();
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
		int maxInstance = 0;
		for (StatisticsPanel sp : sps) {
			maxInstance = Math.max(maxInstance, sp.getInstanceN());
		}
		return maxInstance;
	}

//~ID
//ID
	@Override
	public Image getImage() {
		Vector images = new Vector();
		int w = 0, h = 0, curr = 0;
		for (StatisticsPanel sp : sps) {
			Image ci = sp.getImage();
			w = Math.max(w, ci.getWidth(null));
			h += ci.getHeight(null);
			images.addElement(ci);
		}
		Image img = createImage(w, h);
		for (int i = 0; i < images.size(); i++) {
			Image ci = (Image) images.elementAt(i);
			img.getGraphics().drawImage(ci, 0, curr, null);
			curr += ci.getHeight(null);
		}
		return img;
	}

	@Override
	public String getName() {
		String name = PlotGeneratorsDescriptor.getToolName(getMethodId()) + " " + getInstanceN();
		if (name == null) {
			name = getMethodId() + " " + getInstanceN();
		}
		return name;
	}
//~ID
}
