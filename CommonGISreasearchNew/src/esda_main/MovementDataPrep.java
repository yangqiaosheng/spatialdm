package esda_main;

import spade.analysis.tools.db_tools.movement.preprocess.MoveDataPrepUI;

/**
 * Created by IntelliJ IDEA.
 * User: gennady
 * Date: Jul 7, 2010
 * Time: 2:58:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class MovementDataPrep {
	public static void main(String[] args) {
		MoveDataPrepUI ui = new MoveDataPrepUI();
		ui.runPreprocessor();
	}
}
