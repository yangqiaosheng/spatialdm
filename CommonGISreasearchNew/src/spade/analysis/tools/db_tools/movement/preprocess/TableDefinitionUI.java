package spade.analysis.tools.db_tools.movement.preprocess;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.util.Vector;

import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Line;
import spade.lib.basicwin.MultiSelector;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: 05-Jan-2007
 * Time: 11:05:11
 * To change this template use File | Settings | File Templates.
 */
public class TableDefinitionUI extends Panel {
	// source table
	protected String srcTableName = null;
	// prefix of tables to be created with names prefix_0 ... prefix_3
	protected String tableName = null;
	// column names in the source table: x,y,t,id
	protected String xCName = null, yCName = null, tCName = null, idCName = null;
	// Vector of extra columns: at input contains all columns of the table except x,y,t,id;
	// at output - only selected (if any) or null
	protected Vector extraCNames = null;

	TextField tfTableName = null, tfDummyID = null;
	MultiSelector ms = null;

	public String getTableName() {
		return tfTableName.getText();
	}

	public String getDummyID() {
		return (tfDummyID == null) ? null : tfDummyID.getText();
	}

	public String[] getExtraCNames() {
		return (ms == null) ? null : ms.getSelectedItems();
	}

	public TableDefinitionUI(String srcTableName, boolean bCoordinatesAreGeo, String xCName, String yCName, String tCName, String idCName, Vector extraCNames) {
		super();
		this.srcTableName = srcTableName;
		this.xCName = xCName;
		this.yCName = yCName;
		this.tCName = tCName;
		this.idCName = idCName;
		this.extraCNames = extraCNames;
		//
		tableName = srcTableName + ((bCoordinatesAreGeo) ? "$$" : "$");
		setLayout(new ColumnLayout());
		add(new Label("Source table: " + srcTableName), Label.LEFT);
		Panel p = new Panel(new BorderLayout());
		add(p);
		p.add(new Label("Resulting table name:"), BorderLayout.WEST);
		p.add(tfTableName = new TextField(tableName), BorderLayout.CENTER);
		add(new Label("* If table with such name already exists, it will be deleted"));
		add(new Line(false));
		if (idCName == null) {
			p = new Panel(new BorderLayout());
			add(p);
			p.add(new Label("Dummy entity ID:"), BorderLayout.WEST);
			p.add(tfDummyID = new TextField("1"), BorderLayout.CENTER);
			add(new Line(false));
		}
		if (extraCNames != null && extraCNames.size() > 0) {
			add(new Label("Choose addtitional attributes:"));
			ms = new MultiSelector(extraCNames, true);
			add(ms);
			add(new Line(false));
		}
	}
}
