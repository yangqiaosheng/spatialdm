package spade.analysis.manipulation;

import java.awt.Label;
import java.awt.Panel;

import spade.analysis.classification.AnyClassManipulator;
import spade.analysis.classification.QualitativeClassifier;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.vis.database.AttributeDataPortion;
import spade.vis.mapvis.ColorAndShapeVisualizer;
import spade.vis.mapvis.SimpleSignPresenter;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Mar 10, 2010
 * Time: 4:13:34 PM
 * Used for a visualizer representing combinations of values of two qualitative attributes
 * by icons varying in colors and shapes
 */
public class ColorAndShapeManipulator extends Panel implements Manipulator, Destroyable {
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;
	protected AnyClassManipulator qClassMan = null;
	protected SimpleSignManipulator shapeMan = null;

	/**
	* Construction of map manipulator. Returns true if successfully constructed.
	* Arguments: 1) supervisor responsible for dynamic linking of displays;
	* 2) visualizer or classifier to be manipulated; 3) table with source data;
	* 4) table filter
	* The Manipulator must check if the visualizer has the appropriate type.
	* For VisComparison visualizer should be an instance of NumberDrawer.
	*/
	@Override
	public boolean construct(Supervisor sup, Object visualizer, AttributeDataPortion dataTable) {
		if (visualizer == null || dataTable == null)
			return false;
		if (!(visualizer instanceof ColorAndShapeVisualizer))
			return false;
		ColorAndShapeVisualizer csv = (ColorAndShapeVisualizer) visualizer;
		SimpleSignPresenter sPres = csv.getSimpleSignPresenter();
		if (sPres == null)
			return false;
		QualitativeClassifier qClassifier = csv.getQualitativeClassifier();
		if (qClassifier == null)
			return false;
		qClassMan = new AnyClassManipulator();
		if (!qClassMan.construct(sup, qClassifier, dataTable))
			return false;
		shapeMan = new SimpleSignManipulator();
		if (!shapeMan.construct(sup, sPres, dataTable))
			return false;
		FoldablePanel fp1 = new FoldablePanel(qClassMan, new Label("Colors"));
		fp1.open();
		FoldablePanel fp2 = new FoldablePanel(shapeMan, new Label("Shapes"));
		fp2.open();
		setLayout(new ColumnLayout());
		add(fp1);
		add(new Line(false));
		add(fp2);
		return true;
	}

	/**
	* Calls destroy() of its SizeFocuser
	*/
	@Override
	public void destroy() {
		if (shapeMan != null) {
			shapeMan.destroy();
		}
		if (qClassMan != null) {
			qClassMan.destroy();
		}
		destroyed = true;
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}
}
