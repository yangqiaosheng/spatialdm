package spade.analysis.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.Dialogs;
import spade.vis.space.LayerManager;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 06-Oct-2006
 * Time: 15:36:16
 * The component allows the user to construct a map layer consisting of
 * rectangles that form a regular grid
*/
public class GridBuilder implements DataAnalyser, ActionListener {
	private Frame frame = null;

	/**
	* Returns true when the tool has everything necessary for its operation.
	* A GridBuilder does not need any additional classes and therefore always
	* returns true.
	*/
	@Override
	public boolean isValid(ESDACore core) {
		return true;
	}

	/**
	* This method constructs and starts the tool. Everything the tool may need
	* for integration with other components of the system can be received
	* from the system's core passed as an argument.
	*/
	@Override
	public void run(ESDACore core) {
		LayerManager lm = core.getDataKeeper().getMap(0);
		boolean geo = false;
		if (lm != null) {
			geo = lm.isGeographic();
		} else {
			geo = Dialogs.askYesOrNo(core.getUI().getMainFrame(), "Are the coordinates geographic (X is longitude and Y is latitude)?", "Geographic?");
		}
		GridBuildPanel gbp = new GridBuildPanel(core, this, geo);
		frame = new Frame("Construct a grid layer");
		frame.setLayout(new BorderLayout());
		frame.add(gbp);
		frame.pack();
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize(), fs = frame.getSize();
		frame.setLocation(ss.width - fs.width - 50, 50);
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("finish") || e.getActionCommand().equals("cancel")) {
			frame.dispose();
		}
	}
}
