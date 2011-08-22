package spade.vis.mapvis;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ResourceBundle;

import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.OKDialog;
import spade.lib.lang.Language;

/**
* Constructs and starts dialogs for changing parameters of signs on the map
* for visualization methods implementing the SignDrawer interface.
*/

public class SignParamsController implements WindowListener, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");

	protected Window dialog = null; //running dialogs on parameter change
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	/**
	* Constructs and displays a dialog for changing parameters.
	*/
	public void startChangeParameters(Object vis) {
		if (vis == null)
			return;
		Visualizer colorChanger = null;
		if (vis instanceof Visualizer) {
			colorChanger = (Visualizer) vis;
			if (!colorChanger.canChangeColors()) {
				colorChanger = null;
			}
		}
		SignDrawer sd = null;
		if (vis instanceof SignDrawer) {
			sd = (SignDrawer) vis;
		}
		if (colorChanger == null && sd == null)
			return;
		if (colorChanger == null) {
			changeSignParams(sd);
		} else if (sd == null) {
			colorChanger.startChangeColors();
		} else {
			//ask the user if s/he wants to change sign parameters or colors
			Panel p = new Panel(new ColumnLayout());
			p.add(new Label(res.getString("what_to_modify")));
			Panel pp = new Panel(new GridLayout(1, 2));
			CheckboxGroup cbg = new CheckboxGroup();
			Checkbox cbCol = new Checkbox(res.getString("color_scale"), cbg, true);
			Checkbox cbPar = new Checkbox(res.getString("sign_params"), cbg, false);
			p.add(cbCol);
			pp.add(cbPar);
			p.add(pp);
			OKDialog okd = new OKDialog(CManager.getAnyFrame(null), res.getString("what_is_modified"), true);
			okd.addContent(p);
			okd.show();
			if (okd.wasCancelled())
				return;
			if (cbCol.getState()) {
				colorChanger.startChangeColors();
			} else {
				changeSignParams(sd);
			}
		}
	}

	protected void changeSignParams(SignDrawer sd) {
		//start a dialog and assign it to "dialog"
		dialog = new SignParamsDlg(CManager.getAnyFrame(null), sd);
		dialog.addWindowListener(this);
		dialog.pack();
		Dimension frsz = dialog.getSize();
		int sw = Metrics.scrW(), sh = Metrics.scrH();
		if (frsz.width < 250) {
			frsz.width = 250;
		}
		if (frsz.width > sw * 2 / 3) {
			frsz.width = sw * 2 / 3;
		}
		if (frsz.height > sh * 2 / 3) {
			frsz.height = sh * 2 / 3;
		}
		dialog.setBounds((sw - frsz.width) / 2, (sh - frsz.height) / 2, frsz.width, frsz.height);
		dialog.show();
	}

	protected void closeDialog() {
		if (dialog != null) {
			dialog.dispose();
		}
		dialog = null;
	}

	@Override
	public void destroy() {
		closeDialog();
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public void windowClosing(WindowEvent e) {
		if (e.getSource() == dialog) {
			closeDialog();
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}
}