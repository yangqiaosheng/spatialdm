package spade.analysis.classification;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;

import spade.lib.basicwin.Destroyable;
import spade.lib.util.IntArray;
import spade.vis.database.ObjectContainer;
import spade.vis.database.ObjectFilter;
import spade.vis.map.LegendDrawer;
import spade.vis.spec.ToolSpec;

/**
* The basic class to be subclassed by all tools for interactive classification:
* on the basis of one or two attributes, by dominance, freehand classification etc.
*/

public abstract class Classifier implements ObjectColorer, LegendDrawer, Destroyable {
	/**
	* The color for hidden classes
	*/
	public static Color hiddenClassColor = Color.gray, defaultColor = Color.lightGray;
	/**
	* Identifier of this classification method
	*/
	protected String methodId = null;
	/**
	 * The collective name of the classes (to be shown in the class manipulation field)
	 */
	protected String name = null;
	/**
	 * The container in which the objects are classified
	 */
	protected ObjectContainer oCont = null;
	/**
	 * Contains the class numbers (starting from 0) corresponding to the
	 * classified objects.
	 */
	protected int[] objClassNumbers = null;
	/**
	* The numbers of the classes currently hidden from view (shown in neutral
	* color)
	*/
	protected IntArray hiddenClasses = new IntArray(10, 10);
	/**
	 * What classes will be shown in the legend
	 */
	protected boolean showInLegend[] = null;

	protected boolean destroyed = false;

	abstract public int getNClasses();

	abstract public String getClassName(int classN);

	abstract public Color getClassColor(int classN);

	/**
	 * Returns the container in which the objects are classified
	 */
	@Override
	public ObjectContainer getObjectContainer() {
		return oCont;
	}

	/**
	 * Sets the container in which the objects are classified
	 */
	public void setObjectContainer(ObjectContainer oCont) {
		this.oCont = oCont;
	}

	/**
	* Returns the identifier of the set of objects this classifier deals with
	*/
	@Override
	public String getEntitySetIdentifier() {
		if (oCont == null)
			return null;
		return oCont.getEntitySetIdentifier();
	}

	/**
	* Returns the identifier of the container this classifier deals with
	*/
	@Override
	public String getContainerIdentifier() {
		if (oCont == null)
			return null;
		return oCont.getContainerIdentifier();
	}

	/**
	 * Returns an array of the identifiers of the classified objects.
	 */
	public String[] getObjectIds() {
		if (oCont == null)
			return null;
		int nObj = oCont.getObjectCount();
		if (nObj < 1)
			return null;
		String objIds[] = new String[nObj];
		for (int i = 0; i < nObj; i++) {
			objIds[i] = oCont.getObjectId(i);
		}
		return objIds;
	}

	/**
	 * Returns the collective name of the classes (may be shown in the class manipulation field)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Assigns the collective name of the classes (may be shown in the class manipulation field)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Informs whether colors assigned to the classes may be changed individually.
	 * By default, returns false.
	 */
	public boolean allowChangeClassColor() {
		return false;
	}

	/**
	 * Changes the color assigned to a single class. By default, does nothing.
	 */
	public void setColorForClass(int classN, Color color) {
	}

	/**
	* Through this function the component constructing the classifier can set the
	* identifier of the classification method.
	*/
	public void setMethodId(String id) {
		methodId = id;
	}

	/**
	* Returns the identifier of the visualization method implemented by this
	* class.
	*/
	public String getMethodId() {
		return methodId;
	}

//-------------- Operations to hide and expose classes -------------------
	/**
	* Some classes may be hidden from view. This function allows to check
	* if a class is hidden or not.
	*/
	public boolean isClassHidden(int classN) {
		return hiddenClasses.indexOf(classN) >= 0;
	}

	/**
	* This function allows to hide a class from view.
	*/
	public void setClassIsHidden(boolean value, int classN) {
		int idx = hiddenClasses.indexOf(classN);
		if (value)
			if (idx < 0) {
				hiddenClasses.addElement(classN);
			} else {
				; //already hidden
			}
		else if (idx >= 0) {
			hiddenClasses.removeElementAt(idx);
		} else {
			; //not hidden
		}
	}

	/**
	* Returns the number of hidden classes.
	*/
	public int getHiddenClassCount() {
		return hiddenClasses.size();
	}

	/**
	* Makes all classes visible.
	*/
	public void exposeAllClasses() {
		hiddenClasses.removeAllElements();
	}

	/**
	* Makes all classes invisible.
	*/
	public void hideAllClasses() {
		hiddenClasses.removeAllElements();
		for (int i = 0; i < getNClasses(); i++) {
			hiddenClasses.addElement(i);
		}
	}

//--------------- main functionality: classification of objects -----------
	/**
	 * Returns the number of the class for the object with the given identifier.
	 */
	abstract public int getObjectClass(String objId);

	/**
	 * Returns the number of the class for the object with the given index in the container.
	 */
	abstract public int getObjectClass(int objIdx);

	/**
	* Returns the color for the object with the given identifier, depending on
	* the current classification.
	*/
	@Override
	public Color getColorForObject(String objId) {
		int classN = getObjectClass(objId);
		if (classN < 0)
			return null;
		if (isClassHidden(classN))
			return hiddenClassColor;
		return getClassColor(classN);
	}

	/**
	* Returns the color for the object with the given index in the container, depending on
	* the current classification.
	*/
	@Override
	public Color getColorForObject(int objIdx) {
		int classN = getObjectClass(objIdx);
		if (classN < 0)
			return null;
		if (isClassHidden(classN))
			return hiddenClassColor;
		return getClassColor(classN);
	}

	/**
	 * Returns the class numbers (starting from 0) corresponding to the
	 * classified objects. A value below 0 means that the corresponding object
	 * is treated as noise.
	 */
	public int[] getObjectClassNumbers() {
		if (objClassNumbers != null && objClassNumbers.length == oCont.getObjectCount())
			return objClassNumbers;
		int oClNum[] = new int[oCont.getObjectCount()];
		for (int i = 0; i < oClNum.length; i++) {
			oClNum[i] = getObjectClass(i);
		}
		objClassNumbers = oClNum;
		return objClassNumbers;
	}

//----------- notification about changes of classes and colors ---------
	/**
	* A Classifier may have listeners of changes of classes.
	* The listeners should implement the PropertyChangeListener interface.
	* To handle the list of listeners and notify them about changes of the
	* object set or object data, the DataTable uses a PropertyChangeSupport.
	*/
	protected PropertyChangeSupport pcSupport = null;

	/**
	* Registeres a listener of changes of classification. The
	* listener must implement the PropertyChangeListener interface.
	*/
	@Override
	public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
		if (l == null)
			return;
		if (pcSupport == null) {
			pcSupport = new PropertyChangeSupport(this);
		}
		pcSupport.addPropertyChangeListener(l);
		//System.out.println("Classifier added listener "+l.toString());
	}

	/**
	* Unregisteres a listener of changes of oclassification.
	*/
	@Override
	public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
		if (l == null || pcSupport == null)
			return;
		pcSupport.removePropertyChangeListener(l);
		//System.out.println("Classifier removed listener "+l.toString());
	}

	/**
	* Notify all the listeners about changes of the classes.
	*/
	public void notifyClassesChange() {
		objClassNumbers = null;
		showInLegend = null;
		notifyChange("classes", null);
	}

	/**
	* Notify all the listeners about changes of the colors.
	*/
	public void notifyColorsChange() {
		notifyChange("colors", null);
	}

	/**
	* An internal method used to notify all the listeners about changes of
	* classification (what=="classes") or colors (what=="colors").
	* The classifier may also notify the listeners when it is destroyed.
	* In this case the property name is "destroyed".
	*/
	public void notifyChange(String what) {
		notifyChange(what, null);
	}

	protected void notifyChange(String what, Object value) {
		if (what.equals("classes")) {
			showInLegend = null;
			objClassNumbers = null;
		}
		if (pcSupport == null)
			return;
		pcSupport.firePropertyChange(what, null, value);
	}

	public void classesHaveChanged() {
		objClassNumbers = null;
		showInLegend = null;
	}

	/**
	 * What classes are shown in the legend
	 */
	public boolean[] getShowInLegend() {
		return showInLegend;
	}

	/**
	 * Sets what classes will be shown in the legend
	 */
	public void setShowInLegend(boolean[] showInLegend) {
		this.showInLegend = showInLegend;
	}

	/**
	* A LegendDrawer should be able to add its description at the end of the
	* legend formed by previous legend drawers. The argument startY specifies
	* the vertical position from which the LegendDrawer should start drawing
	* its part of the legend.The argument leftMarg specifies the left margin
	* (amount of space on the left to be kept blank). The argument prefW
	* specifies the preferrable width of the legend (to avoid horizontal
	* scrolling).
	* The method should return the rectangle occupied by the drawn part of
	* the legend.
	*/
	@Override
	public abstract Rectangle drawLegend(Component c, Graphics g, int startY, int leftmarg, int prefW);

	public abstract Rectangle drawClassStatistics(Graphics g, int startY, int leftmarg, int prefW);

	/**
	* Returns the total number of objects being classified
	*/
	public abstract int getSetSize();

	/**
	* Returns the number of objects (table records) fitting in each class
	*/
	public IntArray getClassSizes() {
		getObjectClassNumbers();
		if (objClassNumbers == null)
			return null;
		int c[] = new int[getNClasses()];
		for (int i = 0; i < getNClasses(); i++) {
			c[i] = 0;
		}
		for (int objClassNumber : objClassNumbers) {
			int classN = objClassNumber;
			if (classN >= 0) {
				++c[classN];
			}
		}
		IntArray counts = new IntArray(getNClasses(), 5);
		for (int i = 0; i < getNClasses(); i++) {
			counts.addElement(c[i]);
		}
		return counts;
	}

	/**
	 * Returns the number of active (after filtering) objects fitting in each class
	 */
	public IntArray getFilteredClassSizes() {
		ObjectFilter filter = oCont.getObjectFilter();
		if (filter == null || !filter.areObjectsFiltered())
			return null;
		getObjectClassNumbers();
		if (objClassNumbers == null)
			return null;
		int c[] = new int[getNClasses()];
		for (int i = 0; i < getNClasses(); i++) {
			c[i] = 0;
		}
		for (int i = 0; i < objClassNumbers.length; i++)
			if (filter.isActive(i)) {
				int classN = objClassNumbers[i];
				if (classN >= 0) {
					++c[classN];
				}
			}
		IntArray counts = new IntArray(getNClasses(), 5);
		for (int i = 0; i < getNClasses(); i++) {
			counts.addElement(c[i]);
		}
		return counts;
	}

	/**
	* A classifier must give the user an opportunity to change interactively
	* colors assigned to classes. This method starts the procedure of class
	* color changing.
	*/
	public abstract void startChangeColors();

	/**
	* Makes necessary operations for destroying and notifies its listeners about
	* being destroyed.
	*/
	@Override
	public void destroy() {
		if (isDestroyed())
			return;
		destroyed = true;
		notifyChange("destroyed", null);
	}

	/**
	* Replies whether is destroyed or not
	*/
	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

//ID
	public Hashtable getVisProperties() {
		return null;
	}

	public void setVisProperties(Hashtable param) {
	}

//~ID
	/**
	* Returns the specification of this classifier to be used for saving the
	* system's state.
	*/
	public ToolSpec getSpecification() {
		ToolSpec spec = new ToolSpec();
		spec.tagName = "tool";
		spec.methodId = methodId;
		spec.properties = getVisProperties();
		return spec;
	}
}
