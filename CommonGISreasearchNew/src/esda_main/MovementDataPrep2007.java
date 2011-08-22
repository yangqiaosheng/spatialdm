package esda_main;

import spade.analysis.tools.db_tools.movement.preprocess.MoveDataPrepUI2007;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: 17-Oct-2007
 * Time: 12:31:29
 * To change this template use File | Settings | File Templates.
 */
public class MovementDataPrep2007 {
	public static void main(String[] args) {
		MoveDataPrepUI2007 ui = new MoveDataPrepUI2007();
		ui.runPreprocessor();
	}
}
