package spade.vis.mapvis;

import java.awt.Component;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.classification.TableClassifier;
import spade.analysis.manipulation.Manipulator;
import spade.analysis.system.Supervisor;
import spade.lib.lang.Language;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.geometry.Geometry;

/**
* SimpleDataMapper supports construction of visualizers for representing
* thematic data on a map. Does not construct data transformers, etc.
* Does not provide information about the availability of the visualiser classes.
*/
public class SimpleDataMapper {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	/**
	* Information about implementation of visualization methods necessary for
	* construction of visualizers and manupulators. For each implemented
	* visualization method there is a line specifying:
	* 1) method identifier (internal, not visible for users)
	* 2) method name (to be shown to users)
	* 3) name of the class implementing the method for area objects
	* 4) name of the class implementing the method for sign objects (if different
	*    from (2), otherwise null)
	* 5) name of the class implementing the manipulator
	*/
	static protected final String mimpl[][] = MapVisRegister.getVisMethodsInfo();
	/**
	* Special visualization methods used only for time-series data. Typically these
	* methods are not proposed for selection together with the other methods.
	*/
	static protected final String timeVisMethods[][] = MapVisRegister.getTimeVisMethodsInfo();
	/**
	* The error message
	*/
	protected String err = null;

	/**
	 * Returns the index of the method with the given identifier in the given array
	 * of visualisation methods
	 */
	static public int getMethodIndex(String methodId, String methodList[][]) {
		if (methodId == null || methodList == null)
			return -1;
		for (int i = 0; i < methodList.length; i++)
			if (methodId.equals(methodList[i][0]))
				return i;
		return -1;
	}

	/**
	* Returns the index of the method with the given identifier in the array of
	* ALL methods
	*/
	static protected int getMethodIndex(String methodId) {
		return getMethodIndex(methodId, mimpl);
	}

	/**
	* Returns the index of the method with the given identifier in the array of
	* the special methods for time-series data
	*/
	static public int getTimeVisMethodIndex(String methodId) {
		return getMethodIndex(methodId, timeVisMethods);
	}

	/**
	 * Returns the full name of the method with the given identifier.
	 * The name may differ depending on the object type, which is specified as
	 * the second argument
	 */
	static public String getMethodName(String methodId, char objType) {
		String methodList[][] = null;
		switch (objType) {
		case Geometry.area:
			methodList = MapVisRegister.getVisMethodsForAreas();
			break;
		case Geometry.point:
			methodList = MapVisRegister.getVisMethodsForPoints();
			break;
		case Geometry.line:
			methodList = MapVisRegister.getVisMethodsForLines();
			break;
		}
		if (methodList == null) {
			methodList = mimpl;
		}
		int n = getMethodIndex(methodId, methodList);
		if (n < 0)
			return null;
		return methodList[n][1];
	}

	/**
	* Constructs a visualizer according to the given method identifier and object
	* type. The returned object may be a Visualizer or a Classifier
	*/
	public Object constructVisualizer(String methodId, char objType) {
		int midx = getMethodIndex(methodId);
		if (midx < 0)
			return null;
		String className = mimpl[midx][2];
		if (className == null)
			return null; //the method is not implemented
		if (mimpl[midx][3] != null && //there is a separate class for point objects
				objType == Geometry.point) {
			className = mimpl[midx][3];
		}
		try {
			Object vis = Class.forName(className).newInstance();
			if (vis != null) {
				if (vis instanceof Visualizer) {
					((Visualizer) vis).setVisualizationId(methodId);
				}
				if (vis instanceof Classifier) {
					((Classifier) vis).setMethodId(methodId);
				}
				return vis;
			}
		} catch (Exception e) {
			err = e.toString();
			System.out.println(err);
		}
		return null;
	}

	/**
	* Tries to visualise the given attributes from the given data portion
	* on the map. Assumes that the data portion has been already linked to
	* the active layer. Returns true on success. If visualisation fails, the error
	* message explains the reason.
	*/
	public Visualizer visualizeAttributes(String methodId, AttributeDataPortion dataTable, Vector attributes, char objType) {
		err = null;
		// construct visualizer
		Object v = constructVisualizer(methodId, objType);
		if (v == null)
			return null;
		return visualizeAttributes(v, methodId, dataTable, attributes, objType);
	}

	/**
	* Visualises the given attributes from the given data portion
	* on the map using the given (previously constructed) visualizer. Returns true
	* on success. If visualisation fails, the error message explains the reason.
	*/
	public Visualizer visualizeAttributes(Object v, String methodId, AttributeDataPortion dataTable, Vector attributes, char objType) {
		err = null;
		if (v == null)
			return null;
		Visualizer vis = null;
		if (v instanceof Visualizer) {
			vis = (Visualizer) v;
		} else if (v instanceof Classifier) {
			if (v instanceof TableClassifier) {
				TableClassifier tc = (TableClassifier) v;
				tc.setTable(dataTable);
				tc.setAttributes(attributes);
			}
			ClassDrawer cd = (objType == Geometry.point) ? new ClassSignDrawer() : new ClassDrawer();
			cd.setClassifier((Classifier) v);
			vis = cd;
		}
		if (vis == null)
			return null;
		vis.setVisualizationName(getMethodName(methodId, objType));
		vis.setVisualizationId(methodId);
		// provide statistics

		if (vis instanceof DataPresenter) {
			DataPresenter dpres = (DataPresenter) vis;
			dpres.setDataSource(dataTable);

			if (!dpres.isApplicable(attributes)) {
				err = dpres.getErrorMessage();
				dpres.destroy();
				return null;
			}
			dpres.setAttributes(attributes);
			if (!dpres.checkSemantics()) {
				err = dpres.getErrorMessage();
				dpres.destroy();
				return null;
			}
			dpres.setup();
			Vector aVis = dpres.getAttributes();
			if (aVis != null && aVis.size() > 0) {
				Vector attrNames = new Vector(aVis.size(), 5);
				for (int i = 0; i < aVis.size(); i++) {
					String aid = (String) aVis.elementAt(i), name = null;
					if (aid != null) {
						Attribute attr = dataTable.getAttribute(aid);
						if (attr != null) {
							name = attr.getName();
						}
					}
					attrNames.addElement(name);
				}
				dpres.setAttributes(aVis, attrNames);
			}
		}
		return vis;
	}

	/**
	* Generates a map manipulation component, depending on the visualization method.
	*/
	public Component getMapManipulator(String methodId, Visualizer vis, Supervisor sup, AttributeDataPortion dataTable) {
		if (methodId == null || vis == null)
			return null;
		int midx = getMethodIndex(methodId), tvmidx = -1;
		if (midx < 0) {
			tvmidx = getTimeVisMethodIndex(methodId);
			if (tvmidx < 0)
				return null;
		}

		Component manComp = null;
		String className = null;
		if (midx >= 0) {
			className = mimpl[midx][4];
		} else {
			className = timeVisMethods[tvmidx][4]; //the name of the class implementing the manipulator
		}
		if (className != null) { //the manipulator is implemented
			Object m = null;
			try {
				m = Class.forName(className).newInstance();
			} catch (Exception e) {
				System.out.println(e.toString());
			}
			if (m != null && (m instanceof Component)) {
				if (m instanceof Manipulator) {
					Manipulator man = (Manipulator) m;
					Object visualizer = vis;
					if (vis instanceof ClassDrawer) {
						ClassDrawer cd = (ClassDrawer) vis;
						visualizer = cd.getClassifier();
					}
					man.construct(sup, visualizer, dataTable);
				}
				manComp = (Component) m;
			}
		} else {
			System.out.println("the manipulator is not implemend");
		}
		return manComp;
	}

	/**
	* If data presentation of a map failed, returns the message
	* explaining the reason of this.
	*/
	public String getErrorMessage() {
		return err;
	}

}
