package spade.vis.mapvis;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.analysis.classification.ObjectColorer;
import spade.analysis.classification.TableClassifier;
import spade.analysis.transform.AttributeTransformer;
import spade.lib.basicwin.Metrics;
import spade.lib.basicwin.StringInRectangle;
import spade.lib.lang.Language;
import spade.vis.database.DataItem;
import spade.vis.database.DataTreater;
import spade.vis.database.ThematicDataItem;
import spade.vis.database.ThematicDataOwner;
import spade.vis.dataview.DataViewRegulator;
import spade.vis.dataview.TransformedDataPresenter;
import spade.vis.map.MapContext;
import spade.vis.spec.ToolSpec;

/**
* Represents results of any classification on a map by returning the
* corresponding colors from the getPresentation(...) method.
* A ClassDrawer is always linked to some classifier, listens to changes of
* the classification, and takes colors for geographical objects from the
* classifier.
*/

public class ClassDrawer extends BaseVisualizer implements PropertyChangeListener, DataTreater, DataViewRegulator, TransformedDataPresenter {
	static ResourceBundle res = Language.getTextResource("spade.vis.mapvis.Res");
	/**
	* A ClassDrawer is always linked to some classifier, listens to changes of
	* the classification, and takes colors for geographical objects from the
	* classifier.
	*/
	protected Classifier classifier = null;
	/**
	* When the classifier linked to this ClassDrawer is a TableClassifier (i.e.
	* classifies according to thematic data), this is a reference to the
	* classifier cast to TableClassifier
	*/
	protected TableClassifier tClassifier = null;

	/**
	* Stores the reference to the classifier. Registers utself as a listener
	* of classification changes.
	*/
	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
		tClassifier = null;
		if (classifier != null) {
			classifier.addPropertyChangeListener(this);
			if (classifier instanceof TableClassifier) {
				tClassifier = (TableClassifier) classifier;
			}
		}
	}

	public Classifier getClassifier() {
		return classifier;
	}

	/**
	* The Visualizer sets its parameters (if needed).
	*/
	@Override
	public void setup() {
	}

	/**
	 * Returns a reference to the classifier.
	 */
	@Override
	public ObjectColorer getObjectColorer() {
		return classifier;
	}

	/**
	* This method is called by a GeoObject. The GeoObject sends its DataItem
	* to the Visualizer, and the Visualizer returns an Object specifying the
	* presentation of data contained in this DataItem. This may be, in
	* particular, Color or Diagram. Typically the argument DataItem is
	* a ThematicDataItem, although in some cases (e.g. manual classification)
	* this is not required.
	*/
	@Override
	public Object getPresentation(DataItem dit, MapContext mc) {
		if (dit == null)
			return null;
		if (classifier == null)
			return null;
		if (tClassifier != null) {
			if (dit instanceof ThematicDataItem)
				return tClassifier.getColorForRecord((ThematicDataItem) dit);
			if (dit instanceof ThematicDataOwner)
				return tClassifier.getColorForRecord(((ThematicDataOwner) dit).getThematicData());
		}
		return classifier.getColorForDataItem(dit);
	}

	@Override
	public boolean isDiagramPresentation() {
		return false;
	}

	/**
	* The method from the LegendDrawer interface.
	* Draws the common part of the legend irrespective of the presentation method
	* (e.g. the name of the map), then calls drawMethodSpecificLegend(...)
	*/
	@Override
	public Rectangle drawLegend(Component c, Graphics g, int startY, int leftMarg, int prefW) {
		if (!enabled)
			return drawReducedLegend(c, g, startY, leftMarg, prefW);
		drawCheckbox(c, g, startY, leftMarg);
		int w = switchSize + Metrics.mm(), y = startY;
		g.setColor(Color.black);
		String name = getVisualizationName();
		if (name != null) {
			Point p = StringInRectangle.drawText(g, res.getString("vis_method") + ": " + name, leftMarg + w, y, prefW - w, false);
			y = p.y;
			w = p.x - leftMarg;
		}
		if (y < startY + switchSize + Metrics.mm()) {
			y = startY + switchSize + Metrics.mm();
		}
		if (tClassifier != null && tClassifier.getTable() != null) {
			Point p = StringInRectangle.drawText(g, tClassifier.getTable().getName(), leftMarg, y, prefW, true);
			y = p.y;
			if (w < p.x - leftMarg) {
				w = p.x - leftMarg;
			}
			AttributeTransformer aTrans = tClassifier.getAttributeTransformer();
			if (aTrans != null) {
				String descr = aTrans.getDescription();
				if (descr != null) {
					descr = res.getString("Data_transformation") + ": " + descr;
					p = StringInRectangle.drawText(g, descr, leftMarg, y, prefW, true);
					y = p.y;
					if (w < p.x - leftMarg) {
						w = p.x - leftMarg;
					}
				}
			}
		}
		Rectangle r = drawMethodSpecificLegend(g, y, leftMarg, prefW);
		if (r != null) {
			if (r.width < w) {
				r.width = w;
			}
			r.height += r.y - startY;
			r.y = startY;
			return r;
		}
		return new Rectangle(leftMarg, startY, w, y - startY);
	}

	@Override
	public Rectangle drawMethodSpecificLegend(Graphics g, int startY, int leftmarg, int prefW) {
		if (classifier == null)
			return new Rectangle(leftmarg, startY, 0, 0);
		return classifier.drawLegend(null, g, startY, leftmarg, prefW);
	}

	@Override
	public void drawIcon(Graphics g, int x, int y, int w, int h) {
		if (classifier != null && classifier.getNClasses() > 0) {
			g.setColor(classifier.getClassColor(0));
		} else {
			g.setColor(Color.red);
		}
		g.fillRect(x, y, w, h);
		int dx = 4 * w / 5, dy = h;
		if (classifier != null)
			if (classifier.getNClasses() > 1) {
				g.setColor(classifier.getClassColor(1));
			} else {
				;
			}
		else {
			g.setColor(Color.green);
		}
		g.fillArc(x - dx, y, 2 * dx, 2 * dy, 0, 90);
		g.setColor(Color.black);
		g.drawArc(x - dx, y, 2 * dx, 2 * dy, 0, 90);
		dy = h / 2;
		dx = 3 * w / 5;
		if (classifier != null)
			if (classifier.getNClasses() > 1) {
				g.setColor(classifier.getClassColor(classifier.getNClasses() - 1));
			} else {
				;
			}
		else {
			g.setColor(Color.yellow);
		}
		g.fillArc(x + w - dx, y + h - dy, 2 * dx, 2 * dy, 90, 90);
		g.setColor(Color.black);
		g.drawArc(x + w - dx, y + h - dy, 2 * dx, 2 * dy, 90, 90);
		g.drawRect(x, y, w, h);
	}

	/**
	* Reacts to changes of classification
	*/
	@Override
	public void propertyChange(PropertyChangeEvent pce) {
		notifyVisChange();
	}

	/**
	* Replies whether the color scale used by this visualization method may be
	* changed. This does not include interactive analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeColors() {
		return classifier != null;
	}

	/**
	* A classifier must give the user an opportunity to change interactively
	* colors assigned to classes. Here the ClassDrawer invokes the method
	* startChangeColors of its classifier.
	*/
	@Override
	public void startChangeColors() {
		if (classifier != null) {
			classifier.startChangeColors();
		}
	}

	/**
	* Must reply whether any parameters of this visualization method may be
	* changed, e.g. colors or sizes of signs. This does not include interactive
	* analytical manipulation.
	* Returns true.
	*/
	@Override
	public boolean canChangeParameters() {
		return true;
	}

	/**
	* Constructs and displays a dialog for changing parameters of this
	* visualization method: colors assigned to the classes.
	*/
	@Override
	public void startChangeParameters() {
		startChangeColors();
	}

	/**
	* A method from the DataTreater interface.
	* If the classifier performs classification on the basis of some attributes
	* (i.e. implements, in its turn, the DataTreater interface), returns a vector
	* of IDs of the attributes used for classification. The attributes are taken
	* from the * classifier. Otherwise, returns null.
	*/
	@Override
	public Vector getAttributeList() {
		if (classifier == null || !(classifier instanceof DataTreater))
			return null;
		return ((DataTreater) classifier).getAttributeList();
	}

	/**
	* A method from the DataTreater interface.
	* Replies whether it is linked to the data set (table) with the given identifier
	*/
	@Override
	public boolean isLinkedToDataSet(String setId) {
		return tableId != null && tableId.equals(setId);
	}

	/**
	* A method from the DataTreater interface.
	* Must return a vector of colors used for representation of the attributes
	* this Data Treater deals with. By default, returns null.
	*/
	@Override
	public Vector getAttributeColors() {
		return null;
	}

	/**
	* A method from the DataViewRegulator interface.
	* Replies whether attributes with null values should be shown in data popups.
	* For this purpose, checks whether the classifier is a TableClassifier and,
	* if so, whether it prohibits displaying attributes with null values.
	* In all other cases, returns true.
	*/
	@Override
	public boolean getShowAttrsWithNullValues() {
		if (tClassifier == null)
			return true;
		return tClassifier.getShowAttrsWithNullValues();
	}

	/**
	* A method from the DataViewRegulator interface.
	* Returns the class number for the data record with the given index
	*/
	@Override
	public int getRecordClassN(int recN) {
		if (tClassifier == null)
			return -1;
		return tClassifier.getRecordClass(recN);
	}

	/**
	* A method from the DataViewRegulator interface.
	* Returns the name of the class with the given number
	*/
	@Override
	public String getClassName(int classN) {
		if (classifier == null || classN < 0)
			return null;
		return classifier.getClassName(classN);
	}

	/**
	* A method from the DataViewRegulator interface.
	* Returns the color of the class with the given number
	*/
	@Override
	public Color getClassColor(int classN) {
		if (classifier == null || classN < 0)
			return null;
		return classifier.getClassColor(classN);
	}

	/**
	* A method from the TransformedDataPresenter interface.
	* Returns the transformed value corresponding to the given row and column of
	* the original table. If the value is not transformed, returns null.
	*/
	@Override
	public String getTransformedValue(int rowN, int colN) {
		if (classifier == null || !(classifier instanceof TransformedDataPresenter))
			return null;
		return ((TransformedDataPresenter) classifier).getTransformedValue(rowN, colN);
	}

	/**
	* Makes necessary operations for destroying, in particular, unregisters from
	* listening table change events. Must be destroyed!
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		if (classifier != null) {
			classifier.removePropertyChangeListener(this);
			classifier.destroy();
		}
		super.destroy();
	}

//ID
	@Override
	public Hashtable getVisProperties() {
		return classifier.getVisProperties();
	}

	@Override
	public void setVisProperties(Hashtable param) {
		classifier.setVisProperties(param);
		notifyVisChange();
	}

	@Override
	public ToolSpec getVisSpec() {
		ToolSpec spec = super.getVisSpec();
		if (spec == null) {
			spec = new ToolSpec();
		}
		spec.tagName = getTagName();
		spec.methodId = visId;
		spec.location = getLocation();
		spec.table = tableId;
		spec.attributes = getAttributeList();
		if (classifier != null) {
			ToolSpec tsp = classifier.getSpecification();
			if (tsp != null) {
				spec.transformSeqSpec = tsp.transformSeqSpec;
			}
		}
		Hashtable prop = getVisProperties();
		if (prop != null)
			if (spec.properties == null) {
				spec.properties = prop;
			} else {
				for (Enumeration e = prop.keys(); e.hasMoreElements();) {
					Object key = e.nextElement();
					spec.properties.put(key, prop.get(key));
				}
			}
		return spec;
	}
//~ID
}
