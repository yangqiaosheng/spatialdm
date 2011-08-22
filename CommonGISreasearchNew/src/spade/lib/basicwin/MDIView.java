package spade.lib.basicwin;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;

/*
import java.util.ResourceBundle;
import spade.lib.lang.Language;
*/

// To be removed from sources. For debugging purposes only.

public class MDIView extends Frame implements DataAnalyser, ActionListener, LayoutChangeListener {

	//static ResourceBundle res = Language.getTextResource("spade.lib.basicwin.Res");
	private ESDACore core = null;
	private LayoutSelector lsm = null;

	public MDIView() {
		super("Layout Control");
	}

	public void start() {
		pack();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension sz = getSize();
		setLocation((d.width - sz.width) / 4, (d.height - sz.height) / 4);
/*
    if (controlFrame!=null) {
      controlFrame.pack();
      controlFrame.show();
      controlFrame.setLocation((d.width-sz.width)/4,(d.height-sz.height)/4-controlFrame.getSize().height);
    }
*/		show();
	}

	@Override
	public void run(ESDACore core) {
		this.core = core;

		LayoutSelector lsm = new LayoutSelector(this, 0, true);
		add(lsm);
		addWindowListener(lsm);
		lsm.addLayoutChangeListener(this);
		start();
		core.getUI().showMessage("MDI Layout Switcher 1.2");
	}

	@Override
	public void layoutChanged(int layoutType) {
		pack();
	}

	@Override
	public boolean isValid(ESDACore esda) {
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	}
}
