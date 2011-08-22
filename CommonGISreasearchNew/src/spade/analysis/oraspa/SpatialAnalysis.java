package spade.analysis.oraspa;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.util.Vector;

import oracle.sdoapi.OraSpatialManager;
import oracle.sdoapi.adapter.GeometryAdapter;
import oracle.sdoapi.geom.CurveString;
import oracle.sdoapi.geom.GeometryFactory;
import oracle.sdoapi.geom.InvalidGeometryException;
import oracle.sdoapi.geom.LineString;
import oracle.sdoapi.geom.MultiCurvePolygon;
import oracle.sdoapi.geom.Polygon;
import oracle.sql.STRUCT;
import spade.analysis.system.ESDACore;
import spade.analysis.system.Supervisor;
import spade.analysis.system.SystemUI;
import spade.analysis.tools.DataAnalyser;
import spade.analysis.tools.SingleInstanceTool;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Centimeter;
import spade.lib.basicwin.OKDialog;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DLayerManager;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolygon;
import spade.vis.geometry.RealPolyline;
import spade.vis.geometry.RealRectangle;

public class SpatialAnalysis implements DataAnalyser, SingleInstanceTool {
	protected Connection conn = null;
	protected boolean isConnected = true;
	protected ESDACore core = null;
	protected DLayerManager dlm = null;
	protected Supervisor sup = null;
	protected String sourceDT = "ST";
	protected String bufzoneDT = "BZ";
	protected String geomMetaDT = "USER_SDO_GEOM_METADATA";
	protected long count = 0;
	protected String url = null;
	protected String user = null;
	protected String pass = null;

	public SpatialAnalysis() {

	}

	protected void openConnection() {
		spade.lib.util.Parameters params = core.getSystemSettings();
		String par = (String) params.getParameter("ORACLE_SPATIAL_CONNECT");
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (Exception e) {
			System.out.println("Exception:" + e.getMessage());
			return;

		}

		if (par == null) {
			if (url != null) {

			} else {
				OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Oracle Spatial Connection", true);
				Panel p = new Panel(new GridLayout(5, 2));
				TextField serverTF = new TextField("");
				TextField portTF = new TextField("1521");
				TextField sidTF = new TextField("");
				TextField userTF = new TextField("");
				TextField passTF = new TextField("");
				passTF.setEchoChar('*');

				p.add(new Label("Server"));
				p.add(serverTF);
				p.add(new Label("Port"));
				p.add(portTF);
				p.add(new Label("Database"));
				p.add(sidTF);
				p.add(new Label("User"));
				p.add(userTF);
				p.add(new Label("Password"));
				p.add(passTF);

				dia.addContent(p);
				dia.show();
				if (dia.wasCancelled())
					return;
				url = "jdbc:oracle:thin:@" + serverTF.getText() + ":" + portTF.getText() + ":" + sidTF.getText();
				user = userTF.getText();
				pass = passTF.getText();
			}

		} else {
			StringTokenizer tz = new StringTokenizer(par, " ");
			url = tz.nextToken("[");
			user = tz.nextToken("[/");
			pass = tz.nextToken("/]");
		}

		try {
			//conn=DriverManager.getConnection("jdbc:oracle:thin:@infinity:1521:infanta","markos","markos");

			System.out.println("URL:" + url);
			conn = DriverManager.getConnection(url, user, pass);
			//params.setParameter("ORACLE_SPATIAL_CONNECT",url+"["+user+"/"+pass+"]");
		} catch (SQLException se) {
			System.out.println(se.getMessage());
			OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Error", false);
			dia.addContent(new Label("connection is not available."));
			dia.show();
			return;
		}
		System.out.println("connection to oracle :OK!");
	}

	protected void closeConnection() {
		try {
			if (conn != null) {
				conn.close();
				conn = null;
			}
		} catch (SQLException se) {
			System.out.println(se.getMessage());

		}

	}

	/**
	* Returns true when the tool has everything necessary for its operation.
	* For example, if the tool manages a number of analysis methods, it should
	* check whether the class for at least one method is available.
	*/
	public boolean isValid(ESDACore core) {

		return isConnected;

	}

	protected static boolean firstRun = true;

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	public void run(ESDACore core) {

		this.core = core;

		openConnection();

		if (conn == null) {
			isConnected = false;
			return;
		}

		if (firstRun) {
			dropTemporalTables();
		}

		firstRun = false;

		dlm = (DLayerManager) core.getDataKeeper().getMap(core.getUI().getCurrentMapN());
		sup = core.getSupervisor();

		Frame fr = null;
		if (core.getUI() != null) {
			fr = core.getUI().getMainFrame();
		}
		if (fr == null) {
			fr = CManager.getAnyFrame();
		}

		OKDialog dlg = new OKDialog(fr, "Spatial Analysis with Oracle Spatial", true);
		CheckboxGroup cg = new CheckboxGroup();

		Checkbox cbs[] = new Checkbox[] { new Checkbox("Create buffer layer around layer objects", cg, false), new Checkbox("Add relate column into attribute table", cg, false), new Checkbox("Drop temporal tables", cg, false) };

		Panel p = new Panel(new GridLayout(cbs.length, 1));
		for (Checkbox cb : cbs) {
			p.add(cb);
		}

		dlg.addContent(p);
		dlg.show();

		if (dlg.wasCancelled())
			return;

		Checkbox c = cg.getSelectedCheckbox();
		int fn = -1;
		if (c != null) {
			for (int i = 0; i < cbs.length; i++) {
				if (cbs[i] == c) {
					fn = i;
					break;
				}
			}

		}

		switch (fn) {
		case 0:
			createBufferZone();
			break;
		case 1:
			createRelateColumn();
			break;
		case 2:
			dropTemporalTables();
			break;
		default:
			;
		}

		// open connection
		// dialog: clear und close,  data, buffersation, intersection

		closeConnection();

	}

	protected void createBufferZone() {

		Frame fr = null;
		if (core.getUI() != null) {
			fr = core.getUI().getMainFrame();
		}
		if (fr == null) {
			fr = CManager.getAnyFrame();
		}
		OKDialog dlg = new OKDialog(fr, "Spatial analysis-buffer zone", true);
		Choice ch = new Choice();
		ch.addItem("unknown");
		Vector lrs = dlm.getLayers();
		for (int i = 0; i < lrs.size(); i++) {
			DGeoLayer dl = (DGeoLayer) lrs.elementAt(i);
			ch.addItem(dl.getName() + " (" + dl.getType() + ")");
		}

		Panel p = new Panel(new GridLayout(5, 2));
		p.add(new Label("source layer:"));
		p.add(ch);
		p.add(new Label("new buffer layer:"));
		TextField nlTF = new TextField("buffer");
		p.add(nlTF);
		p.add(new Label("distance around objects :"));
		TextField dsTF = new TextField("0.035");
		p.add(dsTF);
		p.add(new Label("scale :"));
		Centimeter cm = new Centimeter();
		SystemUI ui = core.getUI();
		float sc = ui.getMapViewer(ui.getCurrentMapN()).getMapDrawer().getMapContext().getPixelValue() * cm.getMinimumSize().width * ((DLayerManager) dlm).user_factor;
		Panel pp = new Panel(new FlowLayout());
		pp.add(new Label(spade.lib.util.StringUtil.floatToStr(sc, 2)));/*+" "+((DLayerManager)dlm).user_unit));*/
		pp.add(cm);
		p.add(pp);
		p.add(new Label("tolerance:"));
		TextField tolTF = new TextField("0.0001");
		p.add(tolTF);

		dlg.addContent(p);
		dlg.show();

		if (dlg.wasCancelled())
			return;
		int n = ch.getSelectedIndex();
		if (n < 1)
			return;
		DGeoLayer dl = dlm.getLayer(n - 1);
		if (dl.getType() == 'R')
			return;

		Highlighter highlighter = sup.getHighlighter(dl.getEntitySetIdentifier());

		String sql = null;
		Statement stmt = null;

// drop table
		try {
			stmt = conn.createStatement();
			sql = "DROP TABLE " + sourceDT;
			stmt.executeUpdate(sql);

			//stmt.close();
			System.out.println("table is dropped.");
		} catch (SQLException se) {
			System.out.println(se);
		}

// delete reference to it from meta data table

		try {

			stmt = conn.createStatement();
			sql = "DELETE FROM " + geomMetaDT + " WHERE TABLE_NAME = '" + sourceDT + "'";
			int sn = stmt.executeUpdate(sql);
			stmt.close();
			System.out.println("reference is deleted:" + sn);
		} catch (SQLException se) {
			System.out.println(se);
		}

// create source table
		try {
			stmt = conn.createStatement();
			sql = "CREATE TABLE " + sourceDT + " (ID CHAR(25), GEOM MDSYS.SDO_GEOMETRY)";
			stmt.executeUpdate(sql);

			//stmt.close();
			System.out.println("table '" + sourceDT + "' is created.");

		} catch (SQLException de) {
			System.out.println(de);
			return;
		}

		try {
			stmt = conn.createStatement();
			sql = "CREATE TABLE " + bufzoneDT + " (TLAYER CHAR(25), GEOM MDSYS.SDO_GEOMETRY)";
			stmt.executeUpdate(sql);

			//stmt.close();
			System.out.println("table '" + bufzoneDT + "' is created.");

		} catch (SQLException de) {
			System.out.println(de);
		}

		RealRectangle rr = dl.getCurrentLayerBounds();

// add reference to it to meta data table
		try {
			stmt = conn.createStatement();

			sql = "INSERT INTO " + geomMetaDT + " VALUES ('" + sourceDT + "', 'GEOM', MDSYS.SDO_DIM_ARRAY(" + "MDSYS.SDO_DIM_ELEMENT('X', " + rr.rx1 + ", " + rr.rx2 + ", 0.0000005), " + "MDSYS.SDO_DIM_ELEMENT('Y', " + rr.ry1 + ", " + rr.ry2
					+ ", 0.0000005)), 1)";

			System.out.println(sql);
			stmt.execute(sql);
			//stmt.close();
			System.out.println("reference is inserted into meta data table.");
		} catch (SQLException de) {
			System.out.println(de);
		}

		count = 0;
		for (int i = 0; i < dl.getObjectCount(); i++) {
			if (dl.isObjectActive(i)) {
				Vector so = null;
				if (highlighter != null) {
					so = highlighter.getSelectedObjects();
				}
				if (so == null || so.size() == 0 || so.contains(dl.getObjectId(i))) {
					try {
						insertIntoTable(dl.getObjectId(i), dl.getObject(i));
					} catch (SQLException e) {

					}

				}
			}
		}

		System.out.println(count + " rows were inserted.");

		try {
			stmt = conn.createStatement();
			sql = "insert into " + bufzoneDT + " (TLAYER,GEOM) " + " select '" + nlTF.getText() + "', SDO_AGGR_UNION(MDSYS.SDOAGGRTYPE(" + "SDO_GEOM.SDO_BUFFER(c.geom," + dsTF.getText() + "," + tolTF.getText() + ")," + tolTF.getText() + ")) from "
					+ sourceDT + " c";
			System.out.println(sql);
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "select GEOM from " + bufzoneDT + " where TLAYER='" + nlTF.getText() + "'";
			System.out.println(sql);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				System.out.println("rs:ok");
				GeometryAdapter sdoAdapter = OraSpatialManager.getGeometryAdapter("SDO", "8.1.6", STRUCT.class, STRUCT.class, null, conn);
				STRUCT struct = (STRUCT) rs.getObject(1);
				oracle.sdoapi.geom.Geometry geom = null;
				try {
					geom = sdoAdapter.importGeometry(struct);
				} catch (Exception e) {
				}

				count = 0;
				DGeoLayer nlr = new DGeoLayer();
				nlr.setType(Geometry.area);
				nlr.setName(nlTF.getText());
				nlr.setWholeLayerBounds(rr);
				core.getDataLoader().addMapLayer(nlr, -1);

				LayerData ld = new LayerData();

				addToLayerData(ld, geom);
				nlr.receiveSpatialData(ld);
				nlr.notifyPropertyChange("data_updated", null, null);
				nlr.notifyPropertyChange("ObjectSet", null, null);
				nlr.notifyPropertyChange("ObjectData", null, null);

			}

			System.out.println(count + " polylines were recieved.");

		} catch (Exception se) {
			System.out.println(se);
		}

	}

	protected void addToLayerData(LayerData ld, oracle.sdoapi.geom.Geometry gm) {

		if (gm instanceof MultiCurvePolygon) {
			oracle.sdoapi.geom.Geometry gmm[] = ((MultiCurvePolygon) gm).getGeometryArray();
			for (oracle.sdoapi.geom.Geometry element : gmm) {
				if (element instanceof MultiCurvePolygon || element instanceof oracle.sdoapi.geom.CurvePolygon) {
					addToLayerData(ld, element);
				}

			}
		} else if (gm instanceof oracle.sdoapi.geom.CurvePolygon) {
			oracle.sdoapi.geom.CurvePolygon cpoly = (oracle.sdoapi.geom.CurvePolygon) gm;
			oracle.sdoapi.geom.CurveString els = cpoly.getExteriorRing();
			oracle.sdoapi.geom.CurveString ils[] = cpoly.getInteriorRingArray();

			SpatialEntity spe = new SpatialEntity("1");
			spe.setGeometry(convertCurveString(els, ils));
			ld.addDataItem(spe);
			++count;

		}
	}

	protected spade.vis.geometry.Geometry convertCurveString(oracle.sdoapi.geom.CurveString cs, oracle.sdoapi.geom.CurveString ils[]) {

		if (ils == null)
			return convertCurveString(cs);

		RealPolygon polygon = new RealPolygon();
		polygon.isClosed = false;
		polygon.p = ((RealPolyline) convertCurveString(cs)).p;
		polygon.pp = new Vector();//new RealPolyline[ils.length];
		for (CurveString il : ils) {
			polygon.pp.addElement((RealPolyline) convertCurveString(il));
		}

		return polygon;
	}

	protected spade.vis.geometry.Geometry convertCurveString(oracle.sdoapi.geom.CurveString cs) {
		oracle.sdoapi.geom.CoordPoint cp[] = cs.getPointArray();

		RealPolyline line = new RealPolyline();
		line.isClosed = false;

		line.p = new RealPoint[cp.length];
		for (int i = 0; i < cp.length; i++) {
			line.p[i] = new RealPoint();
			line.p[i].x = (float) cp[i].getX();
			line.p[i].y = (float) cp[i].getY();
		}

		return line;
	}

	protected spade.vis.geometry.Geometry convertPolygon(Polygon origPolygon) throws Exception {
		LineString intLS[] = (LineString[]) origPolygon.getInteriorRingArray();
		LineString extLS = (LineString) origPolygon.getExteriorRing();
		if (intLS == null)
			return convertLineString(extLS);
		MultiGeometry mg = new MultiGeometry();
		mg.addPart(convertPolygon((Polygon) extLS));
		for (LineString element : intLS) {
			mg.addPart(convertPolygon((Polygon) element));
		}
		return mg;
	}

	protected spade.vis.geometry.Geometry convertLineString(LineString origLineString) throws Exception {
		double coordArray[] = origLineString.getCoordArray();
		int numPoints = coordArray.length / 2;
		RealPolyline line = new RealPolyline();
		line.p = new RealPoint[numPoints];
		for (int i = 0; i < numPoints; i++) {
			line.p[i] = new RealPoint();
			line.p[i].x = (float) coordArray[2 * i];
			line.p[i].y = (float) coordArray[2 * i + 1];
			;
		}
		return line;
	}

	protected void insertIntoTable(String id, DGeoObject go) throws SQLException {

		try {

			oracle.sdoapi.geom.Geometry gm = null;

			Geometry gmm = go.getGeometry();

			if (gmm instanceof MultiGeometry) {
				int n = ((MultiGeometry) gmm).getPartsCount();
				for (int i = 0; i < n; i++) {
					Geometry g = ((MultiGeometry) gmm).getPart(i);
					insertSO(id, g);

				}
			} else if (gmm instanceof RealPolyline) {
				insertSO(id, gmm);
			} else if (gmm instanceof RealPolygon) {
				insertSO(id, gmm);
			} else if (gmm instanceof RealPoint) {
				insertSO(id, gmm);
			} else {
				System.out.println("class is unknown");
			}

		} catch (Exception ex) {
			System.out.println("insert into '" + sourceDT + "':" + ex.getMessage());
		}

	}

	protected void insertSO(String id, Geometry gmm) throws Exception {
		String sql = "INSERT INTO " + sourceDT + " (ID, GEOM) VALUES(?,?)";
		PreparedStatement stat = conn.prepareStatement(sql);
		GeometryAdapter sdoAdapter = OraSpatialManager.getGeometryAdapter("SDO", "8.1.6", null, STRUCT.class, null, conn);
		GeometryFactory gF = OraSpatialManager.getGeometryFactory();

		char gmty = gmm.getType();
		oracle.sdoapi.geom.Geometry gm = null;
		if (gmm instanceof RealPolyline || gmm instanceof RealPolygon) {

			RealPoint rp[] = ((RealPolyline) gmm).p;
			double outerOrdArray[] = new double[rp.length * 2];
			for (int i = 0; i < rp.length; i++) {
				outerOrdArray[i * 2] = rp[i].x;
				outerOrdArray[i * 2 + 1] = rp[i].y;
			}
			oracle.sdoapi.geom.Geometry interiorLineString[] = null;
			if (gmm instanceof RealPolygon) {
				if (((RealPolygon) gmm).pp != null) {
					Vector pp = ((RealPolygon) gmm).pp;
					interiorLineString = new oracle.sdoapi.geom.Geometry[pp.size()];
					for (int i = 0; i < pp.size(); i++) {
						rp = ((RealPolyline) pp.elementAt(i)).p;
						outerOrdArray = new double[rp.length * 2];
						for (int j = 0; j < rp.length; j++) {
							outerOrdArray[j * 2] = rp[j].x;
							outerOrdArray[j * 2 + 1] = rp[j].y;
						}
						interiorLineString[i] = gF.createLineString(outerOrdArray);
					}
				}
			}

			oracle.sdoapi.geom.Geometry outerLineString = gF.createLineString(outerOrdArray);
			if (gmty == 'A') {
				gm = gF.createPolygon((LineString) outerLineString, (LineString[]) interiorLineString);
			} else if (gmty == 'L') {
				gm = outerLineString;
			} else {
				System.out.println("type is unknown:" + gmty + " class:" + gmm.getClass().getClass());
			}

		} else if (gmm instanceof RealPoint && gmty == 'P') {
			RealPoint rp = (RealPoint) gmm;
			gm = gF.createPoint((double) rp.x, (double) rp.y);
		} else {
			System.out.println("type is unknown:" + gmty + " class:" + gmm.getClass().getClass());
		}

		if (gm == null)
			return;

		STRUCT sdo = (STRUCT) sdoAdapter.exportGeometry(oracle.sql.STRUCT.class, gm);
		stat.setString(1, id);
		stat.setObject(2, sdo);
		if (stat.executeUpdate() > 0) {
			count++;
		}
		stat.close();

	}

	protected oracle.sdoapi.geom.Geometry convertGeom(GeometryFactory gF, Geometry gmm) {
		oracle.sdoapi.geom.Geometry geom = null;
		char gmty = gmm.getType();

		try {

			if (gmm instanceof RealPolyline) {

				RealPoint rp[] = ((RealPolyline) gmm).p;
				double outerOrdArray[] = new double[rp.length * 2];
				for (int i = 0; i < rp.length; i++) {
					outerOrdArray[i * 2] = rp[i].x;
					outerOrdArray[i * 2 + 1] = rp[i].y;
				}

				oracle.sdoapi.geom.Geometry outerLineString = gF.createLineString(outerOrdArray);
				if (gmty == 'A') {
					geom = gF.createPolygon((LineString) outerLineString, null);
				} else if (gmty == 'L') {
					geom = outerLineString;
				}

			} else if (gmm instanceof MultiGeometry) {

				int n = ((MultiGeometry) gmm).getPartsCount();

				oracle.sdoapi.geom.Geometry collection[] = new oracle.sdoapi.geom.Geometry[n];
				for (int i = 0; i < n; i++) {
					Geometry gm = ((MultiGeometry) gmm).getPart(i);
					collection[i] = convertGeom(gF, gm);
				}
				geom = gF.createGeometryCollection(collection);
			} else if (gmm instanceof spade.vis.geometry.RealPoint && gmty == 'P') {
				geom = gF.createPoint(((RealPoint) gmm).x, ((RealPoint) gmm).y);
			}

		} catch (InvalidGeometryException ex) {
			System.out.println(ex.getClass().getName());
		}

		return geom;
	}

	protected void createRelateColumn() {

		OKDialog dia = new OKDialog(core.getUI().getMainFrame(), "Spatial analysis - add relate column", true);
		Panel p = new Panel(new GridLayout(9, 2));
		p.add(new Label("Source relate layer"));
		Choice slCh = new Choice();
		p.add(slCh);
		p.add(new Label("Source buffer layer"));
		Choice bzCh = new Choice();
		p.add(bzCh);
		p.add(new Label("New relate column"));
		TextField iclTF = new TextField("relate");
		p.add(iclTF);
		p.add(new Label("Tolerance"));
		TextField tolTF = new TextField("0.0001");
		p.add(tolTF);

		String mask[] = new String[] { "ANYINTERACT", "CONTAINS", "COVEREDBY", "COVERS", "DISJOINT", "EQUAL", "INSIDE", "OVERLAPBDYDISJOINT", "OVERLAPBDYINTERSECT", "TOUCH" };
		Checkbox chk[] = new Checkbox[mask.length];
		for (int i = 0; i < mask.length; i++) {
			chk[i] = new Checkbox(mask[i]);
			p.add(chk[i]);
		}

		slCh.addItem("unknown");
		Vector lrs = dlm.getLayers();
		for (int i = 0; i < lrs.size(); i++) {
			DGeoLayer dl = (DGeoLayer) lrs.elementAt(i);
			slCh.addItem(dl.getName() + " (" + dl.getType() + ")");
		}

		// bzone
		try {
			Statement stmt = conn.createStatement();
			String sql = "select TLAYER from " + bufzoneDT;
			ResultSet rs = stmt.executeQuery(sql);
			bzCh.addItem("unknown");
			while (rs.next()) {
				bzCh.addItem(rs.getString(1));
			}
			stmt.close();
		} catch (SQLException se) {
			System.out.println(se);
		}

		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;

		if (slCh.getSelectedIndex() == 0)
			return;
		if (bzCh.getSelectedIndex() == 0)
			return;
		if (iclTF.getText().equals(""))
			return;
		if (tolTF.getText().equals(""))
			return;

		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("delete " + sourceDT);
		} catch (SQLException se) {

		}

		int n = slCh.getSelectedIndex();

		DGeoLayer dl = dlm.getLayer(n - 1);
		System.out.println(dl.getName());

		DataTable dt = (DataTable) dl.getThematicData();
		//if(dt.hasData())
		dt.loadData();

		count = 0;
		for (int i = 0; i < dl.getObjectCount(); i++) {
			try {
				insertIntoTable(dl.getObjectId(i), dl.getObject(i));
			} catch (SQLException e) {

			}

		}

		System.out.println(count + " rows were inserted.");

		//dia =  new OKDialog(core.getUI().getMainFrame(),"Spatial Analysis-Intersection Column",true);
		//dia.show();
		// add column into table

		dt.addAttribute(iclTF.getText(), AttributeTypes.character);
		int iclN = dt.getAttrIndex(iclTF.getText());
		System.out.println(iclTF.getText() + ":" + iclN);
		Statement stmt = null;

		String maska = "";
		for (int i = 0; i < mask.length; i++) {
			if (chk[i].getState()) {
				maska += "+" + mask[i];
			}
		}
		maska = maska.substring(1);
		if (maska.equals(""))
			return;

		try {

			String sql = "select id,SDO_GEOM.RELATE(geom,'" + maska + "'," + "(select geom from " + bufzoneDT + " where TLAYER = '" + bzCh.getSelectedItem().trim() + "')," + tolTF.getText() + ")" + " from " + sourceDT;

			System.out.println(sql);
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			int ii = 0;
			while (rs.next()) {
				String id = rs.getString(1).trim();
				int idx = -1;
				for (int i = 0; i < dt.getDataItemCount(); i++) {
					if (dt.getObjectId(i).equals(id)) {
						idx = i;
						break;
					}
				}

				DataRecord dr = (DataRecord) dt.getDataItem(idx);
				String val = !rs.getString(2).trim().equalsIgnoreCase("FALSE") ? "T" : "F";
				ii++;
				System.out.println(ii + ":" + id + ":" + val);

				if (dr != null) {
					dr.setAttrValue(val, iclN);
				} else {
					System.out.println("dr=null");
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void dropTemporalTables() {
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("drop table " + sourceDT);
			stmt.close();
			stmt = conn.createStatement();
			stmt.executeUpdate("drop table " + bufzoneDT);
			stmt.close();

		} catch (SQLException se) {
			System.out.println(se);
		}

	}

}
