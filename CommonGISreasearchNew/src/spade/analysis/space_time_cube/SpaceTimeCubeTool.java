package spade.analysis.space_time_cube;

import spade.analysis.system.ESDACore;
import spade.analysis.tools.DataAnalyser;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Apr 25, 2008
 * Time: 2:18:08 PM
 * Creates and displays a space-time cube.
 */
public class SpaceTimeCubeTool implements DataAnalyser {

	protected ESDACore core = null;

	/**
	 * Returns true.
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
		if (core == null || core.getUI() == null)
			return;
		this.core = core;
		/*
		TemporalDataManager timeMan=core.getDataKeeper().getTimeManager();
		if (timeMan==null || timeMan.getContainerCount()<1) {
		  showMessage("No time-referenced data exist!",true);
		  return;
		}
		*/
		SpaceTimeCubeView stcw = new SpaceTimeCubeView(core);
		stcw.construct();
		stcw.setName("Space-time cube");
		core.getDisplayProducer().showGraph(stcw);
	}

	protected void showMessage(String msg, boolean error) {
		if (core != null && core.getUI() != null) {
			core.getUI().showMessage(msg, error);
		} else if (error) {
			System.out.println("!--> " + msg);
		}
	}
}
