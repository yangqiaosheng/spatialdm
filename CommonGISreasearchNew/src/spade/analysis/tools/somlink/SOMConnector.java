package spade.analysis.tools.somlink;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Vector;

import spade.analysis.classification.NumAttr1Classifier;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.DisplayProducer;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.analysis.tools.BaseAnalyser;
import spade.analysis.tools.SingleInstanceTool;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.SelectDialog;
import spade.lib.basicwin.TextCanvas;
import spade.lib.color.ColorScale2D;
import spade.lib.color.ColorSelDialog;
import spade.lib.util.CopyFile;
import spade.lib.util.FloatArray;
import spade.lib.util.IdMaker;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.time.vis.DataVisAnimator;
import spade.time.vis.TimeArranger;
import spade.time.vis.VisAttrDescriptor;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.ObjectFilter;
import spade.vis.database.Parameter;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DLayerManager;
import spade.vis.dmap.MapCanvas;
import spade.vis.dmap.MapMetrics;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MosaicSign;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapContext;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.ClassDrawer;
import spade.vis.mapvis.ClassSignDrawer;
import spade.vis.mapvis.MultiClassPresenter;
import spade.vis.mapvis.Visualizer;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import ui.AttributeChooser;
import ui.MapBkgImageMaker;
import ui.TableManager;
import useSOM.RunIXSOM;
import useSOM.SOMCellInfo;
import useSOM.SOMResult;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Oct 23, 2009
 * Time: 12:03:51 PM
 * An interface to the iXsom implementation of SOM from the TU of Darmstadt
 */
public class SOMConnector extends BaseAnalyser implements SingleInstanceTool, PropertyChangeListener {
	protected String applPath = null, somInputDir = null;
	protected RunIXSOM runSOM = null;
	/**
	 * Describes the current application of SOM
	 */
	protected SOMApplInfo somAp = null;
	/**
	 * Descriptors of the previous applications of SOM
	 */
	protected Vector<SOMApplInfo> allSOMApplications = null;
	/**
	 * Used for producing a small image with map background
	 */
	protected MapBkgImageMaker imgMaker = null;
	protected DGeoLayer bkgLayer = null;
	protected MapCanvas smallMap = null;
	protected DGeoLayer themLayer = null;

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core) {
		if (runSOM != null) {
			showMessage("The SOM tool is currently running!", true);
			return;
		}
		if (imgMaker != null) {
			imgMaker.stopWork();
			imgMaker = null;
			bkgLayer = null;
			smallMap = null;
		}
		somAp = new SOMApplInfo();
		somAp.tblSOM = null;
		somAp.paramSOM = null;
		this.core = core;
		if (core == null || core.getUI() == null)
			return;
		DataTable table = selectTable(core);
		if (table == null)
			return;
		AttributeChooser attrSel = new AttributeChooser();
		Vector attributes = attrSel.selectTopLevelAttributes(table, null, null, true, "Select the attributes to use in the analysis:", core.getUI());
		if (attributes == null || attributes.size() < 1)
			return;
		//check if all attributes have children and depend on the same single parameter
		Parameter param = null;
		Attribute at = (Attribute) attributes.elementAt(0);
		if (at.hasChildren()) {
			Attribute child = at.getChild(0);
			if (child.getParameterCount() == 1) {
				param = table.getParameter(child.getParamName(0));
			}
		}
		if (param != null && attributes.size() > 1) {
			//check if all other attributes also depend on this parameter
			for (int i = 1; i < attributes.size() && param != null; i++) {
				at = (Attribute) attributes.elementAt(i);
				if (!at.hasChildren()) {
					param = null;
				} else {
					Attribute child = at.getChild(0);
					if (child.getParameterCount() != 1) {
						param = null;
					} else if (!child.getParamName(0).equals(param.getName())) {
						param = null;
					}
				}
			}
		}
		boolean applySOMtoParam = false;
		if (param != null && param.getValueCount() > 5) {
			SelectDialog selDia = new SelectDialog(getFrame(), "Apply SOM", "What would you like to group?");
			selDia.addOption("values of the parameter \"" + param.getName() + "\"", "param", true);
			selDia.addOption("rows of the table", "rows", false);
			selDia.show();
			if (selDia.wasCancelled())
				return;
			applySOMtoParam = selDia.getSelectedOptionN() == 0;
			showMessage("SOM will be applied to " + ((applySOMtoParam) ? "parameter values" : "table rows"), false);
		}
		//create the feature vectors for SOM
		String fvPath = makeFeaturesFilePath();
		if (fvPath == null)
			return;
		FileWriter fvFileWriter = null;
		try {
			fvFileWriter = new FileWriter(fvPath, false);
		} catch (IOException ioe) {
			showMessage("Failed to create file " + fvPath, true);
			System.out.println(ioe.toString());
			return;
		}
		if (fvFileWriter == null)
			return;
		BufferedWriter writer = new BufferedWriter(fvFileWriter);

		ObjectFilter tFilter = table.getObjectFilter();
		if (tFilter != null && !tFilter.areObjectsFiltered()) {
			tFilter = null;
		}

		boolean err = false;
		int nrows = 0;
		if (applySOMtoParam) {
			int colNs[][] = new int[attributes.size()][param.getValueCount()];
			for (int i = 0; i < attributes.size(); i++) {
				for (int j = 0; j < param.getValueCount(); j++) {
					colNs[i][j] = -1;
				}
				at = (Attribute) attributes.elementAt(i);
				for (int j = 0; j < at.getChildrenCount(); j++) {
					Attribute child = at.getChild(j);
					int aIdx = table.getAttrIndex(child.getIdentifier());
					if (aIdx < 0) {
						continue;
					}
					Object pval = child.getParamValue(param.getName());
					int pvIdx = param.getValueIndex(pval);
					if (pvIdx >= 0) {
						colNs[i][pvIdx] = aIdx;
					}
				}
			}
			try {
				writer.write("TimeID\tName\tObjectID");
				if (attributes.size() == 1) {
					for (int i = 0; i < table.getDataItemCount(); i++)
						if (tFilter == null || tFilter.isActive(i)) {
							writer.write("\t" + table.getDataItemName(i));
						} else {
							;
						}
				} else {
					for (int k = 0; k < attributes.size(); k++) {
						String aName = ((Attribute) attributes.elementAt(k)).getName();
						for (int i = 0; i < table.getDataItemCount(); i++)
							if (tFilter == null || tFilter.isActive(i)) {
								writer.write("\t" + aName + " (" + table.getDataItemName(i) + ")");
							}
					}
				}
				writer.write("\r\n");
				for (int j = 0; j < param.getValueCount(); j++) {
					writer.write("1\t" + param.getValue(j) + "\t" + String.valueOf(j + 1));
					for (int i = 0; i < table.getDataItemCount(); i++)
						if (tFilter == null || tFilter.isActive(i)) {
							for (int k = 0; k < attributes.size(); k++) {
								if (colNs[k][j] < 0) {
									writer.write("\t0");
								} else {
									String val = table.getAttrValueAsString(colNs[k][j], i);
									if (val == null) {
										val = "0";
									}
									writer.write("\t" + val);
								}
							}
						}
					writer.write("\r\n");
					++nrows;
					if (nrows % 100 == 0) {
						showMessage("Writing feature vectors to file: " + nrows + " rows stored", false);
					}
				}
			} catch (IOException e) {
				showMessage("File writing error: " + e.getMessage(), true);
				err = true;
			}
		} else {
			IntArray colNs = new IntArray(100, 100);
			for (int i = 0; i < attributes.size(); i++) {
				at = (Attribute) attributes.elementAt(i);
				if (!at.hasChildren()) {
					int idx = table.getAttrIndex(at.getIdentifier());
					if (idx >= 0) {
						colNs.addElement(idx);
					}
				} else {
					for (int j = 0; j < at.getChildrenCount(); j++) {
						Attribute child = at.getChild(j);
						int idx = table.getAttrIndex(child.getIdentifier());
						if (idx >= 0) {
							colNs.addElement(idx);
						}
					}
				}
			}

			showMessage("Writing feature vectors to file " + fvPath, false);
			try {
				writer.write("TimeID\tName\tObjectID");
				for (int j = 0; j < colNs.size(); j++) {
					writer.write("\t" + table.getAttributeName(colNs.elementAt(j)));
				}
				writer.write("\r\n");
				for (int i = 0; i < table.getDataItemCount(); i++)
					if (tFilter == null || tFilter.isActive(i)) {
						writer.write("1\t" + StringUtil.eliminateCommas(table.getDataItemName(i)) + "\t" + String.valueOf(i + 1));
						for (int j = 0; j < colNs.size(); j++) {
							String val = table.getAttrValueAsString(colNs.elementAt(j), i);
							if (val == null) {
								val = "0";
							}
							writer.write("\t" + val);
						}
						writer.write("\r\n");
						++nrows;
						if (nrows % 100 == 0) {
							showMessage("Writing feature vectors to file: " + nrows + " rows stored", false);
						}
					}
			} catch (IOException e) {
				showMessage("File writing error: " + e.getMessage(), true);
				err = true;
			}
		}
		if (!err) {
			showMessage(nrows + " feature vectors have been written to file " + fvPath, false);
		}

		try {
			writer.close();
		} catch (IOException ioe) {
		}

		if (err)
			return;

		if (runSOM == null) {
			runSOM = new RunIXSOM();
			String libPath = core.getSystemSettings().getParameterAsString("PATH_TO_SOM");
			if (libPath != null) {
				System.out.println("Path to SOM = " + libPath);
				runSOM.setSOMLibraryPath(libPath);
			}
			if (!runSOM.checkSOMLibrary()) {
				showMessage("The SOM library has not been found!", true);
				runSOM = null;
				System.gc();
				return;
			}
			runSOM.setOwner(this);
		}
		if (runSOM == null)
			return;
		somAp.tblSOM = table;
		somAp.paramSOM = param;
		somAp.applySOMtoParam = applySOMtoParam;
		somAp.selAttrs = new Vector<Attribute>(attributes.size());
		GeoLayer layer = core.getDataKeeper().getTableLayer(somAp.tblSOM);
		if (layer != null && layer instanceof DGeoLayer) {
			somAp.tblSOMlayer = (DGeoLayer) layer;
		}
		for (int i = 0; i < attributes.size(); i++) {
			somAp.selAttrs.addElement((Attribute) attributes.elementAt(i));
		}
		if (applySOMtoParam) {
			if (somAp.paramSOM.isTemporal()) {
				somAp.objType = SOMApplInfo.Times;
			}
			if (somAp.tblSOMlayer != null) {
				somAp.featuresType = SOMApplInfo.Spatial_Distributions;
			}
		} else {
			if (somAp.tblSOMlayer != null) {
				somAp.objType = SOMApplInfo.Places;
			}
			if (somAp.paramSOM != null)
				if (somAp.paramSOM.isTemporal()) {
					somAp.featuresType = SOMApplInfo.Time_Series;
				} else {
					somAp.featuresType = SOMApplInfo.Parametric_Attribute;
				}
			else {
				somAp.featuresType = SOMApplInfo.Multiattribute_Profiles;
			}
		}
		if (somAp.tblSOMlayer != null) {
			makeImageLayer();
		}
		boolean ok = runSOM.startSOM("test", fvPath, null);
		if (!ok) {
			showMessage("Failed to start SOM!", true);
			runSOM = null;
			System.gc();
			return;
		}
		if (!applySOMtoParam) {
			makeMosaicImages();
		}
	}

	protected void processSOMResult(SOMResult result) {
		if (result == null)
			return;
		if (somAp.tblSOM == null)
			return;
		if (somAp.somRes != null) {
			somAp = somAp.getCopy();
			somAp.somRes = null;
			somAp.samPr = null;
			somAp.projection = null;
			somAp.tblSOMneuro = null;
			somAp.colIdxSOMCells = -1;
		}
		somAp.somRes = result;
		if (somAp.applySOMtoParam) {
			applySOMResultsToParamValues();
		} else {
			applySOMResultsToTableRows();
		}
		if (allSOMApplications == null) {
			allSOMApplications = new Vector<SOMApplInfo>(20, 20);
		}
		allSOMApplications.addElement(somAp.getCopy());
		somAp.tblSOMneuro = SOMPostProcessor.makeTableWithSOMNeuronsData(somAp);
		if (somAp.tblSOMneuro != null) {
			somAp.tblSOMneuro.setName("SOM neurons " + StringUtil.timeToString(new GregorianCalendar(), ":"));
			core.getDataLoader().addTable(somAp.tblSOMneuro);
		}
		SOMMatrixView smw = new SOMMatrixView(somAp, core);
		if (bkgLayer != null) {
			smw.setMapBkgLayer(bkgLayer);
		}
		core.getDisplayProducer().showGraph(smw);
	}

	protected void applySOMResultsToTableRows() {
		if (somAp.somRes == null || somAp.tblSOM == null)
			return;
		int colIdx = putSOMResultsInTableColumn(somAp.somRes, somAp.tblSOM);
		if (colIdx < 0) {
			showMessage("Failed to get SOM results!", true);
			return;
		}
		somAp.tblSOMResult = somAp.tblSOM;
		somAp.colIdxSOMCells = colIdx;
		showMessage("SOM results have been stored in the table!", false);
		if (somAp.tblSOMlayer != null) {
			Vector clAttr = new Vector(1, 1);
			clAttr.addElement(somAp.tblSOM.getAttributeId(colIdx));
			Visualizer lvis = somAp.tblSOMlayer.getVisualizer(), lbvis = somAp.tblSOMlayer.getBackgroundVisualizer();
			boolean layerHasVisualizer = lvis != null || lbvis != null;
			if (layerHasVisualizer && somAp.tblSOMlayer.getType() == Geometry.area && (lvis == null || lbvis == null)) {
				layerHasVisualizer = lvis == null || !lvis.isDiagramPresentation();
			}
			boolean useMainView = !layerHasVisualizer || !core.getUI().getUseNewMapForNewVis();
			MapViewer mapView = core.getUI().getMapViewer((useMainView) ? "main" : "_blank_");
			DGeoLayer layer = somAp.tblSOMlayer;
			if (!useMainView) {
				int lidx = mapView.getLayerManager().getIndexOfLayer(layer.getContainerIdentifier());
				if (lidx < 0) {
					mapView = core.getUI().getMapViewer("main");
				} else {
					layer = (DGeoLayer) mapView.getLayerManager().getGeoLayer(lidx);
				}
			}
			DisplayProducer dpr = core.getDisplayProducer();
			dpr.displayOnMap("qualitative_colour", somAp.tblSOM, clAttr, layer, mapView);
		}
	}

	/**
	 * Creates a new column in the given table and puts there the result, i.e.
	 * SOM cells corresponding to the objects of the table.
	 * @return index of the new column with the results
	 */
	protected int putSOMResultsInTableColumn(SOMResult result, DataTable table) {
		String colNamePrefix = "SOM cell", colName = colNamePrefix;
		for (int i = 2; table.findAttrByName(colName) >= 0; i++) {
			colName = colNamePrefix + " (" + i + ")";
		}
/*
    colName= Dialogs.askForStringValue(getFrame(),
        "Column name?",colName,
        "A new column will be created in the table "+table.getName(),"New column",true);
    if (colName==null) return -1;
*/
		Attribute attr = new Attribute("_SOM_results_" + (table.getAttrCount() + 1), AttributeTypes.character);
		attr.setName(colName);
		table.addAttribute(attr);
		int colIdx = table.getAttrCount() - 1;
		Attribute attr1 = new Attribute("_SOM_proto_" + (table.getAttrCount() + 1), AttributeTypes.logical);
		attr1.setName("Prototype?");
		table.addAttribute(attr1);
		int cellIdx = 0;
		Vector<String> values = new Vector(result.xdim * result.ydim, 1);
		Vector<Color> colors = new Vector(result.xdim * result.ydim, 1);
		for (int y = 0; y < result.ydim; y++) {
			for (int x = 0; x < result.xdim; x++) {
				SOMCellInfo sci = result.cellInfos[x][y];
				if (sci.nObj > 0 && sci.oIds != null) {
					//values.addElement(String.valueOf(cellIdx));
					String classLabel = "xy_" + (x + 1) + "_" + (y + 1);
					values.addElement(classLabel);
					Color color = sci.cellColor;
					if (color == null) {
						color = ColorScale2D.getColor(y, x, result.ydim, result.xdim);
					}
					colors.addElement(color);
					for (int i = 0; i < sci.oIds.size(); i++) {
						int id = sci.oIds.get(i).intValue();
						if (id > 0) {
							DataRecord rec = table.getDataRecord(id - 1);
							if (rec != null) {
								rec.setAttrValue(classLabel, colIdx);
								rec.setAttrValue((id == sci.protoId) ? "yes" : "no", colIdx + 1);
							}
						}
					}
				}
				++cellIdx;
			}
		}
		if (values.size() < 1)
			return -1;
		String av[] = new String[values.size()];
		Color ac[] = new Color[colors.size()];
		av = values.toArray(av);
		ac = colors.toArray(ac);
		attr.setValueListAndColors(av, ac);
		return colIdx;
	}

	protected void applySOMResultsToParamValues() {
		if (somAp.somRes == null || somAp.paramSOM == null)
			return;
		if (somAp.tblSOMResult == null) {
			DataTable parValTbl = findParamValTable();
			if (parValTbl == null) {
				//create the table
				String tblName = "SOM results for " + somAp.paramSOM.getName();
				String genName = somAp.paramSOM.getName();
				Panel p = new Panel(new ColumnLayout());
				p.add(new Label("A new table will be created.", Label.CENTER));
				p.add(new Label("Table name:"));
				TextField tntf = new TextField(tblName);
				p.add(tntf);
				p.add(new Label("Generic name of an item described by a record:"));
				TextField gntf = new TextField(genName);
				p.add(gntf);
				OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "New table", true);
				dia.addContent(p);
				dia.show();
				if (dia.wasCancelled())
					return;
				String str = tntf.getText();
				if (str != null && str.trim().length() > 0) {
					tblName = str.trim();
				}
				str = gntf.getText();
				if (str != null && str.trim().length() > 0) {
					genName = str.trim();
				} else {
					genName = null;
				}

				parValTbl = new DataTable();
				parValTbl.setName(tblName);
				if (genName != null) {
					parValTbl.setGenericNameOfEntity(genName);
				}
				Attribute atParVal = null;
				Object parVal = somAp.paramSOM.getFirstValue();
				boolean paramIsTemporal = parVal instanceof TimeMoment;
				if (paramIsTemporal) {
					parValTbl.setNatureOfItems(DataTable.NATURE_TIME);
					atParVal = new Attribute("xxx", AttributeTypes.time);
				} else if (parVal instanceof Integer) {
					atParVal = new Attribute("xxx", AttributeTypes.integer);
				} else if ((parVal instanceof Float) || (parVal instanceof Double)) {
					atParVal = new Attribute("xxx", AttributeTypes.real);
				} else {
					atParVal = new Attribute("xxx", AttributeTypes.character);
				}
				atParVal.setName(somAp.paramSOM.getName());
				atParVal.setIdentifier(IdMaker.makeId(somAp.paramSOM.getName(), parValTbl));
				parValTbl.addAttribute(atParVal);

				for (int i = 0; i < somAp.paramSOM.getValueCount(); i++) {
					Object val = somAp.paramSOM.getValue(i);
					DataRecord rec = new DataRecord(String.valueOf(i + 1), val.toString());
					parValTbl.addDataRecord(rec);
					if (!(val instanceof String)) {
						rec.setDescribedObject(val);
					}
					rec.setAttrValue(val, 0);
				}

				DataLoader dLoader = core.getDataLoader();
				dLoader.addTable(parValTbl);
			}
			somAp.tblSOMResult = parValTbl;
		}
		int colIdx = putSOMResultsInTableColumn(somAp.somRes, somAp.tblSOMResult);
		if (colIdx < 0) {
			showMessage("Failed to get SOM results!", true);
			return;
		}
		somAp.colIdxSOMCells = colIdx;
		showMessage("SOM results have been stored in the table!", false);
		if (somAp.paramSOM.getFirstValue() instanceof TimeMoment) {
			TimeArranger tar = new TimeArranger(somAp.tblSOMResult, somAp.colIdxSOMCells, core.getSupervisor());
			core.getDisplayProducer().showGraph(tar);
		} else {
			Vector attr = new Vector(1, 1);
			attr.addElement(somAp.tblSOMResult.getAttributeId(colIdx));
			core.getDisplayProducer().display(somAp.tblSOMResult, attr, "classifier_1_qual_attr", null);
		}
	}

	protected DataTable findParamValTable() {
		if (somAp.paramSOM == null || somAp.tblSOM == null || allSOMApplications == null)
			return null;
		for (int i = 0; i < allSOMApplications.size(); i++) {
			SOMApplInfo somapi = allSOMApplications.elementAt(i);
			if (somapi.applySOMtoParam && somapi.paramSOM != null && somapi.tblSOMResult != null && somapi.tblSOM.equals(somAp.tblSOM) && somapi.paramSOM.getName().equalsIgnoreCase(somAp.paramSOM.getName()))
				return somapi.tblSOMResult;
		}
		return null;
	}

	/**
	 * Tries to create an image layer to be used as a background for small maps.
	 */
	protected void makeImageLayer() {
		if (imgMaker != null)
			return;
		MapViewer mapView = core.getUI().getCurrentMapViewer();
		if (mapView == null)
			return;
		DLayerManager lman = (DLayerManager) mapView.getLayerManager();
		if (lman == null)
			return;
		boolean toDraw[] = new boolean[lman.getLayerCount()];
		for (int i = 0; i < lman.getLayerCount(); i++) {
			DGeoLayer layer = lman.getLayer(i);
			toDraw[i] = layer.getLayerDrawn();
/*
      toDraw[i]=layer.getLayerDrawn() &&
        (layer.getVisualizer()==null && layer.getBackgroundVisualizer()==null) ||
        layer.getType()==Geometry.raster;
*/
		}
		float ext[] = mapView.getMapExtent();
		RealRectangle terr = new RealRectangle(ext[0], ext[1], ext[2], ext[3]);
		float w = terr.rx2 - terr.rx1, h = terr.ry2 - terr.ry1;
		int iw = Math.round(50f * getFrame().getToolkit().getScreenResolution() / 25.33f);
		double ratio = 1;
		if (mapView.getMapDrawer().getMapContext() instanceof MapMetrics) {
			MapMetrics mm = (MapMetrics) mapView.getMapDrawer().getMapContext();
			ratio = mm.getStep() / mm.getStepY();
		}
		int ih = (int) Math.round(Math.ceil(ratio * iw * h / w));
		imgMaker = new MapBkgImageMaker(core, lman, toDraw, true, terr, new Dimension(iw, ih), this);
		imgMaker.start();
		System.out.println("Started MapBkgImageMaker...");
	}

	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(runSOM)) {
			if (e.getPropertyName().equals("SOM_closed")) {
				e = null;
				runSOM = null;
				System.gc();
			} else if (e.getPropertyName().equals("SOM_result")) {
				processSOMResult((SOMResult) e.getNewValue());
			}
		} else if (e.getSource().equals(imgMaker) && e.getPropertyName().equals("bkg_map")) {
			if (e.getNewValue() != null && (e.getNewValue() instanceof DGeoLayer)) {
				System.out.println("Received background image layer from MapBkgImageMaker");
				bkgLayer = (DGeoLayer) e.getNewValue();
				MapViewer mapView = core.getUI().getCurrentMapViewer();
				DLayerManager lman = (DLayerManager) mapView.getLayerManager();
				bkgLayer.setGeographic(lman.isGeographic());
				if (somAp.applySOMtoParam) {
					DLayerManager lm = (DLayerManager) lman.makeCopy(false);
					if (lm.getOSMLayer() != null) {
						lm.getOSMLayer().setLayerDrawn(false);
					}
					lm.addGeoLayer(bkgLayer);
					if (somAp.tblSOMlayer != null) {
						themLayer = (DGeoLayer) somAp.tblSOMlayer.makeCopy();
						lm.addGeoLayer(themLayer);
					}
					smallMap = new MapCanvas();
					smallMap.setMapContent(lm);
					Image img = imgMaker.getMapBkgImage();
					int iw = img.getWidth(null), ih = img.getHeight(null);
					smallMap.setPreferredSize(iw, ih);
					smallMap.setSize(iw, ih);
					MapContext mc = smallMap.getMapContext();
					mc.setVisibleTerritory(bkgLayer.getWholeLayerBounds());
					mc.setViewportBounds(0, 0, iw, ih);
					makeMapImages();
				}
			}
			imgMaker = null;
		}
	}

	/**
	 * Tries to find a visualization of the layer to which the SOM will be applied
	 * by numeric classifier in order to take from it the information about
	 * user-preferred breaks and colors
	 */
	protected NumAttr1Classifier findClassifier() {
		if (core == null || somAp == null || somAp.tblSOM == null || somAp.tblSOMlayer == null)
			return null;
		Supervisor sup = core.getSupervisor();
		if (sup == null || sup.getSaveableToolCount() < 1)
			return null;
		Vector<MapViewer> vmv = new Vector<MapViewer>(sup.getSaveableToolCount(), 10);
		for (int i = sup.getSaveableToolCount() - 1; i >= 0; i--)
			if (sup.getSaveableTool(i) instanceof MapViewer) {
				vmv.addElement((MapViewer) sup.getSaveableTool(i));
			}
		if (vmv.size() < 1)
			return null;
		Vector<NumAttr1Classifier> vcl = new Vector<NumAttr1Classifier>(10, 10);
		for (int i = 0; i < vmv.size(); i++) {
			LayerManager lman = vmv.elementAt(i).getLayerManager();
			int lIdx = lman.getIndexOfLayer(somAp.tblSOMlayer.getContainerIdentifier());
			if (lIdx < 0) {
				continue;
			}
			DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(lIdx);
			ClassDrawer cld = null;
			if (layer.getVisualizer() != null && (layer.getVisualizer() instanceof ClassDrawer)) {
				cld = (ClassDrawer) layer.getVisualizer();
			}
			if (cld == null && layer.getBackgroundVisualizer() != null && (layer.getBackgroundVisualizer() instanceof ClassDrawer)) {
				cld = (ClassDrawer) layer.getBackgroundVisualizer();
			}
			if (cld == null) {
				continue;
			}
			if (cld.getClassifier() instanceof NumAttr1Classifier) {
				vcl.addElement((NumAttr1Classifier) cld.getClassifier());
			}
		}
		if (vcl.size() < 1)
			return null;
		Panel mainP = new Panel(new ColumnLayout());
		mainP.add(new Label("Do you wish to use the breaks and colors from an existing classifier?"));
		CheckboxGroup cbg = new CheckboxGroup();
		Checkbox cb[] = new Checkbox[vcl.size() + 1];
		for (int i = 0; i < vcl.size(); i++) {
			FloatArray breaks = vcl.elementAt(i).getBreaks();
			String str = String.valueOf(breaks.elementAt(0));
			for (int j = 1; j < breaks.size(); j++) {
				str += "; " + String.valueOf(breaks.elementAt(j));
			}
			cb[i] = new Checkbox(str, false, cbg);
			mainP.add(cb[i]);
			Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			for (int j = 0; j < breaks.size() + 1; j++) {
				Color c = vcl.elementAt(i).getClassColor(j);
				if (c != null) {
					Label l = new Label(" ");
					l.setBackground(c);
					p.add(l);
				}
			}
			mainP.add(p);
		}
		cb[vcl.size()] = new Checkbox("create a new color mapping", true, cbg);
		mainP.add(cb[vcl.size()]);
		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Breaks and colors", true);
		dia.addContent(mainP);
		dia.show();
		if (dia.wasCancelled())
			return null;
		if (cb[vcl.size()].getState())
			return null;
		for (int i = 0; i < vcl.size(); i++)
			if (cb[i].getState())
				return vcl.elementAt(i);
		return null;
	}

	/**
	 * Parameters for the color encoding of attribute values
	 */
	protected float posHue = Float.NaN, negHue = Float.NaN;
	protected Color midColor = null;

	protected void getDesiredColorScale() {
		//if (Float.isNaN(posHue) || Float.isNaN(negHue)) {
		float hues[] = { -1101.0f, 0.6f };
		String prompts[] = { "Positive color?", "Negative color?" };
		ColorSelDialog csd = new ColorSelDialog(2, hues, Color.white, prompts, true, true);
		OKDialog okd = new OKDialog(core.getUI().getMainFrame(), csd.getName(), false);
		okd.addContent(csd);
		okd.show();
		posHue = csd.getHueForItem(0);
		negHue = csd.getHueForItem(1);
		midColor = csd.getMidColor();
		//}
	}

	protected void makeMosaicImages() {
		if (somAp.tblSOM == null || somAp.selAttrs == null || runSOM == null)
			return;
		NumAttr1Classifier nClSel = findClassifier();
		if (nClSel == null) {
			getDesiredColorScale();
		}
		int pixelSize = 5;
		int ncols = 0;
		if (somAp.paramSOM != null) {
			ncols = Math.min(12, somAp.paramSOM.getValueCount());
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label("Generating images for SOM cells", Label.CENTER));
			p.add(new Line(false));
			TextCanvas tc = new TextCanvas();
			tc.addTextLine("The chosen attribute(s) depend(s) on parameter \"" + somAp.paramSOM.getName() + "\" with " + somAp.paramSOM.getValueCount() + " values ranging from " + somAp.paramSOM.getFirstValue() + " to "
					+ somAp.paramSOM.getLastValue() + ".");
			p.add(tc);
			p.add(new Label("How to represent the feature vectors?"));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox cbRect = new Checkbox("by rectangular mosaics", true, cbg);
			p.add(cbRect);
			TextField tfNC = new TextField(String.valueOf(ncols), 3);
			Panel pp = new Panel(new FlowLayout());
			pp.add(new Label("Number of columns:"));
			pp.add(tfNC);
			p.add(pp);
			Checkbox cbSpiral = new Checkbox("by spiral mosaics", false, cbg);
			p.add(cbSpiral);
			OKDialog okd = new OKDialog(core.getUI().getMainFrame(), "Image generation", true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			if (cbRect.getState()) {
				String str = tfNC.getText();
				if (str != null) {
					try {
						ncols = Integer.parseInt(str.trim());
					} catch (NumberFormatException e) {
						ncols = 12;
					}
				}
			} else {
				ncols = 0;
			}
		}
		somAp.images = new HashMap<Integer, BufferedImage>(2 * somAp.tblSOM.getDataItemCount());
		if (somAp.paramSOM != null) {
			NumAttr1Classifier nClassifier[] = new NumAttr1Classifier[somAp.selAttrs.size()];
			DataVisAnimator anim[] = new DataVisAnimator[somAp.selAttrs.size()];
			int nVals = somAp.paramSOM.getValueCount();
			Color colors[][] = new Color[somAp.selAttrs.size()][nVals];
			for (int aIdx = 0; aIdx < somAp.selAttrs.size(); aIdx++) {
				Attribute parent = somAp.selAttrs.elementAt(aIdx);
				nClassifier[aIdx] = new NumAttr1Classifier(somAp.tblSOM, parent.getChild(0).getIdentifier());
				if (nClSel != null) {
					nClassifier[aIdx].setBreaks(nClSel.getBreaks());
					nClassifier[aIdx].setColors(nClSel.getColors());
					nClassifier[aIdx].setPositiveHue(nClSel.getPositiveHue());
					nClassifier[aIdx].setNegativeHue(nClSel.getNegativeHue());
					nClassifier[aIdx].setMiddleColor(nClSel.getMiddleColor());
					nClassifier[aIdx].setMiddleValue(nClSel.getMiddleValue());
				} else {
					nClassifier[aIdx].setInitialNClasses(15);
				}
				anim[aIdx] = new DataVisAnimator();
				Vector<VisAttrDescriptor> aDescr = new Vector<VisAttrDescriptor>(1, 1);
				VisAttrDescriptor vad = new VisAttrDescriptor();
				vad.parent = somAp.selAttrs.elementAt(aIdx);
				vad.isTimeDependent = true;
				aDescr.addElement(vad);
				anim[aIdx].setup(somAp.tblSOM, aDescr, somAp.paramSOM, nClassifier[aIdx]);
				nClassifier[aIdx].setup();
				if (nClSel == null) {
					nClassifier[aIdx].setPositiveHue(posHue);
					nClassifier[aIdx].setNegativeHue(negHue);
					nClassifier[aIdx].setMiddleColor(midColor);
					nClassifier[aIdx].setupColors();
				}
			}
			for (int oIdx = 0; oIdx < somAp.tblSOM.getDataItemCount(); oIdx++) {
				for (int i = 0; i < nVals; i++) {
					for (int aIdx = 0; aIdx < somAp.selAttrs.size(); aIdx++) {
						anim[aIdx].setParamValIndex(i);
						int clIdx = nClassifier[aIdx].getRecordClass(oIdx);
						colors[aIdx][i] = nClassifier[aIdx].getClassColor(clIdx);
					}
				}
				BufferedImage im = (ncols > 0) ? makeRectangularMosaic(colors, ncols, pixelSize) : makeSpiralMosaic(colors, pixelSize);
				if (im != null) {
					somAp.images.put(oIdx + 1, im);
				}
			}
			for (NumAttr1Classifier element : nClassifier) {
				element.destroy();
			}
		} else {
			Vector<String> colIds = new Vector<String>(100, 100);
			for (int i = 0; i < somAp.selAttrs.size(); i++) {
				Attribute at = somAp.selAttrs.elementAt(i);
				if (!at.hasChildren()) {
					colIds.addElement(at.getIdentifier());
				} else {
					for (int j = 0; j < at.getChildrenCount(); j++) {
						colIds.addElement(at.getChild(j).getIdentifier());
					}
				}
			}
			NumAttr1Classifier nClassifier[] = new NumAttr1Classifier[colIds.size()];
			for (int i = 0; i < colIds.size(); i++) {
				nClassifier[i] = new NumAttr1Classifier(somAp.tblSOM, colIds.elementAt(i));
				if (nClSel != null) {
					nClassifier[i].setBreaks(nClSel.getBreaks());
					nClassifier[i].setColors(nClSel.getColors());
					nClassifier[i].setPositiveHue(nClSel.getPositiveHue());
					nClassifier[i].setNegativeHue(nClSel.getNegativeHue());
					nClassifier[i].setMiddleColor(nClSel.getMiddleColor());
					nClassifier[i].setMiddleValue(nClSel.getMiddleValue());
				} else {
					nClassifier[i].setInitialNClasses(15);
				}
				nClassifier[i].setup();
				if (nClSel == null) {
					nClassifier[i].setPositiveHue(posHue);
					nClassifier[i].setNegativeHue(negHue);
					nClassifier[i].setMiddleColor(midColor);
					nClassifier[i].setupColors();
				}
			}
			Color colors[][] = new Color[1][colIds.size()];
			Dimension dim = getSuitableLayout(colIds.size(), pixelSize, pixelSize, 0, 0);
			for (int oIdx = 0; oIdx < somAp.tblSOM.getDataItemCount(); oIdx++) {
				for (int i = 0; i < colIds.size(); i++) {
					int clIdx = nClassifier[i].getRecordClass(oIdx);
					colors[0][i] = nClassifier[i].getClassColor(clIdx);
				}
				BufferedImage im = makeRectangularMosaic(colors, dim.width, pixelSize);
				if (im != null) {
					somAp.images.put(oIdx + 1, im);
				}
			}
			for (NumAttr1Classifier element : nClassifier) {
				element.destroy();
			}
		}
		if (!somAp.images.isEmpty()) {
			runSOM.setImagesForObjects(somAp.images);
		}
	}

	protected Dimension getSuitableLayout(int nItems, int itemW, int itemH, int gap, int marg) {
		if (nItems <= 1)
			return new Dimension(1, 1);
		int nColumns = 0, nRows = nItems, w = 0, h = 0;
		do {
			++nColumns;
			nRows = (int) Math.ceil((double) nItems / nColumns);
			w = itemW * nColumns + gap * (nColumns - 1) + 2 * marg;
			h = itemH * nRows + gap * (nRows - 1) + 2 * marg;
		} while (h > 1.5 * w);
		return new Dimension(nColumns, nRows);
	}

	protected Point[] computeMosaicSymbolsPositions(int nSymb, int nColumns, int nRows, int symbW, int symbH, int gap, int marg) {
		Point symbOrig[] = new Point[nSymb];
		symbOrig[0] = new Point(marg, marg);
		if (nSymb > 1) {
			int idx = 0;
			for (int y = 0; y < nRows && idx < nSymb; y++) {
				for (int x = 0; x < nColumns && idx < nSymb; x++) {
					symbOrig[idx] = new Point(marg + x * (symbW + gap), marg + y * (symbH + gap));
					++idx;
				}
			}
		}
		return symbOrig;
	}

	protected BufferedImage makeRectangularMosaic(Color colors[][], int ncols, int pixelSize) {
		int nSymb = colors.length; //there may be mosaics for multiple attributes;
		int nVals = colors[0].length;
		int nrows = (int) Math.ceil(1.0 * nVals / ncols);
		int symbW = ncols * pixelSize, symbH = nrows * pixelSize;
		int marg = 2, gap = 4;
		Dimension dim = getSuitableLayout(nSymb, symbW, symbH, gap, marg);
		Point symbOrig[] = computeMosaicSymbolsPositions(nSymb, dim.width, dim.height, symbW, symbH, gap, marg);
		int w = symbW * dim.width + gap * (dim.width - 1) + 2 * marg + 1, h = symbH * dim.height + gap * (dim.height - 1) + 2 * marg + 1;
		BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics g = im.getGraphics();
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, w + 1, h + 1);
		for (int sIdx = 0; sIdx < nSymb; sIdx++) {
			int cn = 0, rn = 0;
			int x = symbOrig[sIdx].x, y = symbOrig[sIdx].y;
			for (int i = 0; i < nVals; i++) {
				g.setColor(colors[sIdx][i]);
				g.fillRect(x, y, pixelSize + 1, pixelSize + 1);
				++cn;
				x += pixelSize;
				if (cn >= ncols) {
					cn = 0;
					x = symbOrig[sIdx].x;
					++rn;
					y += pixelSize;
				}
			}
		}
		return im;
	}

	protected Point spiralPos[] = null;
	protected int nTilesX = 0, nTilesY = 0;

	protected BufferedImage makeSpiralMosaic(Color colors[][], int pixelSize) {
		int nSymb = colors.length; //there may be mosaics for multiple attributes;
		int nVals = colors[0].length;
		if (nTilesX < pixelSize || nTilesY < pixelSize || spiralPos == null || spiralPos.length != nVals) {
			//compute positions of the mosaic tiles in the spiral
			spiralPos = new Point[nVals];
			int x = 0, y = 0;
			spiralPos[0] = new Point(x, y);
			int nStepsToDo = 1;
			int nSteps = 0, nTurns = 0;
			int dir = 1; // 1=E, 2=S, 3=W, 4=N
			int minX = 0, maxX = 0, minY = 0, maxY = 0;
			for (int i = 1; i < spiralPos.length; i++) {
				switch (dir) {
				case 1:
					++x;
					if (maxX < x) {
						maxX = x;
					}
					break;
				case 2:
					++y;
					if (maxY < y) {
						maxY = y;
					}
					break;
				case 3:
					--x;
					if (minX > x) {
						minX = x;
					}
					break;
				case 4:
					--y;
					if (minY > y) {
						minY = y;
					}
					break;
				}
				spiralPos[i] = new Point(x, y);
				++nSteps;
				if (nSteps == nStepsToDo) {
					//turn clockwise
					++dir;
					nSteps = 0;
					if (dir > 4) {
						dir = 1;
					}
					++nTurns;
					if (nTurns == 2) {
						++nStepsToDo;
						nTurns = 0;
					}
				}
			}
			nTilesX = maxX - minX + 1;
			nTilesY = maxY - minY + 1;
			for (int i = 0; i < spiralPos.length; i++) {
				spiralPos[i].x -= minX;
				spiralPos[i].y -= minY;
				//all coordinates will be non-negative
			}
		}
		int marg = 2, internalGap = 1, gap = 4;
		int symbW = nTilesX * pixelSize + (nTilesX - 1) * internalGap;
		int symbH = nTilesY * pixelSize + (nTilesY - 1) * internalGap;
		Dimension dim = getSuitableLayout(nSymb, symbW, symbH, gap, marg);
		Point symbOrig[] = computeMosaicSymbolsPositions(nSymb, dim.width, dim.height, symbW, symbH, gap, marg);
		int w = symbW * dim.width + gap * (dim.width - 1) + 2 * marg, h = symbH * dim.height + gap * (dim.height - 1) + 2 * marg;
		BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics g = im.getGraphics();
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, w + 1, h + 1);
		for (int sIdx = 0; sIdx < nSymb; sIdx++) {
			int ox = symbOrig[sIdx].x, oy = symbOrig[sIdx].y;
			for (int i = 0; i < spiralPos.length; i++) {
				int x = spiralPos[i].x, y = spiralPos[i].y;
				int xx = x * (pixelSize + internalGap) + ox, yy = y * (pixelSize + internalGap) + oy, pw = pixelSize, ph = pixelSize;
				if (i > 0)
					if (spiralPos[i - 1].x < x) {
						xx -= internalGap;
						pw += internalGap;
					} else if (spiralPos[i - 1].x > x) {
						pw += internalGap;
					} else if (spiralPos[i - 1].y < y) {
						yy -= internalGap;
						ph += internalGap;
					} else if (spiralPos[i - 1].y > y) {
						ph += internalGap;
					}
				g.setColor(colors[sIdx][i]);
				g.fillRect(xx, yy, pw, ph);
			}
		}
		return im;
	}

	protected void makeMapImages() {
		if (smallMap == null || somAp.tblSOM == null || somAp.paramSOM == null || somAp.selAttrs == null || themLayer == null || runSOM == null)
			return;
		NumAttr1Classifier nClSel = findClassifier();
		if (nClSel == null) {
			getDesiredColorScale();
		}
		int nAttr = somAp.selAttrs.size();
		NumAttr1Classifier nClassifier[] = new NumAttr1Classifier[nAttr];
		DataVisAnimator anim[] = new DataVisAnimator[nAttr];
		for (int aIdx = 0; aIdx < nAttr; aIdx++) {
			Attribute parent = somAp.selAttrs.elementAt(aIdx);
			nClassifier[aIdx] = new NumAttr1Classifier(somAp.tblSOM, parent.getChild(0).getIdentifier());
			if (nClSel != null) {
				nClassifier[aIdx].setBreaks(nClSel.getBreaks());
				nClassifier[aIdx].setColors(nClSel.getColors());
				nClassifier[aIdx].setPositiveHue(nClSel.getPositiveHue());
				nClassifier[aIdx].setNegativeHue(nClSel.getNegativeHue());
				nClassifier[aIdx].setMiddleColor(nClSel.getMiddleColor());
				nClassifier[aIdx].setMiddleValue(nClSel.getMiddleValue());
			} else {
				nClassifier[aIdx].setInitialNClasses(15);
			}
			anim[aIdx] = new DataVisAnimator();
			Vector<VisAttrDescriptor> aDescr = new Vector<VisAttrDescriptor>(1, 1);
			VisAttrDescriptor vad = new VisAttrDescriptor();
			vad.parent = somAp.selAttrs.elementAt(aIdx);
			vad.isTimeDependent = true;
			aDescr.addElement(vad);
			anim[aIdx].setup(somAp.tblSOM, aDescr, somAp.paramSOM, nClassifier[aIdx]);
			nClassifier[aIdx].setup();
			if (nClSel == null) {
				nClassifier[aIdx].setPositiveHue(posHue);
				nClassifier[aIdx].setNegativeHue(negHue);
				nClassifier[aIdx].setMiddleColor(midColor);
				nClassifier[aIdx].setupColors();
			}
		}
		Visualizer vis = null;
		if (nAttr == 1) {
			ClassDrawer cd = (themLayer.getType() == Geometry.point) ? new ClassSignDrawer() : new ClassDrawer();
			cd.setClassifier(nClassifier[0]);
			vis = cd;
		} else {
			MultiClassPresenter mcp = new MultiClassPresenter();
			Vector cl = new Vector(nAttr, 1);
			for (NumAttr1Classifier element : nClassifier) {
				cl.addElement(element);
			}
			mcp.setClassifiers(cl);
			mcp.setDataSource(somAp.tblSOM);
			Vector attrIds = new Vector(nAttr, 1);
			for (int i = 0; i < nAttr; i++) {
				attrIds.addElement(nClassifier[i].getAttrId());
			}
			mcp.setAttributes(attrIds);
			vis = mcp;
			Dimension dim = getSuitableLayout(nClassifier.length, 1, 1, 0, 0);
			int ncols = dim.width;
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label("Generating map images for SOM cells", Label.CENTER));
			p.add(new Line(false));
			TextCanvas tc = new TextCanvas();
			tc.addTextLine("The values of the " + nClassifier.length + " attributes will be represented by" + " colors of pixels in rectangular mosais symbols.");
			p.add(tc);
			TextField tfNC = new TextField(String.valueOf(ncols), 3);
			Panel pp = new Panel(new FlowLayout());
			pp.add(new Label("Desired number of columns:"));
			pp.add(tfNC);
			p.add(pp);
			OKDialog okd = new OKDialog(core.getUI().getMainFrame(), "Map image generation", false);
			okd.addContent(p);
			okd.show();
			String str = tfNC.getText();
			if (str != null) {
				try {
					ncols = Integer.parseInt(str.trim());
				} catch (NumberFormatException e) {
					ncols = dim.width;
				}
			}
			int pixelSize = 5;
			MosaicSign mos = mcp.getSymbol();
			mos.setSegmColors(new Color[nClassifier.length]);
			mos.setNColumns(ncols);
			mos.setWidth(ncols * pixelSize);
			int nrows = (int) Math.ceil(1.0 * nClassifier.length / ncols);
			mos.setHeight(nrows * pixelSize);
			mos.setMustDrawFrame(true);
		}
		themLayer.setVisualizer(vis);
		themLayer.setLayerDrawn(true);
		Frame mFrame = new Frame("Temporary");
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Left);
		mFrame.setLayout(cl);
		mFrame.add(smallMap);
		mFrame.pack();
		mFrame.setVisible(true);
		somAp.images = new HashMap<Integer, BufferedImage>(2 * somAp.paramSOM.getValueCount());
		int n = 0;
		for (int i = 0; i < somAp.paramSOM.getValueCount(); i++) {
			for (DataVisAnimator element : anim) {
				element.setParamValIndex(i);
			}
			synchronized (smallMap) {
				smallMap.invalidateImage();
				BufferedImage im = (BufferedImage) smallMap.getMapAsImage();
				if (im != null) {
					somAp.images.put(i + 1, im);
					++n;
				}
			}
		}
		for (NumAttr1Classifier element : nClassifier) {
			element.destroy();
		}
		showMessage(n + " map images generated!", false);
		System.out.println(n + " map images generated!");
		mFrame.dispose();
		runSOM.setImagesForObjects(somAp.images);
	}

	protected DataTable selectTable(ESDACore core) {
		DataKeeper dk = core.getDataKeeper();
		if (dk.getTableCount() < 1)
			return null;
		TableManager tman = new TableManager();
		tman.setDataKeeper(dk);
		tman.setUI(core.getUI());
		int tn = tman.selectTableNumber("Select Table");
		if (tn < 0)
			return null;
		AttributeDataPortion tbl = dk.getTable(tn);
		if (!(tbl instanceof DataTable)) {
			showMessage("The table is not a DataTable!", true);
			return null;
		}
		return (DataTable) tbl;
	}

	public String getApplPath() {
		String applPath = core.getDataKeeper().getApplicationPath();
		if (applPath != null) {
			File f = new File(applPath);
			try {
				String str = f.getCanonicalPath();
				applPath = str;
			} catch (Exception e) {
			}
		}
		return applPath;
	}

	protected String makeFeaturesFilePath() {
		if (applPath == null) {
			applPath = getApplPath();
			if (applPath == null) {
				File currDir = new File(".");
				applPath = currDir.getAbsolutePath(); //ends with "/."
				int idx = CopyFile.lastSeparatorPos(applPath);
				if (applPath.charAt(idx + 1) == '.') {
					applPath = applPath.substring(0, idx + 1);
				}
			}
			applPath = CopyFile.attachSeparator(CopyFile.getDir(applPath));
			System.out.println("Path: " + applPath);
		}
		if (somInputDir == null) {
			somInputDir = applPath + "SOM_input";
			File dir = new File(somInputDir);
			if ((!dir.exists() || !dir.isDirectory()) && !dir.mkdir()) {
				somInputDir = applPath;
			}
			somInputDir = CopyFile.attachSeparator(somInputDir);
		}
		GregorianCalendar gc = new GregorianCalendar();
		String fname = "fv-" + numToString(gc.get(Calendar.YEAR), 4) + "-" + numToString(gc.get(Calendar.MONTH) + 1, 2) + "-" + numToString(gc.get(Calendar.DAY_OF_MONTH), 2) + "-" + numToString(gc.get(Calendar.HOUR_OF_DAY), 2) + "-"
				+ numToString(gc.get(Calendar.MINUTE), 2) + "-" + numToString(gc.get(Calendar.SECOND), 2) + ".fv2";
		return somInputDir + fname;
	}

	protected String numToString(int num, int length) {
		String str = String.valueOf(num);
		if (str.length() >= length)
			return str;
		return StringUtil.padString(str, '0', length, true);
	}
}
