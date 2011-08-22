package spade.analysis.vis3d;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FocusListener;
import spade.lib.basicwin.Focuser;
import spade.lib.basicwin.FocuserCanvas;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.OKDialog;
import spade.lib.basicwin.Slider;
import spade.lib.basicwin.SplitPanel;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.lib.util.NumRange;
import spade.lib.util.StringUtil;
import spade.time.TimeMoment;
import spade.vis.database.AttributeDataPortion;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealRectangle;
import spade.vis.map.MapDraw;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.space.ObjectManager;
import ui.AttributeChooser;

/*
* This is slightly modified standard Focuser
* Changes:
* - if 2 deliniters are moving simultaneously only one notification
*   about focus change is sent to all listeners
* - if Focuser has text fields connected they are updated
*   using StringUtil.FloatToStr function (needed to format numbers)
*/
class MyFocuser extends Focuser {
	private int nLimitMovings = 0;
	private boolean dynamicNotifyFocusChange = false;

	public MyFocuser() {
		super();
	}

	public void setNotifyFocusChangeDynamically(boolean isAllowed) {
		dynamicNotifyFocusChange = isAllowed;
	}

	protected void notifyLimitMoving(int n, float currValue) {
		if (minDelimMoving && maxDelimMoving) {
			if (nLimitMovings == 0) {
				nLimitMovings++;
				return;
			} else if (nLimitMovings == 1) {
				nLimitMovings = 0;
				setTextsInTextFields();
				if (dynamicNotifyFocusChange) {
					notifyFocusChange();
				}
			}
			return;
		}
		super.notifyLimitMoving(n, currValue);
	}

	@Override
	protected void setTextsInTextFields() {
		if (minTF != null && maxTF != null)
			if (currMinTime != null && currMaxTime != null) {
				minTF.setText(currMinTime.toString());
				maxTF.setText(currMaxTime.toString());
			} else {
				minTF.setText(StringUtil.doubleToStr(currMin, absMin, absMax));
				maxTF.setText(StringUtil.doubleToStr(currMax, absMin, absMax));
			}
	}
}

public class Sample3DView extends SplitPanel implements Destroyable, ItemListener, ActionListener, FocusListener, PropertyChangeListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.vis3d.Res");
	private ESDACore core = null;
	// interface
	public boolean destroyed = false;
	public Color bgcLabels = Color.getHSBColor(0.0f, 0.0f, 0.8f);
	protected Panel pControl = null;
	protected Panel p3D = null;
	protected Slider slMapPos = null;
	protected TextField tfCurrMapPos = null, tfMinMapPos = null, tfMaxMapPos = null;
	protected FlatControl fc = null;
	protected HeightControl hc = null;
	protected Choice chProjSelector = null;
	protected Checkbox cbDynUpdate = null, cbClassicFocussing = null, cbDrawFrame = null, cbDrawLines = null, cbDrawAreasAsPoints = null, cbInvertAttrValues = null, cbStretchZooming = null, cbStickMap = null;
	protected boolean canSwitchToPoints = true, inverseRepresentation = false;
	private Frame f3DView = null; // if Sample3DView panel has been put in a window

	// Data
	private LayerManager3D lManager = null;

	protected int selectedAttrIdx = -1;
	protected int selectedLayerIdx = -1;

	protected String attrId = null, attrName = null;
	protected MapCanvas3D c3DView = null;
	protected MyFocuser f = null;
	protected double minZ, maxZ;
	protected float zFactor = Float.NaN, zdif = Float.NaN;
	/**
	 * Indicates whether the attribute used in the vertical dimension is temporal
	 */
	protected boolean isAttrTemporal = false;
	/**
	 * If the attribute used in the vertical dimension is temporal, these
	 * fields contain the minimum and maximum time moments
	 */
	protected TimeMoment minTime = null, maxTime = null;

	protected Label lAttrName = null;

	boolean initOK = true;

	public Sample3DView(ESDACore core, DLayerManager lman) {
		super(true);
		AttributeDataPortion table = null;
		RealRectangle rr = null;
		this.core = core;
		Supervisor sup = core.getSupervisor();

		//------------- make new GeoLayer with 3D data representation: DGeoLayerZ
		GeoLayer gl = selectLayer(lman); // first, select base layer
		if (gl == null) {
			initOK = !initOK;
			return;
		}
		table = gl.getThematicData();
		if (table == null) {
			initOK = !initOK;
			return;
		}
		attrId = selectAttribute(table); // second: select attribute to show in 3D
		if (attrId == null) {
			initOK = !initOK;
			return;
		}
		if (!gl.hasThematicData(table)) {
			gl.receiveThematicData(table);
		}
		// calculate sizes of XY and Z dimensions
		rr = lman.getWholeTerritoryBounds();
		if (rr == null) {
			rr = lman.getCurrentTerritoryBounds();
		}
		if (rr == null) {
			System.out.println("Cannot start the PerspectiveView tool correctly: do not know territory bounds!");
			rr = new RealRectangle();
		}
		float xdif = rr.rx2 - rr.rx1, ydif = rr.ry2 - rr.ry1;
		zdif = (xdif > ydif) ? xdif : ydif;
		//zdif=(xdif<ydif)?xdif:ydif;
		DGeoLayerZ zlayer = setupZLayer(lman, (DGeoLayer) gl); // setup DGeoLayerZ

		//------------ setting up interface
		float x_pos_init = (rr.rx2 + rr.rx1) / 2 - 3 * Math.max(Math.abs(xdif), Math.abs(ydif)) / 2, y_pos_init = rr.ry1 - Math.max(Math.abs(xdif), Math.abs(ydif));
		EyePosition viewerPosition = new EyePosition(x_pos_init, y_pos_init, zdif / 2);

		fc = new FlatControl(viewerPosition, rr.rx1, rr.rx2, rr.ry1, rr.ry2, 0, zdif);
		hc = new HeightControl(viewerPosition, rr.rx1, rr.rx2, rr.ry1, rr.ry2, 0, zdif);
		c3DView = new MapCanvas3D(viewerPosition, 0f, zdif);
		makeInterface(c3DView);

		// ------------------------------------------ finalize 3D view setup
		ObjectManager3D objMan = new ObjectManager3D(sup);
		objMan.setMap(c3DView);
		objMan.setGeoLayer(zlayer);
		objMan.setObjectEventHandler(sup); //object events will be sent to the supervisor
		objMan.setIDAttribute3D(attrId);
		lManager.setObjectManager(objMan);
		c3DView.setMapContent(lManager);
		lManager.setUsePointsInsteadOfAreas(cbDrawAreasAsPoints.getState());
		lManager.setDrawBoundingFrame(cbDrawFrame.getState());
		lManager.setDrawProjectionLines(cbDrawLines.getState());
		f3DView = core.getDisplayProducer().makeWindow(this,
		// following text: "3D View: "
				res.getString("3D_View_") + attrName);
		table.addPropertyChangeListener(this);
	}

	protected GeoLayer selectLayer(LayerManager lman) {
		List lst = new List(8);
		IntArray lNums = new IntArray(20, 10);

		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer gl = lman.getGeoLayer(i);
			if (gl.getLayerDrawn() && (gl.getType() == Geometry.point || gl.getType() == Geometry.line || gl.getType() == Geometry.area) && gl.getThematicData() != null) {
				lNums.addElement(i);
				lst.add(gl.getName());
			}
		}
		if (lNums.size() < 1) {
			if (core.getUI() != null) {
				// following text:"No map layers of appropriate type!"
				core.getUI().showMessage(res.getString("No_map_layers_of"), true);
			}
			return null;
		}
		lst.select(0);
		Panel p = new Panel(new BorderLayout());
		// following text:"Select the layer to show in 3D:"
		p.add(new Label(res.getString("Select_the_layer_to")), "North");
		p.add(lst, "Center");

		Frame fr = CManager.getFrame(this);
		if (fr == null) {
			fr = core.getUI().getMainFrame();
		}
		// following text:"Select the layer to show in 3D"
		OKDialog okd = new OKDialog(fr, res.getString("Select_the_layer_to1"), true);
		okd.addContent(p);
		okd.show();
		if (okd.wasCancelled())
			return null;
		selectedLayerIdx = lNums.elementAt(lst.getSelectedIndex());
		GeoLayer gl = lman.getGeoLayer(selectedLayerIdx);
		return gl;
	}

	public String selectAttribute(AttributeDataPortion tbl) {
		AttributeChooser attrSel = new AttributeChooser();
		if (attrSel.selectColumns(tbl, null, null, true, res.getString("Select_the_attribute"), null) == null)
			return null;
		Vector colIds = attrSel.getSelectedColumnIds();
		if (colIds == null || colIds.size() < 1)
			return null;
		String attrId = (String) colIds.elementAt(0);
		if (colIds.size() > 1) { //there must be a single attribute!
			List lstAttr = new List(10);
			IntArray idxAttr = new IntArray(20, 10);

			for (int i = 0; i < colIds.size(); i++) {
				int idx = tbl.getAttrIndex((String) colIds.elementAt(i));
				if (idx < 0) {
					continue;
				}
				lstAttr.add(tbl.getAttributeName(idx));
				idxAttr.addElement(idx);
			}
			if (idxAttr.size() < 1)
				return null;
			lstAttr.select(0);
			Panel pSelectAttr = new Panel(new BorderLayout());
			pSelectAttr.add(new Label(res.getString("Select_single_attribute")), "North");
			pSelectAttr.add(lstAttr, "Center");
			Frame fr = CManager.getFrame(this);
			if (fr == null) {
				fr = core.getUI().getMainFrame();
			}
			OKDialog okd = new OKDialog(fr, res.getString("Select_single_attribute"), true);
			okd.addContent(pSelectAttr);
			okd.show();
			if (okd.wasCancelled())
				return null;
			selectedAttrIdx = idxAttr.elementAt(lstAttr.getSelectedIndex());
			attrId = tbl.getAttributeId(selectedAttrIdx);
		} else {
			selectedAttrIdx = tbl.getAttrIndex(attrId);
		}
		attrName = tbl.getAttributeName(selectedAttrIdx);
		isAttrTemporal = tbl.isAttributeTemporal(selectedAttrIdx);
		if (lManager != null) {
			ObjectManager oMan = lManager.getObjectManager();
			if (oMan instanceof ObjectManager3D) {
				((ObjectManager3D) oMan).setIDAttribute3D(attrId);
			}
		}
		return attrId;
	}

	protected void setupAttribute(DGeoLayerZ layer) {
		minZ = Float.NaN;
		maxZ = minZ;
		minTime = null;
		maxTime = null;
		layer.setZAttrIndex(selectedAttrIdx);
		for (int i = 0; i < layer.getObjectCount(); i++) {
			DGeoObjectZ gobjZ = (DGeoObjectZ) layer.getObject(i);
			if (gobjZ == null || gobjZ.getData() == null) {
				continue;
			}
			// update 3DGeoObjects with absolute attribute values
			//gobjZ.setZPosition(0.0f);
			gobjZ.setZPosition(Float.NaN);
			gobjZ.setZAttrValue(Float.NaN);
			double val = gobjZ.getData().getNumericAttrValue(selectedAttrIdx);
			if (Double.isNaN(val)) {
				continue;
			}
			gobjZ.setZPosition(val);
			gobjZ.setZAttrValue(val);
			if (Double.isNaN(minZ) || minZ > val) {
				minZ = val;
			}
			if (Double.isNaN(maxZ) || maxZ < val) {
				maxZ = val;
			}
			if (isAttrTemporal) {
				Object oval = gobjZ.getData().getAttrValue(selectedAttrIdx);
				if (oval != null && (oval instanceof TimeMoment)) {
					TimeMoment t = (TimeMoment) oval;
					if (minTime == null || minTime.compareTo(t) > 0) {
						minTime = t;
					}
					if (maxTime == null || maxTime.compareTo(t) < 0) {
						maxTime = t;
					}
				}
			}
		}
		if (minTime != null) {
			minTime = minTime.getCopy();
		}
		if (maxTime != null) {
			maxTime = maxTime.getCopy();
		}
		// make Z-values relative for 3D-presentation
		updateZValues(minZ, maxZ);
		updateInterface();
	}

	protected DGeoLayerZ setupZLayer(DLayerManager lman, DGeoLayer gl) {
		DGeoLayerZ layer = new DGeoLayerZ();
		layer.setContainerIdentifier(gl.getContainerIdentifier());
		layer.setEntitySetIdentifier(gl.getEntitySetIdentifier());
		layer.setDrawingParameters(gl.getDrawingParameters());

		// required for ObjectSet event reaction
		gl.addPropertyChangeListener(this);
		// ~
		minZ = Float.NaN;
		maxZ = minZ;
		minTime = null;
		maxTime = null;

		// Handle selected attribute
		layer.setZAttrIndex(selectedAttrIdx);
		for (int i = 0; i < gl.getObjectCount(); i++) {
			DGeoObject gobj = gl.getObject(i);
			if (gobj == null || gobj.getData() == null) {
				continue;
			}
			double val = gobj.getData().getNumericAttrValue(selectedAttrIdx);
			if (Double.isNaN(val)) {
				continue;
			}
			DGeoObjectZ zobj = new DGeoObjectZ(gobj);
			// initialize 3DGeoObjects with absolute attribute values
			zobj.setZPosition(val);
			zobj.setZAttrValue(val);
			if (Double.isNaN(minZ) || minZ > val) {
				minZ = val;
			}
			if (Double.isNaN(maxZ) || maxZ < val) {
				maxZ = val;
			}
			if (isAttrTemporal) {
				Object oval = gobj.getData().getAttrValue(selectedAttrIdx);
				if (oval != null && (oval instanceof TimeMoment)) {
					TimeMoment t = (TimeMoment) oval;
					if (minTime == null || minTime.compareTo(t) > 0) {
						minTime = t;
					}
					if (maxTime == null || maxTime.compareTo(t) < 0) {
						maxTime = t;
					}
				}
			}
			layer.addGeoObject(zobj);
		}
		if (minTime != null) {
			minTime = minTime.getCopy();
		}
		if (maxTime != null) {
			maxTime = maxTime.getCopy();
		}

		lManager = new LayerManager3D();
		lManager.terrName = lman.terrName;
		lManager.user_factor = lman.user_factor;
		lManager.setUserUnit(lman.getUserUnit());
		lManager.setGeographic(lman.isGeographic(), false);
		lManager.initialExtent = lman.initialExtent;
		lManager.fullExtent = lman.fullExtent;

		for (int i = 0; i < lman.getLayerCount(); i++) {
			GeoLayer l = lman.getLayer(i);
			if (l.getType() != Geometry.image && l.getType() != Geometry.raster) {
				if (l.equals(gl) && l.getType() == Geometry.point) {
					continue;
				}
				DGeoLayer dl = (DGeoLayer) l.makeCopy();
				if (dl == null) {
					continue;
				}
				dl.setVisualizer(null);
				dl.setBackgroundVisualizer(null);
				lManager.addGeoLayer(dl);
			}
		}

		if (layer.getObjectCount() > 0) {
			layer.setDataTable(gl.getThematicData());
			layer.setObjectFilter(gl.getObjectFilter());
			layer.setVisualizer(gl.getVisualizer());
			lManager.setSpecialLayer(layer, gl);
			// make Z-values relative for 3D-presentation
			updateZValues(minZ, maxZ);
			canSwitchToPoints = !(layer.getType() == Geometry.point || (layer.getVisualizer() != null ? layer.getVisualizer().isDiagramPresentation() : false));
		}
		// System.out.println("Setup ZLayer:: "+layer.getObjectCount()+" Z objects created");
		return layer;
	}

	private void makeInterface(MapCanvas3D c3DView) {
		p3D = new Panel(new BorderLayout());
		addSplitComponent(p3D, 0.75f);

		int tfLen = 7;
		if (minTime != null) {
			tfLen = Math.max(tfLen, minTime.toString().length());
		}
		if (maxTime != null) {
			tfLen = Math.max(tfLen, maxTime.toString().length());
		}
		tfCurrMapPos = new TextField(tfLen);
		if (minTime != null) {
			tfCurrMapPos.setText(minTime.toString());
		} else {
			tfCurrMapPos.setText(Double.toString(minZ));
		}
		tfCurrMapPos.addActionListener(this);
		tfMinMapPos = new TextField(tfLen);
		if (minTime != null) {
			tfMinMapPos.setText(minTime.toString());
		} else {
			tfMinMapPos.setText(Double.toString(minZ));
		}
		tfMinMapPos.addActionListener(this);
		tfMaxMapPos = new TextField(tfLen);
		if (maxTime != null) {
			tfMaxMapPos.setText(maxTime.toString());
		} else {
			tfMaxMapPos.setText(Double.toString(maxZ));
		}
		tfMaxMapPos.addActionListener(this);
		CheckboxGroup cbgProjection = new CheckboxGroup();

		// following text:"Projection lines"
		cbDrawLines = new Checkbox(res.getString("Projection_lines"), false);
		// following text:"Bounding frame"
		cbDrawFrame = new Checkbox(res.getString("Bounding_frame"), true);
		// following text:"Perspective"
		Checkbox cbPersProj = new Checkbox(res.getString("Perspective"), cbgProjection, true);
		// following text:"Parallel"
		Checkbox cbParProj = new Checkbox(res.getString("Parallel"), cbgProjection, false);
		//Checkbox
		// following text:"Invert attribute values"
		cbInvertAttrValues = new Checkbox(res.getString("Invert_attribute"), inverseRepresentation);
		// following text:"Points vs. polygons"
		cbDrawAreasAsPoints = new Checkbox(res.getString("Points_vs_polygons"), true);
		// following text:"Dynamic update"
		cbDynUpdate = new Checkbox(res.getString("Dynamic_update"), false);
		// following text:"Stretch in Z-dimension"
		cbClassicFocussing = new Checkbox(res.getString("Stretch_in_Z"), false);
		cbStickMap = new Checkbox(res.getString("Stick_map_to"), true);
		cbStretchZooming = new Checkbox(res.getString("Stretch_in_XY"), true);

		cbDrawLines.setName("DrawProjectionLines");
		cbDrawFrame.setName("DrawBoundingFrame");
		cbPersProj.setName("PerspectiveProjection");
		cbParProj.setName("ParallelProjection");
		cbInvertAttrValues.setName("InvertAttrValues");
		cbDynUpdate.setName("DynamicUpdateComp");
		cbClassicFocussing.setName("ClassicFocussing");
		cbDrawAreasAsPoints.setName("UsePointsInsteadOfAreas");
		cbStickMap.setName("StickMap");
		cbStretchZooming.setName("StretchZooming");
		cbDrawAreasAsPoints.setEnabled(canSwitchToPoints);
		cbDrawAreasAsPoints.setVisible(canSwitchToPoints);
		cbStickMap.setEnabled(cbClassicFocussing.getState());
		cbStickMap.setState(cbClassicFocussing.getState());
		/** interface for selection of parallel projection type
		chProjSelector=new Choice();
		chProjSelector.addItem("Orthographic");
		chProjSelector.addItem("Cavalier");
		chProjSelector.addItem("Cabinet");
		chProjSelector.setEnabled(cbParProj.getState());
		chProjSelector.addItemListener(this);
		**/
		// following text: "Parallel orthographic"
		if (chProjSelector == null) {
			cbParProj.setLabel(res.getString("Parallel_orthographic"));
		}
		cbDrawLines.addItemListener(this);
		cbDrawFrame.addItemListener(this);
		cbInvertAttrValues.addItemListener(this);
		cbDynUpdate.addItemListener(this);
		cbPersProj.addItemListener(this);
		cbParProj.addItemListener(this);
		cbDrawAreasAsPoints.addItemListener(this);
		cbClassicFocussing.addItemListener(this);
		cbStickMap.addItemListener(this);
		cbStretchZooming.addItemListener(this);

		//------------------------- make panel with focussing and comparison
		slMapPos = new Slider(this, (float) minZ, (float) maxZ, (float) minZ);
		if (minTime != null && maxTime != null) {
			slMapPos.setAbsMinMaxTime(minTime, maxTime);
		}
		slMapPos.setNAD(cbDynUpdate.getState());
		slMapPos.setBackground(Color.lightGray);
		slMapPos.setShowMinMaxLabels(false);

		Panel pFSWest = null, pFSCenter = null, pFSEast = null;
		pFSWest = new Panel();
		pFSCenter = new Panel();
		pFSEast = new Panel();

		ColumnLayout lPFSCenter = null;
		GridLayout lPFSWest = null, lPFSEast = null;

		lPFSWest = new GridLayout(2, 1, 0, 0);
		lPFSCenter = new ColumnLayout();
		lPFSEast = new GridLayout(2, 1, 0, 0);

		lPFSCenter.setAlignment(ColumnLayout.Hor_Stretched);

		pFSWest.setLayout(lPFSWest);
		pFSCenter.setLayout(lPFSCenter);
		pFSEast.setLayout(lPFSEast);

		Panel pAnalysis = new Panel(new BorderLayout());

		f = new MyFocuser();
		f.setAbsMinMax((float) minZ, (float) maxZ);
		if (minTime != null && maxTime != null) {
			f.setAbsMinMaxTime(minTime, maxTime);
		}
		f.addFocusListener(this);
		f.setTextDrawing(true);
		f.setCurrMinMax((float) minZ, (float) maxZ);
		f.setSpacingFromAxis(7);
		f.setIsLeft(true);
		f.setAttributeNumber(selectedAttrIdx);
		f.setIsUsedForQuery(true);
		f.setTextFields(tfMinMapPos, tfMaxMapPos);

		FocuserCanvas fCanvas = new FocuserCanvas(f, false);
		f.setCanvas(fCanvas);

		pFSWest.add(tfMinMapPos);
		pFSWest.add(new Label(res.getString("Compare_to_"), Label.LEFT));
		pFSCenter.add(fCanvas);
		pFSCenter.add(slMapPos);
		pFSEast.add(tfMaxMapPos);
		pFSEast.add(tfCurrMapPos);

		pAnalysis.add(pFSWest, "West");
		pAnalysis.add(pFSCenter, "Center");
		pAnalysis.add(pFSEast, "East");

		//----------------- add other optional interface controls
		// following text:"Focussing:"
		Label lDefault = new Label(res.getString("Focussing_"), Label.CENTER);
		lDefault.setBackground(bgcLabels);
		Panel pOptions = new Panel();
		ColumnLayout cl = new ColumnLayout();
		cl.setAlignment(ColumnLayout.Hor_Stretched);
		pOptions.setLayout(cl);
		pOptions.add(new Line(false));
		pOptions.add(lDefault);
		pOptions.add(cbClassicFocussing);
		pOptions.add(cbStickMap);
		pOptions.add(new Line(false));
		// following text:"Options:"
		lDefault = new Label(res.getString("Options_"), Label.CENTER);
		lDefault.setBackground(bgcLabels);

		pOptions.add(lDefault);
		pOptions.add(cbDynUpdate);
		pOptions.add(cbStretchZooming);
		pOptions.add(cbDrawLines);
		pOptions.add(cbDrawFrame);
		pOptions.add(cbDrawAreasAsPoints);
		// -------------- finally build the whole 3D control panel
		pControl = new Panel();
		pControl.setLayout(new BorderLayout());
		Panel pNavCtrls = new Panel(new GridLayout(2, 1, 0, 0));
		// following text:"Viewpoint control"
		lDefault = new Label(res.getString("Viewpoint_control"), Label.CENTER);
		lDefault.setBackground(bgcLabels);

		pControl.add(lDefault, "North");
		pNavCtrls.add(fc);
		pNavCtrls.add(hc);
		pControl.add(pNavCtrls, "Center");
		Panel p = new Panel(new ColumnLayout());
		Panel pParallel = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pParallel.add(cbParProj);
		if (chProjSelector != null) {
			pParallel.add(chProjSelector);
		}
		Panel pAttrSelect = new Panel(new BorderLayout());
		// following text:"Attribute: "
		lDefault = new Label(res.getString("Attribute_"), Label.CENTER);
		lDefault.setBackground(bgcLabels);
		pAttrSelect.add(lDefault, "North");
		String aName = (attrName != null ? attrName : attrId);
		lAttrName = new Label(aName, Label.CENTER);
		// following text:"Change"
		Button bAttrChange = new Button(res.getString("Change"));
		bAttrChange.addActionListener(this);
		pAttrSelect.add(lAttrName, "Center");
		pAttrSelect.add(bAttrChange, "East");
		pAttrSelect.add(cbInvertAttrValues, "South");
		p.add(new Line(false));
		p.add(pAttrSelect);
		p.add(new Line(false));
		// following text: "Projection"
		lDefault = new Label(res.getString("Projection"), Label.CENTER);
		lDefault.setBackground(bgcLabels);
		p.add(lDefault);
		p.add(cbPersProj);
		p.add(pParallel);
		p.add(pOptions);
		pControl.add(p, "South");
		addSplitComponent(pControl, 0.25f);
		//----------------------------------------------- adding MapCanvas3D
		p = new Panel();
		ColumnLayout clMainWin = new ColumnLayout();
		clMainWin.setAlignment(ColumnLayout.Hor_Stretched);
		p.setLayout(clMainWin);
		p.add(pAnalysis);

		p3D.add(p, "North");
		p3D.add(c3DView, "Center");
		fc.addEyePositionListener(c3DView);
		fc.addEyePositionListener(hc);
		hc.addEyePositionListener(c3DView);
		fc.setAllowDynamicUpdate(cbDynUpdate.getState());
		hc.setAllowDynamicUpdate(cbDynUpdate.getState());
	}

	@Override
	public void destroy() {
		lManager.destroy();
		destroyed = true;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (c3DView == null)
			return;
		MapMetrics3D mmetr3D = (MapMetrics3D) c3DView.getMapContext();
		if (mmetr3D == null)
			return;
		Object src = ae.getSource();
		if (src instanceof Button) {
			// call change attribute dialog here
			changeAttribute();
			return;
		} else if ((src instanceof TextField) && slMapPos != null) {
			TextField tfTemp = (TextField) src;
			if (tfTemp == this.tfCurrMapPos) {
				String sValue = tfTemp.getText();
				double fValue = Double.NaN;
				if (minTime != null) {
					TimeMoment t = minTime.getCopy();
					if (t.setMoment(sValue)) {
						fValue = t.toNumber();
					}
				}
				if (Double.isNaN(fValue)) {
					try {
						fValue = Double.valueOf(sValue).doubleValue();
					} catch (NumberFormatException nfe) {
						fValue = slMapPos.getValue();
					}
				}
				if (fValue != slMapPos.getValue() && fValue >= slMapPos.getAbsMin() && fValue <= slMapPos.getAbsMax()) {
					slMapPos.setValue(fValue);
					c3DView.setZ0((float) (zFactor * Math.abs(fValue - minZ)), (float) fValue);
				}
			}
		} else if ((src instanceof Slider) && tfCurrMapPos != null) {
			c3DView.setZ0((float) (zFactor * (slMapPos.getValue() - minZ)), (float) slMapPos.getValue());
			if (minTime != null) {
				tfCurrMapPos.setText(minTime.valueOf((long) slMapPos.getValue()).toString());
			} else {
				tfCurrMapPos.setText(StringUtil.doubleToStr(slMapPos.getValue(), slMapPos.getAbsMin(), slMapPos.getAbsMax()));
			}
		}
	}

	@Override
	public void focusChanged(Object source, double lowerLimit, double upperLimit) {
		if (c3DView == null)
			return;
		MapMetrics3D mmetr3D = (MapMetrics3D) c3DView.getMapContext();
		if (mmetr3D == null)
			return;
		//System.out.println("3DView:: focusMin: "+lowerLimit+"focusMax: "+upperLimit);
		if (source instanceof MyFocuser) {
			if (lManager != null && lManager.getSpecialLayer() != null) {
				((DGeoLayerZ) lManager.getSpecialLayer()).setFocusChanged(lowerLimit, upperLimit);
			}
			if (cbClassicFocussing.getState()) {
				// needed for standard focussing only
				updateZValues(lowerLimit, upperLimit);
				slMapPos.setValues(lowerLimit, upperLimit, slMapPos.getValue());
				if (slMapPos.getValue() < lowerLimit) {
					slMapPos.setValue(lowerLimit);
				} else if ((slMapPos.getValue() > lowerLimit) && (slMapPos.getValue() < upperLimit) && cbStickMap.getState()) {
					//slMapPos.setValue(lowerLimit);
					slMapPos.setValue(upperLimit == slMapPos.getValue() ? upperLimit : lowerLimit);
				} else if (slMapPos.getValue() > upperLimit) {
					slMapPos.setValue(cbStickMap.getState() ? lowerLimit : upperLimit);
				}
				if (minTime != null) {
					tfCurrMapPos.setText(minTime.valueOf((long) slMapPos.getValue()).toString());
				} else {
					tfCurrMapPos.setText(StringUtil.doubleToStr(slMapPos.getValue(), slMapPos.getAbsMin(), slMapPos.getAbsMax()));
				}

				double newZ0 = zFactor * (slMapPos.getValue() - minZ);
				if (newZ0 > mmetr3D.getMaxZ()) {
					newZ0 = mmetr3D.getMaxZ();
				}
				if (newZ0 < mmetr3D.getMinZ()) {
					newZ0 = mmetr3D.getMinZ();
				}
				mmetr3D.setZ0(newZ0);
			}
			mmetr3D.setZLimits(zFactor * (lowerLimit - minZ), zFactor * (upperLimit - minZ));
			if (cbClassicFocussing.getState()) {
				mmetr3D.computeScaleFactor();
			}
			lManager.notifyPropertyChange("content", null, null, true, false);
		}
	}

	@Override
	public void limitIsMoving(Object source, int n, double currValue) {
		if (!cbDynUpdate.getState())
			return;
		//System.out.println("Limit is moving...");

		if (c3DView == null)
			return;
		MapMetrics3D mmetr3D = (MapMetrics3D) c3DView.getMapContext();
		if (mmetr3D == null)
			return;
		if (source instanceof Focuser) {
			if (n == 0) {
				if (lManager != null && lManager.getSpecialLayer() != null) {
					((DGeoLayerZ) lManager.getSpecialLayer()).setMinLimitChanged(currValue);
				}
				if (cbClassicFocussing.getState()) {
					if (slMapPos.getValue() <= currValue) {
						slMapPos.setValues(currValue, slMapPos.getAbsMax(), currValue);
					} else if (!cbStickMap.getState()) {
						slMapPos.setAbsMin(currValue);
					} else {
						slMapPos.setValues(currValue, slMapPos.getAbsMax(), currValue);
					}
					updateZValues(currValue, slMapPos.getAbsMax());
				}
				mmetr3D.setMinZ(zFactor * (currValue - minZ));

				if (cbClassicFocussing.getState())
					if (mmetr3D.getZ0() < mmetr3D.getMinZ()) {
						mmetr3D.setZ0(mmetr3D.getMinZ());
					} else {
						mmetr3D.setZ0(zFactor * (slMapPos.getValue() - minZ));
					}
			}
			if (n == 1) {
				if (lManager != null && lManager.getSpecialLayer() != null) {
					((DGeoLayerZ) lManager.getSpecialLayer()).setMaxLimitChanged(currValue);
				}
				if (cbClassicFocussing.getState()) {
					if (!cbStickMap.getState()) {
						if (slMapPos.getValue() >= currValue) {
							slMapPos.setValues(slMapPos.getAbsMin(), currValue, currValue);
						} else if (!cbStickMap.getState()) {
							slMapPos.setAbsMax(currValue);
						} else {
							slMapPos.setValues(slMapPos.getAbsMin(), currValue, currValue);
						}
					} else {

					}
					updateZValues(slMapPos.getAbsMin(), currValue);
				}
				mmetr3D.setMaxZ(zFactor * (currValue - minZ));
				if (cbClassicFocussing.getState())
					if (mmetr3D.getZ0() > mmetr3D.getMaxZ()) {
						mmetr3D.setZ0(mmetr3D.getMaxZ());
					} else {
						mmetr3D.setZ0(zFactor * (slMapPos.getValue() - minZ));
					}
			}
			if (minTime != null) {
				tfCurrMapPos.setText(minTime.valueOf((long) slMapPos.getValue()).toString());
			} else {
				tfCurrMapPos.setText(StringUtil.doubleToStr(slMapPos.getValue(), slMapPos.getAbsMin(), slMapPos.getAbsMax()));
			}
			mmetr3D.computeScaleFactor();
			lManager.notifyPropertyChange("content", null, null, true, false);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent ie) {
		if (c3DView == null)
			return;
		MapMetrics3D mmetr3D = (MapMetrics3D) c3DView.getMapContext();
		Checkbox cb = null;
		Choice ddList = null;
		Object objSrc = ie.getSource();

		if (objSrc instanceof Checkbox) {
			cb = (Checkbox) objSrc;
			if (cb.getName().equalsIgnoreCase("InvertAttrValues")) {
				if (inverseRepresentation == cb.getState())
					return;
				inverseRepresentation = cb.getState();
				c3DView.mmetr3D.setInvertZDimension(inverseRepresentation);
				hc.setInverted(inverseRepresentation);
				lManager.notifyPropertyChange("content", null, null, true, false);
			} else if (cb.getName().equalsIgnoreCase("DrawProjectionLines")) {
				lManager.setDrawProjectionLines(cb.getState());
			} else if (cb.getName().equalsIgnoreCase("DrawBoundingFrame")) {
				lManager.setDrawBoundingFrame(cb.getState());
			} else if (cb.getName().equalsIgnoreCase("UsePointsInsteadOfAreas")) {
				lManager.setUsePointsInsteadOfAreas(cb.getState());
			} else if (cb.getName().equalsIgnoreCase("PerspectiveProjection")) {
				if (mmetr3D == null)
					return;
				if (cb.getState()) {
					mmetr3D.setProjectionType(MapMetrics3D.PERSPECTIVE);
				}
				if (chProjSelector != null) {
					chProjSelector.setEnabled(false);
				}
				lManager.notifyPropertyChange("content", null, null, true, false);
			} else if (cb.getName().equalsIgnoreCase("ParallelProjection")) {
				if (mmetr3D == null)
					return;
				if (chProjSelector != null) {
					chProjSelector.setEnabled(cb.getState());
				}
				if (cb.getState()) {
					if (chProjSelector != null) {
						mmetr3D.setProjectionType(chProjSelector.getSelectedIndex());
					} else {
						mmetr3D.setProjectionType(MapMetrics3D.PARALLEL_ORTHOGRAPHIC);
					}
				}
				lManager.notifyPropertyChange("content", null, null, true, false);
			} else if (cb.getName().equalsIgnoreCase("DynamicUpdateComp")) {
				f.setNotifyFocusChangeDynamically(cb.getState());
				slMapPos.setNAD(cb.getState());
				fc.setAllowDynamicUpdate(cb.getState());
				hc.setAllowDynamicUpdate(cb.getState());
			} else if (cb.getName().equalsIgnoreCase("ClassicFocussing")) {
				if (!(mmetr3D.isClassicalFocussing && cbClassicFocussing.getState())) {
					setFocussingMode(cbClassicFocussing.getState());
				}
			} else if (cb.getName().equalsIgnoreCase("StretchZooming")) {
				mmetr3D.setXYScaling(cbStretchZooming.getState());
				RealRectangle rrVisTerr = c3DView.getMapContext().getVisibleTerritory();
				rebuild3DView(rrVisTerr, mmetr3D.isXYScaling);
				//System.out.println("XY-Zooming is "+cbStretchZooming.getState());
				//lManager.notifyPropertyChange("content",null,null,true,false);
			} else if (cb.getName().equalsIgnoreCase("StickMap")) {
				;
			} else {
				;
			}
		} else if (objSrc instanceof Choice) {
			ddList = (Choice) objSrc;
			if (mmetr3D == null)
				return;
			mmetr3D.setProjectionType(chProjSelector.getSelectedIndex());
			lManager.notifyPropertyChange("content", null, null, true, false);
		}
	}

	/**
	*  The function recalculates Z-axis scaling factor using actual
	*  min/max values from focuser (low,hi)
	*/
	protected void updateZValues(double low, double hi) {
		maxZ = hi;
		minZ = low;
		DGeoLayerZ specLayer = (DGeoLayerZ) lManager.getSpecialLayer();
		if (specLayer.getObjectCount() > 0) {
			zFactor = (float) (zdif / Math.abs(maxZ - minZ));
			specLayer.setZFactor(zFactor);
			specLayer.updateZValues(minZ, maxZ);
		}
	}

	/**
	*  The function called to finalize procedure of attribute change:
	*  replace old attribute data with new one in the interface
	*/
	protected void updateInterface() {
		f.setAbsMinMax((float) minZ, (float) maxZ);
		slMapPos.setValues((float) minZ, (float) maxZ, (float) minZ);
		if (minTime != null && maxTime != null) {
			f.setAbsMinMaxTime(minTime, maxTime);
			slMapPos.setAbsMinMaxTime(minTime, maxTime);
		}
		f.setCurrMinMax((float) minZ, (float) maxZ);
		f.refresh();
		//inverseRepresentation=false;
		cbClassicFocussing.setState(false);
		//cbInvertAttrValues.setState(inverseRepresentation);
		setFocussingMode(false);
		c3DView.setZ0(0.0f, (float) minZ);
		if (minTime != null) {
			tfCurrMapPos.setText(minTime.toString());
		} else {
			tfCurrMapPos.setText(StringUtil.doubleToStr(minZ, minZ, maxZ));
		}
		lAttrName.setText(attrName);
		cbDrawAreasAsPoints.setEnabled(canSwitchToPoints);
		// following text:"3D View: "
		if (f3DView != null) {
			f3DView.setTitle(res.getString("3D_View_") + attrName);
		}
	}

	protected void setFocussingMode(boolean isClassic) {
		if (c3DView == null)
			return;
		MapMetrics3D mmetr3D = (MapMetrics3D) c3DView.getMapContext();
		if (mmetr3D == null)
			return;
		mmetr3D.isClassicalFocussing = isClassic;
		cbStickMap.setEnabled(isClassic);
		if (!isClassic) {
			cbStickMap.setState(false);
		}
		if (!mmetr3D.isClassicalFocussing) {
			slMapPos.setValues(f.getAbsMin(), f.getAbsMax(), slMapPos.getValue());
			updateZValues(f.getAbsMin(), f.getAbsMax());
			// if (cbStickMap.getState())
			c3DView.setZ0((float) (zFactor * (slMapPos.getValue() - minZ)), (float) slMapPos.getValue());
			//else
			if (minTime != null) {
				tfCurrMapPos.setText(minTime.valueOf((long) slMapPos.getValue()).toString());
			} else {
				tfCurrMapPos.setText(StringUtil.doubleToStr(slMapPos.getValue(), slMapPos.getAbsMin(), slMapPos.getAbsMax()));
			}
		}
		f.notifyFocusChange();
	}

	public void changeAttribute() {
		String userSelectedAttr = null;
		if (lManager == null) {
			DLayerManager dlm = (DLayerManager) core.getDataKeeper().getMap(core.getUI().getCurrentMapN());
			GeoLayer gl = selectLayer(dlm);
			if (gl == null)
				return;

			AttributeDataPortion table = gl.getThematicData();
			userSelectedAttr = selectAttribute(table);
			if (userSelectedAttr == null)
				return;
			else {
				attrId = userSelectedAttr;
			}
			setupZLayer(dlm, (DGeoLayer) gl);
			return;
		}
		DGeoLayer l = lManager.getSpecialLayer();
		AttributeDataPortion dTable = null;
		if (l != null) {
			dTable = l.getThematicData();
			userSelectedAttr = selectAttribute(dTable);
			if (userSelectedAttr == null)
				return; // user has cancelled attribute selection
			else {
				attrId = userSelectedAttr;
			}
			setupAttribute((DGeoLayerZ) l);
		} else {
			System.out.println("No special layer found");
		}
	}

	/*
	* The function is needed for reconstruction of 3D-Layer
	* if number of geo-objects has been changed in the original layer,
	* from which it had been created.
	*/
	public void updateLayerZ() {
		if (lManager == null) {
			System.out.println("WARNING: LayerManager3D not found");
			DLayerManager dlm = (DLayerManager) core.getDataKeeper().getMap(core.getUI().getCurrentMapN());
			GeoLayer gl = selectLayer(dlm);
			if (gl == null)
				return;
			AttributeDataPortion table = gl.getThematicData();
			String userSelectedAttr = null;
			userSelectedAttr = selectAttribute(table);
			if (userSelectedAttr == null)
				return;
			else {
				attrId = userSelectedAttr;
			}
			setupZLayer(dlm, (DGeoLayer) gl);
			return;
		}

		DGeoLayer gl = lManager.getOriginalLayer();

		DGeoLayerZ layer = (DGeoLayerZ) lManager.getSpecialLayer();
		/*
		System.out.println("Now we are updating our DGeoLayerZ: "+layer.getEntitySetIdentifier());
		System.out.println("Update ZLayer:: Original layer has "+gl.getObjectCount()+" objects");
		System.out.println("Update ZLayer:: current Z layer has "+layer.getObjectCount()+" Z objects");
		*/

		layer.removeAllObjects();

		minZ = Float.NaN;
		maxZ = minZ;
		minTime = null;
		maxTime = null;

		for (int i = 0; i < gl.getObjectCount(); i++) {
			DGeoObject gobj = gl.getObject(i);
			if (gobj == null || gobj.getData() == null) {
				continue;
			}
			double val = gobj.getData().getNumericAttrValue(selectedAttrIdx);
			if (Double.isNaN(val)) {
				continue;
			}
			DGeoObjectZ zobj = new DGeoObjectZ(gobj);
			// initialize 3DGeoObjects with absolute attribute values
			zobj.setZPosition(val);
			zobj.setZAttrValue(val);
			if (Double.isNaN(minZ) || minZ > val) {
				minZ = val;
			}
			if (Double.isNaN(maxZ) || maxZ < val) {
				maxZ = val;
			}
			if (isAttrTemporal) {
				Object oval = gobj.getData().getAttrValue(selectedAttrIdx);
				if (oval != null && (oval instanceof TimeMoment)) {
					TimeMoment t = (TimeMoment) oval;
					if (minTime == null || minTime.compareTo(t) > 0) {
						minTime = t;
					}
					if (maxTime == null || maxTime.compareTo(t) < 0) {
						maxTime = t;
					}
				}
			}
			layer.addGeoObject(zobj);
		}

		if (minTime != null) {
			minTime = minTime.getCopy();
		}
		if (maxTime != null) {
			maxTime = maxTime.getCopy();
		}

		if (layer.getObjectCount() > 0) {
			updateZValues(minZ, maxZ);
			canSwitchToPoints = !(layer.getType() == Geometry.point || (layer.getVisualizer() != null ? layer.getVisualizer().isDiagramPresentation() : false));
		}
		// System.out.println("Update ZLayer:: "+layer.getObjectCount()+" Z objects added");
		layer.notifyPropertyChange("ObjectSet", null, null);
	}

	public void registerAsMapListener(MapDraw map) {
		if (map != null) {
			map.addPropertyChangeListener(this);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		String prName = pce.getPropertyName();
		if (prName.equals("MapScale")) {
			RealRectangle rrVisTerr = null;
			Object objNewPropertyValue = pce.getNewValue();
			if (objNewPropertyValue instanceof RealRectangle) {
				rrVisTerr = (RealRectangle) objNewPropertyValue;
			}
			if (rrVisTerr != null) {
				rebuild3DView(rrVisTerr, cbStretchZooming.getState());
			}
		} else if (prName.equals("ObjectSet")) {
			// System.out.println("Sample3DView :: "+prName);
			updateLayerZ();
		} else if ((pce.getSource() instanceof AttributeDataPortion) && (prName.equals("values") || prName.equals("data_added") || prName.equals("data_removed") || prName.equals("data_updated"))) {
			AttributeDataPortion table = (AttributeDataPortion) pce.getSource();
			NumRange nr = table.getAttrValueRange(attrId);
			if (nr != null) {
				minZ = nr.minValue;
				maxZ = nr.maxValue;
				if (minTime != null) {
					minTime.setMoment(minTime.valueOf((long) minZ));
				}
				if (maxTime != null) {
					maxTime.setMoment(maxTime.valueOf((long) maxZ));
				}
				updateInterface();
			}
		}
	}

	/*
	* Rebuilds 3DView to fit new visible territory
	*/
	public void rebuild3DView(RealRectangle newTerritory, boolean xyScalingNeeded) {
		RealRectangle rr = newTerritory;
		MapMetrics3D mmetr3D = c3DView.mmetr3D;
		if (mmetr3D == null || newTerritory == null)
			return;

		boolean noTerrChanges = newTerritory.equals(mmetr3D.getVisibleTerritory());

		// if (noTerrChanges) System.out.println("No visible territory changes");
		float xdif = rr.rx2 - rr.rx1, ydif = rr.ry2 - rr.ry1;
		zdif = (xdif > ydif) ? xdif : ydif;
		if (xyScalingNeeded) {
			c3DView.setupZ(0, zdif, (float) (zFactor * (slMapPos.getValue() - minZ)), (float) slMapPos.getValue());
		}
		c3DView.setVisibleTerritory(rr);
		if (xyScalingNeeded) {
			updateZValues(mmetr3D.isClassicalFocussing ? f.getCurrMin() : f.getAbsMin(), mmetr3D.isClassicalFocussing ? f.getCurrMax() : f.getAbsMax());
		}
		//fc.rebuild(null,rr.rx1,rr.rx2,rr.ry1,rr.ry2,0,zdif);
		//hc.rebuild(null,rr.rx1,rr.rx2,rr.ry1,rr.ry2,0,zdif);
		f.notifyFocusChange();
	}
}
