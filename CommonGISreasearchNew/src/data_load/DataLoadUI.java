package data_load;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.AttrDataReader;
import spade.analysis.system.CompositeDataReader;
import spade.analysis.system.DataLoader;
import spade.analysis.system.DataReader;
import spade.analysis.system.DataReaderFactory;
import spade.analysis.system.GeoDataReader;
import spade.analysis.system.MultiLayerReader;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.SelectDialog;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DrawingParameters;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;

/**
* Runs a dialog for loading data. Uses available data readers registered in
* DataReaderRegister
*/
public class DataLoadUI implements ActionListener {
	static ResourceBundle res = Language.getTextResource("data_load.Res");
	/**
	* The component responsible for managing and providing access to all data
	* loaded in the system
	*/
	protected DataLoader dataLoader = null;
	/**
	* The system UI can display status messages and provide a reference to the
	* main window of the system
	*/
	protected SystemUI ui = null;
	/**
	* The dialog for selection of the input data format
	*/
	protected Dialog d = null;

	/**
	* Displays a dialog in which the user selects the format of data to be loaded
	*/
	public void start(DataLoader dk, SystemUI sysUI) {
		if (dk == null || dk.getDataReaderFactory() == null || dk.getDataReaderFactory().getAvailableReaderCount() < 1)
			return;
		dataLoader = dk;
		ui = sysUI;
		DataReaderFactory rfac = dk.getDataReaderFactory();
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0f;
		c.weighty = 1.0f;
		c.fill = GridBagConstraints.HORIZONTAL;
		Panel p = new Panel(gridbag);
		for (int i = 0; i < rfac.getAvailableReaderCount(); i++) {
			Button b = new Button(rfac.getAvailableReaderName(i));
			b.setActionCommand(rfac.getAvailableReaderId(i));
			c.gridwidth = 1;
			gridbag.setConstraints(b, c);
			p.add(b);
			b.addActionListener(this);
			Label l = new Label("   " + rfac.getAvailableReaderDescr(i));
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
		}
		//check for existence of the DataServerConnector
		boolean dscExists = false;
		try {
			Class.forName("data_load.connect_server.DataServerConnector");
			dscExists = true;
		} catch (Exception e) {
		}
		if (dscExists) {
			Line line = new Line(false);
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(line, c);
			p.add(line);
			//following text:"Use Data Server"
			Button b = new Button(res.getString("Use_Data_Server"));
			b.setActionCommand("data_server");
			c.gridwidth = 1;
			gridbag.setConstraints(b, c);
			p.add(b);
			b.addActionListener(this);
			//following text:"   load any type of data using the Data Server"
			Label l = new Label(res.getString("load_any_type_of_data"));
			c.gridwidth = GridBagConstraints.REMAINDER;
			gridbag.setConstraints(l, c);
			p.add(l);
		}

		Frame f = null;
		if (ui != null) {
			f = ui.getMainFrame();
		}
		if (f == null) {
			f = CManager.getAnyFrame();
		}
		//following text:"Load data"
		d = new Dialog(f, res.getString("Load_data"), true);
		d.setLayout(new BorderLayout());
		//following text:"Load data from"
		d.add(new Label(res.getString("Load_data_from")), "North");
		d.add(p, "Center");
		p = new Panel(new FlowLayout(FlowLayout.CENTER, 4, 4));
		//following text:"Cancel"
		Button b = new Button(res.getString("Cancel"));
		b.setActionCommand("cancel");
		b.addActionListener(this);
		p.add(b);
		d.add(p, "South");
		d.pack();
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension sz = d.getSize();
		d.setLocation((ss.width - sz.width) / 2, (ss.height - sz.height) / 2);
		d.show();
	}

	/**
	* Reaction to pressing buttons in the dialog
	*/
	@Override
	public void actionPerformed(ActionEvent e) {
		d.dispose();
		String cmd = e.getActionCommand();
		if (cmd == null || cmd.equals("cancel"))
			return;
		DataReader reader = null;
		DataReaderFactory rfac = dataLoader.getDataReaderFactory();
		if (cmd.equals("data_server")) {
			try {
				reader = (DataReader) Class.forName("data_load.connect_server.DataServerConnector").newInstance();
			} catch (Exception ex) {
				if (ui != null) {
					ui.showMessage(ex.toString(), true);
				}
				return;
			}
		} else {
			reader = rfac.constructReader(cmd);
			if (reader == null) {
				if (ui != null) {
					ui.showMessage(rfac.getErrorMessage(), true);
				}
				return;
			}
		}
		reader.setUI(ui);
		if (reader instanceof CompositeDataReader) {
			((CompositeDataReader) reader).setDataReaderFactory(rfac);
		}
		if (!reader.loadData(true))
			return;
		int mapN = 0;
		if (ui != null) {
			mapN = ui.getCurrentMapN();
		}
		if (mapN < 0) {
			mapN = 0;
		}
		Vector layers = new Vector(10, 5), tables = new Vector(10, 5);
		if (reader instanceof MultiLayerReader) {
			MultiLayerReader mlReader = (MultiLayerReader) reader;
			for (int i = 0; i < mlReader.getLayerCount(); i++) {
				DGeoLayer layer = mlReader.getMapLayer(i);
				if (layer != null) {
					layers.addElement(layer);
					tables.addElement(mlReader.getAttrData(i));
				}
			}
		} else {
			if (reader instanceof GeoDataReader) {
				DGeoLayer layer = ((GeoDataReader) reader).getMapLayer();
				if (layer != null) {
					layers.addElement(layer);
				}
			}
			if (reader instanceof AttrDataReader) {
				DataTable table = ((AttrDataReader) reader).getAttrData();
				if (table != null) {
					tables.addElement(table);
				}
			}
		}
		if (layers.size() < 1 && tables.size() < 1)
			return;
/*
    for (int i=0; i<layers.size(); i++) {
      DGeoLayer layer=(DGeoLayer)layers.elementAt(i);
      layer.setName(Dialogs.askForStringValue(ui.getMainFrame(),
        "Desired layer name?",layer.getName(),null,"Layer name",false));
    }
    for (int i=0; i<tables.size(); i++) {
      DataTable table=(DataTable)tables.elementAt(i);
      table.setName(Dialogs.askForStringValue(ui.getMainFrame(),
        "Desired table name?",table.getName(),null,"Table name",false));
    }
*/
		if (layers.size() < 1) {
			for (int i = 0; i < tables.size(); i++) {
				DataTable table = (DataTable) tables.elementAt(i);
				if (table == null) {
					continue;
				}
				int tableN = dataLoader.addTable(table);
				//select a layer to link to the table
				if (mapN < 0) {
					continue;
				}
				LayerManager lman = dataLoader.getMap(mapN);
				if (lman == null || lman.getLayerCount() < 1) {
					continue;
				}
				SelectDialog dia = new SelectDialog(CManager.getAnyFrame(), res.getString("Select_a_layer"), res.getString("Select_a_layer_to"));
				for (int j = 0; j < lman.getLayerCount(); j++) {
					GeoLayer gl = lman.getGeoLayer(j);
					if (gl.getType() != Geometry.image && gl.getType() != Geometry.raster) {
						dia.addOption(gl.getName(), gl.getContainerIdentifier(), false);
					}
				}
				dia.show();
				if (dia.wasCancelled())
					return;
				String layerId = dia.getSelectedOptionId();
				GeoLayer gl = dataLoader.linkTableToMapLayer(tableN, mapN, layerId);
				if (gl != null && ui != null) {
					ui.showMessage(res.getString("The_table_has_been") + gl.getName());
				}
			}
		} else {
			for (int i = 0; i < layers.size(); i++) {
				DGeoLayer layer = (DGeoLayer) layers.elementAt(i);
				DrawingParameters dp = layer.getDrawingParameters();
				Random random = new Random(System.currentTimeMillis());
				dp.lineColor = Color.getHSBColor(random.nextFloat(), random.nextFloat(), random.nextFloat());
				dp.fillColor = Color.getHSBColor(random.nextFloat(), 0.3f, 0.9f);
				dataLoader.addMapLayer(layer, mapN);
				if (tables.size() > i && tables.elementAt(i) != null) {
					DataTable table = (DataTable) tables.elementAt(i);
					int tableN = dataLoader.addTable(table);
					//register the link between the layer and the table
					dataLoader.setLink(layer, tableN);
				} else {
					if (layer.getType() == Geometry.image || layer.getType() == Geometry.raster) {
						continue;
					}
					//select a table to link to the layer
					if (dataLoader.getTableCount() < 1) {
						continue;
					}
					SelectDialog dia = new SelectDialog(CManager.getAnyFrame(), res.getString("Select_a_table"), res.getString("What_table_refers_to") + layer.getName() + "?");
					for (int j = 0; j < dataLoader.getTableCount(); j++) {
						AttributeDataPortion t = dataLoader.getTable(j);
						dia.addOption(t.getName(), t.getContainerIdentifier(), false);
					}
					dia.show();
					if (dia.wasCancelled())
						return;
					int tableN = dia.getSelectedOptionN();
					dataLoader.linkTableToMapLayer(tableN, mapN, layer.getContainerIdentifier());
					if (ui != null) {
						ui.showMessage(res.getString("The_table_has_been") + layer.getName());
					}
				}
			}
		}
	}
}
