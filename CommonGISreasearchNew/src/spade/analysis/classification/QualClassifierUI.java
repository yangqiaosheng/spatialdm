package spade.analysis.classification;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.system.Supervisor;
import spade.lib.basicwin.Destroyable;
import spade.vis.database.AttributeDataPortion;

/**
 * Created by IntelliJ IDEA.
 * User: Natascha
 * Date: Aug 5, 2009
 * Time: 12:58:40 PM
 * Used for applying a qualitative classifier to a table when there is
 * no corresponding map layer.
 */
public class QualClassifierUI extends Panel implements Destroyable, PropertyChangeListener {
	/**
	* The supervisor, which can propagate object classes
	*/
	protected Supervisor supervisor = null;
	/**
	* The table containing the dynamic attribute representing the classification
	* results
	*/
	protected AttributeDataPortion table = null;
	/**
	* The index of the dynamic attribute in the table
	*/
	protected int attrIdx = -1;

	protected QualitativeClassifier qClassifier = null;

	protected AnyClassManipulator clMan = null;
	/**
	* Indicates whether the classifier is currently used for visualization
	* on a map
	*/
	protected boolean usedOnMap = false;
	/**
	* Indicates the "destroyed" state
	*/
	protected boolean destroyed = false;

	protected ScrollPane scp = null;

	/**
	* Constructs the UI. Arguments:
	* @param table - the table with the objects to classify
	* @param attrIdx - the index of the resulting dynamic attribute in the table
	* @param supervisor - the supervisor, which can propagate object classes
	*/
	public QualClassifierUI(AttributeDataPortion table, int attrIdx, Supervisor supervisor) {
		this.table = table;
		this.attrIdx = attrIdx;
		this.supervisor = supervisor;
		if (table == null || attrIdx < 0 || attrIdx >= table.getAttrCount() || supervisor == null)
			return;
		table.addPropertyChangeListener(this);
		qClassifier = new QualitativeClassifier();
		qClassifier.setTable(table);
		Vector attr = new Vector(1, 1);
		attr.addElement(table.getAttributeId(attrIdx));

		qClassifier.setAttributes(attr);
		setLayout(new BorderLayout());
		add(new Label(table.getAttributeName(attrIdx), Label.CENTER), BorderLayout.NORTH);
		clMan = new AnyClassManipulator();
		clMan.construct(supervisor, qClassifier, table);
		scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scp.add(clMan);
		add(scp, BorderLayout.CENTER);
	}

	@Override
	public String getName() {
		if (table != null)
			return table.getAttributeName(attrIdx);
		else
			return null;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension sm = clMan.getPreferredSize(), ss = scp.getPreferredSize(), sAll = super.getPreferredSize();
		int w = Math.max(sAll.width, sm.width) + scp.getVScrollbarWidth() + 10, h = sAll.height - ss.height + sm.height + 10;
		return new Dimension(w, h);
	}

	public Classifier getClassifier() {
		return qClassifier;
	}

	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		if (pce.getSource().equals(table)) {
			if (pce.getPropertyName().equals("destroyed") || pce.getPropertyName().equals("structure_complete")) {
				destroy();
				return;
			}
			boolean changed = false;
			if (pce.getPropertyName().equals("data_added") || pce.getPropertyName().equals("data_removed") || pce.getPropertyName().equals("data_updated")) {
				changed = true;
			} else if (pce.getPropertyName().equals("values")) {
				Vector attr = (Vector) pce.getNewValue();
				if (attr == null || attr.size() < 1)
					return;
				changed = attr.contains(table.getAttributeId(attrIdx));
			}
			if (!changed)
				return;
			qClassifier.setup();
		}
	}

	/**
	* Makes necessary operations for destroying.
	*/
	@Override
	public void destroy() {
		table.removePropertyChangeListener(this);
		supervisor.removeObjectColorer(qClassifier);
		if (!usedOnMap) {
			qClassifier.destroy();
		}
		clMan.destroy();
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
