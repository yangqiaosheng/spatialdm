package data_load.readers;

import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.StringTokenizer;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.CompositeDataReader;
import spade.analysis.system.DataReaderFactory;
import spade.analysis.system.GeoDataReader;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataPortion;
import spade.vis.database.DataRecord;
import spade.vis.database.DataSupplier;
import spade.vis.database.DataTable;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.database.ThematicDataItem;
import spade.vis.dmap.DGeoLayer;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.MultiGeometry;
import spade.vis.geometry.RealPoint;
import spade.vis.geometry.RealPolygon;
import spade.vis.geometry.RealPolyline;
import spade.vis.spec.DataSourceSpec;

import com.esri.sde.sdk.client.SeColumnDefinition;
import com.esri.sde.sdk.client.SeConnection;
import com.esri.sde.sdk.client.SeError;
import com.esri.sde.sdk.client.SeException;
import com.esri.sde.sdk.client.SeExtent;
import com.esri.sde.sdk.client.SeLayer;
import com.esri.sde.sdk.client.SeQuery;
import com.esri.sde.sdk.client.SeRow;
import com.esri.sde.sdk.client.SeShape;
import com.esri.sde.sdk.client.SeSqlConstruct;
import com.esri.sde.sdk.client.SeTable;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Fraunhofer AIS.SPADE</p>
 * @author unascribed
 * @version 1.0
 */

public class ArcSDEReader extends BaseDataReader implements GeoDataReader, AttrDataReader, CompositeDataReader, DataSupplier {

	protected SeConnection conn = null;
	protected SeLayer layer = null;
	protected DGeoLayer lr = null;
	protected boolean mayAskUser = false;
	protected DataTable dtab = null;
	protected LayerData ldata = null;

	public ArcSDEReader() {
	}

	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		this.mayAskUser = mayAskUser;
		if (mayAskUser) {

			// Dialog

			OKDialog dlg = new OKDialog(ui.getMainFrame(), "", true);
			Panel p = new Panel(new GridLayout(5, 2));
			dlg.addContent(p);
			TextField textFields[] = new TextField[5];
			String labelTexts[] = new String[] { "Server", "Port", "Database", "User", "Password" };
			String defaultValues[] = new String[] { "infinity", "5151", "infanta", "sde", "sde" };

			for (int i = 0; i < labelTexts.length; i++) {
				Label l = new Label(labelTexts[i]);
				textFields[i] = new TextField(defaultValues[i]);

				p.add(l);
				p.add(textFields[i]);
			}

			textFields[4].setEchoChar('*');

			dlg.show();

			if (dlg.wasCancelled()) {
				setDataReadingInProgress(false);
				return false;
			}
			for (int i = 0; i < textFields.length; i++) {
				if (textFields[i].getText().trim().equals("")) {
					showMessage("The field '" + labelTexts[i] + "' is empty !", true);
					setDataReadingInProgress(false);
					return false;
				}
			}
			/*
			String server = "infinity";
			int port  = 5151;
			String database = "infanta";
			String layerName = "plzzip1";
			String user = "sde";
			String password = "sde";
			*/

			if (spec == null) {
				spec = new DataSourceSpec();
			}
			spec.format = "ARCSDE";
			spec.user = textFields[3].getText();
			spec.password = textFields[4].getText();
			spec.url = textFields[0].getText() + ":" + textFields[1].getText() + "/" + textFields[2].getText();

		}

		getMapLayer();
		if (lr == null) {
			dataError = true;
			setDataReadingInProgress(false);
			return false;
		}

		spec.geoFieldName = layer.getSpatialColumn();
		setDataReadingInProgress(false);

		lr.notifyPropertyChange("data_updated", null, null);
		lr.notifyPropertyChange("ObjectSet", null, null);
		lr.notifyPropertyChange("ObjectData", null, null);
		return true;
	}

	@Override
	public DGeoLayer getMapLayer() {
		if (lr != null)
			return lr;
		try {

			String layerName = spec.source;
			String password = spec.password;
			String user = spec.user;

			StringTokenizer tz = new StringTokenizer(spec.url + 1, ":/");
			String server = tz.nextToken();
			int port = Integer.parseInt(tz.nextToken());
			String database = tz.nextToken();

			System.out.println("Connecting to server...");
			conn = new SeConnection(server, port, database, user, password);
			System.out.println("Connection Successful! \n");

			Vector layerList = conn.getLayers();
			if (layerList.size() == 0)
				throw new Exception("no layers");

			if (this.mayAskUser) {
				OKDialog dlg = new OKDialog(ui.getMainFrame(), "", true);
				java.awt.List ch = new java.awt.List();
				dlg.addContent(ch);

				for (int i = 0; i < layerList.size(); i++) {
					SeLayer l = (SeLayer) layerList.elementAt(i);
					ch.add(l.getName());
				}
				dlg.show();
				if (dlg.wasCancelled())
					return null;
				layer = (SeLayer) layerList.elementAt(ch.getSelectedIndex());
				layerName = layer.getName();
				spec.source = layerName;
			} else {
				for (int i = 0; i < layerList.size(); i++) {
					SeLayer l = (SeLayer) layerList.elementAt(i);
					if (l.getName().equalsIgnoreCase(layerName)) {
						layer = l;
					}
				}
			}

			if (layer == null)
				throw new Exception("layer is not found");

			int shpCN = -1;
			SeTable tbl = new SeTable(conn, layerName);
			SeColumnDefinition[] colDefs = tbl.describe();
			int idCN = -1;
			int nameCN = -1;
			String[] cols = new String[colDefs.length];
			if (this.mayAskUser) {
				OKDialog dlg = new OKDialog(ui.getMainFrame(), "", true);
				java.awt.List ch = new java.awt.List();
				Panel p1 = new Panel(new ColumnLayout());
				dlg.addContent(p1);
				Panel p2 = new Panel(new GridLayout(2, 2));
				p1.add(ch);
				p1.add(p2);
				TextField idTF = new TextField("1");
				TextField nameTF = new TextField("1");
				p2.add(new Label("ID"));
				p2.add(idTF);
				p2.add(new Label("Name"));
				p2.add(nameTF);

				shpCN = -1;

				for (int i = 0; i < colDefs.length; i++) {
					cols[i] = colDefs[i].getName();
					if (cols[i].equalsIgnoreCase(layer.getSpatialColumn())) {
						shpCN = i;
					}
					ch.add((i + 1) + "  " + cols[i]);

				}

				if (shpCN < 0)
					return null;
				dlg.show();

				if (dlg.wasCancelled())
					return null;
				idCN = Integer.parseInt(idTF.getText()) - 1;
				nameCN = Integer.parseInt(nameTF.getText()) - 1;
				spec.idFieldN = idCN;
				spec.nameFieldN = nameCN;

			} else {
				idCN = spec.idFieldN;
				nameCN = spec.nameFieldN;
				shpCN = -1;

				for (int i = 0; i < colDefs.length; i++) {
					cols[i] = colDefs[i].getName();
					if (cols[i].equalsIgnoreCase(layer.getSpatialColumn())) {
						shpCN = i;
					}

				}

			}

			SeSqlConstruct sqlC = new SeSqlConstruct(layerName);
			SeQuery query = new SeQuery(conn, cols, sqlC);
			query.prepareQuery();
			query.execute();

			SeRow row = query.fetch();
			if (row == null)
				throw new Exception("No rows fetched");

			LayerData ld = new LayerData();

			lr = new DGeoLayer();
			lr.setType(Geometry.area);
			if (layer != null) {
				lr.setName(layer.getName());
			}
			lr.setDataSource(spec);
			if (layer != null) {
				SeExtent ext = layer.getExtent();
				if (!ext.isEmpty()) {
					lr.setWholeLayerBounds((float) ext.getMinX(), (float) ext.getMinY(), (float) ext.getMaxX(), (float) ext.getMaxY());
				}
			}

			int rown = 0;
			int idd = 0;

			dtab = new DataTable();

			for (SeColumnDefinition colDef : colDefs) {
				dtab.addAttribute(colDef.getName(), getDescartesType(colDef.getType()));

			}

			while (row != null) {

				String id = getDescartesValue(idCN, row);
				String name = getDescartesValue(nameCN, row);
				if (id.trim().equalsIgnoreCase("")) {
					id = "undefined" + (++idd);
				}
				//System.out.println("["+id+"]");
				SeShape shp = row.getShape(shpCN);
				DataRecord drec = new DataRecord(id, name);
				for (int i = 0; i < row.getNumColumns(); i++) {
					drec.addAttrValue(getDescartesValue(i, row));
				}
				dtab.addDataRecord(drec);

				addShape(ld, shp, id, rown);

				row = query.fetch();
				if (rown % 50 == 0) {
					showMessage("read " + rown + " shapes", false);
				}
				rown++;

			}

			query.close();

			lr.receiveSpatialData(ld);

			conn.close();

		} catch (Exception ex) {
			ex.printStackTrace();
			lr = null;
			if (ex instanceof SeException) {
				SeError se = ((SeException) ex).getSeError();
				showMessage("ArcSDE error:" + se.getSdeError() + ":" + se.getSdeErrMsg(), true);

			} else {
				showMessage(ex.getMessage(), true);
			}

		}

		return lr;
	}

	protected void addShape(LayerData ld, SeShape shape, String id, int recnum) throws Exception {
		Vector vp = new Vector();
		double points[][][] = shape.getAllCoords();
		int numParts = shape.getNumParts();

		for (int partNo = 0; partNo < numParts; partNo++) {
			int numSubParts = shape.getNumSubParts(partNo + 1);

			for (int subPartNo = 0; subPartNo < numSubParts; subPartNo++) {

				int numCoords = shape.getNumPoints(partNo + 1, subPartNo + 1);
				RealPolygon line = new RealPolygon();
				line.p = new RealPoint[numCoords];
				line.isClosed = true;

				for (int pointNo = 0; pointNo < numCoords * 2; pointNo += 2) {
					RealPoint rp = new RealPoint();
					rp.x = (float) points[partNo][subPartNo][pointNo];
					rp.y = (float) points[partNo][subPartNo][(pointNo + 1)];
					line.p[pointNo / 2] = rp;
				}

				vp.addElement(line);

			}
		}

		for (int i = 0; i < vp.size(); i++) {
			RealPolygon p = (RealPolygon) vp.elementAt(i);
			if (p.p != null) {
				for (int j = 0; j < vp.size(); j++) {
					RealPolygon q = (RealPolygon) vp.elementAt(j);
					if (q.p != null && i != j) {
						if (p.contains(q)) {
							RealPolyline l = new RealPolyline();
							l.p = q.p;
							l.isClosed = true;
							if (p.pp == null) {
								p.pp = new Vector();
							}
							p.pp.addElement(l);
							q.p = null;
						}
					}
				}
			}
		}

		MultiGeometry mg = new MultiGeometry();
		for (int i = 0; i < vp.size(); i++) {
			RealPolyline pl = (RealPolyline) vp.elementAt(i);
			if (pl.p != null) {
				mg.addPart(pl);
			}
		}

		SpatialEntity spe = new SpatialEntity(id);
		spe.setGeometry(mg);
		if (dtab != null) {
			if (dtab.getDataItem(recnum) instanceof ThematicDataItem) {
				spe.setThematicData((ThematicDataItem) dtab.getDataItem(recnum));
			}
			if (dtab.getDataItemName(recnum) != null) {
				spe.setName(dtab.getDataItemName(recnum));
			}
		}
		ld.addDataItem(spe);

	}

	protected char getDescartesType(int sdeType) {
		switch (sdeType) {
		case SeColumnDefinition.TYPE_INTEGER:
		case SeColumnDefinition.TYPE_SMALLINT:
			return AttributeTypes.integer;
		case SeColumnDefinition.TYPE_DOUBLE:
		case SeColumnDefinition.TYPE_FLOAT:
			return AttributeTypes.real;
		}
		return AttributeTypes.character;
	}

	protected String getDescartesValue(int colNum, SeRow row) throws Exception {

		switch (row.getColumnDef(colNum).getType()) {
		case SeColumnDefinition.TYPE_INTEGER:
			return row.getInteger(colNum).toString();
		case SeColumnDefinition.TYPE_SMALLINT:
			return row.getShort(colNum).toString();
		case SeColumnDefinition.TYPE_DOUBLE:
			return row.getDouble(colNum).toString();
		case SeColumnDefinition.TYPE_FLOAT:
			return row.getFloat(colNum).toString();
		case SeColumnDefinition.TYPE_STRING:
			return row.getString(colNum);
		}
		return "?";
	}

	public void constructTable() {
		if (dtab != null)
			return;
	}

	@Override
	public DataTable getAttrData() {
		if (dtab == null) {
			constructTable();
		}
		return dtab;

	}

	@Override
	public void setDataReaderFactory(DataReaderFactory factory) {

	}

	@Override
	public DataPortion getData() {
		if (ldata != null)
			return ldata;
		if (dataError)
			return null;
		if (loadData(false))
			return ldata;
		return null;
	}

	@Override
	public DataPortion getData(Vector bounds) {
		return getData();
	}

	@Override
	public void clearAll() {
		dtab = null;
		ldata = null;
	}
}
