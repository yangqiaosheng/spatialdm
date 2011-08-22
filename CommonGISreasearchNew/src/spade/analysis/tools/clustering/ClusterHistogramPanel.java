package spade.analysis.tools.clustering;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.ScrollPane;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import spade.analysis.classification.Classifier;
import spade.lib.basicwin.CManager;
import spade.lib.basicwin.ColumnLayout;
import spade.lib.basicwin.Destroyable;
import spade.lib.basicwin.PopupManager;
import spade.lib.basicwin.Slider;
import spade.lib.basicwin.TextCanvas;
import spade.lib.color.CS;
import spade.lib.util.IntArray;
import spade.lib.util.StringUtil;
import spade.vis.action.HighlightListener;
import spade.vis.action.Highlighter;
import spade.vis.action.ObjectEvent;
import spade.vis.database.Attribute;
import spade.vis.database.DataRecord;
import spade.vis.database.DataTable;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGeoObject;

/**
 * Created by IntelliJ IDEA.
 * Creator: N.Andrienko
 * Date: 19-Apr-2007
 * Time: 17:32:04
 */
public class ClusterHistogramPanel extends Panel implements ActionListener, Destroyable, HighlightListener, MouseListener, MouseMotionListener, PropertyChangeListener {
	/**
	 * Contains results of clustering by OPTICS (in particular, ordering of the objects)
	 */
	protected Clusterer clusterer = null;
	/**
	 * The clusterer may be either LayerClusterer or TableClusterer.
	 * In the case of LayerClusterer, this is a reference to the same
	 * clusterer.
	 */
	protected LayerClusterer lClusterer = null;
	/**
	 * The table in which to store the results of clustering
	 */
	protected DataTable objectData = null;
	/**
	 * The index of the table column in which to store the results of clustering
	 */
	protected int colIdx = -1;
	/**
	 * The index of the table column containing the assignment of the objects to refined clusters
	 * (e.g. by means of KMedoids clustering algorithm)
	 */
	protected int refinedClusterColN = -1;
	/**
	 * Can refine clusters produced by OPTICS by means of further clustering (e.g. using KMedoids algorithm).
	 */
	protected ClusterRefiner clRefiner = null;

	protected ClusterHistogram clHist = null;
	protected float min = Float.NaN, max = Float.NaN;
	protected TextField thrTF = null;
	int nClusters = 0;
	/**
	 * The identifiers of the clustered objects. The order is determined by the
	 * clustering algorithm.
	 */
	protected String[] objIds = null;
	/**
	 * Contains the cluster numbers (starting from 0) corresponding to the
	 * clustered objects. A value below 0 means that the corresponding object
	 * is treated as noise.
	 */
	protected int[] objClusters = null;
	/**
	 * The colors assigned to the clusters (this does not include the
	 * color for the noise).
	 */
	protected Color[] clColors = null;
	/**
	 * The color assigned to indicate the noise.
	 */
	protected Color noiseColor = Color.gray.darker();
	/**
	 * Identifiers of selected objects
	 */
	protected IntArray selObjNs = null;

	protected Highlighter highlighter = null;
	/**
	 * Represents the objects on the map according to their classes
	 */
	protected Classifier classifier = null;

	/**
	 * Listeners of cluster changes
	 */
	protected Vector listeners = null;
	boolean destroyed = false;
	/**
	 * Used for generation of unique instance names
	 */
	protected static int instanceN = 0;

	/**
	 * @param clusterer - Contains results of clustering by OPTICS (in particular, ordering of the objects)
	 * @param objectData - The table in which to store the results of clustering
	 * @param colN - The index of the table column in which to store the results of clustering
	 * @param description - a description of how the ordering of the objects have been produced
	 */
	public ClusterHistogramPanel(Clusterer clusterer, DataTable objectData, int colN, String description) {
		if (clusterer == null || objectData == null || colN < 0)
			return;
		this.clusterer = clusterer;
		this.objectData = objectData;
		this.colIdx = colN;
		max = (float) clusterer.getDistanceThreshold();
		if (Float.isNaN(max))
			return;
		Vector<DClusterObject> objectsOrdered = clusterer.getObjectsOrdered();
		if (objectsOrdered == null)
			return;
		if (clusterer instanceof LayerClusterer) {
			lClusterer = (LayerClusterer) clusterer;
		}
		clHist = new ClusterHistogram(objectsOrdered);
		min = clHist.getMin();
		if (Float.isNaN(clHist.getMax()))
			return;
		if (clHist.getMax() < max) {
			max = clHist.getMax();
		}
		clHist.setThreshold(max);
		++instanceN;

		setLayout(new BorderLayout());
		Dimension hSize = clHist.getPreferredSize(), sSize = getToolkit().getScreenSize();
		if (hSize.width <= sSize.width - 100) {
			add(clHist, BorderLayout.CENTER);
		} else {
			ScrollPane scp = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
			scp.add(clHist);
			add(scp, BorderLayout.CENTER);
		}
		if (description != null) {
			TextCanvas tc = new TextCanvas();
			tc.setText(description);
			tc.setPreferredSize(Math.min(hSize.width, sSize.width), 10);
			add(tc, BorderLayout.NORTH);
		}
		Slider sl = new Slider(this, min, max, max);
		sl.setNAD(true);
		Panel pp = new Panel(new BorderLayout());
		pp.add(sl, BorderLayout.CENTER);
		thrTF = new TextField(StringUtil.floatToStr(max, 0, max));
		sl.setTextField(thrTF);
		pp.add(thrTF, BorderLayout.WEST);
		Panel p = new Panel(new ColumnLayout());
		p.add(pp);
		pp = new Panel(new FlowLayout(FlowLayout.CENTER, 50, 5));
		Button b = new Button("Apply");
		b.setActionCommand("apply");
		b.addActionListener(this);
		pp.add(b);
		if (lClusterer != null) {
			b = new Button("Build classifier");
			b.setActionCommand("refine");
			b.addActionListener(this);
			pp.add(b);
		}
		p.add(pp);
		add(p, BorderLayout.SOUTH);
		determineObjectClusters();
	}

	@Override
	public String getName() {
		return super.getName() + " " + instanceN;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension d = super.getPreferredSize();
		d.height += 150;
		d.width += 200;
		return d;
	}

	/**
	 * Represents the objects on the map according to their classes
	 */
	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
		classifier.addPropertyChangeListener(this);
	}

	/**
	 * Can refine clusters produced by OPTICS by means of further clustering (e.g. using KMedoids algorithm).
	 */
	public void setClusterRefiner(ClusterRefiner clRefiner) {
		this.clRefiner = clRefiner;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof Slider) {
			Slider sl = (Slider) e.getSource();
			float val = (float) sl.getValue();
			clHist.setThreshold(val);
			return;
		}
		if (e.getActionCommand().equals("apply")) {
			if (determineObjectClusters()) {
				notifyPropertyChange("clusters", null, null);
			}
			return;
		}
		if (e.getActionCommand().equals("refine")) {
			if (lClusterer != null && clRefiner != null) {
				int colN = clRefiner.refineClusters(lClusterer, objectData, colIdx, refinedClusterColN);
				if (colN >= 0) {
					refinedClusterColN = colN;
					if (e.getSource() instanceof Button) {
						((Button) e.getSource()).setLabel("Update classifier");
						CManager.validateAll((Button) e.getSource());
					}
				}
			}
			return;
		}
	}

	/**
	 * Returns an array of the identifiers of the clustered objects. The order
	 * is determined by the clustering algorithm.
	 */
	public String[] getObjectIds() {
		if (objIds != null && objIds.length > 0)
			return objIds;
		Vector<DClusterObject> objectsOrdered = clusterer.getObjectsOrdered();
		if (objectsOrdered == null)
			return null;
		int nObj = objectsOrdered.size();
		if (nObj < 1)
			return null;
		objIds = new String[nObj];
		for (int i = 0; i < nObj; i++) {
			objIds[i] = objectsOrdered.elementAt(i).id;
		}
		return objIds;
	}

	/**
	 * Returns the total number of clusters
	 */
	public int getClusterCount() {
		return nClusters;
	}

	/**
	 * Returns the cluster numbers (starting from 0) corresponding to the
	 * clustered objects. A value below 0 means that the corresponding object
	 * is treated as noise.
	 */
	public int[] getObjectClusters() {
		return objClusters;
	}

	/**
	 * Returns the colors assigned to the clusters (this does not include the
	 * color for the noise).
	 */
	public Color[] getClusterColors() {
		if (clColors != null && clColors.length == nClusters)
			return clColors;
		Color cc[] = clColors;
		clColors = new Color[nClusters];
		for (int i = 0; i < nClusters; i++) {
			clColors[i] = null;
			if (classifier != null) {
				clColors[i] = classifier.getClassColor(i);
				if (i < classifier.getNClasses() && classifier.getClassName(i).equals("noise")) {
					noiseColor = clColors[i];
					clColors[i] = null;
				}
			}
			if (clColors[i] == null)
				if (cc != null && cc.length > i) {
					clColors[i] = cc[i];
				} else if (i < CS.niceColors.length) {
					clColors[i] = CS.getNiceColor(i);
				} else if (i < CS.niceColors.length * 3) {
					clColors[i] = clColors[i - CS.niceColors.length].darker();
				} else {
					clColors[i] = Color.getHSBColor((float) Math.random(), (float) Math.max(Math.random(), 0.5), (float) Math.max(Math.random(), 0.5));
				}
		}
		return clColors;
	}

	/**
	 * Returns the color assigned to the noise
	 */
	public Color getColorForNoise() {
		return noiseColor;
	}

	/**
	 * Changes the color assigned to a single cluster. If clN is less than zero,
	 * changes the color used for the noise.
	 */
	public void setColorForCluster(int clN, Color color) {
		if (color == null)
			return;
		if (clN < 0) {
			noiseColor = color;
			clHist.setNoiseColor(noiseColor);
		} else {
			if (clColors == null || clN >= clColors.length)
				return;
			clColors[clN] = color;
			clHist.setClusterColors(clColors);
		}
		clHist.redraw();
	}

	protected void getColorsFromClassifier() {
		if (classifier == null)
			return;
		for (int i = 0; i < classifier.getNClasses(); i++)
			if (i < clColors.length)
				if (classifier.getClassName(i).equals("noise")) {
					noiseColor = classifier.getClassColor(i);
				} else {
					clColors[i] = classifier.getClassColor(i);
				}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getSource().equals(classifier))
			if (e.getPropertyName().equals("colors")) {
				getColorsFromClassifier();
				clHist.setClusterColors(clColors);
				clHist.setNoiseColor(noiseColor);
				clHist.redraw();
			} else if (e.getPropertyName().equals("destroyed")) {
				destroy();
			}
	}

	/**
	 * Determines the cluster numbers (starting from 0) corresponding to the
	 * clustered objects. A value below 0 means that the corresponding object
	 * is treated as noise.
	 * Returns true if the clusters changed.
	 */
	protected boolean determineObjectClusters() {
		int oldNClusters = nClusters;
		nClusters = 0;
		float thr = clHist.getThreshold();
		if (Float.isNaN(thr) || thr <= clHist.getMin() || thr > clHist.getMax()) {
			if (objClusters != null) {
				for (int i = 0; i < objClusters.length; i++) {
					objClusters[i] = -1;
				}
			}
			return nClusters != oldNClusters;
		}
		Vector<DClusterObject> objectsOrdered = clusterer.getObjectsOrdered();
		int nObj = objectsOrdered.size();
		int objCl[] = new int[nObj];
		boolean changed = objClusters == null;
		for (int i = 0; i < nObj; i++) {
			objCl[i] = -1; //noise
			DClusterObject clObj = objectsOrdered.elementAt(i);
			if (!Double.isNaN(clObj.reachabilityDistance))
				if (clObj.reachabilityDistance <= thr)
					if (i == 0 || (i > 0 && objCl[i - 1] < 0)) { //new cluster
						++nClusters;
						objCl[i] = nClusters - 1;
					} else {
						objCl[i] = objCl[i - 1]; //continuation of the previous cluster
					}
				else {
					;
				}
			else if (i < nObj - 1 && !Double.isNaN(clObj.coreDistance) && clObj.coreDistance < thr) {
				DClusterObject next = objectsOrdered.elementAt(i + 1);
				if (!Double.isNaN(next.reachabilityDistance) && next.reachabilityDistance < thr) {
					//this is a center of a new cluster
					++nClusters;
					objCl[i] = nClusters - 1;
				}
			}
			clObj.clusterIdx = objCl[i];
			changed = changed || (objCl[i] != objClusters[i]);
		}
		if (changed) {
			objClusters = objCl;
			//store the results in the table
			Attribute attr = objectData.getAttribute(colIdx);
			String vals[] = new String[nClusters + 1];
			Color c[] = new Color[nClusters + 1];
			Color cc[] = getClusterColors();
			for (int i = 0; i < nClusters; i++) {
				vals[i] = String.valueOf(i + 1);
				c[i] = cc[i];
			}
			vals[nClusters] = "noise";
			c[nClusters] = getColorForNoise();
			attr.setValueListAndColors(vals, c);
			for (int i = 0; i < nObj; i++) {
				String id = objectsOrdered.elementAt(i).id;
				DataRecord rec = null;
				int idx = objectData.indexOf(id);
				if (idx >= 0) {
					rec = objectData.getDataRecord(idx);
				}
				if (rec == null && lClusterer != null) {
					DGeoObject gobj = lClusterer.getDGeoObject(objectsOrdered.elementAt(i));
					rec = new DataRecord(id, gobj.getLabel());
					objectData.addDataRecord(rec);
					gobj.setThematicData(rec);
				}
				int classN = objClusters[i];
				if (classN < 0 || classN >= nClusters) {
					rec.setAttrValue(vals[nClusters], colIdx);
				} else {
					rec.setAttrValue(vals[classN], colIdx);
				}
			}
			Vector v = new Vector(1, 1);
			v.addElement(objectData.getAttributeId(colIdx));
			objectData.notifyPropertyChange("values", null, v);
			getObjectOrder();
		}
		getColorsFromClassifier();
		clHist.setObjectClusters(objClusters);
		clHist.setClusterColors(getClusterColors());
		clHist.setNoiseColor(getColorForNoise());
		clHist.redraw();
		return changed;
	}

	/**
	 * Defines the order of drawing objects in the layer so that the noise
	 * objects are drawn first.
	 */
	public int[] getObjectOrder() {
		if (lClusterer == null || lClusterer.getLayer() == null)
			return null;
		DGeoLayer layer = lClusterer.getLayer();
		int ncl = getClusterCount();
		if (ncl < 1)
			return null;
		String objIds[] = getObjectIds();
		if (objIds == null || objIds.length < 1)
			return null;
		int objCl[] = getObjectClusters();
		if (objCl == null)
			return null;
		String layerObjIds[] = new String[layer.getObjectCount()];
		for (int i = 0; i < layer.getObjectCount(); i++) {
			layerObjIds[i] = layer.getObjectId(i);
		}
		int order[] = new int[objCl.length];
		int k = 0;
		for (int cl = -1; cl < ncl && k < order.length; cl++) {
			for (int i = 0; i < objCl.length && k < order.length; i++)
				if (objCl[i] == cl) {
					int idx = -1;
					for (int j = 0; j < layerObjIds.length && idx < 0; j++)
						if (objIds[i].equalsIgnoreCase(layerObjIds[j])) {
							idx = j;
						}
					if (idx >= 0) {
						order[k++] = idx;
					}
				}
		}
		layer.setOrder(order);
		return order;
	}

	/**
	 * Registers a listener of cluster changes
	 */
	public void addClusterChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (listeners == null) {
			listeners = new Vector(5, 5);
		}
		if (!listeners.contains(l)) {
			listeners.addElement(l);
		}
	}

	/**
	 * Removes a listener of cluster changes
	 */
	public void removeClusterChangeListener(PropertyChangeListener l) {
		if (l == null || listeners == null)
			return;
		int idx = listeners.indexOf(l);
		if (idx >= 0) {
			listeners.removeElementAt(idx);
		}
	}

	public void notifyPropertyChange(String propName, Object oldValue, Object newValue) {
		if (listeners == null || listeners.size() < 1)
			return;
		PropertyChangeEvent e = new PropertyChangeEvent(this, propName, oldValue, newValue);
		for (int i = 0; i < listeners.size(); i++) {
			((PropertyChangeListener) listeners.elementAt(i)).propertyChange(e);
		}
	}

	@Override
	public void destroy() {
		if (highlighter != null) {
			highlighter.removeHighlightListener(this);
		}
		if (lClusterer != null && lClusterer.getLayer() != null) {
			lClusterer.getLayer().setOrder(null);
		}
		destroyed = true;
		notifyPropertyChange("destroy", null, null);
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	public void setHighlighter(Highlighter hl) {
		highlighter = hl;
		if (highlighter != null) {
			highlighter.addHighlightListener(this);
			clHist.addMouseListener(this);
			clHist.addMouseMotionListener(this);
			if (popM == null) {
				popM = new PopupManager(clHist, "", true);
				popM.setOnlyForActiveWindow(false);
			}
		}
	}

	/**
	* Notification about change of the set of objects to be transiently
	* highlighted. The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument highlighted is a vector of identifiers of
	* currently highlighted objects.
	*/
	@Override
	public void highlightSetChanged(Object source, String setId, Vector highlighted) {
	}

	/**
	* Notification about change of the set of objects to be selected (durably
	* highlighted). The argument "source" is usually a reference to a
	* Highlighter. The argument setId is the identifier of the set the
	* highlighted objects belong to (e.g. map layer or table).
	* The argument selected is a vector of identifiers of
	* currently selected objects.
	*/
	@Override
	public void selectSetChanged(Object source, String setId, Vector selected) {
		String oIds[] = getObjectIds();
		if (oIds == null)
			return;
		if (selObjNs != null) {
			selObjNs.removeAllElements();
		}
		if (selected != null && selected.size() > 0) {
			if (selObjNs == null) {
				selObjNs = new IntArray(50, 50);
			}
			for (int i = 0; i < oIds.length; i++)
				if (StringUtil.isStringInVectorIgnoreCase(oIds[i], selected)) {
					selObjNs.addElement(i);
				}
		}
		clHist.setSelectedObjectNs(selObjNs);
		clHist.redraw();
	}

	/**
	 * Invoked when the mouse button has been clicked (pressed
	 * and released) on a component.
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		String oIds[] = getObjectIds();
		if (oIds == null)
			return;
		if (e.getClickCount() > 1) {
			if (selObjNs != null && selObjNs.size() > 0) {
				highlighter.clearSelection(this);
			}
			return;
		}
		int n = clHist.getObjNForPos(e.getX(), e.getY());
		if (n < 0)
			return;
		ObjectEvent oe = new ObjectEvent(this, ObjectEvent.click, e, highlighter.getEntitySetIdentifier(), oIds[n]);
		highlighter.processObjectEvent(oe);
	}

	/**
	 * Invoked when a mouse button has been pressed on a component.
	 */
	@Override
	public void mousePressed(MouseEvent e) {
	}

	/**
	 * Invoked when a mouse button has been released on a component.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * Invoked when the mouse enters a component.
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
	}

	/**
	 * Invoked when the mouse exits a component.
	 */
	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
	}

	// Data structures for popups showing mouse-over details
	protected PopupManager popM = null;

	@Override
	public void mouseMoved(MouseEvent e) {
		if (popM == null)
			return;
		String oIds[] = getObjectIds();
		if (oIds == null)
			return;
		int popPointedN = clHist.getObjNForPos(e.getX(), e.getY());
		if (popPointedN >= 0) {
			String str = "column " + popPointedN + "\n" + "id=" + oIds[popPointedN];
			float rDist[] = clHist.getReachabilityDistances(), cDist[] = clHist.getCoreDistances();
			if (rDist != null && popPointedN < rDist.length) {
				str += "\n reachability distance=" + rDist[popPointedN] + "\n core disatance=" + cDist[popPointedN];
			}
			popM.setText(str);
			popM.setKeepHidden(false);
			popM.startShow(e.getX(), e.getY());
		} else {
			popM.setText("");
		}
	}

}
