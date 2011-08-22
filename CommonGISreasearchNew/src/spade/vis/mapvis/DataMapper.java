package spade.vis.mapvis;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.TableClassifier;
import spade.analysis.manipulation.Manipulator;
import spade.analysis.manipulation.SetupDiagramsButton;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.AttributeTransformer;
import spade.analysis.transform.TransformedDataSaver;
import spade.analysis.transform.TransformerOwner;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.Parameters;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.database.ThematicDataSupplier;
import spade.vis.geometry.Geometry;

/**
* DataMapper supports selection of methods for visualization of thematic data
* on a map.
* * changes: hdz, 2004.004 method isMethodApplicabel:
* - set Attributes and datatable for tableclassifier, fo testing there if it
* is applicable for numerical values
*/
public class DataMapper extends SimpleDataMapper {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	/**
	* Some visualization methods can be externally switched off. In this variable
	* the settings for the methods (ON or OFF) are stored.
	*/
	protected Parameters methodSwitch = null;
	/**
	* The list of identifiers of available methods (to avoid multiple checking of
	* presence of classes)
	*/
	protected Vector availableMethods = null;

	/**
	* Finds a default representation for the given set of attributes, depending
	* on their number and types.
	*/
	public String getDefaultMethodId(AttributeDataPortion dataTable, Vector attributes, char objType) {
		if (dataTable == null || attributes == null || attributes.size() < 1 || (objType != Geometry.area && objType != Geometry.point))
			return null;
		IntArray fnumbers = new IntArray(attributes.size(), 5);
		for (int i = 0; i < attributes.size(); i++) {
			int n = dataTable.getAttrIndex((String) attributes.elementAt(i));
			if (n < 0)
				return null; //the attribute is not found
			fnumbers.addElement(n);
		}
		String methodId = null;
		if (attributes.size() == 1)
			if (dataTable.isAttributeNumeric(fnumbers.elementAt(0)) || dataTable.isAttributeTemporal(fnumbers.elementAt(0))) {
				methodId = "value_paint";
			} else {
				methodId = "qualitative_colour";
			}
		else {
			//check whether all attributes are numeric or qualitative
			int nNum = 0;
			for (int i = 0; i < fnumbers.size(); i++)
				if (dataTable.isAttributeNumeric(fnumbers.elementAt(i))) {
					++nNum;
				}
			if (nNum > 0 && nNum < fnumbers.size())
				return null; //mixed numeric and non-numeric
			if (nNum == 0) {
				methodId = "icons";
			} else if (fnumbers.size() == 2) {
				methodId = "class2D"; //cross-classification
			} else if (dataTable instanceof DataTable) {
				DataTable dt = (DataTable) dataTable;
				if (dt.getSemanticsManager().areAttributesComparable(attributes, null)) {
					methodId = "parallel_bars"; //parallel bars
				} else {
					methodId = "radial_bars";
				}
			}
		}
		if (methodId == null)
			return null;
		if (!isMethodAvailable(methodId))
			return null;
		return methodId;
	}

	/**
	* Some visualization methods can be externally switched off. Through this
	* method settings for the methods (ON or OFF) are passed to the DataMapper.
	*/
	public void setMethodSwitch(Parameters param) {
		methodSwitch = param;
	}

	protected boolean isOff(String methodId) {
		if (methodSwitch == null)
			return false;
		return methodSwitch.checkParameterValue(methodId, "OFF");
	}

	/**
	* Returns a vector of identifiers of available methods
	*/
	protected Vector getAvailableMethodList() {
		if (availableMethods == null) {
			availableMethods = new Vector(20, 5);
			for (int i = 0; i < mimpl.length; i++)
				if (mimpl[i][2] != null && //the name of the class implementing the method
						!isOff(mimpl[i][0])) { //the method is not switched off in settings
					try {
						Class.forName(mimpl[i][2]);
						availableMethods.addElement(mimpl[i][0]);
						System.out.println("available: " + mimpl[i][0] + ", class = " + mimpl[i][2]);
					} catch (Exception e) {
					}
				}
			availableMethods.trimToSize();
		}
		return availableMethods;
	}

	/**
	 * Returns a vector of identifiers of available methods suitable for the given
	 * type of objects
	 */
	public Vector getAvailableMethodList(char objType) {
		if (availableMethods == null) {
			getAvailableMethodList();
		}
		if (availableMethods == null)
			return null;
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
		if (methodList == null)
			return availableMethods;
		Vector methods = new Vector(methodList.length, 1);
		for (String[] element : methodList)
			if (availableMethods.contains(element[0])) {
				methods.addElement(element[0]);
			}
		return methods;
	}

	/**
	* Replies whether the method with the given identifier (see visMethods)
	* is implemented or available in the system.
	*/
	public boolean isMethodAvailable(String methodId) {
		if (methodId == null)
			return false;
		if (availableMethods == null) {
			getAvailableMethodList();
		}
		if (availableMethods == null)
			return false;
		return availableMethods.contains(methodId);
	}

	/**
	* Returns the number of available methods
	*/
	public int getAvailableMethodCount() {
		if (availableMethods == null) {
			getAvailableMethodList();
		}
		return availableMethods.size();
	}

	/**
	 * Returns the number of available methods suitable for the given
	 * type of objects
	 */
	public int getAvailableMethodCount(char objType) {
		Vector methods = getAvailableMethodList(objType);
		if (methods == null)
			return 0;
		return methods.size();
	}

	/**
	* Returns the identifier of the AVAILABLE method with the given index in the
	* list of ALL available methods (irrespective of the object type)
	*/
	protected String getAvailableMethodId(int methodN) {
		if (methodN < 0 || methodN >= getAvailableMethodCount())
			return null;
		return (String) availableMethods.elementAt(methodN);
	}

	/**
	* Replies if the method with the given identifier is applicable
	* to the given data (attributes and values contained in the table).
	* Generates an error message if not applicable.
	* Calls isMethodApplicable(methodId,dataTable,attributes,objType,true),
	* i.e. allows construction of a visualizer to check applicability.
	*/
	public boolean isMethodApplicable(String methodId, AttributeDataPortion dataTable, Vector attributes, char objType) {
		return isMethodApplicable(methodId, dataTable, attributes, objType, true);
	}

	/**
	* Replies if the method with the given identifier is applicable
	* to the given data (attributes and values contained in the table).
	* Generates an error message if not applicable.
	* The last argument specifies if the method should construct the visualizer
	* to check the applicability.
	*/
	protected boolean isMethodApplicable(String methodId, AttributeDataPortion dataTable, Vector attributes, char objType, boolean mayConstructVisualizer) {
		err = null;
		if (!isMethodAvailable(methodId)) {
			// following string: methodId+": the method is not available!"
			err = methodId + res.getString("_the_method_is_not");
			return false;
		}
		/*
		if (objType!=Geometry.area && objType!=Geometry.point) {
		  // following string: "Unsupported type of geographical objects!"
		  err=res.getString("Unsupported_type_of"); return false;
		}
		*/
		if (dataTable == null) {
			// following string: "No data source provided!"
			err = res.getString("No_data_source");
			return false;
		}
		if (!(dataTable instanceof ThematicDataSupplier)) {
			// following string: "No ObjectDataSupplier available!"
			err = res.getString("No_ObjectDataSupplier");
			return false;
		}
		if (!dataTable.hasData()) {
			// following string: "No data in the data source!"
			err = res.getString("No_data_in_the_data");
			return false;
		}
		if (attributes == null || attributes.size() < 1) {
			// following string: "No attributes specified!"
			err = res.getString("No_attributes");
			return false;
		}
		for (int i = 0; i < attributes.size(); i++)
			if (dataTable.getAttribute((String) attributes.elementAt(i)) == null) {
				// following string: "The attribute "+attributes.elementAt(i)+" is not found in the data source!"
				err = res.getString("The_attribute") + attributes.elementAt(i) + res.getString("is_not_found_in_the");
				return false;
			}
		if (!mayConstructVisualizer)
			return true;
		Object vis = constructVisualizer(methodId, objType);
		if (vis == null)
			return false; //err is already set
		if (vis instanceof DataPresenter) {
			DataPresenter dpr = (DataPresenter) vis;
			dpr.setDataSource(dataTable);
			//check attribute numbers and types
			boolean applicable = dpr.isApplicable(attributes);
			dpr.destroy();
			if (!applicable) {
				err = dpr.getErrorMessage();
				return false;
			}
		} else if (vis instanceof TableClassifier) {
			int nattr = attributes.size();
			char types[] = new char[nattr];
			for (int i = 0; i < nattr; i++) {
				types[i] = dataTable.getAttributeType((String) attributes.elementAt(i));
			}
			TableClassifier tc = (TableClassifier) vis;
			tc.setAttributes(attributes); //hdz
			tc.setTable(dataTable); //hdz
			boolean appl = tc.isApplicable(nattr, types);
			tc.destroy();
			if (!appl) {
				// following string: "The attribute is not applicable for "+methodId
				if (nattr == 1) {
					err = res.getString("The_attribute_is_not") + methodId;
				} else {
					err = res.getString("The_attributes_are") + methodId;
				}
				return false;
			}
		}
		return true;
	}

	/**
	* Replies if the available method with the given number is applicable
	* to the given data.
	* Generates an error message if not applicable.
	*/
	protected boolean isAvailableMethodApplicable(int methodN, AttributeDataPortion dataTable, Vector attributes, char objType) {
		return isMethodApplicable(getAvailableMethodId(methodN), dataTable, attributes, objType);
	}

	/**
	* Tries to visualise the given attributes from the given data portion
	* on the map. Assumes that the data portion has been already linked to
	* the active layer. Returns true on success. If visualisation fails, the error
	* message explains the reason.
	*/
	@Override
	public Visualizer visualizeAttributes(String methodId, AttributeDataPortion dataTable, Vector attributes, char objType) {
		err = null;
		// check applicability
		if (!isMethodApplicable(methodId, dataTable, attributes, objType, false))
			return null;
		// construct visualizer
		Object v = constructVisualizer(methodId, objType);
		if (v == null)
			return null;
		return visualizeAttributes(v, methodId, dataTable, attributes, objType);
	}

	/**
	* Generates a map manipulation component, depending on the visualization method.
	*/
	@Override
	public Component getMapManipulator(String methodId, Visualizer vis, Supervisor sup, AttributeDataPortion dataTable) {
		if (methodId == null || vis == null)
			return null;
		int midx = getMethodIndex(methodId), tvmidx = -1;
		if (midx < 0) {
			tvmidx = getTimeVisMethodIndex(methodId);
		}

		Component signMan = null;
		if (vis instanceof SignDrawer) { //produce a controller of selective sign drawing
			//check if the visualizer does not represent point objects by colored
			//circles - in this case selective drawing makes no sense
			boolean paintsPoints = (vis instanceof ClassSignDrawer) || (midx >= 0 && vis.getClass().getName().equals(mimpl[midx][3])) || (tvmidx >= 0 && vis.getClass().getName().equals(timeVisMethods[tvmidx][3]));
			Component selDraw = null;
			if (!paintsPoints) {
				try {
					Manipulator sdc = (Manipulator) Class.forName("spade.analysis.manipulation.SelectiveDrawingControlPanel").newInstance();
					if (sdc != null && (sdc instanceof Component) && sdc.construct(sup, vis, dataTable)) {
						selDraw = (Component) sdc;
					}
				} catch (Exception e) {
				}
			}
			SetupDiagramsButton b = new SetupDiagramsButton((SignDrawer) vis);
			Panel p = new Panel(new FlowLayout(FlowLayout.CENTER));
			p.add(b);
			if (selDraw == null) {
				signMan = p;
			} else {
				Panel pp = new Panel(new ColumnLayout());
				pp.add(selDraw);
				pp.add(p);
				signMan = pp;
			}
		}
		Component manComp = signMan;
		String className = (midx >= 0) ? mimpl[midx][4] : //the name of the class implementing the manipulator
				(tvmidx >= 0) ? timeVisMethods[tvmidx][4] : null;
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
				if (signMan != null) {
					Panel p = new Panel(new BorderLayout());
					p.add(manComp, "Center");
					p.add(signMan, "South");
					manComp = p;
				}
			}
		}
		TransformerOwner trow = null;
		if (vis instanceof TransformerOwner) {
			trow = (TransformerOwner) vis;
		} else if (vis instanceof ClassDrawer) {
			ClassDrawer cld = (ClassDrawer) vis;
			if (cld.getClassifier() instanceof TransformerOwner) {
				trow = (TransformerOwner) cld.getClassifier();
			}
		}
		Component transUI = null;
		if (trow != null) {
			AttributeTransformer aTrans = trow.getAttributeTransformer();
			if (aTrans != null) {
				transUI = aTrans.getUI();
			}
			if (transUI != null) {
				try {
					Object obj = Class.forName("spade.analysis.transform.TransformedDataSaverImplement").newInstance();
					if (obj != null && (obj instanceof TransformedDataSaver)) {
						TransformedDataSaver trsav = (TransformedDataSaver) obj;
						if (trsav.setup(trow.getAttributeTransformer())) {
							Component c = trsav.getUI();
							if (c != null)
								if ((transUI instanceof Container) && (((Container) transUI).getLayout() instanceof ColumnLayout)) {
									((Container) transUI).add(new Line(false));
									((Container) transUI).add(c);
								} else {
									Panel p = new Panel(new ColumnLayout());
									p.add(transUI);
									p.add(new Line(false));
									p.add(c);
									transUI = p;
								}
						}
					}
				} catch (Exception e) {
				}
			}
		}
		if (transUI != null)
			if (manComp == null) {
				manComp = transUI;
			} else {
				Panel p = new Panel(new ColumnLayout());
				FoldablePanel fp = new FoldablePanel(transUI, new Label(res.getString("Data_transformation")));
				p.add(fp);
				p.add(new Line(false));
				Panel pp = new Panel(new BorderLayout());
				pp.add(p, BorderLayout.NORTH);
				pp.add(manComp, BorderLayout.CENTER);
				manComp = pp;
			}
		return manComp;
	}

}
