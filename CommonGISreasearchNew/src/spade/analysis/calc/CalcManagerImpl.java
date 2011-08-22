package spade.analysis.calc;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DisplayProducer;
import spade.analysis.system.Supervisor;
import spade.analysis.transform.TransformerGenerator;
import spade.lib.basicwin.CManager;
import spade.lib.help.Helper;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.TickThread;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.geometry.Geometry;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.DataMapper;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.spec.ToolSpec;

public class CalcManagerImpl implements CalcManager, WindowListener, PropertyChangeListener {
	static public ResourceBundle res = Language.getTextResource("spade.analysis.calc.Res");
	/**
	* Identifiers of all known calculation methods. Some of them may be not
	* available in the current system configuration.
	*/
	public static final int RemoveOutliers = 0, CalculateSum = 1, Normalization = 2, CalculateChange = 3, CalculateFormula = 4, CalculateAverage = 10, CalculateVariance = 11, CalculatePercentiles = 12, CountValues = 13,
			CalculateWeightedAverage = 14, CalculateIdealPoint = 20, ValuesToOrder = 21, CalculateOWA = 22, CalculateParetoSet = 23, CalculateWLC = 24, CalculateMCC = 25, CalculateSimilarity = 30, CalculateSimilarityClass = 31,
			CalculateDominance = 32, RuleValidation = 33;
	public static final int methodIds[] = { RemoveOutliers, CalculateSum, Normalization, CalculateChange, CalculateFormula, CalculateAverage, CalculateVariance, CalculatePercentiles, CountValues, CalculateWeightedAverage, CalculateIdealPoint,
			CalculateWLC, CalculateOWA, CalculateParetoSet, CalculateMCC, ValuesToOrder, CalculateSimilarity, CalculateSimilarityClass, CalculateDominance, RuleValidation };
	public static final String methodCharIds[] = { "remove_outliers", "calc_sum", "normalize", "calc_change", "calc_formula", "calc_average", "calc_variance", "calc_percentiles", "count_values", "calc_weighted_average", "rank", "ranc_WLC", "owa",
			"calc_ParetoSet", "calc_MCC", "values_to_order", "similarity", "similarity_class", "calc_dominance", "rule_validation" };
	/**
	* The array of explanations about each of the methods available. Constructed
	* when the methods are checked for availability.
	*/
	protected String expl[] = null;
	/**
	* The array of prompts for attribute selection for each of the methods available.
	* Constructed when the methods are checked for availability.
	*/
	protected String prompts[] = null;
	/**
	* The array of minimum attribute numbers for each of the calculation methods
	*/
	protected int minAttrN[] = null;
	/**
	* The array of maximum attribute numbers for each of the calculation methods
	*/
	protected int maxAttrN[] = null;
	/**
	* The supervisor is used for dynamic linking between displays
	*/
	protected Supervisor supervisor = null;
	/**
	* The display producer is used for visual representation of data on maps
	* and other graphics
	*/
	protected DisplayProducer displayProducer = null;
	/**
	* References to the calculation dialogs started
	*/
	protected Vector dialogs = null;
	/**
	* Error message
	*/
	protected String err = null;
	/**
	* Identifiers of methods that are available in the current system configuration
	*/
	protected IntArray mAvail = null;

	/**
	* Returns the name of the method with the given identifier
	*/
	@Override
	public String getMethodName(int methodId) {
		switch (methodId) {
		case RemoveOutliers:
			return "Remove outliers and/or fill missing values";
		case CalculateSum:
			return res.getString("Sum_of_columns");
		case Normalization:
			return res.getString("Percentages_and");
			//case NormalizationCHCC: return res.getString("Percentages_and");
		case CalculateChange:
			return res.getString("Change_difference");
		case CalculateFormula:
			return res.getString("Arbitrary_formula");
		case CalculateAverage:
			return res.getString("Average_among_columns");
		case CalculateVariance:
			return res.getString("Variance_among");
		case CalculatePercentiles:
			return res.getString("Percentiles");
		case CountValues:
			return res.getString("Count_value");
		case CalculateWeightedAverage:
			return res.getString("Weighted_average");
		case RuleValidation:
			return res.getString("Rule_validation");
		case CalculateIdealPoint:
			return res.getString("Ideal_Point_Analysis");
		case ValuesToOrder:
			return res.getString("Values_Order");
		case CalculateOWA:
			return res.getString("Ordered_Weighted");
		case CalculateParetoSet:
			return "Pareto Set";
		case CalculateMCC:
			return "Multiple Criteria Comparison";
		case CalculateWLC:
			return "Weighted Linear Combination";
		case CalculateSimilarity:
			return res.getString("Similarity_distance_");
		case CalculateSimilarityClass:
			return res.getString("Similarity");
		case CalculateDominance:
			return res.getString("Dominant_attribute");
		}
		return null;
	}

	/**
	* Returns the index of the method with the given identifier in the array of
	* all methods.
	*/
	protected int getMethodIndex(int methodId) {
		for (int i = 0; i < methodIds.length; i++)
			if (methodIds[i] == methodId)
				return i;
		return -1;
	}

	/**
	* Returns an explanation about the method with the given identifier
	*/
	@Override
	public String getMethodExplanation(int methodId) {
		int idx = getMethodIndex(methodId);
		if (idx < 0)
			return null;
		if (expl == null) {
			getNAvailableMethods();
		}
		if (expl == null)
			return null;
		return expl[idx];
	}

	/**
	* Returns an attribute selection prompt about the method with the given identifier
	*/
	@Override
	public String getAttrSelectionPrompt(int methodId) {
		int idx = getMethodIndex(methodId);
		if (idx < 0)
			return null;
		if (prompts == null) {
			getNAvailableMethods();
		}
		if (prompts == null)
			return null;
		return prompts[idx];
	}

	/**
	* Returns the minimum number of attributes needed for the method with
	* the given identifier
	*/
	@Override
	public int getMinAttrNumber(int methodId) {
		int idx = getMethodIndex(methodId);
		if (idx < 0)
			return 1;
		if (minAttrN == null) {
			getNAvailableMethods();
		}
		if (minAttrN == null)
			return 1;
		return minAttrN[idx];
	}

	/**
	* Returns the maximum number of attributes needed for the method with
	* the given identifier. If the maximum number is unlimited, returns -1.
	*/
	@Override
	public int getMaxAttrNumber(int methodId) {
		int idx = getMethodIndex(methodId);
		if (idx < 0)
			return -1;
		if (maxAttrN == null) {
			getNAvailableMethods();
		}
		if (maxAttrN == null)
			return -1;
		return maxAttrN[idx];
	}

	/**
	* Returns the Class corresponding to the method with the given identifier
	*/
	protected Class getClassForMethod(int methodId) {
		try {
			switch (methodId) {
			case RemoveOutliers:
				return Class.forName("spade.analysis.calc.OutlierRemover");
			case CalculateSum:
				return Class.forName("spade.analysis.calc.Summator");
			case Normalization:
				return Class.forName("spade.analysis.calc.Normalizer");
				//case NormalizationCHCC: return Class.forName("spade.analysis.calc.NormalizerCHCC");
			case CalculateChange:
				return Class.forName("spade.analysis.calc.ChangeCalculator");
			case CalculateFormula:
				return Class.forName("spade.analysis.calc.FormulaCalculator");
			case CalculateAverage:
				return Class.forName("spade.analysis.calc.AverageCalculator");
			case CalculateVariance:
				return Class.forName("spade.analysis.calc.VarianceCalculator");
			case CalculatePercentiles:
				return Class.forName("spade.analysis.calc.PercentileCalculator");
			case CountValues:
				return Class.forName("spade.analysis.calc.ValueCounter");
			case CalculateWeightedAverage:
				return Class.forName("spade.analysis.calc.WeightedAverageCalc");
			case RuleValidation:
				return Class.forName("spade.analysis.calc.RuleValidator");
			case CalculateIdealPoint:
				return Class.forName("spade.analysis.calc.IdealPointCalc");
			case CalculateParetoSet:
				return Class.forName("spade.analysis.calc.ParetoSetCalc");
			case CalculateMCC:
				return Class.forName("spade.analysis.calc.MultiCriteriaComparison");
			case ValuesToOrder:
				return Class.forName("spade.analysis.calc.OrdererByValues");
			case CalculateOWA:
				return Class.forName("spade.analysis.calc.OWACalcDlg");
			case CalculateWLC:
				return Class.forName("spade.analysis.calc.WeightedLinearCombinationCalc");
			case CalculateSimilarity:
				return Class.forName("spade.analysis.calc.SimilarityCalc");
			case CalculateSimilarityClass:
				return Class.forName("spade.analysis.calc.SimilarityClassCalc");
			case CalculateDominance:
				return Class.forName("spade.analysis.calc.DominanceCalc");
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	* Returns the name of the group for the method with the given index,
	* e.g. "statistics" or "decision support"
	*/
	@Override
	public String getMethodGroupName(int methodId) {
		switch (methodId / 10) {
		// following text: "Arithmetics"
		case 0:
			return (res.getString("Arith"));
			// following text: "Statistics"
		case 1:
			return res.getString("Stat");
			// following text: "Decision support"
		case 2:
			return res.getString("Decis");
			// following text: "Multidimensional analysis"
		case 3:
			return res.getString("Mult");
		}
		return null;
	}

	/**
	* Checks whether the method with the given index is applicable to the specified
	* attributes. The vector attrDescr contains descriptors of the source
	* attributes for calculations. A descriptor contains a reference to an
	* attribute and, possibly, a list of selected values of relevant parameters.
	* The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* fn is an array of column numbers.
	*/
	@Override
	public boolean isApplicable(int methodId, AttributeDataPortion dTable, int fn[], Vector attrDescr) {
		if (fn == null || fn.length == 0)
			return false;
		if (methodId != CountValues) {
			for (int i = 0; i < fn.length; i++)
				if (fn[i] < 0 || !dTable.isAttributeNumeric(fn[i]))
					return false;
		}
		if (methodId == CalculateChange) {
			if (attrDescr != null && attrDescr.size() == 2)
				return true;
			return fn.length == 2;
		}
		if (methodId == RuleValidation)
			return fn.length >= 2;
		return true;
	}

	/**
	* Checks whether the method with the given index is applicable to the specified
	* attributes. The vector attrDescr contains descriptors of the source
	* attributes for calculations. A descriptor contains a reference to an
	* attribute and, possibly, a list of selected values of relevant parameters.
	* The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* Vector attr contains low-level attribute identifiers, i.e. attributes
	* directly corresponding to table columns (1:1). The vector attrDescr may
	* contain parents of these attributes; hence, its size may be less than the
	* size of the vector attr.
	*/
	@Override
	public boolean isApplicable(int methodId, AttributeDataPortion dTable, Vector attr, Vector attrDescr) {
		return isApplicable(methodId, dTable, dTable.getAttrIndices(attr), attrDescr);
	}

	/**
	* Sets the supervisor used for dynamic linking between displays
	*/
	@Override
	public void setSupervisor(Supervisor sup) {
		supervisor = sup;
	}

	/**
	* Sets the display producer used for visual representation of data on maps
	* and other graphics
	*/
	@Override
	public void setDisplayProducer(DisplayProducer dprod) {
		displayProducer = dprod;
	}

	/**
	* Returns the number of methods that are available in the current system
	* configuration
	*/
	@Override
	public int getNAvailableMethods() {
		if (mAvail == null) { //find out which classes are available
			mAvail = new IntArray(methodIds.length, 5);
			if (expl == null) {
				expl = new String[methodIds.length];
			}
			if (prompts == null) {
				prompts = new String[methodIds.length];
			}
			if (minAttrN == null) {
				minAttrN = new int[methodIds.length];
			}
			if (maxAttrN == null) {
				maxAttrN = new int[methodIds.length];
			}
			for (int i = 0; i < methodIds.length; i++) {
				expl[i] = null;
				prompts[i] = null;
				Class cl = getClassForMethod(methodIds[i]);
				if (cl != null) {
					mAvail.addElement(methodIds[i]);
					try {
						Calculator calc = (Calculator) cl.newInstance();
						expl[i] = calc.getExplanation();
						prompts[i] = calc.getAttributeSelectionPrompt();
						minAttrN[i] = calc.getMinAttrNumber();
						maxAttrN[i] = calc.getMaxAttrNumber();
					} catch (Exception e) {
					}
				}
			}
		}
		return mAvail.size();
	}

	/**
	* Checks if the specified calculation method is available
	*/
	@Override
	public boolean isMethodAvailable(int methodId) {
		for (int i = 0; i < getNAvailableMethods(); i++)
			if (mAvail.elementAt(i) == methodId)
				return true;
		return false;
	}

	/**
	* Returns the index of an available method by its index in the list of
	* available methods
	*/
	@Override
	public int getAvailableMethodId(int idx) {
		if (idx < 0 || idx >= getNAvailableMethods())
			return -1;
		return mAvail.elementAt(idx);
	}

	/**
	* Produces an instance of the class performing the specified kind of
	* calculation
	*/
	protected Calculator constructCalculator(int methodId, DataTable dTable, int fn[], Vector attrDescr) {
		if (!isMethodAvailable(methodId))
			return null;
		Class cl = getClassForMethod(methodId);
		if (cl == null) {
			// following text: "Cannot construct a calculator for the method "
			err = res.getString("Cannot1") + getMethodName(methodId);
			return null;
		}
		Calculator calc = null;
		try {
			calc = (Calculator) cl.newInstance();
		} catch (Exception e) {
			// following text: "Cannot construct a calculator: "
			err = res.getString("Cannot2") + e.toString();
		}
		if (calc == null)
			return null;
		calc.setTable(dTable);
		calc.setAttrNumbers(fn);
		calc.setAttrDescriptors(attrDescr);
		return calc;
	}

	/**
	* Applies the specified calculation method to the given data. The method is
	* specified by its identifier (not the index!!!). The vector attrDescr contains
	* descriptors of the source attributes selected for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected values
	* of relevant parameters. The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* fn is an array of column numbers.
	*/
	@Override
	public Object applyCalcMethod(int methodId, DataTable dTable, int fn[], Vector attrDescr, String layerId) {
		err = null;
		if (methodId < 0 || getMethodName(methodId) == null) {
			// following text: "Wrong method identifier: "
			err = res.getString("Wrong") + methodId;
			return null;
		}
		if (!isMethodAvailable(methodId)) {
			// following text: "The method "+methodId+" is not available!"
			err = res.getString("The_method") + methodId + res.getString("isnotav");
			return null;
		}
		if (!isApplicable(methodId, dTable, fn, attrDescr)) {
			// following text: "The method "+getMethodName(methodId)+" is not applicable to the selected attributes!"
			err = res.getString("The_method") + getMethodName(methodId) + res.getString("isnotap");
			return null;
		}
		Calculator calc = constructCalculator(methodId, dTable, fn, attrDescr);
		if (calc == null)
			return null;
		if (!(calc instanceof CalcDlg)) {
			if (fn.length > 10 || dTable.getDataItemCount() > 100) {
				supervisor.getUI().showMessage(res.getString("calc_in_progress"));
				CalcThread thr = new CalcThread(calc, this);
				thr.methodId = methodId;
				thr.attrDescr = attrDescr;
				thr.layerId = layerId;
				thr.start();
				TickThread tick = new TickThread(thr, this, 5000L);
				tick.start();
				return calc;
			}
			Vector resultAttrs = calc.doCalculation();
			if (resultAttrs == null && (calc.doesCreateNewAttributes() || calc.getErrorMessage() != null)) {
				if (calc.getErrorMessage() != null) {
					err = calc.getErrorMessage();
				} else {
					err = res.getString("calcerr");
				}
				return null;
			}
			supervisor.getUI().showMessage(res.getString("calc_finished"));
			if (calc.doesCreateNewAttributes()) {
				AttrNameEditor.attrAddedToTable(dTable, resultAttrs);
				tryShowOnMap(dTable, resultAttrs, attrDescr, methodId, layerId);
			} else {
				Vector attr = new Vector(fn.length, 1);
				for (int element : fn) {
					attr.addElement(dTable.getAttributeId(element));
				}
				dTable.notifyPropertyChange("values", null, attr);
			}
			return calc;
		} else {
			CalcDlg dlg = (CalcDlg) calc;
			dlg.setSupervisor(supervisor);
			dlg.setDisplayProducer(displayProducer);
			dlg.setGeoLayerId(layerId);
			dlg.addWindowListener(this);
			if (dialogs == null) {
				dialogs = new Vector(10, 10);
			}
			dialogs.addElement(dlg);
			supervisor.getWindowManager().registerWindow(dlg);
			dlg.doCalculation();
		}
		return calc;
	}

	/**
	* Tries to represent calculation results on a map
	*/
	protected void tryShowOnMap(AttributeDataPortion dTable, Vector resultAttrs, Vector attrDescr, int calcMethodId, String layerId) {
		if (dTable == null || resultAttrs == null || layerId == null)
			return;
		if (resultAttrs.size() > 10)
			return;
		if (supervisor == null || supervisor.getUI() == null)
			return;
		MapViewer mapView = supervisor.getUI().getCurrentMapViewer();
		if (mapView == null || mapView.getLayerManager() == null)
			return;
		int idx = mapView.getLayerManager().getIndexOfLayer(layerId);
		if (idx < 0)
			return;
		DataMapper dataMapper = null;
		if (displayProducer.getDataMapper() != null && (displayProducer.getDataMapper() instanceof DataMapper)) {
			dataMapper = (DataMapper) displayProducer.getDataMapper();
		}
		if (dataMapper == null)
			return;
		String methodId = null;
		GeoLayer themLayer = mapView.getLayerManager().getGeoLayer(idx);
		if (resultAttrs.size() > 1 && (calcMethodId == Normalization || (attrDescr != null && attrDescr.size() == 1))) {
			methodId = "parallel_bars";
		} else {
			methodId = dataMapper.getDefaultMethodId(dTable, resultAttrs, themLayer.getType());
		}
		Object vis = dataMapper.constructVisualizer(methodId, themLayer.getType());
		if (vis == null) {
			supervisor.getUI().showMessage(dataMapper.getErrorMessage(), true);
			return;
		}
		TransformerGenerator.makeTransformerChain(vis, dTable, resultAttrs);
		Visualizer lvis = themLayer.getVisualizer(), lbvis = themLayer.getBackgroundVisualizer();
		boolean layerHasVisualizer = lvis != null || lbvis != null;
		if (layerHasVisualizer && themLayer.getType() == Geometry.area && (lvis == null || lbvis == null))
			if ((vis instanceof Visualizer) && ((Visualizer) vis).isDiagramPresentation()) {
				layerHasVisualizer = lvis != null && lvis.isDiagramPresentation();
			} else {
				layerHasVisualizer = lvis == null || !lvis.isDiagramPresentation();
			}
		boolean useMainView = !layerHasVisualizer || !supervisor.getUI().getUseNewMapForNewVis();
		MapViewer mw = supervisor.getUI().getMapViewer((useMainView) ? "main" : "_blank_");
		if (mw != mapView && mw.getLayerManager() != null) {
			//find the copy of the geographical layer in the new map view
			int lidx = mw.getLayerManager().getIndexOfLayer(layerId);
			if (lidx >= 0) {
				mapView = mw;
				themLayer = mapView.getLayerManager().getGeoLayer(lidx);
				useMainView = false;
			}
		} else {
			useMainView = true;
		}
		Visualizer visualizer = displayProducer.displayOnMap(vis, methodId, dTable, resultAttrs, themLayer, mapView);
		if (!useMainView && (mapView instanceof Component)) {
			Component c = (Component) mapView;
			c.setName(c.getName() + ": " + visualizer.getVisualizationName());
			Frame win = CManager.getFrame(c);
			if (win != null) {
				win.setName(c.getName());
				win.setTitle(c.getName());
			}
		}
		supervisor.registerTool(visualizer);
		mapView.getLayerManager().activateLayer(layerId);
	}

	/**
	* When calculations are performed in a separate thread, this method is used
	* for notifying the CalcManagerImpl about the calculations being finished.
	*/
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("calc_finished") && (e.getSource() instanceof CalcThread)) {
			CalcThread thr = (CalcThread) e.getSource();
			Calculator calc = thr.getCalculator();
			Vector resultAttrs = thr.getResultingAttributes();
			if (resultAttrs == null && (calc.doesCreateNewAttributes() || calc.getErrorMessage() != null)) {
				if (calc.getErrorMessage() != null) {
					err = calc.getErrorMessage();
				} else {
					err = res.getString("calcerr");
				}
				return;
			}
			supervisor.getUI().showMessage(res.getString("calc_finished"));
			if (calc.doesCreateNewAttributes()) {
				AttrNameEditor.attrAddedToTable(calc.getTable(), resultAttrs);
				tryShowOnMap(calc.getTable(), resultAttrs, thr.attrDescr, thr.methodId, thr.layerId);
			} else {
				DataTable dTable = calc.getTable();
				int fn[] = calc.getAttrNumbers();
				if (fn != null && fn.length > 0) {
					Vector attr = new Vector(fn.length, 1);
					for (int element : fn) {
						attr.addElement(dTable.getAttributeId(element));
					}
					dTable.notifyPropertyChange("values", null, attr);
				}
			}
		} else if (e.getPropertyName().equals("tick")) {
			if (e.getNewValue() != null && (e.getNewValue() instanceof Long)) {
				long t = ((Long) e.getNewValue()).longValue() / 1000;
				supervisor.getUI().showMessage(res.getString("calc_in_progress") + " - " + t + " sec elapsed");
			}
		}
	}

	/**
	* Applies the specified calculation method to the given data. The method is
	* specified fy its identifier (not the index!!!). The vector attrDescr contains
	* descriptors of the source attributes selected for calculations. A descriptor
	* contains a reference to an attribute and, possibly, a list of selected values
	* of relevant parameters. The elements of the vector are instances of the class
	* spade.vis.database.AttrDescriptor.
	* Vector attr contains low-level attribute identifiers, i.e. attributes
	* directly corresponding to table columns (1:1). The vector attrDescr may
	* contain parents of these attributes; hence, its size may be less than the
	* size of the vector attr.
	*/
	@Override
	public Object applyCalcMethod(int methodId, DataTable dTable, Vector attr, Vector attrDescr, String layerId) {
		return applyCalcMethod(methodId, dTable, dTable.getAttrIndices(attr), attrDescr, layerId);
	}

	protected void closeDialog(CalcDlg dlg, boolean windowClosed) {
		if (dlg != null) {
			if (!windowClosed) {
				dlg.dispose();
			}
			CManager.destroyComponent(dlg);
			if (dialogs != null) {
				int idx = dialogs.indexOf(dlg);
				if (idx >= 0) {
					dialogs.removeElementAt(idx);
				}
			}
		}
	}

	public void closeAllDialogs() {
		if (dialogs == null)
			return;
		for (int i = 0; i < dialogs.size(); i++) {
			CalcDlg dia = (CalcDlg) dialogs.elementAt(i);
			dia.dispose();
			CManager.destroyComponent(dia);
		}
		dialogs.removeAllElements();
	}

	/**
	* When a table is removed, the CalcManager closes all calculation dialogs that
	* are linked to this table. The table is specified by its identifier.
	*/
	public void tableIsRemoved(String tableId) {
		if (dialogs == null)
			return;
		for (int i = dialogs.size() - 1; i >= 0; i--) {
			CalcDlg dia = (CalcDlg) dialogs.elementAt(i);
			if (dia.isLinkedToDataSet(tableId)) {
				dia.dispose();
				CManager.destroyComponent(dia);
				dialogs.removeElementAt(i);
			}
		}
	}

//----------------- ToolManager interface --------------------------------

	/**
	* For the given string tool identifier (such as "similarity",
	* "similarity_class","rank","evaluate") returns the internal number of the tool
	*/
	protected int getToolNumber(String toolId) {
		if (toolId == null)
			return -1;
		for (int i = 0; i < methodCharIds.length; i++)
			if (toolId.equalsIgnoreCase(methodCharIds[i]))
				return methodIds[i];
		return -1;
	}

	/**
	* Replies whether the specified analysis tool is available. The tool
	* should be one of those listed in the array of tools above (methodCharIds).
	*/
	public boolean isToolAvailable(String toolId) {
		int methodId = getToolNumber(toolId);
		if (methodId < 0)
			return false;
		return isMethodAvailable(methodId);
	}

	/**
	* Replies whether help about the specified tool is available in the system
	*/
	public boolean canHelpWithTool(String toolId) {
		return Helper.canHelp(toolId);
	}

	/**
	* Displays help about the specified tool
	*/
	public void helpWithTool(String toolId) {
		Helper.help(toolId);
	}

	/**
	* Checks whether the specified analysis tool is applicable to the
	* given table and the given attributes
	*/
	public boolean isToolApplicable(String toolId, AttributeDataPortion table, Vector attrs) {
		int methodId = getToolNumber(toolId);
		if (methodId < 0)
			return false;
		if (table == null || !(table instanceof DataTable))
			return false;
		return isApplicable(methodId, (DataTable) table, attrs, null);
	}

	/**
	* Constructs and applies the tool according to the given specification.
	*/
	public Object applyTool(ToolSpec spec, AttributeDataPortion table) {
		return applyTool(spec, table, null);
	}

	/**
	* Constructs and applies the tool according to the given specification.
	*/
	public Object applyTool(ToolSpec spec, AttributeDataPortion table, String geoLayerId) {
		if (spec == null)
			return null;
		return applyTool(spec.methodId, table, spec.attributes, geoLayerId, spec.properties);
	}

	/**
	* Applies the specified analysis tool to the given table and the given
	* attributes. Returns a reference to the tool constructed or null if failed.
	* The argument properties may specify individual properties for the
	* tool to be constructed.
	*/
	public Object applyTool(String toolId, AttributeDataPortion table, Vector attrs, Hashtable properties) {
		return applyTool(toolId, table, attrs, null, properties);
	}

	/**
	* Applies the specified analysis tool to the given table and the given
	* attributes. Returns a reference to the tool constructed or null if failed.
	* The argument properties may specify individual properties for the
	* tool to be constructed.
	*/
	public Object applyTool(String toolId, AttributeDataPortion table, Vector attrs, String geoLayerId, Hashtable properties) {
		err = null;
		int methodId = getToolNumber(toolId);
		if (methodId < 0) {
			// following text: "Unknown calculation method: "+toolId
			err = res.getString("unkncalcm") + toolId;
			return null;
		}
		if (!isMethodAvailable(methodId)) {
			// following text: "The method "+toolId+" is not found among the available methods"
			err = res.getString("The_method") + toolId + res.getString("isnotf");
			return null;
		}
		if (table == null || !(table instanceof DataTable)) {
			// following text: "Inappropriate data source; DataTable required!"
			err = res.getString("inappds");
			return null;
		}
		return applyCalcMethod(methodId, (DataTable) table, table.getAttrIndices(attrs), null, geoLayerId);
	}

	/**
	* If failed to apply the requested tool, the error message explains the
	* reason
	*/
	public String getErrorMessage() {
		return err;
	}

	/**
	* Closes all tools that are currently open
	*/
	public void closeAllTools() {
		closeAllDialogs();
	}

//----------------- WindowListener interface -----------------------------

	public void windowClosing(WindowEvent e) {
		if (e.getSource() instanceof CalcDlg) {
			closeDialog((CalcDlg) e.getSource(), false);
		}
	}

	public void windowClosed(WindowEvent e) {
		if (e.getSource() instanceof CalcDlg) {
			closeDialog((CalcDlg) e.getSource(), true);
		}
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}
}
