package spade.analysis.tools.clustering;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.vis.action.Highlighter;
import spade.vis.database.AttributeTypes;
import spade.vis.database.DataTable;
import spade.vis.dataview.TableViewer;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Feb 2, 2009
 * Time: 5:03:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClassifierBuilderUIextraTable extends Frame {
	protected TableViewer tableViewer = null;
	protected DataTable tblStat = null;

	public DataTable getTblStat() {
		return tblStat;
	}

	protected Supervisor supervisor = null;

	public ClassifierBuilderUIextraTable(Supervisor supervisor) {
		super("Classifier statistics");
		this.supervisor = supervisor;
		setSize(550, 300);
		setLayout(new BorderLayout());
		tblStat = new DataTable();
		tblStat.setEntitySetIdentifier("stat_tbl");
		tblStat.addAttribute("N subclusters", "subcl_n", AttributeTypes.integer);
		tblStat.addAttribute("Max distance threshold", "max_dist", AttributeTypes.real);
		tblStat.addAttribute("N original members", "n_memb", AttributeTypes.integer);
		tblStat.addAttribute("correctly classified", "correct", AttributeTypes.integer);
		tblStat.addAttribute("false negatives", "false_n", AttributeTypes.integer);
		tblStat.addAttribute("false positives", "false_p", AttributeTypes.integer);
		tableViewer = new TableViewer(tblStat, supervisor, null);
		tableViewer.setTreatItemNamesAsNumbers(true);
		Vector attr = new Vector(tblStat.getAttrCount(), 1);
		for (int i = 0; i < tblStat.getAttrCount(); i++) {
			attr.addElement(tblStat.getAttributeId(i));
		}
		tableViewer.setVisibleAttributes(attr);
		tableViewer.setTableLens(true);
		add(tableViewer, BorderLayout.CENTER);
	}

	public void selectSingleRow(String id) {
		Highlighter h = supervisor.getHighlighter("stat_tbl");
		if (h == null || tblStat.getDataItemCount() == 0)
			return;
		Vector v = new Vector(1, 1);
		v.addElement(id);
		h.replaceSelectedObjects(this, v);
	}

}
