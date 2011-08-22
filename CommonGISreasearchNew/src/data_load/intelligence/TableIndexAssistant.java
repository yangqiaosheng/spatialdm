package data_load.intelligence;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.lib.util.IntArray;
import spade.time.TimeRefDescription;
import spade.vis.database.AttributeDataPortion;
import spade.vis.database.DataTable;
import spade.vis.space.GeoLayer;
import spade.vis.spec.CaptionParamDescription;
import spade.vis.spec.DataSourceSpec;
import spade.vis.spec.ParamDescription;
import ui.AttributeChooser;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 12-Jul-2004
 * Time: 16:43:15
 * In a series of dialogs, allows the user to describe the structure of a
 * table with parameter-dependent (in particular, time-dependent) attributes.
 * Indexing can be applied to tables that have been previously loaded in the
 * system.
 */
public class TableIndexAssistant implements TableIndexer {
	static ResourceBundle res = Language.getTextResource("data_load.intelligence.Res");
	/**
	 * A reference to the system's core
	 */
	protected ESDACore core = null;
	/**
	 * A frame used for constructing dialogs
	 */
	protected Frame frame = null;

	/**
	 * Returns the text of the command which can be included in a menu etc.
	 * The indexer can itself listen when the command is activated and start
	 * the process of table indexing.
	 */
	@Override
	public String getCommandText() {
		return res.getString("index_table");
	}

	/**
	 * Returns the identifier of the command (which does not appear in the user
	 * interface, unlike the text of the command)
	 */
	@Override
	public String getCommandId() {
		return "index_table";
	}

	/**
	 * Sets a reference to the system's core.
	 */
	@Override
	public void setCore(ESDACore core) {
		this.core = core;
	}

	/**
	 * Reacts to an activation of the index command.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() != null && e.getActionCommand().equals(getCommandId())) {
			startIndexing();
		}
	}

	/**
	 * Starts the process of table indexing. If there are several tables loaded
	 * in the system, must ask the user which table he wishes to index. The user
	 * is not allowed to index tables that already have parameters.
	 */
	public void startIndexing() {
		if (core == null)
			return;
		DataKeeper dk = core.getDataKeeper();
		if (dk == null)
			return;
		if (dk.getTableCount() < 1) {
			showMessage(res.getString("No_table_found"), true);
			return;
		}
		//Find unindexed tables
		Vector tables = new Vector(dk.getTableCount(), 1);
		for (int i = 0; i < dk.getTableCount(); i++) {
			AttributeDataPortion tbl = dk.getTable(i);
			boolean hasParams = tbl.getParamCount() > 0;
			if (!hasParams && !tbl.hasData()) {
				//the table has no parameters yet and has not been loaded yet
				//possibly, it has descriptors of some parameters
				if (tbl.getDataSource() != null && (tbl.getDataSource() instanceof DataSourceSpec)) {
					DataSourceSpec dss = (DataSourceSpec) tbl.getDataSource();
					if (dss.descriptors != null) {
						for (int j = 0; j < dss.descriptors.size() && !hasParams; j++) {
							Object d = dss.descriptors.elementAt(j);
							if (d != null)
								if ((d instanceof CaptionParamDescription) || (d instanceof ParamDescription) || (d instanceof TimeRefDescription)) {
									hasParams = true;
								}
						}
					}
				}
			}
			if (!hasParams) {
				tables.addElement(tbl);
			}
		}
		if (tables.size() < 1) {
			showMessage(res.getString("No_tables_without_params"), true);
			return;
		}
		AttributeDataPortion table = null;
		if (tables.size() < 2) {
			table = (AttributeDataPortion) tables.elementAt(0);
		} else { //propose the user to select the table to index
			List lst = new List(5);
			for (int i = 0; i < tables.size(); i++) {
				table = (AttributeDataPortion) tables.elementAt(i);
				lst.add(table.getName());
			}
			lst.select(0);
			OKDialog dia = new OKDialog(getFrame(), res.getString("Select_table_to_index"), true);
			dia.addContent(lst);
			dia.show();
			if (dia.wasCancelled())
				return;
			int idx = lst.getSelectedIndex();
			table = (AttributeDataPortion) tables.elementAt(idx);
		}
		if (table == null)
			return;
		if (!table.hasData()) {
			table.loadData();
		}
		if (!table.hasData()) {
			showMessage(res.getString("No_data_in_table") + " " + table.getName(), true);
			return;
		}
		TimeRefIntelligence tint = new TimeRefIntelligence(table);
		OKDialog dia = new OKDialog(getFrame(), res.getString("Time_ref_in_col"), true);
		dia.addContent(tint);
		tint.setNotificationLine(dia.getStatusLine());
		dia.show();
		TimeRefDescription td = null;
		if (!dia.wasCancelled()) {
			td = tint.getTimeRefDescription();
		}

		IntArray timeRefColIndexes = null, paramColIndexes = null;
		Vector parNames = new Vector(10, 10);
		if (td != null) {
			timeRefColIndexes = tint.getTimeRefColIndexes();
			if (td.isParameter) {
				paramColIndexes = timeRefColIndexes;
				parNames.addElement(td.attrName);
			}
		}

		ColumnParamIntelligence cint = new ColumnParamIntelligence(table, timeRefColIndexes);
		dia = new OKDialog(getFrame(), res.getString("Param_in_col"), true);
		dia.addContent(cint);
		tint.setNotificationLine(dia.getStatusLine());
		dia.show();
		Vector colParDescr = null;
		if (!dia.wasCancelled()) {
			IntArray iar = cint.getParamColIndexes();
			if (iar != null && iar.size() > 0) {
				colParDescr = cint.getParamDescriptions();
				if (paramColIndexes == null) {
					paramColIndexes = iar;
				} else {
					for (int i = 0; i < iar.size(); i++) {
						paramColIndexes.addElement(iar.elementAt(i));
					}
				}
				for (int i = 0; i < iar.size(); i++) {
					parNames.addElement(table.getAttributeName(iar.elementAt(i)));
				}
			}
		}

		if (paramColIndexes != null && paramColIndexes.size() > 0) {
			Vector attrIds = new Vector(table.getAttrCount());
			Vector excludeAttrIds = new Vector(paramColIndexes.size(), 1);
			for (int i = 0; i < table.getAttrCount(); i++)
				if (paramColIndexes.indexOf(i) < 0) {
					attrIds.addElement(table.getAttributeId(i));
				} else {
					excludeAttrIds.addElement(table.getAttributeId(i));
				}
			Vector selColIds = null;
			if (attrIds.size() > 1) {
				//ask the user what attributes depend on the parameters
				String prompt = res.getString("Which_attributes_depend_on") + " ";
				if (parNames.size() < 2) {
					prompt += res.getString("parameter");
				} else {
					prompt += res.getString("parameters");
				}
				for (int i = 0; i < parNames.size(); i++) {
					if (i > 0) {
						prompt += ",";
					}
					prompt += " \"" + parNames.elementAt(i).toString() + "\"";
				}
				prompt += "?";
				AttributeChooser attrChooser = new AttributeChooser();
				attrChooser.selectTopLevelAttributes(table, attrIds, excludeAttrIds, false, prompt, core.getUI());
				selColIds = attrChooser.getSelectedColumnIds();
				if (selColIds == null || selColIds.size() < 1 || selColIds.size() == attrIds.size()) {
					selColIds = null;
				}
			}
			if (selColIds != null) {
				Vector depColNames = new Vector(selColIds.size(), 1);
				for (int i = 0; i < selColIds.size(); i++) {
					depColNames.addElement(table.getAttributeName((String) selColIds.elementAt(i)));
				}
				if (td != null) {
					td.dependentCols = depColNames;
				}
				if (colParDescr != null) {
					for (int j = 0; j < colParDescr.size(); j++) {
						ParamDescription pd = (ParamDescription) colParDescr.elementAt(j);
						pd.dependentCols = depColNames;
					}
				}
			}
		}

		if (td != null || colParDescr != null) {
			DataSourceSpec dss = (DataSourceSpec) table.getDataSource();
			if (dss.descriptors == null) {
				dss.descriptors = new Vector(10, 10);
			}
			if (td != null) {
				dss.descriptors.addElement(td);
			}
			if (colParDescr != null) {
				for (int j = 0; j < colParDescr.size(); j++) {
					dss.descriptors.addElement(colParDescr.elementAt(j));
				}
			}
			GeoLayer layer = dk.getTableLayer(table);
			if (core instanceof PropertyChangeListener) {
				table.addPropertyChangeListener((PropertyChangeListener) core);
			}
			if (dk instanceof PropertyChangeListener) {
				table.addPropertyChangeListener((PropertyChangeListener) dk);
				if (td != null && !td.isParameter) {
					if (layer != null && !layer.hasTimeReferences()) {
						layer.addPropertyChangeListener((PropertyChangeListener) dk);
						//the data keeper will listen when this layer gets time references from the table
					}
				}
			}
			Component cmp = core.getUI().getMainFrame();
			Cursor curs = null;
			if (cmp != null) {
				curs = cmp.getCursor();
				cmp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}
			showMessage(res.getString("Wait_table_restructuring"), false);
			((DataTable) table).completeTableStructure();
			if (cmp != null) {
				cmp.setCursor(curs);
			}
			showMessage(res.getString("Table_restructuring_complete"), false);
			if (layer != null) {
				DataSourceSpec spec = (DataSourceSpec) layer.getDataSource();
				if (spec != null && spec.objDescrSource != null && CopyFile.sameFiles(spec.objDescrSource, dss.source)) {
					spec.descriptors = dss.descriptors; //to be saved when saving the project
				}
			}
		}
	}

	protected void showMessage(String txt, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(txt, error);
		}
	}

	protected Frame getFrame() {
		if (frame != null)
			return frame;
		if (core != null && core.getUI() != null) {
			frame = core.getUI().getMainFrame();
		}
		if (frame == null) {
			frame = CManager.getAnyFrame();
		}
		return frame;
	}
}
