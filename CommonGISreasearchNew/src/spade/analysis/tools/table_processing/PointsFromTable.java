package spade.analysis.tools.table_processing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.DataLoader;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.lib.util.IntArray;
import spade.vis.database.Attribute;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.geometry.RealPoint;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 19, 2010
 * Time: 11:34:46 AM
 * Creates a map layer with points whose coordinates are specified in table columns
 */
public class PointsFromTable extends BaseAnalyser {

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		this.core = core;
		if (core == null)
			return;
		DataKeeper dk = core.getDataKeeper();
		if (dk == null || dk.getTableCount() < 1) {
			showMessage("No tables found!", true);
			return;
		}
		AttributeDataPortion tbl = TableProcessor.selectTable(core);
		if (tbl == null)
			return;
		if (tbl.getDataItemCount() < 1) {
			showMessage("No data in the table!", true);
			return;
		}
		IntArray colNs = new IntArray(tbl.getAttrCount(), 10);
		List listX = new List(Math.min(tbl.getAttrCount(), 10)), listY = new List(Math.min(tbl.getAttrCount(), 10));
		for (int i = 0; i < tbl.getAttrCount(); i++) {
			Attribute at = tbl.getAttribute(i);
			if (at.isNumeric()) {
				colNs.addElement(i);
				listX.add(at.getName());
				listY.add(at.getName());
			}
		}
		if (colNs.size() < 2) {
			showMessage("The table must have at least 2 numeric columns!", true);
			return;
		}
		Panel p = new Panel(new GridLayout(1, 2));
		Panel pp = new Panel(new BorderLayout());
		pp.add(new Label("X coordinate:"), BorderLayout.NORTH);
		pp.add(listX, BorderLayout.CENTER);
		p.add(pp);
		pp = new Panel(new BorderLayout());
		pp.add(new Label("Y coordinate:"), BorderLayout.NORTH);
		pp.add(listY, BorderLayout.CENTER);
		p.add(pp);
		pp = new Panel(new ColumnLayout());
		pp.add(new Label("Construction of a point layer", Label.CENTER));
		pp.add(new Label("Specify the table columns containing the coordinates of the points:"));
		pp.add(p);
		OKDialog dia = new OKDialog(getFrame(), "Point layer", true);
		dia.addContent(p);
		dia.show();
		if (dia.wasCancelled())
			return;
		int iX = listX.getSelectedIndex(), iY = listY.getSelectedIndex();
		if (iX < 0 || iY < 0) {
			showMessage("The columns have not been selected!", true);
			return;
		}
		int cX = colNs.elementAt(iX), cY = colNs.elementAt(iY);

		String name = "Points from " + tbl.getName();
/*
    name=Dialogs.askForStringValue(getFrame(),"Name of the layer?",
      name,
      "A new map layer with point objects will be created","Map layer with points",true);
    if (name==null)
      return;
*/
		DataTable table = new DataTable();
		table.setName(name);
		table.addAttribute("Original object ID", "origObjId", AttributeTypes.character);
		table.addAttribute("Original object name", "origObjName", AttributeTypes.character);
		DGeoLayer ptLayer = new DGeoLayer();
		ptLayer.setName(table.getName());
		ptLayer.setType(Geometry.point);
		DrawingParameters dp = ptLayer.getDrawingParameters();
		if (dp == null) {
			dp = new DrawingParameters();
			ptLayer.setDrawingParameters(dp);
		}
		dp.lineColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillColor = Color.getHSBColor((float) Math.random(), 1 - 0.2f * (float) Math.random(), 1 - 0.2f * (float) Math.random());
		dp.fillContours = true;

		for (int i = 0; i < tbl.getDataItemCount(); i++) {
			double x = tbl.getNumericAttrValue(cX, i);
			if (Double.isNaN(x)) {
				continue;
			}
			double y = tbl.getNumericAttrValue(cY, i);
			if (Double.isNaN(y)) {
				continue;
			}
			String id = tbl.getDataItemId(i);
			name = tbl.getDataItemName(i);
			DataRecord rec = new DataRecord(id, name);
			table.addDataRecord(rec);
			rec.setAttrValue(id, 0);
			rec.setAttrValue(name, 1);
			SpatialEntity se = new SpatialEntity(id, name);
			se.setGeometry(new RealPoint((float) x, (float) y));
			DGeoObject dgo = new DGeoObject();
			dgo.setup(se);
			dgo.setThematicData(rec);
			ptLayer.addGeoObject(dgo, false);
		}
		DataLoader dLoader = core.getDataLoader();
		int tblN = dLoader.addTable(table);
		dLoader.addMapLayer(ptLayer, -1);
		dLoader.setLink(ptLayer, tblN);
		ptLayer.setLinkedToTable(true);
		dLoader.processTimeReferencedObjectSet(ptLayer);
		dLoader.processTimeReferencedObjectSet(table);
	}

}
