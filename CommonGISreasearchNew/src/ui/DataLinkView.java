package ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.system.SystemUI;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.SelectDialog;
import spade.lib.lang.Language;
import spade.lib.util.IntArray;
import spade.vis.database.AttributeDataPortion;
import spade.vis.geometry.Geometry;
import spade.vis.space.GeoLayer;
import spade.vis.space.LayerManager;
import spade.vis.spec.DataSourceSpec;

public class DataLinkView extends Panel implements ActionListener, ItemListener {
	static ResourceBundle res = Language.getTextResource("ui.Res");
	/**
	* Currently available map layers
	*/
	protected Vector layers = null;
	/**
	* Map (i.e. layer manager) indexes corresponding to the currently available
	* map layers
	*/
	protected IntArray layerMapNs = null;
	/**
	* Text edit fields for editing layer names
	*/
	protected Vector layerNameTFs = null;
	/**
	* Indexes of the currently available tables in the data keper's list of table
	*/
	protected IntArray tableNs = null;
	/**
	* For each table - index of the corresponding layer in the list of layers
	*/
	protected IntArray layerIdxs = null;
	/**
	* Text edit fields for editing table names
	*/
	protected Vector tableNameTFs = null;
	/**
	* Radio buttons for linking tables to layers
	*/
	protected Vector rbut = null;
	/**
	* The system's core
	*/
	protected ESDACore core = null;

	/**
	* Constructs the UI. Returns true if OK.
	*/
	public boolean construct(ESDACore core) {
		if (core == null)
			return false;
		this.core = core;
		DataKeeper dk = core.getDataKeeper();
		if (dk == null)
			return false;
		if (dk.getMapCount() < 1 && dk.getTableCount() < 1)
			return false; //no data
		layers = new Vector(20, 10);
		layerMapNs = new IntArray(20, 10);
		for (int i = 0; i < dk.getMapCount(); i++) {
			LayerManager lm = dk.getMap(i);
			if (lm == null || lm.getLayerCount() < 1) {
				continue;
			}
			for (int j = 0; j < lm.getLayerCount(); j++) {
				GeoLayer gl = lm.getGeoLayer(j);
				if (gl != null) {
					layers.addElement(gl);
					layerMapNs.addElement(i);
				}
			}
		}
		if (layers.size() < 1 && dk.getTableCount() < 1)
			return false;
		tableNs = new IntArray(dk.getTableCount(), 1);
		layerIdxs = new IntArray(dk.getTableCount(), 1);
		for (int i = 0; i < dk.getTableCount(); i++) {
			AttributeDataPortion table = dk.getTable(i);
			if (table == null) {
				continue;
			}
			String layerName = null;
			;
			if (table.getDataSource() != null && (table.getDataSource() instanceof DataSourceSpec)) {
				DataSourceSpec dss = (DataSourceSpec) table.getDataSource();
				layerName = dss.layerName;
			}
			int layerIdx = -1;
			if (layerName != null && layers.size() > 0) {
				layerName = layerName.toUpperCase();
				for (int j = 0; j < layers.size() && layerIdx < 0; j++) {
					GeoLayer gl = (GeoLayer) layers.elementAt(j);
					if (gl.getDataSource() != null && (gl.getDataSource() instanceof DataSourceSpec)) {
						DataSourceSpec lSpec = (DataSourceSpec) gl.getDataSource();
						if (lSpec.source == null) {
							continue;
						}
						String s1 = lSpec.source.toUpperCase();
						if (s1.endsWith(layerName)) {
							layerIdx = j;
						}
					}
				}
			}
			if (layerIdx < 0 && layers.size() > 0) {
				for (int j = 0; j < layers.size() && layerIdx < 0; j++) {
					GeoLayer gl = (GeoLayer) layers.elementAt(j);
					if (table.equals(gl.getThematicData())) {
						layerIdx = j;
					} else if (table.getEntitySetIdentifier().equals(gl.getEntitySetIdentifier())) {
						layerIdx = j;
					}
				}
			}
			if (layerIdx < 0) {
				tableNs.addElement(i);
				layerIdxs.addElement(layerIdx);
			} else {
				boolean inserted = false;
				for (int j = 0; j < layerIdxs.size() && !inserted; j++)
					if (layerIdxs.elementAt(j) < 0 || layerIdxs.elementAt(j) > layerIdx) {
						tableNs.insertElementAt(i, j);
						layerIdxs.insertElementAt(layerIdx, j);
						inserted = true;
					}
				if (!inserted) {
					tableNs.addElement(i);
					layerIdxs.addElement(layerIdx);
				}
			}
		}
		if (layers.size() < 1 && tableNs.size() < 1)
			return false;
		constructUI();
		return true;
	}

	protected void constructUI() {
		if (layers.size() < 1 && tableNs.size() < 1)
			return;
		int nlines = 0, tIdx = 0;
		for (int i = 0; i < layers.size(); i++) {
			++nlines;
			int nTbl = 0, j = tIdx;
			for (j = tIdx; j < tableNs.size(); j++) {
				int lN = layerIdxs.elementAt(j);
				if (lN < 0 || lN > i) {
					break;
				}
				if (i == lN) {
					++nTbl;
				}
			}
			tIdx = j;
			if (nTbl > 1) {
				nlines += nTbl - 1;
			}
		}
		nlines += tableNs.size() - tIdx;

		if (layers.size() > 0) {
			layerNameTFs = new Vector(layers.size(), 1);
		}
		if (tableNs.size() > 0) {
			tableNameTFs = new Vector(tableNs.size(), 1);
			rbut = new Vector(tableNs.size(), 1);
		}

		Panel lp = new Panel(new GridLayout(nlines, 1)), rp = new Panel(new GridLayout(nlines, 1));
		tIdx = 0;
		DataKeeper dk = core.getDataKeeper();
		for (int i = 0; i < layers.size(); i++) {
			GeoLayer gl = (GeoLayer) layers.elementAt(i);
			TextField tf = new TextField(gl.getName(), 40);
			tf.addActionListener(this);
			layerNameTFs.addElement(tf);
			lp.add(tf);
			int nTbl = 0, j = tIdx;
			CheckboxGroup cbg = null;
			for (j = tIdx; j < tableNs.size(); j++) {
				int lN = layerIdxs.elementAt(j);
				if (lN < 0 || lN > i) {
					break;
				}
				if (i == lN) {
					++nTbl;
					AttributeDataPortion table = dk.getTable(tableNs.elementAt(j));
					if (cbg == null) {
						cbg = new CheckboxGroup();
					}
					Checkbox cb = new Checkbox("", gl.hasThematicData(table), cbg);
					cb.addItemListener(this);
					rbut.addElement(cb);
					Panel pp = new Panel(new BorderLayout());
					pp.add(cb, BorderLayout.WEST);
					rp.add(pp);
					tf = new TextField(table.getName(), 40);
					tf.addActionListener(this);
					tableNameTFs.addElement(tf);
					pp.add(tf, BorderLayout.CENTER);
					if (nTbl > 1) {
						lp.add(new Label(""));
					}
				}
			}
			if (nTbl == 0) {
				rp.add(new Label(""));
			}
			tIdx = j;
		}
		for (int i = tIdx; i < tableNs.size(); i++) {
			lp.add(new Label(""));
			Panel pp = new Panel(new BorderLayout());
			rp.add(pp);
			if (layers.size() > 0) {
				Button b = new Button(res.getString("link"));
				b.setActionCommand("link_" + tableNs.elementAt(i));
				b.addActionListener(this);
				pp.add(b, BorderLayout.WEST);
			} else {
				pp.add(new Label(""), BorderLayout.WEST);
			}
			TextField tf = new TextField(dk.getTable(tableNs.elementAt(i)).getName(), 40);
			tableNameTFs.addElement(tf);
			pp.add(tf, BorderLayout.CENTER);
		}
		Panel mainP = new Panel(new BorderLayout(5, 0));
		mainP.add(lp, BorderLayout.WEST);
		mainP.add(rp, BorderLayout.EAST);

		setLayout(new BorderLayout());
		if (layers.size() > 0)
			if (tableNs.size() > 0) {
				Panel p = new Panel(new GridLayout(1, 2));
				p.add(new Label(res.getString("layers") + ":"));
				p.add(new Label(res.getString("tables") + ":"));
				add(p, BorderLayout.NORTH);
			} else {
				add(new Label(res.getString("layers") + ":"), BorderLayout.NORTH);
			}
		else {
			add(new Label(res.getString("tables") + ":"), BorderLayout.NORTH);
		}

		//if (nlines<=15)
		add(mainP, BorderLayout.CENTER);
		/*
		else {
		  ScrollPane scp=new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		  scp.add(mainP);
		  add(scp,BorderLayout.CENTER);
		}*/
	}

	public void checkNames() {
		if (layerNameTFs != null) {
			for (int i = 0; i < layerNameTFs.size(); i++) {
				TextField tf = (TextField) layerNameTFs.elementAt(i);
				String str = tf.getText();
				if (str != null) {
					str = str.trim();
					if (str.length() < 1) {
						str = null;
					}
				}
				GeoLayer gl = (GeoLayer) layers.elementAt(i);
				if (str == null) {
					tf.setText(gl.getName());
				} else if (!str.equals(gl.getName())) {
					gl.setName(str);
				}
			}
		}
		if (tableNameTFs != null) {
			for (int i = 0; i < tableNameTFs.size(); i++) {
				TextField tf = (TextField) tableNameTFs.elementAt(i);
				String str = tf.getText();
				if (str != null) {
					str = str.trim();
					if (str.length() < 1) {
						str = null;
					}
				}
				AttributeDataPortion table = core.getDataKeeper().getTable(tableNs.elementAt(i));
				if (str == null) {
					tf.setText(table.getName());
				} else if (!str.equals(table.getName())) {
					table.setName(str);
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		checkNames();
		if (e.getSource() instanceof Button) {
			String cmd = e.getActionCommand();
			if (!cmd.startsWith("link_"))
				return;
			cmd = cmd.substring(5);
			int tIdx = -1;
			try {
				tIdx = Integer.valueOf(cmd).intValue();
			} catch (NumberFormatException nfe) {
				return;
			}
			if (tIdx < 0)
				return;
			AttributeDataPortion table = core.getDataKeeper().getTable(tIdx);
			if (table == null)
				return;
			SystemUI ui = core.getUI();
			Frame frame = (ui != null) ? ui.getMainFrame() : null;
			if (frame == null) {
				frame = CManager.getAnyFrame();
			}
			SelectDialog dia = new SelectDialog(frame, res.getString("Select_a_layer"), res.getString("Select_a_layer_to"));
			dia.addLabel(table.getName());
			for (int j = 0; j < layers.size(); j++) {
				GeoLayer gl = (GeoLayer) layers.elementAt(j);
				if (gl.getType() != Geometry.image && gl.getType() != Geometry.raster) {
					dia.addOption(gl.getName(), gl.getContainerIdentifier(), false);
				}
			}
			dia.show();
			if (dia.wasCancelled())
				return;
			int lIdx = dia.getSelectedOptionN();
			GeoLayer gl = (GeoLayer) layers.elementAt(lIdx);
			gl.receiveThematicData(table);
			if (core.getDataLoader().linkTableToMapLayer(tIdx, layerMapNs.elementAt(lIdx), gl.getContainerIdentifier()) != null) {
				if (ui != null) {
					ui.showMessage(res.getString("The_table_has_been") + gl.getName());
				}
				int k = tableNs.indexOf(tIdx);
				boolean inserted = false;
				for (int i = 0; i < k && !inserted; i++)
					if (layerIdxs.elementAt(i) < 0 || layerIdxs.elementAt(i) > lIdx) {
						tableNs.removeElementAt(k);
						layerIdxs.removeElementAt(k);
						tableNs.insertElementAt(tIdx, i);
						layerIdxs.insertElementAt(lIdx, i);
						inserted = true;
					}
				if (!inserted) {
					layerIdxs.setElementAt(lIdx, k);
				}
				setVisible(false);
				removeAll();
				constructUI();
				setVisible(true);
				CManager.validateAll(this);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		checkNames();
		if (e.getSource() instanceof Checkbox) {
			Checkbox cb = (Checkbox) e.getSource();
			if (!cb.getState())
				return;
			int idx = rbut.indexOf(cb);
			if (idx < 0)
				return;
			int lIdx = layerIdxs.elementAt(idx);
			if (lIdx < 0)
				return;
			AttributeDataPortion table = core.getDataKeeper().getTable(tableNs.elementAt(idx));
			GeoLayer layer = (GeoLayer) layers.elementAt(lIdx);
			if (!layer.hasThematicData(table)) {
				layer.receiveThematicData(table);
				layer.setThematicFilter(table.getObjectFilter());
			}
		}
	}
}