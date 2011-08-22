package spade.analysis.tools;

import java.awt.Frame;

import spade.analysis.system.ESDACore;
import spade.lib.basicwin.CManager;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Feb 20, 2009
 * Time: 2:10:30 PM
 */
public abstract class BaseAnalyser implements DataAnalyser {
	protected ESDACore core = null;

	public void setCore(ESDACore core) {
		this.core = core;
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}

	protected Frame getFrame() {
		if (core == null || core.getUI() == null)
			return CManager.getAnyFrame();
		Frame fr = core.getUI().getMainFrame();
		if (fr == null) {
			fr = CManager.getAnyFrame();
		}
		return fr;
	}

	/**
	 * Returns true when the tool has everything necessary for its operation.
	 * For example, if the tool manages a number of analysis methods, it should
	 * check whether the class for at least one method is available.
	 * By default returns true.
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
	public abstract void run(ESDACore core);
}
