package spade.analysis.plot;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.generators.PlotGeneratorsDescriptor;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.PlotCanvas;
import spade.lib.basicwin.StringInRectangle;
import spade.vis.action.ObjectEvent;
import spade.vis.action.ObjectEventHandler;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTreater;
import spade.vis.event.EventSource;
import spade.vis.spec.SaveableTool;
import spade.vis.spec.ToolSpec;

/**
* Implements the graphical display type known in statistics as "scatterplot
* matrix". Includes several dynamically linked scatterplots.
*/

public class ScatterMatrix extends Panel implements Destroyable, DataTreater, SaveableTool, ObjectEventHandler, EventSource, PrintableImage {
	/**
	* Identifier of this visualization method
	*/
	protected String methodId = null;
	/**
	* Used to generate unique identifiers of instances of ScatterMatrix
	*/
	protected static int nInstances = 0;
	protected int instanceN = 0;

	/**
	* The source of the data to be shown on the plots
	*/
	protected AttributeDataPortion dataTable = null;
	/**
	* Identifiers of the atributes represented in this scatterplot matrix
	*/
	protected Vector attributes = null;

	protected Supervisor supervisor = null;
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

//ID
	protected Vector scatterPlots = new Vector();

//~ID

	public ScatterMatrix(Supervisor sup, AttributeDataPortion dataTable, Vector attributes) {
		instanceN = ++nInstances;
		supervisor = sup;
		this.dataTable = dataTable;
		this.attributes = attributes;
		if (supervisor != null) {
			supervisor.registerObjectEventSource(this);
			supervisor.registerDataDisplayer(this);
		}

		int nattr = attributes.size();
		setLayout(new GridLayout(nattr, nattr));
		for (int i = 0; i < nattr; i++) {
			int fny = dataTable.getAttrIndex((String) attributes.elementAt(i));
			for (int j = 0; j < nattr; j++)
				if (i == j) {
					add(new Label(dataTable.getAttributeName(fny), Label.CENTER));
				} else {
					int fnx = dataTable.getAttrIndex((String) attributes.elementAt(j));
					ScatterPlot sp = new ScatterPlotWithFocusers(false, true, supervisor, this);
//ID
					scatterPlots.addElement(sp);
//~ID
					sp.setDataSource(dataTable);
					sp.setFieldNumbers(fnx, fny);
					sp.setIsZoomable(true);
					sp.setup();
					sp.checkWhatSelected();
					PlotCanvas canvas = new PlotCanvas();
					sp.setCanvas(canvas);
					canvas.setContent(sp);
					add(canvas);
				}
		}
	}

	/**
	* A method from the ObjectEventHandler interface.
	* The ScatterMatrix receives object events from its scatterplots and tranferres
	* them to the supervisor.
	*/
	@Override
	public void processObjectEvent(ObjectEvent oevt) {
		if (supervisor != null) {
			ObjectEvent e = new ObjectEvent(this, oevt.getType(), oevt.getSourceMouseEvent(), oevt.getSetIdentifier(), oevt.getAffectedObjects());
			e.dataT = oevt.getDataTreater();
			supervisor.processObjectEvent(e);
		}
	}

//---------------- implementation of the EventSource interface ------------
	/**
	* The EventSource answers whether it can produce the specified
	* type of events.
	*/
	@Override
	public boolean doesProduceEvent(String eventId) {
		return eventId.equals(ObjectEvent.click) || eventId.equals(ObjectEvent.point) || eventId.equals(ObjectEvent.frame);
	}

	/**
	* Returns a unique identifier of the event source (may be produced
	* automatically, used only internally, not shown to the user).
	* The identifier is used for explicit linking of producers and recipients of
	* object events.
	*/
	@Override
	public String getIdentifier() {
		return "Scatterplot_Matrix_" + instanceN;
	}

	/**
	* Makes necessary operations for destroying, e.g. unregisters from
	* listening highlighting and other events.
	*/
	@Override
	public void destroy() {
		if (supervisor != null) {
			supervisor.removeObjectEventSource(this);
			supervisor.removeDataDisplayer(this);
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

	public int getInstanceN() {
		return instanceN;
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
		Hashtable prop = new Hashtable();
		String sh = "", sv = "";
		for (int i = 0; i < scatterPlots.size(); i++) {
			Hashtable curProp = ((ScatterPlotWithFocusers) scatterPlots.elementAt(i)).getProperties();
			sh += curProp.get("rangeHorizontal") + ",";
			sv += curProp.get("rangeVertical") + ",";
		}
		prop.put("rangeHorizontal", sh.substring(0, sh.length() - 1));
		prop.put("rangeVertical", sv.substring(0, sv.length() - 1));
		return prop;
	}

	/**
	* After the plot is constructed, it may be requested to setup its individual
	* properties according to the given list of stored properties.
	* The base Plot class does nothing in this method.
	*/
	@Override
	public void setProperties(Hashtable properties) {
		if (scatterPlots.size() == 0)
			return;
		try {
			String sh = (String) properties.get("rangeHorizontal");
			StringTokenizer sth = new StringTokenizer(sh, " ,");
			String sv = (String) properties.get("rangeVertical");
			StringTokenizer stv = new StringTokenizer(sv, " ,");
			Hashtable prop;
			for (int i = 0; i < scatterPlots.size(); i++) {
				prop = new Hashtable();
				prop.put("rangeHorizontal", sth.nextToken() + " " + sth.nextToken());
				prop.put("rangeVertical", stv.nextToken() + " " + stv.nextToken());
				((ScatterPlotWithFocusers) scatterPlots.elementAt(i)).setProperties(prop);
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

//ID
	@Override
	public Image getImage() {
		Image img = createImage(getBounds().width, getBounds().height);
		int w = getBounds().width / attributes.size();
		int h = getBounds().height / attributes.size();
		int curr = 0;
		for (int y = 0; y < attributes.size(); y++) {
			for (int x = 0; x < attributes.size(); x++) {
				if (y == x) {
					Image ic = createImage(w, h);
					StringInRectangle.drawString(ic.getGraphics(), dataTable.getAttributeName(dataTable.getAttrIndex((String) attributes.elementAt(x))), 0, h / 2 - 8, w, StringInRectangle.Center);
					img.getGraphics().drawImage(ic, w * x, h * y, null);
					continue;
				}
				ScatterPlot sp = (ScatterPlot) scatterPlots.elementAt(curr);
				Image ic = createImage(w, h);
				sp.draw(ic.getGraphics());

				img.getGraphics().drawImage(ic, w * x, h * y, null);

				curr++;
			}
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