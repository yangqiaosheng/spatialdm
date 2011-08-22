package spade.analysis.calc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.lang.Language;
import spade.vis.database.AttrTransform;
import spade.vis.database.DataTable;
import spade.vis.database.DataTreater;
import spade.vis.database.TableStat;
import spade.vis.map.MapViewer;
import spade.vis.space.GeoLayer;

public abstract class CalcDlg extends Frame implements Calculator, DataTreater {
	protected DataTable dTable = null;
	static ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	protected TableStat tStat = null;
	protected AttrTransform aTransf = null;
	/**
	* The numbers (indexes in the table) of the source attributes for calculations
	*/
	protected int fn[] = null;

	public int[] getFn() {
		return fn;
	}

	/**
	* The descriptors of the source attributes for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected
	* values of relevant parameters. The elements of the vector are instances of
	* the class spade.vis.database.AttrDescriptor.
	*/
	protected Vector attrDescr = null;

	protected Supervisor supervisor = null;
	protected DisplayProducer displayProducer = null;
	protected String layerId = null;
	protected MapViewer mapViewer = null;

	public CalcDlg() {
		// following text: "Calculations in table"
		super(res.getString("Calculations_in_table"));
	}

	/**
	* Sets the table in which to do calculations
	*/
	@Override
	public void setTable(DataTable table) {
		dTable = table;
	}

	/**
	* Returns the table in which the calculations are done
	*/
	@Override
	public DataTable getTable() {
		return dTable;
	}

	/**
	* Sets the numbers of the source attributes for calculations
	*/
	@Override
	public void setAttrNumbers(int attrNumbers[]) {
		fn = attrNumbers;
	}

	/**
	 * Returns the numbers of the source attributes for calculations
	 */
	@Override
	public int[] getAttrNumbers() {
		return fn;
	}

	/**
	* Sets the descriptors of the source attributes for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected
	* values of relevant parameters. The elements of the vector are instances of
	* the class spade.vis.database.AttrDescriptor.
	*/
	@Override
	public void setAttrDescriptors(Vector attrDescr) {
		this.attrDescr = attrDescr;
	}

	/**
	* Sets the supervisor supporting dynamic linking between displays
	*/
	public void setSupervisor(Supervisor sup) {
		this.supervisor = sup;
		if (supervisor != null) {
			supervisor.registerDataDisplayer(this);
		}
	}

	/**
	* A DisplayProducer is used to construct plots, show results on map etc.
	*/
	public void setDisplayProducer(DisplayProducer dprod) {
		displayProducer = dprod;
	}

	/**
	* Results of calculations may be represented on a map. For this purpose
	* a reference to the appropriate map layer is needed.
	*/
	public void setGeoLayerId(String identifier) {
		layerId = identifier;
	}

	/**
	* Tries to find an appropriate map view for the representation of
	* computation results. May propose the user to create a new map window
	* to avoid destroying the current visualizations in the main window.
	* Stores the map view for further use (e.g. for repeated visualization
	* of calculation results).
	* The agrument @arg must shows whether the user has an option to refuse
	* from representing computation results on a map.
	*/
	protected MapViewer getMapView(boolean must) {
		if (mapViewer != null)
			return mapViewer;
		if (layerId == null)
			return null;
		if (supervisor == null || supervisor.getUI() == null)
			return null;
		MapViewer mapView = supervisor.getUI().getCurrentMapViewer();
		if (mapView == null || mapView.getLayerManager() == null)
			return null;
		int idx = mapView.getLayerManager().getIndexOfLayer(layerId);
		if (idx < 0)
			return null;
		SelectDialog selDia = new SelectDialog(CManager.getAnyFrame((Component) mapView), res.getString("show_on_map"), (must) ? res.getString("show_on_map") : res.getString("want_show_on_map"));
		selDia.addOption(res.getString("main_map"), "0", false);
		selDia.addOption(res.getString("new_map"), "1", true);
		if (!must) {
			selDia.addOption(res.getString("no_map"), "2", false);
		}
		selDia.show();
		if (selDia.wasCancelled())
			return null;
		if (selDia.getSelectedOptionN() == 2)
			return null;
		if (selDia.getSelectedOptionN() == 1) {
			mapView = supervisor.getUI().getMapViewer("_blank_");
			if (mapView == null || mapView.getLayerManager() == null) {
				supervisor.getUI().showMessage(res.getString("failed_make_map"), true);
				return null;
			}
			Frame win = CManager.getFrame((Component) mapView);
			if (win != null) {
				Panel p = new Panel(new BorderLayout());
				p.add(new Label(res.getString("window_name") + "?"), BorderLayout.WEST);
				TextField tf = new TextField(win.getTitle(), 60);
				p.add(tf, BorderLayout.CENTER);
				OKDialog okd = new OKDialog(win, res.getString("window_name"), true);
				okd.addContent(p);
				okd.show();
				if (!okd.wasCancelled()) {
					String str = tf.getText();
					if (str != null) {
						str = str.trim();
						if (str.length() > 0) {
							win.setTitle(str);
						}
					}
				}
			}
		}
		mapViewer = mapView;
		return mapViewer;
	}

	/**
	* Tries to represent the computation results on a map.
	* The agrument @arg must shows whether the user has an option to refuse
	* from representing computation results on a map.
	*/
	protected void tryShowOnMap(Vector attrs, String methodId, boolean must) {
		if (displayProducer == null || layerId == null)
			return;
		MapViewer mapView = getMapView(must);
		if (mapView == null)
			return;
		int idx = mapView.getLayerManager().getIndexOfLayer(layerId);
		if (idx < 0)
			return;
		GeoLayer layer = mapView.getLayerManager().getGeoLayer(idx);
		if (layer == null)
			return;
		if (methodId == null) {
			displayProducer.displayOnMap(dTable, attrs, layer, mapView);
		} else {
			displayProducer.displayOnMap(methodId, dTable, attrs, layer, mapView);
		}
	}

	/**
	* Constructs the dialog appearance
	*/
	protected abstract void makeInterface();

	/**
	 * Should return false if the Calculator only modifies the values of the selected
	 * attributes but does not create any new attributes. By default, returns true.
	 */
	@Override
	public boolean doesCreateNewAttributes() {
		return true;
	}

	/**
	* Starts calculations. Returns null because calculations are combined with
	* visualisation and do not finish immediately.
	*/
	@Override
	public Vector doCalculation() {
		makeInterface();
		show();
		start();
		return null;
	}

	/**
	* Starts the calculation process
	*/
	protected abstract void start();

	/**
	* If there was an error in computation, returns the error message
	* However, a CalcDLG itself displays error messages, if any.
	* Therefore this method returns null.
	*/
	@Override
	public String getErrorMessage() {
		return null;
	}

	/**
	* Allows the user to edit names of attributes added to the table.
	* Uses the class AttrNameEditor.
	*/
	protected void attrAddedToTable(Vector resultAttrs) {
		AttrNameEditor.attrAddedToTable(dTable, resultAttrs);
	}

	/**
	* A method from the DataTreater interface.
	* Returns a vector of IDs of the attributes this Data Treater deals with.
	*/
	@Override
	public Vector getAttributeList() {
		if (fn == null || dTable == null)
			return null;
		Vector attr = new Vector(fn.length, 5);
		for (int element : fn) {
			String id = dTable.getAttributeId(element);
			if (id != null) {
				attr.addElement(id);
			}
		}
		return attr;
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return setId != null && dTable != null && setId.equals(dTable.getContainerIdentifier());
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with. By default, returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		if (fn == null || dTable == null)
			return null;
		Vector colors = new Vector(fn.length, 5);
		for (int element : fn) {
			String id = dTable.getAttributeId(element);
			if (id != null) {
				colors.addElement(supervisor.getColorForAttribute(id));
			}
		}
		return colors;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (supervisor != null) {
			supervisor.removeDataDisplayer(this);
		}
		CManager.destroyComponent(this);
	}
}
