package spade.analysis.manipulation;

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.PopupManager;
import spade.lib.lang.Language;
import spade.vis.mapvis.SignDrawer;
import spade.vis.mapvis.SignParamsController;

/**
* This button initiates the dialog for setting up sign parameters for a
* visualizer that implements the interface SignDrawer. The button may be
* included, in particular, in the manipulator of this visualizer.
*/
public class SetupDiagramsButton extends Button implements ActionListener {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	/**
	* The visualizer that draws diagrams and allows to modify their parameters
	*/
	protected SignDrawer vis = null;

	public SetupDiagramsButton(SignDrawer signDrawer) {
		super(res.getString("Setup_diagrams"));
		addActionListener(this);
		vis = signDrawer;
		// following text:"Change sizes and colors, switch on/off frames"
		new PopupManager(this, res.getString("Change_sizes_and"), true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(this) && vis != null) {
			SignParamsController pc = new SignParamsController();
			pc.startChangeParameters(vis);
		}
	}
}