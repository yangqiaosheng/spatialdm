package connection.oasis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import myXML.data_mapping.Expression;
import myXML.data_mapping.MapToType;
import myXML.data_mapping.MappingType;
import myXML.data_mapping.ValueSpecType;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.EnterPointOnMapUI;
import spade.analysis.tools.schedule.ScheduleToolsManager;
import spade.lib.help.Helper;
import spade.lib.util.Parameters;
import spade.lib.util.StringUtil;
import spade.vis.database.AttributeDataPortion;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapDraw;
import spade.vis.map.MapViewer;
import spade.vis.mapvis.IconPresenter;
import spade.vis.mapvis.SimpleDataMapper;
import spade.vis.preference.IconCorrespondence;
import spade.vis.preference.IconVisSpec;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;
import ui.EvacSchedulingUI;
import ui.ImageSaver;
import ui.MainWin;
import configstart.SysConfigReader;
import core.Core;
import display.DataVisSpec;
import display.DisplaySpec;
import display.Visualiser;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 14-Nov-2005
 * Time: 12:27:14
 */

public class DescartesForOASIS implements Visualiser, WindowListener, ActionListener {
	/**
	* The system core of Descartes that provides access to its main components
	*/
	protected ESDACore core = null;
	/**
	* The main window of Descartes
	*/
	protected MainWin mainWin = null;
	/**
	 * Stores the list of datasets that have been loaded. For each dataset,
	 * contains a hashtable with the identifier of the set (key "setId"),
	 * the reference to the table (key "table"), the reference to the layer
	 * (key "layer") and to the layer manager (key "layerManager").
	 */
	protected Vector dataList = null;
	/**
	 * Supports the use of the schedule-specific tools
	 */
	protected ScheduleToolsManager scheduleToolsManager = null;

	/**
	* Starts Descartes. The argument applFile is the path to a file describing
	* the application to be loaded at start-up. If it is null, Descartes
	* starts with an empty window.
	* Stores the core of Descartes in its internal memory.
	*/
	public Frame runDescartes(String applFile) {
		core = new Core();
		Parameters params = core.getSystemSettings();
		params.setParameter("isLocalSystem", "true");
		params.setParameter("runsInsideOtherSystem", "true");
		SysConfigReader scr = new SysConfigReader(params, null);
		if (scr.readConfiguration("system.cnf")) {
			String path = params.getParameterAsString("BROWSER");
			if (path != null) {
				Helper.setPathToBrowser(path);
			}
			path = params.getParameterAsString("PATH_TO_HELP");
			if (path != null) {
				Helper.setPathToHelpFiles(path);
			}
		}
		((Core) core).makeOptionalComponents();
		mainWin = new MainWin();
		//DescartesUI ui=new DescartesUI(mainWin);
		EvacSchedulingUI ui = new EvacSchedulingUI(mainWin);
		mainWin.add(ui, "Center");
		mainWin.addWindowListener(this);
		ui.startWork(core, applFile);
		scheduleToolsManager = ui.getScheduleToolsManager();
		return mainWin;
	}

	/**
	 * Fulfils the given display specification. Returns true if successful.
	 */
	@Override
	public boolean fulfilDisplaySpec(DisplaySpec spec) {
		if (spec == null)
			return false;
		//If Descartes has not started yet, invoke it.
		if (core == null) {
			runDescartes(spec.projectPath);
		}
		if (spec.dataVisSpecs != null && spec.dataVisSpecs.size() > 0) {
			for (int i = 0; i < spec.dataVisSpecs.size(); i++) {
				Object visSpecObj = spec.dataVisSpecs.elementAt(i);
				if (visSpecObj == null) {
					continue;
				}
				if (visSpecObj instanceof DataVisSpec) {
					DataVisSpec dvs = (DataVisSpec) visSpecObj;
					if (dvs.pathToData != null) {
						if (!loadData(dvs.pathToData, dvs.id, dvs.name)) {
							continue;
						}
						Hashtable ddescr = (Hashtable) dataList.elementAt(dataList.size() - 1);
						if (ddescr.get("table") == null && ddescr.get("layer") == null) {
							continue;
						}
						AttributeDataPortion table = (AttributeDataPortion) ddescr.get("table");
						DGeoLayer layer = (DGeoLayer) ddescr.get("layer");
						if (scheduleToolsManager != null && dvs.meaning != null)
							if (dvs.meaning.equalsIgnoreCase("OBJECTS_TO_SAVE")) {
								scheduleToolsManager.setObjectsToSave(layer);
							} else if (dvs.meaning.equalsIgnoreCase("OBJECTS_POSSIBLY_TO_SAVE")) {
								scheduleToolsManager.setObjectsPossiblyToSave(layer);
							} else if (dvs.meaning.equalsIgnoreCase("SHELTERS")) {
								scheduleToolsManager.setShelters(layer);
							}
						MapViewer mw = core.getUI().getCurrentMapViewer();
						Hashtable vsp = loadVisSpec(dvs.pathToVisSpec);
						if (vsp != null) {
							String methodId = (String) vsp.get("methodId");
							//System.out.println("Vis. specification loaded; method = "+methodId);
							if (table != null && layer != null && vsp.get("visualizer") != null) {
								/*
								System.out.println("The table to visualise: "+table.getContainerIdentifier());
								for (int j=0; j<table.getAttrCount(); j++)
								  System.out.println(" - Attribute <"+table.getAttributeId(j)+"> : type = "+
								      String.valueOf(table.getAttributeType(j)));
								*/
								//visualize the data
								Vector attrs = (Vector) vsp.get("attributes");
								if (attrs == null) {
									continue;
								}
								//System.out.print("The attribute(s) to visualise:");
								for (int j = 0; j < attrs.size(); j++) {
									String attrName = (String) attrs.elementAt(j);
									int idx = table.findAttrByName(attrName);
									if (idx >= 0) {
										attrs.setElementAt(table.getAttributeId(idx), j);
										//System.out.print(" "+attrs.elementAt(j));
									}
								}
								//System.out.println();
								//System.out.println("Trying to visualize...");
								core.getDisplayProducer().displayOnMap(vsp.get("visualizer"), methodId, table, attrs, layer, mw);
							} else if (layer != null && methodId != null && methodId.equalsIgnoreCase("layer_drawing")) {
								DrawingParameters dp = layer.getDrawingParameters();
								Object value = vsp.get("fill_colour");
								if (value != null) {
									dp.fillColor = (Color) value;
									dp.fillContours = true;
								}
								value = vsp.get("border_colour");
								if (value != null) {
									dp.lineColor = (Color) value;
								}
								value = vsp.get("border_thickness");
								if (value != null) {
									dp.lineWidth = ((Integer) value).intValue();
								}
								value = vsp.get("transparency");
								if (value != null) {
									dp.transparency = ((Integer) value).intValue();
								}
								layer.setDrawingParameters(dp);
							}
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Adjusts the territory extent shown in the map window so that all added
	 * layers are visible with a maximum scale.
	 */
	protected void adjustTerrExtent() {
		if (dataList == null || dataList.size() < 1)
			return; //no data have been added
		MapViewer mw = core.getUI().getCurrentMapViewer();
		if (mw == null)
			return;
		RealRectangle terrExtent = null;
		for (int i = 0; i < dataList.size(); i++) {
			Hashtable dd = (Hashtable) dataList.elementAt(i);
			DGeoLayer layer = (DGeoLayer) dd.get("layer");
			if (layer == null) {
				continue;
			}
			RealRectangle lb = layer.getWholeLayerBounds();
			if (lb != null) {
				float x1 = lb.rx1, x2 = lb.rx2, y1 = lb.ry1, y2 = lb.ry2;
				if (terrExtent == null) {
					terrExtent = new RealRectangle(x1, y1, x2, y2);
				} else {
					if (x1 < terrExtent.rx1) {
						terrExtent.rx1 = x1;
					}
					if (x2 > terrExtent.rx2) {
						terrExtent.rx2 = x2;
					}
					if (y1 < terrExtent.ry1) {
						terrExtent.ry1 = y1;
					}
					if (y2 > terrExtent.ry2) {
						terrExtent.ry2 = y2;
					}
				}
			}
		}
		mw.getMapDrawer().adjustExtentToShowArea(terrExtent.rx1, terrExtent.ry1, terrExtent.rx2, terrExtent.ry2);
	}

	/**
	 * Removes the data (set of objects) with the given identifier from the
	 * display. Removes both the table and the layer.
	 */
	@Override
	public void removeData(String dataId) {
		if (dataId == null)
			return;
		if (dataList == null || dataList.size() < 1)
			return; //no data have been added
		int idx = -1;
		for (int i = 0; i < dataList.size() && idx < 0; i++) {
			Hashtable dd = (Hashtable) dataList.elementAt(i);
			if (dataId.equalsIgnoreCase((String) dd.get("setId"))) {
				idx = i;
			}
		}
		if (idx < 0)
			return;
		Hashtable ddescr = (Hashtable) dataList.elementAt(idx);
		DGeoLayer layer = (DGeoLayer) ddescr.get("layer");
		if (layer != null) {
			core.removeMapLayer(layer.getContainerIdentifier(), true);
		} else {
			AttributeDataPortion table = (AttributeDataPortion) ddescr.get("table");
			core.removeTable(table.getContainerIdentifier());
		}
		dataList.removeElementAt(idx);
	}

	/**
	 * Loads the data from the specified file or URL. Returns true if successful.
	 * The argument dataId specified a unique identifier to refer to the items
	 * described by the data.
	 * The argument dataName specifies the name that should be used in the UI
	 * to refer to those data.
	 */
	public boolean loadData(String path, String dataId, String dataName) {
		if (core == null)
			return false;
		DataSourceSpec dss = new DataSourceSpec();
		dss.id = dataId;
		dss.name = dataName;
		dss.source = path;
		DataLoader loader = core.getDataLoader();
		if (loader == null)
			return false;
		int prevTableCount = loader.getTableCount();
		int currMapN = loader.getCurrentMapN(), prevLayerCount = 0;
		if (currMapN < 0) {
			currMapN = 0;
		}
		LayerManager lman = loader.getMap(currMapN);
		if (lman != null) {
			prevLayerCount = lman.getLayerCount();
		}
		if (!loader.loadData(dss))
			return false;
		if (dataList == null) {
			dataList = new Vector(50, 50);
		}
		Hashtable ddescr = new Hashtable();
		ddescr.put("setId", dataId);
		if (loader.getTableCount() > prevTableCount) {
			AttributeDataPortion table = loader.getTable(prevTableCount);
			if (table != null) {
				table.setEntitySetIdentifier(dataId);
				ddescr.put("table", table);
			}
		}
		if (lman == null) {
			lman = loader.getMap(currMapN);
		}
		ddescr.put("layerManager", lman);
		GeoLayer gl = lman.getGeoLayer(prevLayerCount);
		if (gl != null) {
			gl.setEntitySetIdentifier(dataId);
			lman.activateLayer(prevLayerCount);
			ddescr.put("layer", gl);
		}
		dataList.addElement(ddescr);
		//adjustTerrExtent();
		return true;
	}

	/**
	 * Loads a specification of data visualisation. If successful, returns a
	 * hashtable where a vector of attribute names is stored with the key
	 * "attributes" and a visualizer with the key "visualizer".
	 */
	protected Hashtable loadVisSpec(String path) {
		if (path == null)
			return null;
		Expression expr = null;
		try {
			// create a JAXBContext capable of handling classes generated into
			// the myXML.data_processing package
			JAXBContext jc = JAXBContext.newInstance("myXML.data_mapping");
			// create an Unmarshaller
			Unmarshaller u = jc.createUnmarshaller();
			// unmarshal an instance document into a tree of Java content
			// objects composed of classes from the myXML.data_mapping package.
			expr = (Expression) u.unmarshal(new FileInputStream(path));
		} catch (JAXBException je) {
			je.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		if (expr == null)
			return null;
		Vector attrs = null;
		java.util.List lst = expr.getAttribute();
		if (lst != null && !lst.isEmpty()) {
			attrs = new Vector(10, 10);
			for (Iterator iter = lst.iterator(); iter.hasNext();) {
				String attrName = (String) iter.next();
				if (attrName == null) {
					continue;
				}
				attrName = StringUtil.removeQuotes(attrName);
				if (!attrName.equalsIgnoreCase("NONE")) {
					attrs.addElement(attrName);
				} else {
					attrs = null;
					break;
				}
			}
		}
		/*
		if (attrs!=null)
		  for (int i=0; i<attrs.size(); i++)
		    System.out.println("Attribute to visualise: "+attrs.elementAt(i));
		*/
		lst = expr.getMapping();
		if (lst == null || lst.isEmpty())
			return null;
		String method = expr.getMethod();
		//System.out.println("Visualisation method: "+method);
		if (method == null)
			return null;
		Hashtable result = new Hashtable();
		if (attrs != null) {
			result.put("attributes", attrs);
		}
		if (method.equalsIgnoreCase("symbols")) {
			method = "icons";
			result.put("methodId", method);
			IconCorrespondence icor = new IconCorrespondence();
			icor.setAttributes(attrs);
			for (Iterator it = lst.iterator(); it.hasNext();) {
				MappingType mapping = (MappingType) it.next();
				java.util.List list = mapping.getMapTo();
				if (list == null || list.isEmpty()) {
					continue;
				}
				String iconFileName = null;
				float scaleFactor = Float.NaN;
				int borderWidth = 0;
				Color borderColor = null;
				for (Iterator il = list.iterator(); il.hasNext();) {
					MapToType mapTo = (MapToType) il.next();
					//System.out.println(">>> Mapping: what = <"+mapTo.getWhat()+">, value = <"+mapTo.getValue()+">");
					if (mapTo.getWhat().equalsIgnoreCase("file")) {
						iconFileName = mapTo.getValue();
					} else if (mapTo.getWhat().equalsIgnoreCase("scale_factor")) {
						try {
							scaleFactor = Float.valueOf(mapTo.getValue()).floatValue();
						} catch (NumberFormatException nfe) {
						}
					} else if (mapTo.getWhat().equalsIgnoreCase("border_thickness")) {
						try {
							borderWidth = Integer.valueOf(mapTo.getValue()).intValue();
						} catch (NumberFormatException nfe) {
						}
					} else if (mapTo.getWhat().equalsIgnoreCase("border_colour")) {
						try {
							int intVal = Integer.valueOf(mapTo.getValue()).intValue();
							borderColor = new Color(intVal);
						} catch (NumberFormatException nfe) {
						}
					}
				}
				//System.out.println("Icon file: "+iconFileName);
				//System.out.println(">>> border width "+borderWidth+" applies to "+iconFileName);
				//System.out.println(">>> border color "+borderColor+" applies to "+iconFileName);
				if (iconFileName == null) {
					continue;
				}
				iconFileName = StringUtil.removeQuotes(iconFileName);
				list = mapping.getValue();
				if (list == null || list.isEmpty()) {
					continue;
				}
				IconVisSpec ivs = new IconVisSpec();
				ivs.setPathToImage(iconFileName);
				if (!Float.isNaN(scaleFactor)) {
					ivs.setScaleFactor(scaleFactor);
					//System.out.println(">>> Scale factor "+scaleFactor+" applies to "+iconFileName);
				}
				ivs.setFrameWidth(borderWidth);
				ivs.setFrameColor(borderColor);
				for (Iterator il = list.iterator(); il.hasNext();) {
					ValueSpecType vsp = (ValueSpecType) il.next();
					String attrName = vsp.getAttribute();
					if (attrName == null) {
						attrName = (String) attrs.elementAt(0);
					}
					String value = vsp.getValue();
					if (value != null) {
						value = StringUtil.removeQuotes(value);
					}
					ivs.addAttrValuePair(attrName, value);
					//System.out.println("  shows value "+value+" of attribute "+attrName);
				}
				icor.addCorrespondence(ivs);
			}
			SimpleDataMapper dm = core.getDisplayProducer().getDataMapper();
			Object vis = dm.constructVisualizer(method, 'p');
			//System.out.println("Visualizer: "+vis);
			if (vis != null) {
				result.put("visualizer", vis);
				if (vis instanceof IconPresenter) {
					IconPresenter ipres = (IconPresenter) vis;
					ipres.setCorrespondence(icor);
				}
				return result;
			}
		} else if (method.equalsIgnoreCase("colouring")) {
			if (attrs == null) { //all objects to be shown in the same way irrespectively
				//of the thematic data
				result.put("methodId", "layer_drawing");
				for (Iterator it = lst.iterator(); it.hasNext();) {
					MappingType mapping = (MappingType) it.next();
					java.util.List list = mapping.getMapTo();
					if (list == null || list.isEmpty()) {
						continue;
					}
					for (Iterator il = list.iterator(); il.hasNext();) {
						MapToType mapTo = (MapToType) il.next();
						String strVal = mapTo.getValue();
						int intVal = 0;
						if (strVal != null) {
							try {
								intVal = Integer.valueOf(strVal).intValue();
							} catch (NumberFormatException nfe) {
							}
						}
						if (mapTo.getWhat().equalsIgnoreCase("fill_colour")) {
							Color color = new Color(intVal);
							result.put("fill_colour", color);
						} else if (mapTo.getWhat().equalsIgnoreCase("border_colour")) {
							Color color = new Color(intVal);
							result.put("border_colour", color);
						} else if (mapTo.getWhat().equalsIgnoreCase("border_thickness")) {
							result.put("border_thickness", new Integer(intVal));
						} else if (mapTo.getWhat().equalsIgnoreCase("transparency")) {
							result.put("transparency", new Integer(intVal));
						}
					}
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the extent of the currently displayed territory: x1, y1, x2, y2
	 */
	@Override
	public float[] getCurrentExtent() {
		if (core == null || core.getUI() == null)
			return null;
		MapViewer mw = core.getUI().getCurrentMapViewer();
		if (mw == null)
			return null;
		return mw.getMapExtent();
	}

	/**
	 * Returns the full extent of the whole territory: x1, y1, x2, y2
	 */
	@Override
	public float[] getFullExtent() {
		if (core == null || core.getUI() == null)
			return null;
		MapViewer mw = core.getUI().getCurrentMapViewer();
		if (mw == null)
			return null;
		LayerManager lman = mw.getLayerManager();
		if (lman == null || lman.getLayerCount() < 1)
			return null;
		float bounds[] = null;
		for (int i = 0; i < lman.getLayerCount(); i++) {
			DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(i);
			RealRectangle rb = layer.getWholeLayerBounds();
			if (rb == null) {
				continue;
			}
			if (bounds == null) {
				bounds = new float[4];
				bounds[0] = rb.rx1;
				bounds[1] = rb.ry1;
				bounds[2] = rb.rx2;
				bounds[3] = rb.ry2;
			} else {
				if (bounds[0] > rb.rx1) {
					bounds[0] = rb.rx1;
				}
				if (bounds[1] > rb.ry1) {
					bounds[1] = rb.ry1;
				}
				if (bounds[2] < rb.rx2) {
					bounds[2] = rb.rx2;
				}
				if (bounds[3] < rb.ry2) {
					bounds[3] = rb.ry2;
				}
			}
		}
		return bounds;
	}

	/**
	 * Shows the territory fragment specified by the given bounding rectangle
	 */
	@Override
	public void setCurrentExtent(float x1, float y1, float x2, float y2) {
		if (core == null || core.getUI() == null)
			return;
		MapViewer mw = core.getUI().getCurrentMapViewer();
		if (mw == null)
			return;
		mw.showTerrExtent(x1, y1, x2, y2);
	}

	/**
	 * Creates a map view and opens the specified map project path
	 */
	@Override
	public boolean openMapView(String projectPath) {
		if (core == null) {
			runDescartes(projectPath);
		}
		return core != null && core.getUI() != null && core.getUI().getCurrentMapViewer() != null;
	}

	/**
	 * Allows the user to enter a point by clicking on a map.
	 * When finished, notifies the listeners by sending them a property change
	 * event with the property name "point_entered" and the new value containing
	 * the coordinates in an array of two doubles.
	 */
	@Override
	public boolean enterPointOnMap(String winTitle, String prompt) {
		if (core == null || core.getUI() == null || core.getUI().getCurrentMapViewer() == null)
			return false;
		EnterPointOnMapUI enterPointUI = new EnterPointOnMapUI();
		enterPointUI.setOwner(this);
		enterPointUI.setPromptText(prompt);
		enterPointUI.setWindowTitle(winTitle);
		enterPointUI.run(core);
		return true;
	}

	/**
	 * Through this method the EnterPointOnMapUI notifies the owner about
	 * a point having been entered. DescartesForOASIS notifies its listeners by
	 * sending them a property change event with the property name "point_entered"
	 * and the new value containing the coordinates in an array of two doubles.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if ((e.getSource() instanceof EnterPointOnMapUI) && e.getActionCommand().equalsIgnoreCase("make_point")) {
			EnterPointOnMapUI enterPointUI = (EnterPointOnMapUI) e.getSource();
			RealPoint point = enterPointUI.getPoint();
			if (point == null)
				return;
			enterPointUI.restorePoint();
			double coord[] = { point.x, point.y };
			if (listeners != null && listeners.size() > 0) {
				PropertyChangeEvent pce = new PropertyChangeEvent(this, "point_entered", null, coord);
				for (int i = 0; i < listeners.size(); i++) {
					((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
				}
			}
		}
	}

	/**
	 * Creates a map image and saves it to the specified file.
	 * @param path - the file in which the image must be saved
	 * @param format - the required format of the image, one of BMP, JPG, or PNG
	 * @param maxWidth - the limitation of the width of the resulting image, in
	 * pixels. If negative or zero, the width is unlimited and must be equal to
	 * the current width of the map on the screen.
	 * @param maxHeight - the limitation of the height of the resulting image, in
	 * pixels. If negative or zero, the width is unlimited and must be equal to
	 * the current width of the map on the screen.
	 * @param exclude - a list of identifiers of the layers which must NOT be
	 * included in the generation of the image
	 * @return true if successfully generated and saved.
	 */
	@Override
	public boolean makeMapImage(String path, String format, int maxWidth, int maxHeight, Vector exclude) {
		if (path == null)
			return false;
		if (core == null || core.getUI() == null)
			return false;
		MapViewer mw = core.getUI().getCurrentMapViewer();
		if (mw == null || mw.getMapDrawer() == null)
			return false;
		//find out the current size of the screen map and use it to define the
		//size of the image
		MapDraw map = mw.getMapDrawer();
		Dimension size = map.getMapSize();
		if (size == null || size.width < 20 || size.height < 20)
			return false;
		if ((maxWidth > 0 && maxWidth < size.width) || (maxHeight > 0 && maxHeight < size.height)) {
			double ratio = ((double) size.width) / size.height;
			if (maxWidth <= 0) {
				maxWidth = (int) Math.round(ratio * maxHeight);
			} else if (maxHeight <= 0) {
				maxHeight = (int) Math.round(maxWidth / ratio);
			} else {
				float scaleW = ((float) maxWidth) / size.width, scaleH = ((float) maxHeight) / size.height;
				if (scaleW < scaleH) {
					maxHeight = (int) Math.round(maxWidth / ratio);
				} else {
					maxWidth = (int) Math.round(ratio * maxHeight);
				}
			}
		} else {
			maxWidth = size.width;
			maxHeight = size.height;
		}
		Image img = null;
		if (map instanceof Component) {
			img = ((Component) map).createImage(maxWidth, maxHeight);
		}
		if (img == null && (mw instanceof Component)) {
			img = ((Component) mw).createImage(maxWidth, maxHeight);
		}
		if (img == null && core.getUI().getMainFrame() != null) {
			img = core.getUI().getMainFrame().createImage(maxWidth, maxHeight);
		}
		if (img == null)
			return false;

		boolean mapDrawn = false;
		if (exclude != null && exclude.size() > 0) {
			//draw the map without the layers that must be excluded
			LayerManager lman = mw.getLayerManager();
			if (lman != null && lman.getLayerCount() > 0) {
				int exclIndexes[] = new int[exclude.size()];
				int nExcluded = 0;
				for (int i = 0; i < lman.getLayerCount(); i++) {
					DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(i);
					if (layer.getLayerDrawn() && exclude.contains(layer.getEntitySetIdentifier())) {
						layer.getDrawingParameters().drawLayer = false;
						exclIndexes[nExcluded++] = i;
					}
				}
				map.paintToImage(img, maxWidth, maxHeight);
				mapDrawn = true;
				for (int i = 0; i < nExcluded; i++) {
					DGeoLayer layer = (DGeoLayer) lman.getGeoLayer(exclIndexes[i]);
					layer.getDrawingParameters().drawLayer = true;
				}
			}
		}
		if (!mapDrawn) {
			map.paintToImage(img, maxWidth, maxHeight);
		}

		if (format == null) {
			format = "PNG";
		}
		String className = null;
		if (format.equalsIgnoreCase("PNG")) {
			className = "ui.bitmap.SavePNGImage";
		} else if (format.equalsIgnoreCase("BMP")) {
			className = "ui.bitmap.SaveBMPImage";
		} else if (format.equalsIgnoreCase("JPG") || format.equalsIgnoreCase("JPEG")) {
			className = "ui.bitmap.SaveJPEGImage";
		}
		if (className == null) {
			format = "PNG";
			className = "ui.bitmap.SavePNGImage";
		}
		ImageSaver imgSaver = null;
		try {
			imgSaver = (ImageSaver) Class.forName(className).newInstance();
		} catch (Throwable ex) {
			ex.printStackTrace();
			return false;
		}
		if (imgSaver == null || !imgSaver.isAvailable())
			return false;

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(path);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		if (fos == null)
			return false;

		imgSaver.saveImage(fos, img, maxWidth, maxHeight);

		try {
			fos.close();
		} catch (IOException ioe) {
		}

		return true;
	}

	/**
	 * Listeners of the status of the visualiser
	 */
	protected Vector listeners = null;

	/**
	 * Registers a listener of its status. The listener must be informed, in
	 * particular, when the visualiser stops its weok. In this case, the
	 * visualiser must send a PropertyChangeEvent with the property name "status"
	 * and the new value "finished".
	 */
	@Override
	public void addStatusListener(PropertyChangeListener listener) {
		if (listener == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (listeners.contains(listener))
			return;
		listeners.addElement(listener);
	}

	/**
	* Closes the main window of the system and all additional displays.
	*/
	public void quit() {
		if (mainWin != null) {
			mainWin.dispose();
		}
		boolean mayExit = !core.getSystemSettings().checkParameterValue("runsInsideOtherSystem", "true");
		notifyWorkFinish();
		mainWin = null;
		core = null;
		if (mayExit) {
			System.exit(0);
		}
	}

	/**
	 * Notifies the listeners, if any, about the work having stopped
	 */
	protected void notifyWorkFinish() {
		if (listeners != null && listeners.size() > 0) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this, "status", null, "finished");
			for (int i = 0; i < listeners.size(); i++) {
				((PropertyChangeListener) listeners.elementAt(i)).propertyChange(pce);
			}
		}
		listeners = null;
	}

	/**
	 * Removes the status listener, in particular, when the listener has stopped
	 * its functioning.
	 */
	@Override
	public void removeStatusListener(PropertyChangeListener listener) {
		if (listener == null || listeners == null)
			return;
		int idx = listeners.indexOf(listener);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
		if (listeners.size() < 1) {
			core.getSystemSettings().setParameter("runsInsideOtherSystem", "false");
			listeners = null;
		}
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource() == mainWin) {
			quit();
			return;
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}
