package spade.analysis.tools.similarity;

import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.util.Vector;

import spade.analysis.plot.Sammons2DPlot;
import spade.analysis.plot.Sammons2DPlotPanel;
import spade.analysis.system.DataKeeper;
import spade.analysis.system.ESDACore;
import spade.analysis.tools.BaseAnalyser;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.OKDialog;
import spade.vis.database.DataTable;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 29, 2010
 * Time: 1:45:49 PM
 * Applies Sammon's projection to a pre-computed distance matrix
 */
public class SammonProjectionBuilder extends BaseAnalyser {
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
		Vector<DataTable> tables = new Vector<DataTable>(dk.getTableCount(), 1);
		for (int i = 0; i < dk.getTableCount(); i++)
			if (dk.getTable(i) instanceof DataTable) {
				DataTable tbl = (DataTable) dk.getTable(i);
				if (tbl.getDistanceMatrix() != null) {
					tables.addElement(tbl);
				}
			}
		if (tables.size() < 1) {
			showMessage("No tables with distance matrices found!", true);
			return;
		}
		List list = new List(Math.min(Math.max(2, tables.size()), 5));
		for (int i = 0; i < tables.size(); i++) {
			list.add(tables.elementAt(i).getName());
		}
		list.select(tables.size() - 1);
		Panel p = new Panel(new ColumnLayout());
		p.add(new Label("Select the table for Sammon's projection:"));
		p.add(list);
		OKDialog ok = new OKDialog(getFrame(), "Sammon's projection", true);
		ok.addContent(p);
		ok.show();
		if (ok.wasCancelled())
			return;
		int idx = list.getSelectedIndex();
		if (idx < 0)
			return;
		DataTable tbl = tables.elementAt(idx);
		Sammons2DPlot sp = new Sammons2DPlot(true, true, core.getSupervisor(), core.getSupervisor());
		sp.setMethodId("sammons_projection");
		sp.setDataSource(tbl);
		if (!sp.setup(tbl.getDistanceMatrix())) {
			showMessage("Could not produce the projection!", true);
			return;
		}
		sp.checkWhatSelected();
		core.getSupervisor().registerTool(sp);
		Sammons2DPlotPanel sPan = new Sammons2DPlotPanel(sp);
		String title = tbl.getDistMatrixTitle();
		if (title != null) {
			sPan.setName("Sammon's projection by " + title);
		}
		core.getDisplayProducer().showGraph(sPan);
	}
}
