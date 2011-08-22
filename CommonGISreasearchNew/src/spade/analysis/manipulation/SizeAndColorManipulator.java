package spade.analysis.manipulation;

import java.awt.Label;
import java.awt.Panel;
import java.util.ResourceBundle;

import spade.analysis.classification.AnyClassManipulator;
import spade.analysis.classification.QualitativeClassifier;
import spade.analysis.system.Supervisor;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.FoldablePanel;
import spade.lib.basicwin.Line;
import spade.lib.lang.Language;
import spade.vis.database.AttributeDataPortion;
import spade.vis.mapvis.LineThicknessAndColorVisualiser;
import spade.vis.mapvis.LineThicknessVisualiser;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 31-Jan-2007
 * Time: 17:41:56
 * Used for a visualiser representing numeric values by sizes (e.g. line
 * thicknesses) and qualitative values of another attribute by colors.
 */
public class SizeAndColorManipulator extends Panel implements Manipulator, Destroyable {
	static ResourceBundle res = Language.getTextResource("spade.analysis.manipulation.Res");
	/**
	* Indicates "destroyed" state. Initially is false.
	*/
	protected boolean destroyed = false;

	protected SizeFocuser sFoc = null;
	protected AnyClassManipulator qClassMan = null;

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
		if (!(visualizer instanceof LineThicknessAndColorVisualiser))
			return false;
		LineThicknessAndColorVisualiser lThickColVis = (LineThicknessAndColorVisualiser) visualizer;
		LineThicknessVisualiser lThickVis = lThickColVis.getLineThicknessVisualiser();
		if (lThickVis == null)
			return false;
		QualitativeClassifier qClassifier = lThickColVis.getQualitativeClassifier();
		if (qClassifier == null)
			return false;
		sFoc = new SizeFocuser();
		if (!sFoc.construct(sup, lThickVis, dataTable))
			return false;
		qClassMan = new AnyClassManipulator();
		if (!qClassMan.construct(sup, qClassifier, dataTable))
			return false;
		FoldablePanel fp1 = new FoldablePanel(sFoc, new Label(res.getString("Sizes")));
		fp1.open();
		FoldablePanel fp2 = new FoldablePanel(qClassMan, new Label(res.getString("Colors")));
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
		if (sFoc != null) {
			sFoc.destroy();
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
